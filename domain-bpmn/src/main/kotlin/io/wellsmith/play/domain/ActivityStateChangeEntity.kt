package io.wellsmith.play.domain

import java.time.Instant
import java.util.UUID

interface ActivityStateChangeEntity: Entity {

  val processInstanceEntityId: UUID
  val activityId: String
  val fromFlowElementKey: String?
  val time: Instant
  val lifecycleId: UUID
  val state: State  // should be computed, but helps with reconstructing lifecycles
  val tokensArrived: Int
  val inputSetsNeedProcessing: Boolean
  val withdrawn: Boolean
  val workDone: Boolean
  val interruptedByError: Boolean
  val interruptedByNonError: Boolean
  val preCompletionStepsDone: Boolean
  val terminationStepsDone: Boolean



  enum class State(val followsStates: Set<State>) {
    INITIAL(setOf()),
    READY(setOf(INITIAL)),
    ACTIVE(setOf(READY)),
    WITHDRAWN(setOf(READY, ACTIVE)),
    COMPLETING(setOf(ACTIVE)),
    FAILING(setOf(READY, ACTIVE, COMPLETING)),
    TERMINATING(setOf(READY, ACTIVE, COMPLETING)),
    COMPLETED(setOf(COMPLETING)),
    COMPENSATING(setOf(COMPLETED)),
    FAILED(setOf(FAILING, COMPENSATING)),
    TERMINATED(setOf(TERMINATING, COMPENSATING)),
    COMPENSATED(setOf(COMPENSATING));

    fun follows(other: State): Boolean {
      if (followsStates.contains(other)) {
        return true
      }
      else {
        followsStates.forEach {
          if (it.follows(other)) {
            return true
          }
        }
      }

      return false
    }
  }
}