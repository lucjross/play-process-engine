package io.wellsmith.play.engine.visitor

import io.wellsmith.play.engine.PlayEngineConfiguration
import io.wellsmith.play.engine.ProcessInstance
import org.omg.spec.bpmn._20100524.model.TEndEvent

class EndEventVisitor(processInstance: ProcessInstance,
                      playEngineConfiguration: PlayEngineConfiguration,
                      visitors: Visitors,
                      el: TEndEvent):
    ThrowEventVisitor<TEndEvent>(processInstance, playEngineConfiguration, visitors, el) {


}