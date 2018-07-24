package io.wellsmith.play.engine

import io.wellsmith.play.serde.BPMN20Serde
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.omg.spec.bpmn._20100524.model.TEndEvent
import org.omg.spec.bpmn._20100524.model.TProcess
import org.omg.spec.bpmn._20100524.model.TSequenceFlow
import org.omg.spec.bpmn._20100524.model.TStartEvent

class FlowElementGraphTest {

  private val bpmn20Serde = BPMN20Serde()

  @Test
  fun `start-to-end graph constructed correctly`() {

    val filename = "start-to-end.bpmn20.xml"
    val xmlInputStream =
        Thread.currentThread().contextClassLoader.getResourceAsStream(filename)
    val definitions = bpmn20Serde.deserialize(xmlInputStream)
    val process = definitions.rootElement
        .find { it.value is TProcess }!!.value as TProcess
    val graph = FlowElementGraph(process)

    val startEvent = process.flowElement
        .find { it.value is TStartEvent }!!.value

    Assertions.assertEquals(1, graph.nextFlowElements(startEvent).size)

    val sf = graph.nextFlowElements(startEvent).first() as TSequenceFlow
    Assertions.assertEquals(startEvent, sf.sourceRef)

    Assertions.assertEquals(1, graph.nextFlowElements(sf).size)

    val endEvent = graph.nextFlowElements(sf).first() as TEndEvent
    Assertions.assertEquals(sf.targetRef, endEvent)

    Assertions.assertEquals(0, graph.nextFlowElements(endEvent).size)
  }

  @Test
  fun `test allIdsOfFlowElementsToVisitUponProcessInstantiation`() {

    val filename = "flownodes-without-incoming-sequenceflows.bpmn20.xml"
    val xmlInputStream =
        Thread.currentThread().contextClassLoader.getResourceAsStream(filename)
    val definitions = bpmn20Serde.deserialize(xmlInputStream)
    val process = definitions.rootElement
        .find { it.value is TProcess }!!.value as TProcess
    val graph = FlowElementGraph(process)

    val initialVisitIds = graph.allIdsOfFlowElementsToVisitUponProcessInstantiation()
    Assertions.assertEquals(
        listOf("hi", "no-incoming-flow-1", "no-incoming-flow-2"),
        initialVisitIds.sorted())
  }
}