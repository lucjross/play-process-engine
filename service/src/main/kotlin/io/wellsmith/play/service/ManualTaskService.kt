package io.wellsmith.play.service

import io.wellsmith.play.engine.ProcessInstance
import io.wellsmith.play.engine.visitor.Visitors
import io.wellsmith.play.persistence.api.ElementVisitRepository
import io.wellsmith.play.persistence.api.ProcessInstanceRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Deprecated("should be more generic")
@Service
class ManualTaskService(private val visitors: Visitors,
                        private val elementVisitRepository: ElementVisitRepository<*>,
                        private val processInstanceRepository: ProcessInstanceRepository<*>,
                        private val processCache: ProcessCache) {

  fun workIsDone(processInstanceEntityId: UUID, manualTaskId: String) {

    val processInstanceEntity =
        processInstanceRepository.findById(processInstanceEntityId)
            .orElseThrow { EntityNotFoundException(processInstanceEntityId.toString()) }
    val elementVisits = elementVisitRepository.findByProcessInstanceEntityId(
        processInstanceEntityId)
    val graph = processCache.getGraph(
        processInstanceEntity.bpmn20XMLEntityId, processInstanceEntity.processId)
    val processInstance = ProcessInstance(
        graph,
        processInstanceEntity.processId,
        processInstanceEntity.bpmn20XMLEntityId,
        processInstanceEntityId,
        null, /*todo*/
        null,
        null,
        elementVisits)

    // todo - all of the above should be cached some way or another

    val futures = visitors.visitorOfManualTask(
        processInstance, manualTaskId).workIsDone()
  }
}