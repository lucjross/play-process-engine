package io.wellsmith.play.engine.visitor

import io.wellsmith.play.engine.FlowElementVisit
import io.wellsmith.play.engine.PlayEngineConfiguration
import io.wellsmith.play.engine.ProcessInstance
import org.omg.spec.bpmn._20100524.model.TFlowElement
import java.time.Instant
import java.util.concurrent.Future

abstract class FlowElementVisitor<T: TFlowElement>(processInstance: ProcessInstance,
                                                   playEngineConfiguration: PlayEngineConfiguration,
                                                   el: T):
    BaseElementVisitor<T>(processInstance, el) {

  private val clock = playEngineConfiguration.clock

  override fun visit(): List<Future<*>> {

    processInstance.addVisit(FlowElementVisit(el, Instant.now(clock)))
    return mutableListOf()
  }
}