package io.wellsmith.play.engine.visitor

import io.wellsmith.play.domain.ElementVisitEntity
import io.wellsmith.play.engine.FlowNodeVisit
import io.wellsmith.play.engine.PlayEngineConfiguration
import io.wellsmith.play.engine.ProcessInstance
import org.omg.spec.bpmn._20100524.model.TFlowNode
import java.time.Instant
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.Future

abstract class FlowNodeVisitor<T: TFlowNode>(processInstance: ProcessInstance,
                                             playEngineConfiguration: PlayEngineConfiguration,
                                             el: T):
    FlowElementVisitor<T>(processInstance, el) {

  private val elementVisitRepository = playEngineConfiguration.repositoryOf(ElementVisitEntity::class)
  private val entityFactory = playEngineConfiguration.entityFactory
  private val executorService = playEngineConfiguration.executorService
  private val clock = playEngineConfiguration.clock

  override fun visit(): List<Future<*>> {

    val futures = mutableListOf<Future<*>>()
    executorService.submit(Callable<ElementVisitEntity> {
      elementVisitRepository.save(
          entityFactory.elementVisitEntity(
              UUID.randomUUID(),
              processInstance.bpmn20XMLEntityId,
              processInstance.processId,
              processInstance.entityId,
              el.id,
              null,
              Instant.now(clock)))
    }).let { futures.add(it) }

    processInstance.addVisit(FlowNodeVisit(el, Instant.now(clock)))

    return futures
  }
}