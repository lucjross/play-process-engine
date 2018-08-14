package io.wellsmith.play.engine.visitor

import io.wellsmith.play.domain.ElementVisitEntity
import io.wellsmith.play.engine.FlowElementVisit
import io.wellsmith.play.engine.PlayEngineConfiguration
import io.wellsmith.play.engine.ProcessInstance
import io.wellsmith.play.engine.elementKey
import org.omg.spec.bpmn._20100524.model.TFlowElement
import org.omg.spec.bpmn._20100524.model.TFlowNode
import java.time.Instant
import java.util.UUID

internal abstract class FlowNodeVisitor<T: TFlowNode>(
    processInstance: ProcessInstance,
    playEngineConfiguration: PlayEngineConfiguration,
    private val visitors: Visitors,
    el: T
): FlowElementVisitor<T>(processInstance, playEngineConfiguration, el) {

  private val elementVisitRepository = playEngineConfiguration.repositoryOf(ElementVisitEntity::class)
  private val visitationQueue = playEngineConfiguration.visitationQueue()
  private val entityFactory = playEngineConfiguration.entityFactory
  private val executorService = playEngineConfiguration.executorService
  protected val clock = playEngineConfiguration.clock

  override fun visit(fromFlowElement: TFlowElement?) {

    elementVisitRepository.save(
        entityFactory.elementVisitEntity(
            UUID.randomUUID(),
            processInstance.bpmn20XMLEntityId,
            processInstance.processId,
            processInstance.entityId,
            el.id,
            null,
            null,
            fromFlowElement?.elementKey(),
            null,
            Instant.now(clock)))

    processInstance.addVisit(FlowElementVisit(el, Instant.now(clock), fromFlowElement, null))
  }

  internal fun visitNextSequenceFlows() {
    val sequenceFlows = processInstance.graph.nextSequenceFlows(el)
    sequenceFlows.forEach {
      visitationQueue.queue(Visitation(
          it.elementKey(), this::class.simpleName!!, processInstance.entityId) {
        visitors.visitorOf(processInstance, it, sequenceFlows.splitCorrelationId).visit(el)
      })
    }
  }
}