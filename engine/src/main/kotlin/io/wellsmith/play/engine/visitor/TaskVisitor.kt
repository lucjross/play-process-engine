package io.wellsmith.play.engine.visitor

import io.wellsmith.play.engine.PlayEngineConfiguration
import io.wellsmith.play.engine.ProcessInstance
import io.wellsmith.play.engine.compliance.Compliant
import io.wellsmith.play.engine.compliance.Level
import io.wellsmith.play.engine.compliance.Spec
import org.omg.spec.bpmn._20100524.model.TTask
import java.util.concurrent.Future

@Compliant(toSpec = Spec.BPMN_2_0, section = "10.2.3", level = Level.INCOMPLETE)
open class TaskVisitor(processInstance: ProcessInstance,
                       playEngineConfiguration: PlayEngineConfiguration,
                       private val visitors: Visitors,
                       el: TTask):
    ActivityVisitor<TTask>(processInstance, playEngineConfiguration, el) {

  override fun visit(): List<Future<*>> {

    val futures = mutableListOf<Future<*>>()
    super.visit().let { futures.addAll(it) }

    if (TTask::class == el::class) {
      // an Abstract Task is not associated with any work,
      // so the token can immediately move forward.
      processInstance.graph.nextSequenceFlows(el).forEach {
        visitors.visitorOfSequenceFlow(processInstance, it).visit()
            .let { futures.addAll(it) }
      }
    }

    return futures
  }
}