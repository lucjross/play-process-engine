package io.wellsmith.play.engine

import io.wellsmith.play.domain.ElementVisitEntity
import io.wellsmith.play.engine.compliance.Compliant
import io.wellsmith.play.engine.compliance.Level
import io.wellsmith.play.engine.compliance.Spec
import org.omg.spec.bpmn._20100524.model.TCallActivity
import org.omg.spec.bpmn._20100524.model.TEndEvent
import org.omg.spec.bpmn._20100524.model.TFlowElement
import org.omg.spec.bpmn._20100524.model.TFlowNode
import org.omg.spec.bpmn._20100524.model.TProcess
import org.omg.spec.bpmn._20100524.model.TSequenceFlow
import org.springframework.util.ConcurrentReferenceHashMap
import java.time.Instant
import java.util.TreeSet
import java.util.UUID
import java.util.concurrent.ConcurrentMap

class ProcessInstance(internal val graph: FlowElementGraph,
                      val processId: String,
                      val bpmn20XMLEntityId: UUID,
                      val entityId: UUID,
                      val parentProcess: TProcess? = null,
                      val fromSequenceFlow: TSequenceFlow? = null,
                      val calledBy: TCallActivity? = null,
                      elementVisits: List<ElementVisitEntity>? = null) {

  private val visits: ConcurrentMap<TFlowElement, TreeSet<FlowElementVisit>> =
      newVisitsMap().apply {
        if (elementVisits != null) {
          putAll(elementVisits
              .map { FlowElementVisit(
                  graph.flowElementsByKey[it.elementKey()]!!,
                  it.time,
                  it.fromFlowElementId?.let { graph.flowElementsByKey[it] })
              }
              .groupingBy { it.flowElement }
              .aggregateTo(newVisitsMap()) { key, acc, el, first ->
                if (first) newVisitSet().apply { add(el) }
                else acc!!.apply { add(el) }
              }
          )
        }
      }

  fun addVisit(flowElementId: String, time: Instant, fromFlowElement: TFlowElement? = null) {
    addVisit(FlowElementVisit(
        graph.flowElementsByKey[flowElementId]!!, time, fromFlowElement))
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
      throw IllegalArgumentException(
          "No token currently at fromFlowNode ${flowElementVisit.fromFlowElement}")
    }
    else if (flowElementVisit.fromFlowElement == null) {
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
            """.trimIndent().replace('\n', ' '))
        }

      }
    }

    visits.computeIfAbsent(flowElementVisit.flowElement) { newVisitSet() }
        .add(flowElementVisit)
  }

  fun isTokenAt(flowElementKey: String): Boolean {
    val flowElement = graph.flowElementsByKey[flowElementKey]
        ?: throw IllegalArgumentException(
            "Given flowElementKey \"$flowElementKey\" does not refer to a Flow Element" +
                " on Process with id \"$processId\"")

    return isTokenAt(flowElement)
  }

  /**
   * Rules:
   *
   * 1. a "token" is NOT at this node if:
   *   (a) it has not been visited; OR
   *   (b) all of the nodes directly following this node (by Sequence Flow)
   *       have at least one node visit time that is later than
   *       the latest visit time of this node; OR
   *   (c) this node is an End Event, in which case the token is considered to have
   *       left this node as soon as it arrived
   *       (i.e., it has disappeared, in order to eagerly reduce the token count of the instance).
   *
   * 2. a "token" IS at this node if:
   *   (a) it is not targeted by any sequence flows AND no nodes directly following this
   *       node have been visited; OR
   *   (b) there is at least one node directly following this node that has not been visited; OR
   *   (c) this node's latest visit time is later than the latest visit time
   *       of at least one node directly following this node.
   *
   * A note on rule 2(c):
   * This implementation presupposes that,
   * when a token "splits" into multiple tokens (e.g., through a Parallel Gateway),
   * not all nodes are going to be "left" all at the same time, although
   * logically they are.  Hence, it's arbitrarily decided that, during brief periods where
   * [visits] is not entirely up-to-date with the latest node visits,
   * a token is "left behind" at the split point.
   *
   * @return  whether the given [TFlowNode] currently has a "token".
   */
  fun isTokenAt(flowElement: TFlowElement): Boolean {

    val flowElementVisits = visits[flowElement]
    if (flowElementVisits == null || flowElementVisits.size == 0)
      return false

    val followingElementsVisits = graph.nextFlowElements(flowElement)
        .map { visits[it] }

    if (flowElement is TEndEvent ||
        followingElementsVisits.all {
          it != null && flowElementVisits.last().time < it.map { it.time }.max()?: Instant.MIN
        }) {
      return false
    }

    if ((graph.sequenceFlowsByTargetRef[flowElement].orEmpty().isEmpty() &&
            followingElementsVisits.all { it.orEmpty().isEmpty() }
            ) ||
        followingElementsVisits.any {
          it == null || flowElementVisits.last().time > it.map { it.time }.max()?: Instant.MAX
        }) {
      return true
    }

    TODO("unhandled cases?")
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
}

private fun newVisitSet() =
    TreeSet<FlowElementVisit> { o1, o2 ->
      o1.time.compareTo(o2.time)
    }

private fun newVisitsMap() =
    ConcurrentReferenceHashMap<TFlowElement, TreeSet<FlowElementVisit>>()