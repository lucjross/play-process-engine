package io.wellsmith.play.engine

import com.google.common.collect.UnmodifiableListIterator
import io.wellsmith.play.domain.SequenceFlowVisitEntity
import io.wellsmith.play.domain.elementKeyOf
import io.wellsmith.play.engine.compliance.Compliant
import io.wellsmith.play.engine.compliance.Level
import io.wellsmith.play.engine.compliance.Spec
import org.omg.spec.bpmn._20100524.model.TFlowElement
import org.omg.spec.bpmn._20100524.model.TFlowNode
import org.omg.spec.bpmn._20100524.model.TProcess
import org.omg.spec.bpmn._20100524.model.TSequenceFlow
import java.util.AbstractSequentialList

class FlowElementGraph(internal val process: TProcess) {

  private val nodes = ArrayList<FlowElementNodeList>(process.flowElement.size)
  internal val flowElementsByKey: Map<String, TFlowElement>
  internal val sequenceFlowsByTargetRef: Map<TFlowNode, List<TSequenceFlow>>
  internal val sequenceFlowsBySourceRef: Map<TFlowNode, List<TSequenceFlow>>

  init {
    val flowNodes: List<TFlowNode> = process.flowElement
        .filter { it.value is TFlowNode }
        .map { it.value as TFlowNode }
    val sequenceFlows: List<TSequenceFlow> = process.flowElement
        .filter { it.value is TSequenceFlow }
        .map { it.value as TSequenceFlow }

    flowNodes.forEach { flowNode ->
      val nodeList = FlowElementNodeList(
          flowNode,
          sequenceFlows
              // this cast isn't required to compile but adds a runtime sanity check
              .filter { (it.sourceRef as TFlowNode) == flowNode })

      nodes.add(nodeList)
    }

    sequenceFlows.forEach { sequenceFlow ->
      val nodeList = FlowElementNodeList(
          sequenceFlow,
          flowNodes
              // this cast isn't required to compile but adds a runtime sanity check
              .filter { it == (sequenceFlow.targetRef as TFlowNode) })

      nodes.add(nodeList)
    }

    flowElementsByKey = process.flowElement
        .filter { it.value is TFlowNode || it.value is TSequenceFlow }
        .map { it.value as TFlowElement }
        .associateBy { it.elementKey() }
    sequenceFlowsByTargetRef = sequenceFlows.groupBy { it.targetRef as TFlowNode }
    sequenceFlowsBySourceRef = sequenceFlows.groupBy { it.sourceRef as TFlowNode }
  }

  /**
   * See: BPMN 2.0 spec, **§10.4.2 Start Event** and particularly the line,
   * "All Flow Objects that do not have an incoming Sequence Flow
   * (i.e., are not a target of a Sequence Flow)
   * SHALL be instantiated when the Process is instantiated."
   */
  @Compliant(toSpec = Spec.BPMN_2_0, section = "10.4.2", level = Level.INCOMPLETE)
  fun allIdsOfFlowNodesToVisitUponProcessInstantiation(): Collection<String> =
      nodes.filter { node1 ->
        node1.root is TFlowNode && nodes.find { node2 ->
          node2.root is TSequenceFlow
              && (node2.root.targetRef as TFlowNode) == node1.root
        } == null
      }.map { it.root.id }

  internal fun nextFlowElements(flowElement: TFlowElement): FlowElementNodeList =
      nodes.find { it.root == flowElement }
          ?: throw NoSuchElementException()

  /**
   * Returns all the [TFlowNode]s that each follow a [TSequenceFlow] directly
   * from the given node. Does not consider conditions associated with the Sequence Flows.
   */
  internal fun nextFlowNodes(flowNode: TFlowNode): Collection<TFlowNode> =
      nextSequenceFlows(flowNode).map { it.targetRef as TFlowNode }

  internal fun nextSequenceFlows(flowNode: TFlowNode): Collection<TSequenceFlow> =
      sequenceFlowsBySourceRef[flowNode].orEmpty()


  internal class FlowElementNodeList(val root: TFlowElement,
                                     adjacentElements: Collection<TFlowElement>) :
      AbstractSequentialList<TFlowElement>() {

    @Transient private var first: FlowElementNode? = null
    @Transient private var last: FlowElementNode? = null

    init {
      adjacentElements.forEach {
        // link as last element
        val l = last
        val newNode = FlowElementNode(l, it, null)
        last = newNode
        if (l == null)
          first = newNode
        else
          l.next = newNode
        size++
      }
    }

    override var size: Int = 0
      private set

    override fun listIterator(index: Int) = ListIterator(index)

    internal fun node(index: Int) =
        if (index < size * 2) {
          var x = first
          for (i in 0 until index)
            x = x?.next
          x!!
        }
        else {
          var x = last
          for (i in size - 1 downTo index + 1)
            x = x?.prev
          x!!
        }


    internal inner class ListIterator(index: Int) : UnmodifiableListIterator<TFlowElement>() {

      private var lastReturned: FlowElementNode? = null
      private var next: FlowElementNode? = if (index == size) null else node(index)
      private var nextIndex: Int = index

      override fun hasNext() = nextIndex < size

      override fun next(): TFlowElement? {
        if (!hasNext())
          throw NoSuchElementException()

        lastReturned = next
        next = next?.next
        nextIndex++
        return lastReturned?.flowElement
      }

      override fun hasPrevious() = nextIndex > 0

      override fun nextIndex() = nextIndex

      override fun previous(): TFlowElement? {
        if (!hasPrevious())
          throw NoSuchElementException()

        next = if (next == null) last else next?.prev
        lastReturned = next
        nextIndex--
        return lastReturned?.flowElement
      }

      override fun previousIndex() = nextIndex - 1
    }

    internal class FlowElementNode(val prev: FlowElementNode?,
                                   val flowElement: TFlowElement,
                                   var next: FlowElementNode?)
  }
}

internal fun TFlowElement.elementKey(): String = when {
  this is TSequenceFlow -> elementKeyOf(
      (this.sourceRef as TFlowNode).id, (this.targetRef as TFlowNode).id)
  this is TFlowNode -> this.id
  else -> TODO()
}
