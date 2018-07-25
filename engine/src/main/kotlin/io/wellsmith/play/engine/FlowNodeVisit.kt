package io.wellsmith.play.engine

import org.omg.spec.bpmn._20100524.model.TFlowNode
import java.time.Instant

data class FlowNodeVisit(val flowNode: TFlowNode,
                         val time: Instant,
                         val fromFlowNode: TFlowNode? = null) {
}