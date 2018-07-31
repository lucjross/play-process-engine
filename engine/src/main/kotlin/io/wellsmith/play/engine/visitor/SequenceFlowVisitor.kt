package io.wellsmith.play.engine.visitor

import io.wellsmith.play.domain.ElementVisitEntity
import io.wellsmith.play.engine.FlowElementVisit
import io.wellsmith.play.engine.PlayEngineConfiguration
import io.wellsmith.play.engine.ProcessInstance
import io.wellsmith.play.engine.compliance.Compliant
import io.wellsmith.play.engine.compliance.Level
import io.wellsmith.play.engine.compliance.Spec
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
                                   el: TSequenceFlow):
    FlowElementVisitor<TSequenceFlow>(processInstance, playEngineConfiguration, el) {

  private val executorService = playEngineConfiguration.executorService
  private val elementVisitRepository = playEngineConfiguration.repositoryOf(ElementVisitEntity::class)
  private val entityFactory = playEngineConfiguration.entityFactory
  private val clock = playEngineConfiguration.clock

  override fun visit(): List<Future<*>> {

    val futures = mutableListOf<Future<*>>()
    super.visit().let { futures.addAll(it) }

    executorService.submit(Callable<ElementVisitEntity> {
      elementVisitRepository.save(
          entityFactory.elementVisitEntity(
              UUID.randomUUID(),
              processInstance.bpmn20XMLEntityId,
              processInstance.processId,
              processInstance.entityId,
              el.id,
              (el.sourceRef as TFlowNode).id,
              (el.targetRef as TFlowNode).id,
              null,
              Instant.now(clock)))
    }).let { futures.add(it) }

    if (el.conditionExpression != null) {
      TODO()
    }

    visitors.visitorOf(processInstance, el.targetRef as TFlowNode).visit()
        .let { futures.addAll(it) }

    return futures
  }
}