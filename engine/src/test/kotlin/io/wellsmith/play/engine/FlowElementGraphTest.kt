package io.wellsmith.play.engine

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.omg.spec.bpmn._20100524.model.TEndEvent
import org.omg.spec.bpmn._20100524.model.TProcess
import org.omg.spec.bpmn._20100524.model.TSequenceFlow
import org.omg.spec.bpmn._20100524.model.TStartEvent

class FlowElementGraphTest {

  @Test
  fun `start-to-end graph constructed correctly`() {

    val definitions = definitionsFromResource("start-to-end.bpmn20.xml")
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

    val definitions = definitionsFromResource("flownodes-without-incoming-sequenceflows.bpmn20.xml")
    val process = definitions.rootElement
        .find { it.value is TProcess }!!.value as TProcess
    val graph = FlowElementGraph(process)

    val initialVisitIds = graph.allIdsOfFlowNodesToVisitUponProcessInstantiation()
    Assertions.assertEquals(
        listOf("hi", "no-incoming-flow-1", "no-incoming-flow-2"),
        initialVisitIds.sorted())
  }
}