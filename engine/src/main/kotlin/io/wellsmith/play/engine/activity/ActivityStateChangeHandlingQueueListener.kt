package io.wellsmith.play.engine.activity

import io.wellsmith.play.engine.testhook.Synchronizer
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

class ActivityStateChangeHandlingQueueListener(
    private val activityStateChangeHandlingQueue: InMemoryActivityStateChangeHandlingQueue,
    private val activityStateChangeHandler: ActivityStateChangeHandler,
    private val synchronizer: Synchronizer) {

  companion object {
    private val log = LoggerFactory.getLogger(
        ActivityStateChangeHandlingQueueListener::class.java)
  }

  fun listen() {
    Thread {
      while (true) {
        val stateChange: ActivityStateChange? = activityStateChangeHandlingQueue._queue
            .poll(1L, TimeUnit.MINUTES)
        if (log.isDebugEnabled)
          log.debug(stateChange.toString())

        if (stateChange != null) {
          try {
            activityStateChangeHandler.handle(stateChange)
            synchronizer.update("state change: ${stateChange.activityId} to ${stateChange.newState}")
          }
          catch (t: Throwable) {
            if (log.isErrorEnabled)
              log.error(t.message, t)
          }
        }
      }
    }.start()
  }
}