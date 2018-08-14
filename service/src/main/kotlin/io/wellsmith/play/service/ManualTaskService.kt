package io.wellsmith.play.service

import io.wellsmith.play.engine.ActiveProcessInstanceCache
import io.wellsmith.play.engine.visitor.VisitorsWrapper
import org.springframework.stereotype.Service
import java.util.UUID

@Deprecated("should be more generic")
@Service
class ManualTaskService(private val visitorsWrapper: VisitorsWrapper,
                        private val activeProcessInstanceCache: ActiveProcessInstanceCache) {

  fun workIsDone(processInstanceEntityId: UUID, manualTaskId: String) {

    val processInstance = activeProcessInstanceCache.get(processInstanceEntityId)
    visitorsWrapper.workIsDone(processInstance, manualTaskId)
  }
}