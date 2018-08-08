package io.wellsmith.play.engine.visitor

import io.wellsmith.play.engine.PlayEngineConfiguration
import io.wellsmith.play.engine.ProcessInstance
import org.omg.spec.bpmn._20100524.model.TFlowElement

abstract class FlowElementVisitor<T: TFlowElement>(processInstance: ProcessInstance,
                                                   playEngineConfiguration: PlayEngineConfiguration,
                                                   el: T):
    BaseElementVisitor<T>(processInstance, el) {
}