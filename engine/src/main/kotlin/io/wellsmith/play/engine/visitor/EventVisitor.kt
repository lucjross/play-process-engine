package io.wellsmith.play.engine.visitor

import io.wellsmith.play.engine.PlayEngineConfiguration
import io.wellsmith.play.engine.ProcessInstance
import org.omg.spec.bpmn._20100524.model.TEvent

abstract class EventVisitor<T: TEvent>(processInstance: ProcessInstance,
                                       playEngineConfiguration: PlayEngineConfiguration,
                                       visitors: Visitors,
                                       el: T):
    FlowNodeVisitor<T>(processInstance, playEngineConfiguration, visitors, el) {
}