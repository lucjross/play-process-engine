package io.wellsmith.play.engine

import io.wellsmith.play.domain.ElementVisitEntity
import io.wellsmith.play.engine.compliance.Compliant
import io.wellsmith.play.engine.compliance.Level
import io.wellsmith.play.engine.compliance.Spec
import org.omg.spec.bpmn._20100524.model.TCallActivity
import org.omg.spec.bpmn._20100524.model.TEndEvent
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

  private val visits: ConcurrentMap<TFlowNode, TreeSet<FlowNodeVisit>> =
      newVisitsMap().apply {
        if (elementVisits != null) {
          putAll(elementVisits
              .filter { it.baseElementId != null }
              .filter { graph.flowNodesById[it.baseElementId] is TFlowNode }
              .map { FlowNodeVisit(
                  graph.flowNodesById[it.baseElementId]!!,
                  it.time,
                  if (it.fromFlowNodeId != null) graph.flowNodesById[it.fromFlowNodeId!!] else null)
              }
              .groupingBy { it.flowNode }
              .aggregateTo(newVisitsMap()) { key, acc, el, first ->
                if (first) newVisitSet().apply { add(el) }
                else acc!!.apply { add(el) }
              })
        }
      }

  fun addVisit(flowNodeId: String, time: Instant, fromFlowNode: TFlowNode? = null) {
    addVisit(FlowNodeVisit(
        graph.flowNodesById[flowNodeId]!!, time, fromFlowNode))
  }

  fun addVisit(flowNodeVisit: FlowNodeVisit) {

    // check that the visit is currently possible
    // (does not check Sequence Flow conditions)
    val visitedUponInstantiation = graph.allIdsOfFlowNodesToVisitUponProcessInstantiation()
        .contains(flowNodeVisit.flowNode.id)
    if (visitedUponInstantiation && isTokenAt(flowNodeVisit.flowNode)) {
      throw IllegalArgumentException(
          "Flow Object ${flowNodeVisit.flowNode.id} already visited upon Process instantiation")
    }
    else if (flowNodeVisit.fromFlowNode is TFlowNode &&
        !isTokenAt(flowNodeVisit.fromFlowNode.id)) {
      throw IllegalArgumentException(
          "No token currently at fromFlowNode ${flowNodeVisit.fromFlowNode}")
    }
    else if (flowNodeVisit.fromFlowNode == null) {
      if (!visitedUponInstantiation) {
        if (graph.sequenceFlowsByTargetRef[flowNodeVisit.flowNode]
                ?.any { isTokenAt(it.sourceRef as TFlowNode) } == false) {
          throw IllegalArgumentException("""
            Flow Object ${flowNodeVisit.flowNode.id}
            does not follow any Flow Object currently holding a token,
            and has incoming sequence flows
            (is not eligible for activation upon process instantiation).
            """.trimIndent())
        }

      }
    }

    visits.computeIfAbsent(flowNodeVisit.flowNode) { newVisitSet() }
        .add(flowNodeVisit)
  }

  fun isTokenAt(flowNodeId: String): Boolean {
    val flowNode = graph.flowNodesById[flowNodeId]
        ?: throw IllegalArgumentException(
            "Given flowNodeId \"$flowNodeId\" does not refer to a Flow Object" +
                " on Process with id \"$processId\"")

    return isTokenAt(flowNode)
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
  fun isTokenAt(flowNode: TFlowNode): Boolean {

    val flowNodeVisits = visits[flowNode]
    if (flowNodeVisits == null || flowNodeVisits.size == 0)
      return false

    val followingNodesVisits = graph.nextFlowNodes(flowNode)
        .map { visits[it] }

    if (flowNode is TEndEvent ||
        followingNodesVisits.all {
          it != null && flowNodeVisits.last().time < it.map { it.time }.max()?: Instant.MIN
        }) {
      return false
    }

    if ((graph.sequenceFlowsByTargetRef[flowNode].orEmpty().isEmpty() &&
            followingNodesVisits.all { it.orEmpty().isEmpty() }
            ) ||
        followingNodesVisits.any {
          it == null || flowNodeVisits.last().time > it.map { it.time }.max()?: Instant.MAX
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
        graph.flowNodesById.values.isEmpty() -> true
        visits.values.all { it == null || it.isEmpty() } -> false
        graph.flowNodesById.values.none { isTokenAt(it) } -> true

        // todo -
        // "No Activity of the Process is still active."

        else -> false
    }

  internal fun inferPrecedingNodeVisit(flowNode: TFlowNode, time: Instant): FlowNodeVisit? {
    val preceding = graph.sequenceFlowsByTargetRef[flowNode]
        ?.map { it.sourceRef as TFlowNode }.orEmpty()
    if (preceding.isEmpty()) return null

    // in chronological order
    val visitsToPrecedingNodes = preceding.flatMap { visits[it].orEmpty() }
        .sortedBy { it.time }
    for (i in 0 until visitsToPrecedingNodes.size) {
      val visitToPrecedingNode = visitsToPrecedingNodes[i]
      if (time > visitToPrecedingNode.time) {
        return FlowNodeVisit(visitToPrecedingNode.flowNode, visitToPrecedingNode.time)
      }
    }

    // if there are preceding nodes then there should have been a preceding node visit
    // for the given flowNode and time
    throw IllegalStateException()
  }
}

private fun newVisitSet() =
    TreeSet<FlowNodeVisit> { o1, o2 ->
      o1.time.compareTo(o2.time)
    }

private fun newVisitsMap() =
    ConcurrentReferenceHashMap<TFlowNode, TreeSet<FlowNodeVisit>>()