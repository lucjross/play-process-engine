package io.wellsmith.play.engine.visitor

import io.wellsmith.play.engine.ProcessInstance
import io.wellsmith.play.engine.compliance.Compliant
import io.wellsmith.play.engine.compliance.Level
import io.wellsmith.play.engine.compliance.Spec
import org.omg.spec.bpmn._20100524.model.TFlowNode
import org.omg.spec.bpmn._20100524.model.TSequenceFlow
import java.util.concurrent.Future

// todo - refactor to consider sequenceflows as visitable elements themselves.
// persist visits & give them short-lived tokens.
// that way it would be more consistent with the language of the spec.
@Compliant(toSpec = Spec.BPMN_2_0, level = Level.NON_COMPLIANT)
internal class SequenceFlowVisitor(processInstance: ProcessInstance,
                                   el: TSequenceFlow,
                                   private val visitors: Visitors):
    BaseElementVisitor<TSequenceFlow>(processInstance, el) {

  override fun visit(): List<Future<*>> {

    if (el.conditionExpression != null) {
      TODO()
    }

    val futures = visitors.visitorOf(processInstance, el.targetRef as TFlowNode)
        .visit()
    return futures
  }
}