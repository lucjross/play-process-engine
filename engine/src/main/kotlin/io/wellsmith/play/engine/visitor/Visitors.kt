package io.wellsmith.play.engine.visitor

import io.wellsmith.play.engine.PlayEngineConfiguration
import io.wellsmith.play.engine.ProcessInstance
import org.omg.spec.bpmn._20100524.model.TEndEvent
import org.omg.spec.bpmn._20100524.model.TFlowNode
import org.omg.spec.bpmn._20100524.model.TManualTask
import org.omg.spec.bpmn._20100524.model.TProcess
import org.omg.spec.bpmn._20100524.model.TSequenceFlow
import org.omg.spec.bpmn._20100524.model.TStartEvent
import org.omg.spec.bpmn._20100524.model.TTask

class Visitors(val playEngineConfiguration: PlayEngineConfiguration) {

  fun visitorOf(processInstance: ProcessInstance,
                flowNodeId: String): BaseElementVisitor<*> {
    return visitorOf(processInstance,
        processInstance.graph.flowNodesById[flowNodeId]!!)
  }

  @Deprecated("for demo")
  @Suppress("UNCHECKED_CAST")
  fun visitorOfManualTask(processInstance: ProcessInstance,
                          manualTaskId: String): ManualTaskVisitor {
    return ManualTaskVisitor(processInstance, playEngineConfiguration, this,
        processInstance.graph.flowNodesById[manualTaskId] as TManualTask)
  }

  @Suppress("UNCHECKED_CAST")
  fun visitorOf(processInstance: ProcessInstance,
                el: TFlowNode): BaseElementVisitor<*> {

    return when (el::class) {
      TEndEvent::class ->
        EndEventVisitor(processInstance, playEngineConfiguration, el as TEndEvent)
      TManualTask::class ->
        ManualTaskVisitor(processInstance, playEngineConfiguration, this, el as TManualTask)
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
    val sequenceFlow = processInstance.graph.sequenceFlowsByTargetRef.values
        .flatten()
        .find {
          (it.sourceRef as TFlowNode).id == sourceRefId &&
              (it.targetRef as TFlowNode).id == targetRefId
        }!!
    return visitorOfSequenceFlow(processInstance, sequenceFlow)
  }

  fun visitorOfSequenceFlow(processInstance: ProcessInstance,
                            sequenceFlow: TSequenceFlow): BaseElementVisitor<TSequenceFlow> {
    return SequenceFlowVisitor(processInstance, sequenceFlow, this)
  }
}