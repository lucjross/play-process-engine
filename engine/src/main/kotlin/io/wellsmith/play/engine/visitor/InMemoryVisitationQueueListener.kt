package io.wellsmith.play.engine.visitor

import io.wellsmith.play.engine.testhook.Synchronizer
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

class InMemoryVisitationQueueListener(private val visitationQueue: InMemoryVisitationQueue,
                                      private val synchronizer: Synchronizer) {

  companion object {
    private val log = LoggerFactory.getLogger(InMemoryVisitationQueueListener::class.java)
  }

  fun listen() {
    Thread {
      while (true) {
        val visitation: Visitation? = visitationQueue._queue.poll(1L, TimeUnit.MINUTES)
        if (visitation != null) {
          try {
            visitation.work.invoke()
            synchronizer.update("visit: ${visitation.elementKey}")
          }
          catch (t: Throwable) {
            if (log.isErrorEnabled) {
              log.error("Error/exception arose from execution of Visitation," +
                  " originating from Visitor ${visitation.originatingVisitorClassSimpleName}," +
                  " for process instance ${visitation.processInstanceEntityId}",
                  t)
            }
          }
        }
      }
    }.start()
  }
}