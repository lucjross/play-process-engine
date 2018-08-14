package io.wellsmith.play.engine.testhook

/**
 * A utility for guaranteeing happens-before relationships between threaded logic
 * and test assertions. More efficient and reliable for testing than thread sleeps, but
 * requires insertion of test hooks into runtime code, which some might consider a code smell.
 * Use conservatively.
 */
interface Synchronizer {

  /**
   * Called at any point by runtime code in order to notify running tests of changes in application
   * state.
   */
  fun update(action: String, value: Any? = null) {
    // no-op
  }

  fun lock(context: Any) {
    // no-op
  }

  fun unlock(context: Any) {
    // no-op
  }
}