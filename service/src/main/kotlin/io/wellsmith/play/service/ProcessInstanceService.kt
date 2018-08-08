package io.wellsmith.play.service

import io.wellsmith.play.engine.ProcessInstance
import io.wellsmith.play.engine.visitor.Visitors
import io.wellsmith.play.persistence.api.ElementVisitRepository
import io.wellsmith.play.persistence.api.ProcessInstanceRepository
import io.wellsmith.play.service.response.InstantiatedProcess
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ProcessInstanceService(
    private val processInstanceRepository: ProcessInstanceRepository<*>,
    private val processCache: ProcessCache,
    private val visitors: Visitors,
    private val elementVisitRepository: ElementVisitRepository<*>) {

  /**
   * @return  A process instance entity ID
   */
  fun instantiateProcess(bpmn20xmlEntityId: UUID, processId: String): InstantiatedProcess {

    val flowElementGraph = processCache.getGraph(bpmn20xmlEntityId, processId)

    val processInstanceEntityId = UUID.randomUUID()
    val processInstance = ProcessInstance(flowElementGraph, processId, bpmn20xmlEntityId,
        processInstanceEntityId, null, null, null, null)

    // todo - Visitors should be internal to the engine.
    // make a wrapper that doesn't allow arbitrary visit calls
    val futures = visitors.visitorOf(processInstance).visit(null)
    // todo - deal with futures (expose exceptions)

    return InstantiatedProcess(bpmn20xmlEntityId, processId, processInstanceEntityId)
  }

  fun isCompleted(processInstanceEntityId: UUID): Boolean {

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

    return processInstance.isCompleted()
  }
}