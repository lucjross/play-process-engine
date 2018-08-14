package io.wellsmith.play.engine.visitor

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue

class InMemoryVisitationQueue: VisitationQueue {

  internal val _queue: BlockingQueue<Visitation> = ArrayBlockingQueue<Visitation>(1024)

  override fun queue(visitation: Visitation) {
    _queue.put(visitation)
  }
}