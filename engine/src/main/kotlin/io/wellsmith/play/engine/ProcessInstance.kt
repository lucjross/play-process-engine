package io.wellsmith.play.engine

import io.wellsmith.play.domain.ActivityStateChangeEntity
import io.wellsmith.play.domain.ElementVisitEntity
import io.wellsmith.play.engine.activity.ActivityLifecycle
import io.wellsmith.play.engine.compliance.Compliant
import io.wellsmith.play.engine.compliance.Level
import io.wellsmith.play.engine.compliance.Spec
import net.sf.cglib.proxy.Enhancer
import org.omg.spec.bpmn._20100524.model.TActivity
import org.omg.spec.bpmn._20100524.model.TCallActivity
import org.omg.spec.bpmn._20100524.model.TEndEvent
import org.omg.spec.bpmn._20100524.model.TFlowElement
import org.omg.spec.bpmn._20100524.model.TFlowNode
import org.omg.spec.bpmn._20100524.model.TProcess
import org.omg.spec.bpmn._20100524.model.TSequenceFlow
import org.springframework.util.ConcurrentReferenceHashMap
import java.time.Instant
import java.util.LinkedList
import java.util.TreeSet
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

class ProcessInstance(internal val graph: FlowElementGraph,
                      val processId: String,
                      val bpmn20XMLEntityId: UUID,
                      val entityId: UUID,
                      val stateChangeInterceptor: ActivityLifecycle.StateChangeInterceptor,
                      val parentProcess: TProcess? = null,
                      val fromSequenceFlow: TSequenceFlow? = null,
                      val calledBy: TCallActivity? = null,
                      elementVisits: List<ElementVisitEntity>? = null,
                      activityStateChanges: List<ActivityStateChangeEntity>? = null) {

  private val visits: ConcurrentMap<TFlowElement, TreeSet<FlowElementVisit>> =
      elementVisits.orEmpty()
          .map {
            FlowElementVisit(
                graph.flowElementsByKey[it.elementKey()]!!,
                it.time,
                it.fromFlowElementId?.let { graph.flowElementsByKey[it] })
          }
          .groupingBy { it.flowElement }
          .aggregateTo(newVisitsMap()) { key, acc, el, first ->
            if (first)
              newVisitSet().apply { add(el) }
            else
              acc!!.apply { add(el) }
          }

  private val visitsBySplitCorrelation: ConcurrentMap<UUID, LinkedList<FlowElementVisit>> =
      visits.values
          .flatten()
          .filter { it.splitCorrelationId != null }
          .groupingBy { it.splitCorrelationId!! }
          .aggregateTo(ConcurrentHashMap<UUID, LinkedList<FlowElementVisit>>()) { key, acc, el, first ->
            if (first)
              LinkedList<FlowElementVisit>().apply { add(el) }
            else
              acc!!.apply { add(el) }
          }

  private val activityLifecyclesByActivity: ConcurrentMap<TActivity, LinkedList<ActivityLifecycle>> =
      activityStateChanges.orEmpty()
          .groupBy { it.lifecycleId }
          .map {
            it.value.maxWith(Comparator { o1, o2 ->
              // the latest state change recorded for a lifecycle instance
              // is used to instantiate the lifecycle
              when {
                o1.time < o2.time -> -1
                o1.time > o2.time -> 1
                o1.time <= o2.time && o2.state.follows(o1.state) -> -1
                o1.time >= o2.time && o1.state.follows(o2.state) -> 1
                else -> throw IllegalStateException(
                    "Redundant state changes: $o1, $o2")
              }
            })!!
          }
          .map { recreateActivityLifecycle(it) }
          .groupingBy { it.activity }
          .aggregateTo(ConcurrentHashMap<TActivity, LinkedList<ActivityLifecycle>>()) {
            key, acc, el, first ->
            if (first)
              LinkedList<ActivityLifecycle>().apply { add(el) }
            else
              acc!!.apply { add(el) }
          }

  /**
   * Invokes [ActivityLifecycle] secondary constructor
   */
  internal fun recreateActivityLifecycle(stateChangeEntity: ActivityStateChangeEntity):
      ActivityLifecycle {

    val enhancer = Enhancer()
    enhancer.setSuperclass(ActivityLifecycle::class.java)
    enhancer.setCallback(stateChangeInterceptor)
    return enhancer.create(
        arrayOf(TActivity::class.java, ActivityStateChangeEntity::class.java),
        arrayOf(graph.flowElementsByKey[stateChangeEntity.activityId] as TActivity,
            stateChangeEntity)
    ) as ActivityLifecycle
  }

  private val activityLifecyclesById: ConcurrentMap<UUID, ActivityLifecycle> =
      activityLifecyclesByActivity
          .flatMap { it.value }
          .associateByTo(ConcurrentHashMap()) { it.lifecycleId }

  fun addVisit(flowElementId: String, time: Instant,
               fromFlowElementId: String? = null, splitCorrelationId: UUID? = null) {
    addVisit(FlowElementVisit(
        graph.flowElementsByKey[flowElementId]!!, time,
        fromFlowElementId?.let { graph.flowElementsByKey[it]!! },
        splitCorrelationId))
  }

  fun addVisit(flowElementVisit: FlowElementVisit) {

    // check that the visit is currently possible
    // (does not check Sequence Flow conditions)
    if (flowElementVisit.fromFlowElement?.let {
          graph.nextFlowElements(it).contains(flowElementVisit.flowElement) } == false) {
      throw IllegalArgumentException(
          "flowElement with key ${flowElementVisit.flowElement.elementKey()} does not follow" +
              " flowElement with key ${flowElementVisit.fromFlowElement.elementKey()}")
    }

    val visitedUponInstantiation = graph.allIdsOfFlowNodesToVisitUponProcessInstantiation()
        .contains(flowElementVisit.flowElement.id)
    if (visitedUponInstantiation && isTokenAt(flowElementVisit.flowElement)) {
      throw IllegalArgumentException(
          "Flow Object ${flowElementVisit.flowElement.id} already visited upon Process instantiation")
    }
    else if (flowElementVisit.fromFlowElement is TFlowElement &&
        !isTokenAt(flowElementVisit.fromFlowElement.elementKey())) {

      // if there is no token at the preceding element,
      // then this has to be a SequenceFlow correlated to another SequenceFlow,
      // which would entail that the other one has already been traversed as a result
      // of a token split, and this must not have yet received one of the split tokens.
      if (flowElementVisit.splitCorrelationId == null ||
          visitsBySplitCorrelation[flowElementVisit.splitCorrelationId]?.any {
            it.flowElement.elementKey() == flowElementVisit.flowElement.elementKey()
          } == true) {
        throw IllegalArgumentException("""
          No token currently at fromFlowElement ${flowElementVisit.fromFlowElement.elementKey()},
          and flowElement ${flowElementVisit.flowElement.elementKey()}
          is not a Sequence Flow with a split token correlation
          (splitCorrelationId=${flowElementVisit.splitCorrelationId})
          """.cleanMultiline())
      }
    }
    else if (flowElementVisit.fromFlowElement == null && flowElementVisit.flowElement.let {
          it is TSequenceFlow || graph.sequenceFlowsByTargetRef[it] != null }) {
      throw IllegalArgumentException("""
        Nodes precede flowElement ${flowElementVisit.flowElement.elementKey()},
        so fromFlowElement must be provided
        """.cleanMultiline())
    }
    else {
      if (!visitedUponInstantiation) {
        val followsElementWithToken = when (flowElementVisit.flowElement) {
          is TSequenceFlow ->
            graph.flowElementsByKey[
                (flowElementVisit.flowElement.sourceRef as TFlowNode).elementKey()
            ]!!.let { isTokenAt(it) }
          is TFlowNode ->
            graph.sequenceFlowsByTargetRef[flowElementVisit.flowElement]!!
                .any { isTokenAt(it) }
          else -> TODO()
        }
        if (!followsElementWithToken) {
          throw IllegalArgumentException("""
            Flow Element ${flowElementVisit.flowElement.id}
            does not follow any Flow Element currently holding a token
            """.cleanMultiline())
        }

      }
    }

    synchronized(visits) {
      flowElementVisit.splitCorrelationId?.let {
        visitsBySplitCorrelation.getOrPut(it) { LinkedList() }
            .add(flowElementVisit)
      }
      visits.computeIfAbsent(flowElementVisit.flowElement) { newVisitSet() }
          .add(flowElementVisit)
    }

  }

  fun tokenCountAt(flowElementKey: String): Int {
    return tokenCountAt(flowElementOf(flowElementKey))
  }

  fun tokenCountAt(flowElement: TFlowElement): Int {

    synchronized(visits) {

      val elementVisits = visits[flowElement]
          ?: return 0
      if (elementVisits.isEmpty()) {
        return 0
      }

      val followingElementsVisits = graph.nextFlowElements(flowElement)
          .map { visits[it]
              ?.filter { it.fromFlowElement == flowElement }
              ?.toCollection(newVisitSet())
          }
      if (followingElementsVisits.isEmpty()) {
        if (flowElement is TEndEvent)
          return 0
        else
        //todo - if this is an Activity then its lifecycle state will have to be considered
        //if activity is closed then tokens are disappeared
          return elementVisits.count()
      }
      else {
        /*
         * Initially the set of elements following this element is transformed into a TreeSet
         * with a Comparator that returns 0 for visits with equal splitCorrelationIds.
         * This allows multiple tokens arising from a "split" of a single token
         * to be considered as only a single token. This is necessary here because the elements
         * visited with the split tokens are not necessarily visited at exactly
         * the same time.
         *
         * Then, a loop traverses both the element's visits (set VE)
         * and the following elements' visits (set VF),
         * both in reverse,
         * and for each timestamp encountered in VE that is
         * later than the timestamp of the last-encountered element from VF,
         * the count is incremented.
         * For the converse, the count is decremented.
         * At the end, the count will be either incremented with the number of untraversed elements from VE,
         * or decremented with the number of untraversed elements from VF.
         */
        var count = 0
        val allFollowingVisits = followingElementsVisits.filterNotNull()
            .flatMapTo(newVisitSetMatchingSplits()) { it }
        val visitsRevIter = elementVisits.descendingIterator()
        val followingVisitsRevIter = allFollowingVisits.descendingIterator()
        var visit: FlowElementVisit? = null
        var followingVisit: FlowElementVisit? = null
        while (true) {
          if (!visitsRevIter.hasNext() && visit == null) {
            if (count > 0) {
              if (followingVisit != null) count--
              followingVisitsRevIter.forEachRemaining { count-- }
            }

            break
          }
          else if (visit == null) {
            visit = visitsRevIter.next()
          }

          if (!followingVisitsRevIter.hasNext() && followingVisit == null) {
            if (visit != null) count++
            visitsRevIter.forEachRemaining { count++ }
            break
          }
          else if (followingVisit == null) {
            followingVisit = followingVisitsRevIter.next()
          }

          visitLoop@
          while (visit!!.time > followingVisit!!.time) {
            count++
            if (!visitsRevIter.hasNext()) {
              visit = null
              break@visitLoop
            }
            visit = visitsRevIter.next()
          }

          followingVisitLoop@
          while (followingVisit!!.time >= visit.time) {
            count--
            if (!followingVisitsRevIter.hasNext()) {
              followingVisit = null
              break@followingVisitLoop
            }
            followingVisit = followingVisitsRevIter.next()
          }
        }

        return count
      }
    }

  }

  fun isTokenAt(flowElementKey: String): Boolean {
    return isTokenAt(flowElementOf(flowElementKey))
  }

  private fun flowElementOf(flowElementKey: String) =
      graph.flowElementsByKey[flowElementKey]
          ?: throw IllegalArgumentException(
              "Given flowElementKey \"$flowElementKey\" does not refer to a Flow Element" +
                  " on Process with id \"$processId\"")

  fun isTokenAt(flowElement: TFlowElement): Boolean {

    return tokenCountAt(flowElement) > 0
  }

  @Compliant(toSpec = Spec.BPMN_2_0, section = "13.1", level = Level.INCOMPLETE)
  fun isCompleted(): Boolean =
      when {
        // todo -
        // "If the instance was created through an instantiating Parallel Gateway, then all subsequent Events (of that
        // Gateway) MUST have occurred."

        // "There is no token remaining within the Process instance."
        // due to the implementation strategy there is an exception to this, which is the case
        // where there are nodes but none have been visited yet.
        graph.flowElementsByKey.values.isEmpty() -> true
        visits.values.all { it == null || it.isEmpty() } -> false
        graph.flowElementsByKey.values.none { isTokenAt(it) } -> true

        // todo -
        // "No Activity of the Process is still active."

        else -> false
    }

  internal fun inferPrecedingNodeVisit(flowNode: TFlowNode, time: Instant): FlowElementVisit? {
    val preceding = graph.sequenceFlowsByTargetRef[flowNode]
        ?.map { it.sourceRef as TFlowNode }.orEmpty()
    if (preceding.isEmpty()) return null

    // in chronological order
    val visitsToPrecedingNodes = preceding.flatMap { visits[it].orEmpty() }
        .sortedBy { it.time }
    for (i in 0 until visitsToPrecedingNodes.size) {
      val visitToPrecedingNode = visitsToPrecedingNodes[i]
      if (time > visitToPrecedingNode.time) {
        return FlowElementVisit(visitToPrecedingNode.flowElement, visitToPrecedingNode.time)
      }
    }

    // if there are preceding nodes then there should have been a preceding node visit
    // for the given flowElement and time
    throw IllegalStateException()
  }

  internal fun getActivityLifecycles(activity: TActivity): List<ActivityLifecycle> {
    val activityLifecycles =
        activityLifecyclesByActivity.getOrPut(activity) { LinkedList() }
    return activityLifecycles
  }

  internal fun addActivityLifecycle(activity: TActivity, activityLifecycle: ActivityLifecycle) {
    activityLifecyclesByActivity[activity]!!.add(activityLifecycle)
    if (activityLifecyclesById.put(activityLifecycle.lifecycleId, activityLifecycle) != null) {
      throw IllegalStateException("Duplicate ActivityLifecycle")
    }
  }

  internal fun getActivityLifecycle(lifecycleId: UUID) =
      activityLifecyclesById[lifecycleId]!!
}

private fun newVisitSet() =
    TreeSet<FlowElementVisit> { o1, o2 ->
      val c = o1.time.compareTo(o2.time)
      if (c != 0) c else o1.flowElement.elementKey().compareTo(o2.flowElement.elementKey())
    }

private fun newVisitSetMatchingSplits() =
    TreeSet<FlowElementVisit> { o1, o2 ->
        if (o1.splitCorrelationId != null && o1.splitCorrelationId == o2.splitCorrelationId)
          0
        else {
          val c = o1.time.compareTo(o2.time)
          if (c != 0) c else o1.flowElement.elementKey().compareTo(o2.flowElement.elementKey())
        }
    }

private fun newVisitsMap() =
    ConcurrentReferenceHashMap<TFlowElement, TreeSet<FlowElementVisit>>()