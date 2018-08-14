package io.wellsmith.play.engine.visitor

import io.wellsmith.play.domain.ActivityStateChangeEntity
import io.wellsmith.play.engine.activity.ActivityLifecycle
import io.wellsmith.play.engine.PlayEngineConfiguration
import io.wellsmith.play.engine.ProcessInstance
import io.wellsmith.play.engine.compliance.Compliant
import io.wellsmith.play.engine.compliance.Level
import io.wellsmith.play.engine.compliance.Spec
import io.wellsmith.play.engine.elementKey
import net.sf.cglib.proxy.Enhancer
import org.omg.spec.bpmn._20100524.model.TActivity
import org.omg.spec.bpmn._20100524.model.TFlowElement
import java.math.BigInteger
import java.util.UUID

@Compliant(toSpec = Spec.BPMN_2_0, section = "10.2", level = Level.INCOMPLETE)
internal abstract class ActivityVisitor<T: TActivity>(
    processInstance: ProcessInstance,
    private val playEngineConfiguration: PlayEngineConfiguration,
    visitors: Visitors,
    el: T
): FlowNodeVisitor<T>(processInstance, playEngineConfiguration, visitors, el) {

  private val activityStateChangeRepository =
      playEngineConfiguration.repositoryOf(ActivityStateChangeEntity::class)

  override fun visit(fromFlowElement: TFlowElement?) {

    if (el.isIsForCompensation) {
      TODO()
    }

    if (BigInteger.ONE != el.completionQuantity) {
      TODO()
    }

    if (el.ioSpecification != null) {
      // "Not every Activity type defines inputs and outputs, only Tasks,
      // CallableElements (Global Tasks and Processes) MAY define their data requirements"
      // (p. 211)
      TODO()
    }

    // todo - other attributes

    super.visit(fromFlowElement)

    val lifecycles = processInstance.getActivityLifecycles(el)
    var lifecycleToVisit = lifecycles.find {
      it.state() == ActivityLifecycle.State.INITIAL
    }
    if (lifecycleToVisit == null) {
      lifecycleToVisit = newActivityLifecycle(el, processInstance, fromFlowElement)
      processInstance.addActivityLifecycle(el, lifecycleToVisit)
      lifecycleToVisit.initialize()
    }

    lifecycleToVisit.applyToken()
  }

  internal open fun onReady(lifecycle: ActivityLifecycle) {
    // todo - see whether all InputSets are ready
    lifecycle.temp_inputSetsAvailable()
  }

  internal open fun onActive(lifecycle: ActivityLifecycle) {}

  internal open fun onCompleting(lifecycle: ActivityLifecycle) {
    // todo - wait for... ???
    lifecycle.completePreCompletionSteps()
  }

  internal open fun onCompleted(lifecycle: ActivityLifecycle) {

    // "The outgoing Sequence Flows becomes active and a number of tokens,
    // indicated by the attribute CompletionQuantity, is placed on it"
    repeat(el.completionQuantity.toInt()) {
      visitNextSequenceFlows()
    }

    // todo - Data Outputs
  }

  internal open fun completeWork() {
    // todo - this should only be allowed for ManualTask

    val lifecycles = processInstance.getActivityLifecycles(el)
    var lifecycle = lifecycles.find {
      it.state() == ActivityLifecycle.State.ACTIVE
    } ?: throw IllegalStateException("Cannot complete work with no activity in Active state." +
        " lifecycles=${lifecycles.joinToString()}")

    lifecycle.completeWork()
  }

  /**
   * Invokes [ActivityLifecycle] primary constructor
   */
  internal fun newActivityLifecycle(activity: TActivity,
                                    processInstance: ProcessInstance,
                                    fromFlowElement: TFlowElement?): ActivityLifecycle {
    val enhancer = Enhancer()
    enhancer.setSuperclass(ActivityLifecycle::class.java)
    enhancer.setCallback(processInstance.stateChangeInterceptor)
    val lifecycleId = UUID.randomUUID()
    return enhancer.create(
        arrayOf(UUID::class.java, TActivity::class.java, String::class.java),
        arrayOf(lifecycleId, activity, fromFlowElement?.elementKey())
    ) as ActivityLifecycle
  }
}
