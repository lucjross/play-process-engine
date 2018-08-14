package io.wellsmith.play.engine

import com.google.common.cache.CacheBuilder
import io.wellsmith.play.domain.ActivityStateChangeEntity
import io.wellsmith.play.domain.ProcessInstanceEntity
import io.wellsmith.play.engine.activity.ActivityLifecycle
import io.wellsmith.play.engine.activity.ActivityStateChangeHandlingQueue
import io.wellsmith.play.persistence.api.ActivityStateChangeRepository
import io.wellsmith.play.persistence.api.ElementVisitRepository
import io.wellsmith.play.persistence.api.EntityFactory
import io.wellsmith.play.persistence.api.ProcessInstanceRepository
import org.springframework.data.repository.CrudRepository
import java.time.Clock
import java.time.Duration
import java.util.UUID

class ActiveProcessInstanceCache(
    private val processInstanceRepository: ProcessInstanceRepository<*>,
    private val elementVisitRepository: ElementVisitRepository<*>,
    private val activityStateChangeRepository: ActivityStateChangeRepository<*>,
    private val processCache: ProcessCache,
    private val entityFactory: EntityFactory,
    private val clock: Clock,
    private val activityStateChangeHandlingQueue: ActivityStateChangeHandlingQueue,
    expireAfterAccess: Duration = Duration.ofDays(1L),
    maximumSize: Long = 10_000L) {

  /** key = [io.wellsmith.play.domain.ProcessInstanceEntity.id] */
  private val cache = CacheBuilder.newBuilder()
      .expireAfterAccess(expireAfterAccess)
      .maximumSize(maximumSize)
      .build<UUID, ProcessInstance>()

  fun put(processInstanceEntityId: UUID, processInstance: ProcessInstance) {
    cache.put(processInstanceEntityId, processInstance)
  }

  fun get(processInstanceEntityId: UUID): ProcessInstance {

    return cache.get(processInstanceEntityId) {

      val processInstanceEntity: ProcessInstanceEntity =
          processInstanceRepository.findById(processInstanceEntityId)
              .orElseThrow { IllegalArgumentException() }
      val elementVisits = elementVisitRepository.findByProcessInstanceEntityId(
          processInstanceEntityId)
      val graph = processCache.getGraph(
          processInstanceEntity.bpmn20XMLEntityId, processInstanceEntity.processId)
      val activityStates =
          activityStateChangeRepository.findByProcessInstanceEntityId(processInstanceEntityId)

      ProcessInstance(
          graph,
          processInstanceEntity.processId,
          processInstanceEntity.bpmn20XMLEntityId,
          processInstanceEntityId,
          @Suppress("UNCHECKED_CAST") ActivityLifecycle.StateChangeInterceptor(
              activityStateChangeRepository as CrudRepository<ActivityStateChangeEntity, UUID>,
              entityFactory, processInstanceEntityId,
              clock, activityStateChangeHandlingQueue),
          null, /*todo*/
          null,
          null,
          elementVisits,
          activityStates)
    }
  }
}