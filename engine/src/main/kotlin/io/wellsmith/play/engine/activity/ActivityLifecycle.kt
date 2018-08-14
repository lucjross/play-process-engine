package io.wellsmith.play.engine.activity

import io.wellsmith.play.domain.ActivityStateChangeEntity
import io.wellsmith.play.engine.compliance.Compliant
import io.wellsmith.play.engine.compliance.Level
import io.wellsmith.play.engine.compliance.Spec
import io.wellsmith.play.persistence.api.EntityFactory
import net.sf.cglib.proxy.MethodInterceptor
import net.sf.cglib.proxy.MethodProxy
import org.omg.spec.bpmn._20100524.model.TActivity
import org.slf4j.LoggerFactory
import org.springframework.data.repository.CrudRepository
import java.lang.reflect.Method
import java.time.Clock
import java.time.Instant
import java.util.UUID

@Compliant(toSpec = Spec.BPMN_2_0, section = "13.2.2", level = Level.INCOMPLETE)
open class ActivityLifecycle
protected constructor(
    val lifecycleId: UUID,
    val activity: TActivity,
    val fromFlowElementKey: String? = null) {

  companion object {
    private val logger = LoggerFactory.getLogger(ActivityLifecycle::class.java)
  }

  protected constructor(activity: TActivity,
                        activityStateChangeEntity: ActivityStateChangeEntity): this(
      activityStateChangeEntity.lifecycleId,
      activity,
      activityStateChangeEntity.fromFlowElementKey) {

    // assuming that if a state change entity is saved then it has been initialized
    initialized = true

    tokensArrived = activityStateChangeEntity.tokensArrived
    inputSetsNeedProcessing = activityStateChangeEntity.inputSetsNeedProcessing
    withdrawn = activityStateChangeEntity.withdrawn
    workDone = activityStateChangeEntity.workDone
    interruptedByError = activityStateChangeEntity.interruptedByError
    interruptedByNonError = activityStateChangeEntity.interruptedByNonError
    preCompletionStepsDone = activityStateChangeEntity.preCompletionStepsDone
    terminationStepsDone = activityStateChangeEntity.terminationStepsDone
  }

  // val receivedDataInputs:

  private var initialized = false
  private var tokensArrived: Int = 0
  private var inputSetsNeedProcessing = activity.ioSpecification?.dataInput?.isNotEmpty() == true
  private var inputSet: Any? = null
  private var withdrawn = false
  private var workDone = false
  private var interruptedByError = false
  private var interruptedByNonError = false
  private var preCompletionStepsDone = false
  private var terminationStepsDone = false

  fun state() = State.values().first { it.condition.invoke(this) }

  @StateChange
  open fun initialize() {
    if (initialized) {
      throw IllegalStateException("Cannot initialize more than once")
    }

    initialized = true
  }

  @StateChange
  open fun applyToken() {
    if (!initialized) {
      throw IllegalStateException("Not initialized")
    }
    else if (tokensArrived + 1 > activity.startQuantity.toInt()) {
      throw IllegalStateException("Cannot apply more than ${activity.startQuantity.toInt()}" +
          " tokens to activity ${activity.id}")
    }

    tokensArrived++
  }

  @StateChange
  open fun temp_inputSetsAvailable() {
    if (state() != State.READY) {
      throw IllegalStateException("Not Ready")
    }

    inputSetsNeedProcessing = false
  }

  @StateChange
  open fun interruptWithError() {
    if (state() == State.INITIAL) {
      throw IllegalStateException("Activity in initial state cannot be interrupted by error")
    }

    interruptedByError = true
  }

  @StateChange
  open fun interruptWithNonError() {
    if (state() == State.INITIAL) {
      throw IllegalStateException("Activity in initial state cannot be interrupted by non-error")
    }

    interruptedByNonError = true
  }

  @StateChange
  open fun withdraw() {
    if (state() == State.INITIAL) {
      throw IllegalStateException("Activity in initial state cannot be withdrawn")
    }

    withdrawn = true
  }

  @StateChange
  open fun completeWork() {
    if (state() != State.ACTIVE) {
      throw IllegalStateException("Cannot complete work when Activity is not in Active state")
    }

    workDone = true
  }

  @StateChange
  open fun completePreCompletionSteps() {
    if (state() != State.COMPLETING) {
      throw IllegalStateException("Cannot complete pre-Completion steps" +
          " when Activity is not in Completing state")
    }

    preCompletionStepsDone = true
  }

  @StateChange
  open fun completeTerminationSteps() {
    if (state() !in setOf(State.FAILING, State.TERMINATING)) {
      throw IllegalStateException("Cannot complete termination steps" +
          " when Activity is not in Failing or Terminating state")
    }

    terminationStepsDone = true
  }

  override fun toString(): String {
    return "ActivityLifecycle(lifecycleId=$lifecycleId," +
        " activity=$activity," +
        " fromFlowElementKey=$fromFlowElementKey," +
        " state()=${state()}," +
        " tokensArrived=$tokensArrived," +
        " inputSetsNeedProcessing=$inputSetsNeedProcessing," +
        " inputSet=$inputSet," +
        " withdrawn=$withdrawn," +
        " workDone=$workDone," +
        " interruptedByError=$interruptedByError," +
        " interruptedByNonError=$interruptedByNonError," +
        " preCompletionStepsDone=$preCompletionStepsDone," +
        " terminationStepsDone=$terminationStepsDone)"
  }


  enum class State(val entityState: ActivityStateChangeEntity.State,
                            val condition: (ActivityLifecycle) -> Boolean) {
    INITIAL(
        ActivityStateChangeEntity.State.INITIAL,
        { it.initialized &&
            (it.tokensArrived < it.activity.startQuantity.toInt()) }),
    READY(
        ActivityStateChangeEntity.State.READY,
        { (it.tokensArrived == it.activity.startQuantity.toInt())
            && it.inputSetsNeedProcessing
            && !it.interruptedByError
            && !it.interruptedByNonError
            && !it.withdrawn }),
    ACTIVE(
        ActivityStateChangeEntity.State.ACTIVE,
        { (it.tokensArrived == it.activity.startQuantity.toInt())
            && !it.inputSetsNeedProcessing
            && !it.workDone
            && !it.interruptedByError
            && !it.interruptedByNonError
            && !it.withdrawn }),
    WITHDRAWN(
        ActivityStateChangeEntity.State.WITHDRAWN,
        { it.withdrawn }),
    COMPLETING(
        ActivityStateChangeEntity.State.COMPLETING,
        { it.workDone
            && !it.interruptedByError
            && !it.interruptedByNonError
            && !it.withdrawn
            && !it.preCompletionStepsDone }),
    FAILING(
        ActivityStateChangeEntity.State.FAILING,
        { it.interruptedByError
            && !it.interruptedByNonError
            && !it.terminationStepsDone }),
    TERMINATING(
        ActivityStateChangeEntity.State.TERMINATING,
        { it.interruptedByNonError
            && !it.interruptedByError
            && !it.terminationStepsDone }),
    COMPLETED(
        ActivityStateChangeEntity.State.COMPLETED,
        { it.workDone
            && !it.interruptedByError
            && !it.interruptedByNonError
            && !it.withdrawn
            && it.preCompletionStepsDone }),
    COMPENSATING(
        ActivityStateChangeEntity.State.COMPENSATING,
        { false /*todo*/ }),
    FAILED(
        ActivityStateChangeEntity.State.FAILED,
        { it.interruptedByError
            && !it.interruptedByNonError
            && it.terminationStepsDone }),
    TERMINATED(
        ActivityStateChangeEntity.State.TERMINATED,
        { it.interruptedByNonError
            && !it.interruptedByError
            && it.terminationStepsDone }),
    COMPENSATED(
        ActivityStateChangeEntity.State.COMPENSATED,
        { false /*todo*/ });

    companion object {
      val byEntityState = values().associateBy { it.entityState }
    }
  }

  @Retention(AnnotationRetention.RUNTIME)
  @Target(AnnotationTarget.FUNCTION)
  internal annotation class StateChange

  open class StateChangeInterceptor(
      private val activityStateChangeRepository: CrudRepository<ActivityStateChangeEntity, UUID>,
      private val entityFactory: EntityFactory,
      private val processInstanceEntityId: UUID,
      private val clock: Clock,
      private val activityStateChangeHandlingQueue: ActivityStateChangeHandlingQueue
  ): MethodInterceptor {

    override fun intercept(obj: Any,
                           method: Method,
                           args: Array<out Any>,
                           proxy: MethodProxy): Any? {

      if (logger.isTraceEnabled)
        logger.trace("intercepted: ${obj::class.java}, $method, $args, $proxy")

      val l = obj as ActivityLifecycle
      val priorState: State? = if (!l.initialized) null else l.state()

      val ret: Any? = proxy.invokeSuper(obj, args)

      val stateChange = method.getAnnotationsByType(StateChange::class.java)
          .firstOrNull()
      if (stateChange != null) {
        val newState = l.state()
        activityStateChangeRepository.save(
            entityFactory.activityStateChangeEntity(
                UUID.randomUUID(),
                processInstanceEntityId,
                l.activity.id,
                l.fromFlowElementKey,
                Instant.now(clock),
                l.lifecycleId,
                newState.entityState,
                l.tokensArrived, l.inputSetsNeedProcessing,
                l.withdrawn, l.workDone,
                l.interruptedByError, l.interruptedByNonError,
                l.preCompletionStepsDone, l.terminationStepsDone))

        activityStateChangeHandlingQueue.queue(ActivityStateChange(
            processInstanceEntityId,
            l.activity.id,
            l.lifecycleId,
            priorState?.entityState, newState.entityState))
      }

      return ret
    }
  }
}