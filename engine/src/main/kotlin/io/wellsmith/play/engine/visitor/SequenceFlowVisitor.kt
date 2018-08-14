package io.wellsmith.play.engine.visitor

import io.wellsmith.play.domain.ElementVisitEntity
import io.wellsmith.play.engine.FlowElementVisit
import io.wellsmith.play.engine.PlayEngineConfiguration
import io.wellsmith.play.engine.ProcessInstance
import io.wellsmith.play.engine.compliance.Compliant
import io.wellsmith.play.engine.compliance.Level
import io.wellsmith.play.engine.compliance.Spec
import io.wellsmith.play.engine.elementKey
import org.omg.spec.bpmn._20100524.model.TFlowElement
import org.omg.spec.bpmn._20100524.model.TFlowNode
import org.omg.spec.bpmn._20100524.model.TSequenceFlow
import java.time.Instant
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.Future

@Compliant(toSpec = Spec.BPMN_2_0, level = Level.INCOMPLETE)
internal class SequenceFlowVisitor(processInstance: ProcessInstance,
                                   playEngineConfiguration: PlayEngineConfiguration,
                                   private val visitors: Visitors,
                                   el: TSequenceFlow,
                                   private val splitCorrelationId: UUID?):
    FlowElementVisitor<TSequenceFlow>(processInstance, playEngineConfiguration, el) {

  private val executorService = playEngineConfiguration.executorService
  private val elementVisitRepository = playEngineConfiguration.repositoryOf(ElementVisitEntity::class)
  private val visitationQueue = playEngineConfiguration.visitationQueue()
  private val entityFactory = playEngineConfiguration.entityFactory
  private val clock = playEngineConfiguration.clock

  override fun visit(fromFlowElement: TFlowElement?) {

    elementVisitRepository.save(
        entityFactory.elementVisitEntity(
            UUID.randomUUID(),
            processInstance.bpmn20XMLEntityId,
            processInstance.processId,
            processInstance.entityId,
            el.id,
            (el.sourceRef as TFlowNode).id,
            (el.targetRef as TFlowNode).id,
            fromFlowElement?.elementKey(),
            splitCorrelationId,
            Instant.now(clock)))

    processInstance.addVisit(FlowElementVisit(el, Instant.now(clock), fromFlowElement, splitCorrelationId))

    if (el.conditionExpression != null) {
      TODO()
    }

    val targetNode = el.targetRef as TFlowNode
    visitationQueue.queue(Visitation(
        targetNode.elementKey(), this::class.simpleName!!, processInstance.entityId) {
      visitors.visitorOf(processInstance, targetNode).visit(el)
    })
  }
}