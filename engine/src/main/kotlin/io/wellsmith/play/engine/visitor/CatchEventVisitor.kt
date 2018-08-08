package io.wellsmith.play.engine.visitor

import io.wellsmith.play.engine.PlayEngineConfiguration
import io.wellsmith.play.engine.ProcessInstance
import org.omg.spec.bpmn._20100524.model.TCatchEvent
import org.omg.spec.bpmn._20100524.model.TFlowElement
import java.util.UUID
import java.util.concurrent.Future

abstract class CatchEventVisitor<T: TCatchEvent>(processInstance: ProcessInstance,
                                                 playEngineConfiguration: PlayEngineConfiguration,
                                                 visitors: Visitors,
                                                 el: T):
    EventVisitor<T>(processInstance, playEngineConfiguration, visitors, el) {

  override fun visit(fromFlowElement: TFlowElement?): List<Future<*>> {

    val futures = mutableListOf<Future<*>>()

    super.visit(fromFlowElement).let { futures.addAll(it) }

    if (!isNoneEvent()) {
      TODO()
    }
    else {
      visitNextSequenceFlows(futures)
    }

    return futures
  }

  internal fun isNoneEvent() = el.eventDefinition.isEmpty()
}