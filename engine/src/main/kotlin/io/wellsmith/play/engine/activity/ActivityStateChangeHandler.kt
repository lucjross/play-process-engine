package io.wellsmith.play.engine.activity

import io.wellsmith.play.engine.PlayEngineConfiguration
import io.wellsmith.play.engine.visitor.ActivityVisitor
import io.wellsmith.play.engine.visitor.Visitors
import org.slf4j.LoggerFactory

class ActivityStateChangeHandler(playEngineConfiguration: PlayEngineConfiguration) {

  companion object {
    private val log = LoggerFactory.getLogger(ActivityStateChangeHandler::class.java)
  }

  private val activeProcessInstanceCache = playEngineConfiguration.activeProcessInstanceCache
  private val visitors = Visitors(playEngineConfiguration)

  internal fun handle(stateChange: ActivityStateChange) {

    val processInstance = activeProcessInstanceCache.get(
        stateChange.processInstanceEntityId)
    val lifecycle = processInstance.getActivityLifecycle(stateChange.lifecycleId)
    val newState = ActivityLifecycle.State.byEntityState[stateChange.newState]!!
    val visitor =
        visitors.visitorOf(processInstance, lifecycle.activity) as ActivityVisitor<*>
    when (newState) {
      ActivityLifecycle.State.READY ->
        visitor.onReady(lifecycle)
      ActivityLifecycle.State.ACTIVE ->
        visitor.onActive(lifecycle)
      ActivityLifecycle.State.COMPLETING ->
        visitor.onCompleting(lifecycle)
      ActivityLifecycle.State.COMPLETED ->
        visitor.onCompleted(lifecycle)
      else ->
        if (log.isDebugEnabled)
          log.debug("Unhandled state change: $stateChange")
    }
  }
}