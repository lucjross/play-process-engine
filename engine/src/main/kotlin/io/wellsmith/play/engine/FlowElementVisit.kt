package io.wellsmith.play.engine

import org.omg.spec.bpmn._20100524.model.TFlowElement
import org.omg.spec.bpmn._20100524.model.TSequenceFlow
import java.time.Instant
import java.util.UUID

data class FlowElementVisit(val flowElement: TFlowElement,
                            val time: Instant,
                            val fromFlowElement: TFlowElement? = null,
                            val splitCorrelationId: UUID? = null) {

  init {
    if (flowElement !is TSequenceFlow && splitCorrelationId != null) {
      throw IllegalArgumentException("splitCorrelationId not applicable to ${flowElement::class}")
    }
  }

  override fun toString() =
      "FlowElementVisit(flowElement.elementKey=${flowElement.elementKey()}" +
          ", time=$time" +
          ", fromFlowElement.elementKey=${fromFlowElement?.elementKey()}" +
          ", splitCorrelationId=$splitCorrelationId" +
          ", flowElement=$flowElement," +
          ", fromFlowElement=$fromFlowElement)"
}