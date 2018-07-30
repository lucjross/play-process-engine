package io.wellsmith.play.engine.visitor

import io.wellsmith.play.engine.PlayEngineConfiguration
import io.wellsmith.play.engine.ProcessInstance
import org.omg.spec.bpmn._20100524.model.TCatchEvent
import java.util.concurrent.Future

abstract class CatchEventVisitor<T: TCatchEvent>(processInstance: ProcessInstance,
                                                 playEngineConfiguration: PlayEngineConfiguration,
                                                 private val visitors: Visitors,
                                                 el: T):
    EventVisitor<T>(processInstance, playEngineConfiguration, el) {

  override fun visit(): List<Future<*>> {

    val futures = mutableListOf<Future<*>>()

    super.visit().let { futures.addAll(it) }

    if (!isNoneEvent()) {
      TODO()
    }
    else {
      processInstance.graph.nextSequenceFlows(el).forEach {
        visitors.visitorOfSequenceFlow(processInstance, it).visit()
            .let { futures.addAll(it) }
      }
    }

    return futures
  }

  internal fun isNoneEvent() = el.eventDefinition.isEmpty()
}