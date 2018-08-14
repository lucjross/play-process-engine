package io.wellsmith.play.restapp.controller

import io.wellsmith.play.engine.testhook.Synchronizer
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

abstract class TestSynchronizer: Synchronizer {

  companion object {
    private val log = LoggerFactory.getLogger(TestSynchronizer::class.java)
  }

  private val lockMap = ConcurrentHashMap<Any, Pair<Lock, Thread>>()

  abstract override fun update(action: String, value: Any?)

  /**
   * Similar to [java.util.concurrent.locks.Lock.lock], but employs additional threads
   * to serve as "third-party" entrants into monitors. Allows runtime and test code running
   * in a single thread to block one another.
   *
   * If the given context is keyed to a thread, then this call is an "await" call
   * and will block the calling thread, until [TestSynchronizer.unlock] is called
   * with the same "context" by another thread.
   *
   * If the given context is not keyed to a thread, then a new one will be created
   * and started. The thread will create a [ReentrantLock], lock on it, and sleep
   * for an indefinite (long) duration.
   *
   * Thereby, each lock will have exactly one hold,
   * and will be held by exactly one thread, throughout its lifetime.
   */
  override fun lock(context: Any) {

    var awaitLock = false
    var lockInThread = lockMap[context]
    if (lockInThread == null) {
      synchronized(lockMap) {
        lockInThread = lockMap[context]
        if (lockInThread == null) {
          val lock = ReentrantLock()
          val thread = Thread {
            lock.lock()
            try {
              Thread.sleep(24 * 60 * 60 * 1000)
            }
            catch (e: InterruptedException) {
              if (log.isDebugEnabled)
                log.debug("Unlocking $lock")
            }
            finally {
              lock.unlock()
            }
          }
          thread.start()
          lockMap[context] = lock to thread
        }
        else {
          awaitLock = true
        }
      }
    }
    else {
      awaitLock = true
    }

    if (awaitLock) {
      lockInThread!!.first.lock()
    }
  }

  /**
   * Interrupts a thread created by [TestSynchronizer.lock] for the given context key, if it
   * exists. The [Lock] created by the interrupted thread is unlocked and released.
   */
  override fun unlock(context: Any) {
    val lockInThread = lockMap.remove(context)
    if (lockInThread != null) {
      if (log.isDebugEnabled)
        log.debug("Interrupting thread ${lockInThread.second}")

      lockInThread.second.interrupt()
    }
  }
}