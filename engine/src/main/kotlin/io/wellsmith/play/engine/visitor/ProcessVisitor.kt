package io.wellsmith.play.engine.visitor

import io.wellsmith.play.domain.ProcessInstanceEntity
import io.wellsmith.play.engine.PlayEngineConfiguration
import io.wellsmith.play.engine.ProcessInstance
import org.omg.spec.bpmn._20100524.model.TFlowElement
import org.omg.spec.bpmn._20100524.model.TProcess
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.Future

internal class ProcessVisitor(
    processInstance: ProcessInstance,
    playEngineConfiguration: PlayEngineConfiguration,
    private val visitors: Visitors,
    el: TProcess
): BaseElementVisitor<TProcess>(processInstance, el) {

  private val processInstanceRepository = playEngineConfiguration.repositoryOf(ProcessInstanceEntity::class)
  private val entityFactory = playEngineConfiguration.entityFactory
  private val executorService = playEngineConfiguration.executorService
  private val clock = playEngineConfiguration.clock

  override fun visit(fromFlowElement: TFlowElement?): List<Future<*>> {

    val futures = mutableListOf<Future<*>>()
    executorService.submit(Callable<ProcessInstanceEntity> {
      processInstanceRepository.save(
          entityFactory.processInstanceEntity(
              processInstance.entityId,
              processInstance.bpmn20XMLEntityId,
              processInstance.processId))
    }).let { futures.add(it) }

    val instantiationVisitIds =
        processInstance.graph.allIdsOfFlowNodesToVisitUponProcessInstantiation()
    instantiationVisitIds.forEach {
      visitors.visitorOf(processInstance, it).visit(null)
          .forEach { futures.add(it) }
    }

    return futures
  }
}