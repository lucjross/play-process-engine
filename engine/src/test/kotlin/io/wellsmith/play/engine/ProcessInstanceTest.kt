package io.wellsmith.play.engine

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.omg.spec.bpmn._20100524.model.TProcess
import java.util.UUID

class ProcessInstanceTest {

  @Test
  fun `test isTokenAt with parallel-activities`() {

    val definitions = definitionsFromResource("parallel-activities.bpmn20.xml")
    val process = definitions.rootElement
        .find { it.value is TProcess }!!.value as TProcess
    val graph = FlowElementGraph(process)
    val processInstance = ProcessInstance(graph, process.id, UUID.randomUUID())

    // cannot visit a node ahead of nodes with no tokens
    Assertions.assertThrows(IllegalArgumentException::class.java) {
      processInstance.addVisit(FlowNodeVisit(
          graph.flowNodesById["gateway-1"]!!, now(), graph.flowNodesById["hi"]!!))
    }
    Assertions.assertThrows(IllegalArgumentException::class.java) {
      processInstance.addVisit(FlowNodeVisit(
          graph.flowNodesById["gateway-1"]!!, now()))
    }

    graph.allIdsOfFlowNodesToVisitUponProcessInstantiation().forEach {
      processInstance.addVisit(FlowNodeVisit(
          graph.flowNodesById[it]!!, now()))
    }

    Assertions.assertTrue(processInstance.isTokenAt("hi"))
    Assertions.assertFalse(processInstance.isTokenAt("gateway-1"))

    // token already at "hi", so it should not be visitable
    Assertions.assertThrows(IllegalArgumentException::class.java) {
      processInstance.addVisit(FlowNodeVisit(
          graph.flowNodesById["hi"]!!, now()))
    }

    processInstance.addVisit(FlowNodeVisit(
        graph.flowNodesById["gateway-1"]!!, now(), graph.flowNodesById["hi"]))

    Assertions.assertFalse(processInstance.isTokenAt("hi"))
    Assertions.assertTrue(processInstance.isTokenAt("gateway-1"))

    processInstance.addVisit(FlowNodeVisit(
        graph.flowNodesById["task-1"]!!, now()))

    // edge-case behavior per rule 2(c)
    Assertions.assertTrue(processInstance.isTokenAt("gateway-1"))

    processInstance.addVisit(FlowNodeVisit(
        graph.flowNodesById["task-2"]!!, now()))

    Assertions.assertTrue(processInstance.isTokenAt("task-1"))
    Assertions.assertTrue(processInstance.isTokenAt("task-2"))
    Assertions.assertFalse(processInstance.isTokenAt("gateway-1"))

    processInstance.addVisit(FlowNodeVisit(
        graph.flowNodesById["bye"]!!, now()))

    Assertions.assertFalse(processInstance.isTokenAt("task-1"))
    Assertions.assertFalse(processInstance.isTokenAt("task-2"))
    // end event does not hold a token
    Assertions.assertFalse(processInstance.isTokenAt("bye"))
  }

  @Test
  fun `test isCompleted with start-to-end`() {

    val definitions = definitionsFromResource("start-to-end.bpmn20.xml")
    val process = definitions.rootElement
        .find { it.value is TProcess }!!.value as TProcess
    val graph = FlowElementGraph(process)
    val processInstance = ProcessInstance(graph, process.id, UUID.randomUUID())
    Assertions.assertFalse(processInstance.isCompleted())

    processInstance.addVisit("hi", now())
    Assertions.assertFalse(processInstance.isCompleted())

    processInstance.addVisit("bye", now())
    Assertions.assertTrue(processInstance.isCompleted())
  }
}