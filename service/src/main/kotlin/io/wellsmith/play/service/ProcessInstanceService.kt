package io.wellsmith.play.service

import io.wellsmith.play.domain.ActivityStateChangeEntity
import io.wellsmith.play.engine.ActiveProcessInstanceCache
import io.wellsmith.play.engine.PlayEngineConfiguration
import io.wellsmith.play.engine.ProcessCache
import io.wellsmith.play.engine.ProcessInstance
import io.wellsmith.play.engine.activity.ActivityLifecycle
import io.wellsmith.play.engine.visitor.VisitorsWrapper
import io.wellsmith.play.persistence.api.ActivityStateChangeRepository
import io.wellsmith.play.service.response.InstantiatedProcess
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ProcessInstanceService(
    private val processCache: ProcessCache,
    private val visitorsWrapper: VisitorsWrapper,
    private val activeProcessInstanceCache: ActiveProcessInstanceCache,
    private val activityStateChangeRepository: ActivityStateChangeRepository<*>,
    playEngineConfiguration: PlayEngineConfiguration) {

  private val entityFactory = playEngineConfiguration.entityFactory
  private val clock = playEngineConfiguration.clock
  private val activityStateChangeHandlingQueue = playEngineConfiguration.activityStateChangeQueue()
  private val synchronizer = playEngineConfiguration.synchronizer()

  /**
   * @return  A process instance entity ID
   */
  fun instantiateProcess(bpmn20xmlEntityId: UUID, processId: String): InstantiatedProcess {

    val flowElementGraph = processCache.getGraph(bpmn20xmlEntityId, processId)

    val processInstanceEntityId = UUID.randomUUID()
    val processInstance = ProcessInstance(flowElementGraph, processId, bpmn20xmlEntityId,
        processInstanceEntityId,
        @Suppress("UNCHECKED_CAST") ActivityLifecycle.StateChangeInterceptor(
            activityStateChangeRepository as CrudRepository<ActivityStateChangeEntity, UUID>,
            entityFactory, processInstanceEntityId, clock, activityStateChangeHandlingQueue),
        null, null, null, null, null)

    visitorsWrapper.visitProcess(processInstance)

    activeProcessInstanceCache.put(processInstanceEntityId, processInstance)

    return InstantiatedProcess(bpmn20xmlEntityId, processId, processInstanceEntityId)
  }

  fun isCompleted(processInstanceEntityId: UUID): Boolean {

    val processInstance = activeProcessInstanceCache.get(processInstanceEntityId)

    synchronizer.lock("processInstance.isCompleted()")
    try {
      return processInstance.isCompleted()
    }
    finally {
      synchronizer.unlock("processInstance.isCompleted()")
    }
  }
}