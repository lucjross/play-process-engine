package io.wellsmith.play.engine.visitor

import io.wellsmith.play.engine.PlayEngineConfiguration
import io.wellsmith.play.engine.ProcessInstance
import org.omg.spec.bpmn._20100524.model.TStartEvent
import java.util.concurrent.Future

internal class StartEventVisitor(processInstance: ProcessInstance,
                                 playEngineConfiguration: PlayEngineConfiguration,
                                 visitors: Visitors,
                                 el: TStartEvent):
    CatchEventVisitor<TStartEvent>(processInstance, playEngineConfiguration, visitors, el) {

  override fun visit(): List<Future<*>> {

    val futures = mutableListOf<Future<*>>()
    super.visit().let { futures.addAll(it) }

    return futures
  }
}