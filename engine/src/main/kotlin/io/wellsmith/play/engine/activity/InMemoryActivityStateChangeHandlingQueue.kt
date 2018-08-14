package io.wellsmith.play.engine.activity

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue

class InMemoryActivityStateChangeHandlingQueue: ActivityStateChangeHandlingQueue {

  internal val _queue: BlockingQueue<ActivityStateChange> =
      ArrayBlockingQueue<ActivityStateChange>(1024)

  override fun queue(activityStateChange: ActivityStateChange) {
    _queue.put(activityStateChange)
  }
}