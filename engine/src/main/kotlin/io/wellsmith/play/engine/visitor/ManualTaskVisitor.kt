package io.wellsmith.play.engine.visitor

import io.wellsmith.play.engine.PlayEngineConfiguration
import io.wellsmith.play.engine.ProcessInstance
import org.omg.spec.bpmn._20100524.model.TFlowElement
import org.omg.spec.bpmn._20100524.model.TManualTask
import org.slf4j.LoggerFactory
import java.util.UUID
import java.util.concurrent.Future

class ManualTaskVisitor(processInstance: ProcessInstance,
                        playEngineConfiguration: PlayEngineConfiguration,
                        visitors: Visitors,
                        el: TManualTask):
    TaskVisitor(processInstance, playEngineConfiguration, visitors, el) {

  companion object {
    @JvmField
    val logger = LoggerFactory.getLogger(ManualTaskVisitor::class.java)
  }

  override fun visit(fromFlowElement: TFlowElement?): List<Future<*>> {

    val futures = mutableListOf<Future<*>>()
    super.visit(fromFlowElement).let { futures.addAll(it) }

    // "A Manual Task is a Task that is not managed by any business process engine."
    // so, the token must stay on this Task until it is manually completed.

    return futures
  }

  fun workIsDone(): List<Future<*>> {
    // todo - get an Activity "Lifecycle" from the ProcessInstance & update it...
    // e.g.: processInstance.activityLifecycle(el).complete()
    // this will be a major story

    val futures = mutableListOf<Future<*>>()
    visitNextSequenceFlows(futures)

    return futures
  }
}