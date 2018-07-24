package io.wellsmith.play.engine

import org.omg.spec.bpmn._20100524.model.TCallActivity
import org.omg.spec.bpmn._20100524.model.TProcess
import java.util.UUID

class ProcessInstance(val graph: FlowElementGraph,
                      val processId: String,
                      val bpmn20XMLEntityId: UUID,
                      val parentProcess: TProcess? = null,
                      val calledBy: TCallActivity? = null) {


}