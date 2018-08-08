package io.wellsmith.play.engine.visitor

import io.wellsmith.play.engine.PlayEngineConfiguration
import io.wellsmith.play.engine.ProcessInstance
import io.wellsmith.play.engine.compliance.Compliant
import io.wellsmith.play.engine.compliance.Level
import io.wellsmith.play.engine.compliance.Spec
import org.omg.spec.bpmn._20100524.model.TActivity
import org.omg.spec.bpmn._20100524.model.TFlowElement
import java.math.BigInteger
import java.util.UUID
import java.util.concurrent.Future

@Compliant(toSpec = Spec.BPMN_2_0, section = "10.2", level = Level.INCOMPLETE)
abstract class ActivityVisitor<T: TActivity>(
    processInstance: ProcessInstance,
    playEngineConfiguration: PlayEngineConfiguration,
    visitors: Visitors,
    el: T
): FlowNodeVisitor<T>(processInstance, playEngineConfiguration, visitors, el) {

  override fun visit(fromFlowElement: TFlowElement?): List<Future<*>> {

    if (el.isIsForCompensation) {
      TODO()
    }

    if (BigInteger.ONE != el.completionQuantity) {
      TODO()
    }

    if (el.ioSpecification != null) {
      TODO()
    }

    // todo - other attributes

    return super.visit(fromFlowElement)
  }
}