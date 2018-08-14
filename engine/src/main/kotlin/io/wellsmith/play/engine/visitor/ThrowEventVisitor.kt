package io.wellsmith.play.engine.visitor

import io.wellsmith.play.engine.PlayEngineConfiguration
import io.wellsmith.play.engine.ProcessInstance
import org.omg.spec.bpmn._20100524.model.TFlowElement
import org.omg.spec.bpmn._20100524.model.TThrowEvent
import java.util.UUID
import java.util.concurrent.Future

internal abstract class ThrowEventVisitor<T: TThrowEvent>(
    processInstance: ProcessInstance,
    playEngineConfiguration: PlayEngineConfiguration,
    visitors: Visitors,
    el: T
): EventVisitor<T>(processInstance, playEngineConfiguration, visitors, el) {

  override fun visit(fromFlowElement: TFlowElement?) {

    super.visit(fromFlowElement)

    if (!isNoneEvent()) {
      TODO()
    }
    else {
      visitNextSequenceFlows()
    }
  }

  internal fun isNoneEvent() = el.eventDefinition.isEmpty()
}