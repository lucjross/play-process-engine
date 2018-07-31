package io.wellsmith.play.engine

import org.omg.spec.bpmn._20100524.model.TFlowElement
import java.time.Instant

data class FlowElementVisit(val flowElement: TFlowElement,
                            val time: Instant,
                            val fromFlowElement: TFlowElement? = null) {
}