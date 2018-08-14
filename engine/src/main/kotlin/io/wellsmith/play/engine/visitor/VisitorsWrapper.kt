package io.wellsmith.play.engine.visitor

import io.wellsmith.play.engine.PlayEngineConfiguration
import io.wellsmith.play.engine.ProcessInstance

/**
 * Provides functions to visit process elements,
 * for use by components external to the engine.
 */
class VisitorsWrapper(playEngineConfiguration: PlayEngineConfiguration) {

  private val visitationQueue = playEngineConfiguration.visitationQueue()
  private val visitors = Visitors(playEngineConfiguration)

  fun visitProcess(processInstance: ProcessInstance) {
    visitationQueue.queue(Visitation(
        processInstance.processId, null, processInstance.entityId) {
      visitors.visitorOf(processInstance).visit(null)
    })
  }

  @Deprecated("demo")
  fun workIsDone(processInstance: ProcessInstance, manualTaskId: String) {

    if (!processInstance.isTokenAt(manualTaskId)) {
      throw IllegalStateException("Cannot call completeWork on a node with no tokens." +
          " processInstance.entityId=${processInstance.entityId}, manualTaskId=$manualTaskId")
    }

    visitationQueue.queue(Visitation(
        manualTaskId, null, processInstance.entityId) {
      visitors.visitorOfManualTask(processInstance, manualTaskId).completeWork()
    })
  }
}