package io.wellsmith.play.engine.visitor

import io.wellsmith.play.engine.PlayEngineConfiguration
import io.wellsmith.play.engine.ProcessInstance
import org.omg.spec.bpmn._20100524.model.TThrowEvent
import java.util.concurrent.Future

abstract class ThrowEventVisitor<T: TThrowEvent>(processInstance: ProcessInstance,
                                                 playEngineConfiguration: PlayEngineConfiguration,
                                                 el: T):
    EventVisitor<T>(processInstance, playEngineConfiguration, el) {

  override fun visit(): List<Future<*>> {

    val futures = mutableListOf<Future<*>>()

    super.visit().let { futures.addAll(it) }

    if (!isNoneEvent()) {
      TODO()
    }

    return futures
  }

  internal fun isNoneEvent() = el.eventDefinition.isEmpty()
}