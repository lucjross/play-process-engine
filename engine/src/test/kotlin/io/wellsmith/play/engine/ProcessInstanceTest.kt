package io.wellsmith.play.engine

import io.wellsmith.play.domain.ActivityStateChangeEntity
import io.wellsmith.play.domain.elementKeyOf
import io.wellsmith.play.engine.activity.ActivityLifecycle
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.omg.spec.bpmn._20100524.model.ObjectFactory
import org.omg.spec.bpmn._20100524.model.TProcess
import org.omg.spec.bpmn._20100524.model.TTask
import java.time.Instant
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class ProcessInstanceTest {

  private val objectFactory = ObjectFactory()
  @Mock
  private lateinit var stateChangeInterceptor: ActivityLifecycle.StateChangeInterceptor

  @Test
  fun `test tokenCountAt with circular process`() {

    val definitions = definitionsFromResource("circular1.bpmn20.xml")
    val process = definitions.rootElement
        .find { it.value is TProcess }!!.value as TProcess
    val graph = FlowElementGraph(process)
    val processInstance = ProcessInstance(graph, process.id, UUID.randomUUID(), UUID.randomUUID(),
        stateChangeInterceptor)

    graph.allIdsOfFlowNodesToVisitUponProcessInstantiation().forEach {
      processInstance.addVisit(FlowElementVisit(
          graph.flowElementsByKey[it]!!, now()))
    }

    Assertions.assertEquals(1, processInstance.tokenCountAt("hi-1"))
    Assertions.assertEquals(1, processInstance.tokenCountAt("hi-2"))
    Assertions.assertEquals(0, processInstance.tokenCountAt(elementKeyOf("hi-1", "task-1")))
    Assertions.assertEquals(0, processInstance.tokenCountAt(elementKeyOf("hi-2", "task-1")))

    val correlationId1 = UUID.randomUUID()
    processInstance.addVisitNow(elementKeyOf("hi-1", "task-1"), "hi-1", correlationId1)
    processInstance.addVisitNow(elementKeyOf("hi-2", "task-1"), "hi-2", correlationId1)
    Assertions.assertEquals(0, processInstance.tokenCountAt("hi-1"))
    Assertions.assertEquals(0, processInstance.tokenCountAt("hi-2"))
    Assertions.assertEquals(1, processInstance.tokenCountAt(elementKeyOf("hi-1", "task-1")))
    Assertions.assertEquals(1, processInstance.tokenCountAt(elementKeyOf("hi-2", "task-1")))

    processInstance.addVisitNow("task-1", elementKeyOf("hi-1", "task-1"))
    Assertions.assertEquals(0, processInstance.tokenCountAt(elementKeyOf("hi-1", "task-1")))
    Assertions.assertEquals(1, processInstance.tokenCountAt(elementKeyOf("hi-2", "task-1")))
    Assertions.assertEquals(1, processInstance.tokenCountAt("task-1"))

    processInstance.addVisitNow("task-1", elementKeyOf("hi-2", "task-1"))
    Assertions.assertEquals(0, processInstance.tokenCountAt(elementKeyOf("hi-1", "task-1")))
    Assertions.assertEquals(0, processInstance.tokenCountAt(elementKeyOf("hi-2", "task-1")))
    Assertions.assertEquals(2, processInstance.tokenCountAt("task-1"))

    processInstance.addVisitNow(elementKeyOf("task-1", "gateway-1"), "task-1")
    Assertions.assertEquals(1, processInstance.tokenCountAt("task-1"))
    Assertions.assertEquals(1, processInstance.tokenCountAt(elementKeyOf("task-1", "gateway-1")))

    processInstance.addVisitNow("gateway-1", elementKeyOf("task-1", "gateway-1"))
    Assertions.assertEquals(1, processInstance.tokenCountAt("task-1"))
    Assertions.assertEquals(0, processInstance.tokenCountAt(elementKeyOf("task-1", "gateway-1")))
    Assertions.assertEquals(1, processInstance.tokenCountAt("gateway-1"))

    processInstance.addVisitNow(elementKeyOf("gateway-1", "task-1"), "gateway-1")
    Assertions.assertEquals(0, processInstance.tokenCountAt("gateway-1"))
    Assertions.assertEquals(1, processInstance.tokenCountAt(elementKeyOf("gateway-1", "task-1")))

    processInstance.addVisitNow("task-1", elementKeyOf("gateway-1", "task-1"))
    // there was already one token here due to the convergence of two flows from two StartEvents.
    Assertions.assertEquals(2, processInstance.tokenCountAt("task-1"))

    processInstance.addVisitNow(elementKeyOf("task-1", "gateway-1"), "task-1")
    Assertions.assertEquals(1, processInstance.tokenCountAt("task-1"))
    Assertions.assertEquals(1, processInstance.tokenCountAt(elementKeyOf("task-1", "gateway-1")))

    processInstance.addVisitNow(elementKeyOf("task-1", "gateway-1"), "task-1")
    Assertions.assertEquals(0, processInstance.tokenCountAt("task-1"))
    Assertions.assertEquals(2, processInstance.tokenCountAt(elementKeyOf("task-1", "gateway-1")))

    for (i in 0..1)
      processInstance.addVisitNow("gateway-1", elementKeyOf("task-1", "gateway-1"))
    Assertions.assertEquals(0, processInstance.tokenCountAt(elementKeyOf("task-1", "gateway-1")))
    Assertions.assertEquals(2, processInstance.tokenCountAt("gateway-1"))

  }

  @Test
  fun `test isTokenAt with parallel-activities`() {

    val definitions = definitionsFromResource("parallel-activities.bpmn20.xml")
    val process = definitions.rootElement
        .find { it.value is TProcess }!!.value as TProcess
    val graph = FlowElementGraph(process)
    val processInstance = ProcessInstance(graph, process.id, UUID.randomUUID(), UUID.randomUUID(),
        stateChangeInterceptor)

    // cannot visit a node ahead of nodes with no tokens
    Assertions.assertThrows(IllegalArgumentException::class.java) {
      processInstance.addVisitNow("gateway-1", "hi")
    }
    Assertions.assertThrows(IllegalArgumentException::class.java) {
      processInstance.addVisitNow("gateway-1")
    }

    graph.allIdsOfFlowNodesToVisitUponProcessInstantiation().forEach {
      processInstance.addVisitNow(it)
    }

    Assertions.assertTrue(processInstance.isTokenAt("hi"))
    Assertions.assertFalse(processInstance.isTokenAt("gateway-1"))

    // token already at "hi", so it should not be visitable
    Assertions.assertThrows(IllegalArgumentException::class.java) {
      processInstance.addVisitNow("hi")
    }

    // sequence flows must be visited as well
    Assertions.assertThrows(IllegalArgumentException::class.java) {
      processInstance.addVisitNow("gateway-1", "hi")
    }

    processInstance.addVisitNow(elementKeyOf("hi", "gateway-1"), "hi")
    Assertions.assertTrue(processInstance.isTokenAt(
        elementKeyOf("hi", "gateway-1")))

    processInstance.addVisitNow("gateway-1", elementKeyOf("hi", "gateway-1"))
    Assertions.assertFalse(processInstance.isTokenAt(
        elementKeyOf("hi", "gateway-1")))
    Assertions.assertTrue(processInstance.isTokenAt("gateway-1"))

    val splitCorrelationId = UUID.randomUUID()
    processInstance.addVisitNow(elementKeyOf("gateway-1", "task-1"), "gateway-1", splitCorrelationId)
    processInstance.addVisitNow(elementKeyOf("gateway-1", "task-2"), "gateway-1", splitCorrelationId)
    Assertions.assertFalse(processInstance.isTokenAt("gateway-1"))

    processInstance.addVisitNow("task-1", elementKeyOf("gateway-1", "task-1"))
    processInstance.addVisitNow("task-2", elementKeyOf("gateway-1", "task-2"))

    Assertions.assertTrue(processInstance.isTokenAt("task-1"))
    Assertions.assertTrue(processInstance.isTokenAt("task-2"))
    Assertions.assertFalse(processInstance.isTokenAt("gateway-1"))

    processInstance.addVisitNow(elementKeyOf("task-1", "bye"), "task-1")
    processInstance.addVisitNow("bye", elementKeyOf("task-1", "bye"))

    Assertions.assertFalse(processInstance.isTokenAt("task-1"))
    Assertions.assertTrue(processInstance.isTokenAt("task-2"))
    // end event does not hold a token
    Assertions.assertFalse(processInstance.isTokenAt("bye"))

    processInstance.addVisitNow(elementKeyOf("task-2", "bye"), "task-2")
    processInstance.addVisitNow("bye", elementKeyOf("task-2", "bye"))
    Assertions.assertFalse(processInstance.isTokenAt("task-2"))
  }

  @Test
  fun `isCompleted should work with start-to-end`() {

    val definitions = definitionsFromResource("start-to-end.bpmn20.xml")
    val process = definitions.rootElement
        .find { it.value is TProcess }!!.value as TProcess
    val graph = FlowElementGraph(process)
    val processInstance = ProcessInstance(graph, process.id, UUID.randomUUID(), UUID.randomUUID(),
        stateChangeInterceptor)
    Assertions.assertFalse(processInstance.isCompleted())

    processInstance.addVisitNow("hi")
    Assertions.assertFalse(processInstance.isCompleted())

    processInstance.addVisitNow(elementKeyOf("hi", "bye"), "hi")
    Assertions.assertFalse(processInstance.isCompleted())

    processInstance.addVisitNow("bye", elementKeyOf("hi", "bye"))
    Assertions.assertTrue(processInstance.isCompleted())
  }

  @Test
  fun `isCompleted should work with single-task`() {

    val definitions = definitionsFromResource("single-task.bpmn20.xml")
    val process = definitions.rootElement
        .find { it.value is TProcess }!!.value as TProcess
    val graph = FlowElementGraph(process)
    val processInstance = ProcessInstance(graph, process.id, UUID.randomUUID(), UUID.randomUUID(),
        stateChangeInterceptor)
    Assertions.assertFalse(processInstance.isCompleted())

    processInstance.addVisitNow("hi")
    Assertions.assertFalse(processInstance.isCompleted())

    processInstance.addVisitNow(elementKeyOf("hi", "manual-task-1"), "hi")
    Assertions.assertFalse(processInstance.isCompleted())

    processInstance.addVisitNow("manual-task-1", elementKeyOf("hi", "manual-task-1"))
    Assertions.assertFalse(processInstance.isCompleted())

    processInstance.addVisitNow(elementKeyOf("manual-task-1", "bye"), "manual-task-1")
    Assertions.assertFalse(processInstance.isCompleted())

    processInstance.addVisitNow("bye", elementKeyOf("manual-task-1", "bye"))
    Assertions.assertTrue(processInstance.isCompleted())
  }

  @Test
  fun `recreateActivityLifecycle should invoke the right constructor`() {

    val process = TProcess()
        .apply { flowElement.add(
            objectFactory.createTask(TTask()
                .apply { id = "id" })) }
    val entityId = UUID.randomUUID()
    val processInstance = ProcessInstance(
        FlowElementGraph(process), "id", UUID.randomUUID(), entityId,
        stateChangeInterceptor)
    val stateChange = object: ActivityStateChangeEntity {
      override val processInstanceEntityId = entityId
      override val activityId = "id"
      override val fromFlowElementKey: String? = null
      override val time = Instant.now()
      override val lifecycleId = UUID.randomUUID()
      override val state = ActivityStateChangeEntity.State.COMPLETING
      override val tokensArrived = 1
      override val inputSetsNeedProcessing = false
      override val withdrawn = false
      override val workDone = true
      override val interruptedByError = false
      override val interruptedByNonError = false
      override val preCompletionStepsDone = false
      override val terminationStepsDone = false
      override val id = UUID.randomUUID()
    }

    val activityLifecycle = processInstance.recreateActivityLifecycle(stateChange)
    Assertions.assertTrue(activityLifecycle is net.sf.cglib.proxy.Factory)
    Assertions.assertEquals(stateChange.activityId, activityLifecycle.activity.id)
  }

  private fun ProcessInstance.addVisitNow(flowElementId: String,
                                          fromFlowElementId: String? = null,
                                          splitCorrelationId: UUID? = null) {
    return addVisit(flowElementId, now(), fromFlowElementId, splitCorrelationId)
  }
}