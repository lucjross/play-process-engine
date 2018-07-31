package io.wellsmith.play.engine

import io.wellsmith.play.domain.SequenceFlowVisitEntity
import io.wellsmith.play.domain.elementKeyOf
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
    val processInstance = ProcessInstance(graph, process.id, UUID.randomUUID(), UUID.randomUUID())

    // cannot visit a node ahead of nodes with no tokens
    Assertions.assertThrows(IllegalArgumentException::class.java) {
      processInstance.addVisit(FlowElementVisit(
          graph.flowElementsByKey["gateway-1"]!!, now(), graph.flowElementsByKey["hi"]!!))
    }
    Assertions.assertThrows(IllegalArgumentException::class.java) {
      processInstance.addVisit(FlowElementVisit(
          graph.flowElementsByKey["gateway-1"]!!, now()))
    }

    graph.allIdsOfFlowNodesToVisitUponProcessInstantiation().forEach {
      processInstance.addVisit(FlowElementVisit(
          graph.flowElementsByKey[it]!!, now()))
    }

    Assertions.assertTrue(processInstance.isTokenAt("hi"))
    Assertions.assertFalse(processInstance.isTokenAt("gateway-1"))

    // token already at "hi", so it should not be visitable
    Assertions.assertThrows(IllegalArgumentException::class.java) {
      processInstance.addVisit(FlowElementVisit(
          graph.flowElementsByKey["hi"]!!, now()))
    }

    // sequence flows must be visited as well
    Assertions.assertThrows(IllegalArgumentException::class.java) {
      processInstance.addVisit(FlowElementVisit(
          graph.flowElementsByKey["gateway-1"]!!, now(), graph.flowElementsByKey["hi"]))
    }

    processInstance.addVisit(FlowElementVisit(
        graph.flowElementsByKey[elementKeyOf("hi", "gateway-1")]!!, now()))
    Assertions.assertTrue(processInstance.isTokenAt(
        elementKeyOf("hi", "gateway-1")))

    processInstance.addVisit(FlowElementVisit(
        graph.flowElementsByKey["gateway-1"]!!, now(),
        graph.flowElementsByKey[elementKeyOf("hi", "gateway-1")]))
    Assertions.assertFalse(processInstance.isTokenAt(
        elementKeyOf("hi", "gateway-1")))
    Assertions.assertTrue(processInstance.isTokenAt("gateway-1"))

    processInstance.addVisit(FlowElementVisit(
        graph.flowElementsByKey[elementKeyOf("gateway-1", "task-1")]!!, now()))
    processInstance.addVisit(FlowElementVisit(
        graph.flowElementsByKey["task-1"]!!, now()))

    // edge-case behavior per rule 2(c)
    Assertions.assertTrue(processInstance.isTokenAt("gateway-1"))

    processInstance.addVisit(FlowElementVisit(
        graph.flowElementsByKey[elementKeyOf("gateway-1", "task-2")]!!, now()))
    processInstance.addVisit(FlowElementVisit(
        graph.flowElementsByKey["task-2"]!!, now()))

    Assertions.assertTrue(processInstance.isTokenAt("task-1"))
    Assertions.assertTrue(processInstance.isTokenAt("task-2"))
    Assertions.assertFalse(processInstance.isTokenAt("gateway-1"))

    processInstance.addVisit(FlowElementVisit(
        graph.flowElementsByKey[elementKeyOf("task-1", "bye")]!!, now()))
    processInstance.addVisit(FlowElementVisit(
        graph.flowElementsByKey["bye"]!!, now()))

    Assertions.assertFalse(processInstance.isTokenAt("task-1"))
    Assertions.assertTrue(processInstance.isTokenAt("task-2"))
    // end event does not hold a token
    Assertions.assertFalse(processInstance.isTokenAt("bye"))

    processInstance.addVisit(FlowElementVisit(
        graph.flowElementsByKey[elementKeyOf("task-2", "bye")]!!, now()))
    processInstance.addVisit(FlowElementVisit(
        graph.flowElementsByKey["bye"]!!, now()))

    Assertions.assertFalse(processInstance.isTokenAt("task-2"))
  }

  @Test
  fun `isCompleted should work with start-to-end`() {

    val definitions = definitionsFromResource("start-to-end.bpmn20.xml")
    val process = definitions.rootElement
        .find { it.value is TProcess }!!.value as TProcess
    val graph = FlowElementGraph(process)
    val processInstance = ProcessInstance(graph, process.id, UUID.randomUUID(), UUID.randomUUID())
    Assertions.assertFalse(processInstance.isCompleted())

    processInstance.addVisit("hi", now())
    Assertions.assertFalse(processInstance.isCompleted())

    processInstance.addVisit(elementKeyOf("hi", "bye"), now())
    Assertions.assertFalse(processInstance.isCompleted())

    processInstance.addVisit("bye", now())
    Assertions.assertTrue(processInstance.isCompleted())
  }

  @Test
  fun `isCompleted should work with single-task`() {

    val definitions = definitionsFromResource("single-task.bpmn20.xml")
    val process = definitions.rootElement
        .find { it.value is TProcess }!!.value as TProcess
    val graph = FlowElementGraph(process)
    val processInstance = ProcessInstance(graph, process.id, UUID.randomUUID(), UUID.randomUUID())
    Assertions.assertFalse(processInstance.isCompleted())

    processInstance.addVisit("hi", now())
    Assertions.assertFalse(processInstance.isCompleted())

    processInstance.addVisit(elementKeyOf("hi", "manual-task-1"), now())
    Assertions.assertFalse(processInstance.isCompleted())

    processInstance.addVisit("manual-task-1", now())
    Assertions.assertFalse(processInstance.isCompleted())

    processInstance.addVisit(elementKeyOf("manual-task-1", "bye"), now())
    Assertions.assertFalse(processInstance.isCompleted())

    processInstance.addVisit("bye", now())
    Assertions.assertTrue(processInstance.isCompleted())
  }
}