package io.wellsmith.play.engine.visitor

import io.wellsmith.play.domain.elementKeyOf
import io.wellsmith.play.engine.PlayEngineConfiguration
import io.wellsmith.play.engine.ProcessInstance
import org.omg.spec.bpmn._20100524.model.TEndEvent
import org.omg.spec.bpmn._20100524.model.TFlowElement
import org.omg.spec.bpmn._20100524.model.TManualTask
import org.omg.spec.bpmn._20100524.model.TProcess
import org.omg.spec.bpmn._20100524.model.TSequenceFlow
import org.omg.spec.bpmn._20100524.model.TStartEvent
import org.omg.spec.bpmn._20100524.model.TTask
import java.util.UUID

internal class Visitors(val playEngineConfiguration: PlayEngineConfiguration) {

  fun visitorOf(processInstance: ProcessInstance,
                flowNodeId: String): BaseElementVisitor<*> {
    return visitorOf(processInstance,
        processInstance.graph.flowElementsByKey[flowNodeId]!!)
  }

  @Deprecated("for demo")
  @Suppress("UNCHECKED_CAST")
  fun visitorOfManualTask(processInstance: ProcessInstance,
                          manualTaskId: String): ManualTaskVisitor {
    return ManualTaskVisitor(processInstance, playEngineConfiguration, this,
        processInstance.graph.flowElementsByKey[manualTaskId] as TManualTask)
  }

  @Suppress("UNCHECKED_CAST")
  fun visitorOf(processInstance: ProcessInstance,
                el: TFlowElement,
                splitCorrelationId: UUID? = null): BaseElementVisitor<*> {

    if (splitCorrelationId != null && el !is TSequenceFlow)
      throw IllegalArgumentException("splitCorrelationId applicable only to SequenceFlow visitor")

    return when (el::class) {
      TEndEvent::class ->
        EndEventVisitor(processInstance, playEngineConfiguration, this, el as TEndEvent)
      TManualTask::class ->
        ManualTaskVisitor(processInstance, playEngineConfiguration, this, el as TManualTask)
      TSequenceFlow::class ->
        SequenceFlowVisitor(processInstance, playEngineConfiguration, this, el as TSequenceFlow,
            splitCorrelationId)
      TStartEvent::class ->
        StartEventVisitor(processInstance, playEngineConfiguration, this, el as TStartEvent)
      TTask::class ->
        TaskVisitor(processInstance, playEngineConfiguration, this, el as TTask)
      else ->
        TODO("unimplemented element type ${el::class.simpleName}")
    }
  }

  fun visitorOf(processInstance: ProcessInstance): BaseElementVisitor<TProcess> {
    return ProcessVisitor(processInstance, playEngineConfiguration, this,
        processInstance.graph.process)
  }

  fun visitorOfSequenceFlow(processInstance: ProcessInstance,
                            sourceRefId: String,
                            targetRefId: String): BaseElementVisitor<TSequenceFlow> {
    val sequenceFlow =
        processInstance.graph.flowElementsByKey[
            elementKeyOf(sourceRefId, targetRefId)
        ]!! as TSequenceFlow
    return visitorOf(processInstance, sequenceFlow) as SequenceFlowVisitor
  }
}