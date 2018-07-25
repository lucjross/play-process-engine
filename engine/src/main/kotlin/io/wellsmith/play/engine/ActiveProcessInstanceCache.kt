package io.wellsmith.play.engine

import com.google.common.cache.CacheBuilder
import java.time.Duration
import java.util.UUID

// todo
class ActiveProcessInstanceCache(expireAfterAccess: Duration = Duration.ofDays(1L),
                                 maximumSize: Long = 10_000L) {

  /** key = [io.wellsmith.play.domain.ProcessInstanceEntity.id] */
  private val cache = CacheBuilder.newBuilder()
      .expireAfterAccess(expireAfterAccess)
      .maximumSize(maximumSize)
      .build<UUID, ProcessInstance>()

  fun put(processInstanceEntityId: UUID, processInstance: ProcessInstance) {
    cache.put(processInstanceEntityId, processInstance)
  }
}