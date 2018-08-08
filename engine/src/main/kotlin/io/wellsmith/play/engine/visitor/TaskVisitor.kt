package io.wellsmith.play.engine.visitor

import io.wellsmith.play.engine.PlayEngineConfiguration
import io.wellsmith.play.engine.ProcessInstance
import io.wellsmith.play.engine.compliance.Compliant
import io.wellsmith.play.engine.compliance.Level
import io.wellsmith.play.engine.compliance.Spec
import org.omg.spec.bpmn._20100524.model.TFlowElement
import org.omg.spec.bpmn._20100524.model.TTask
import java.util.UUID
import java.util.concurrent.Future

@Compliant(toSpec = Spec.BPMN_2_0, section = "10.2.3", level = Level.INCOMPLETE)
open class TaskVisitor(processInstance: ProcessInstance,
                       playEngineConfiguration: PlayEngineConfiguration,
                       visitors: Visitors,
                       el: TTask):
    ActivityVisitor<TTask>(processInstance, playEngineConfiguration, visitors, el) {

  override fun visit(fromFlowElement: TFlowElement?): List<Future<*>> {

    val futures = mutableListOf<Future<*>>()
    super.visit(fromFlowElement).let { futures.addAll(it) }

    if (TTask::class == el::class) {
      // an Abstract Task is not associated with any work,
      // so the token can immediately move forward.
      visitNextSequenceFlows(futures)
    }

    return futures
  }
}