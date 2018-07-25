package io.wellsmith.play.service

import io.wellsmith.play.domain.ProcessInstanceEntity
import io.wellsmith.play.engine.FlowElementGraph
import io.wellsmith.play.persistence.api.EntityFactory
import io.wellsmith.play.persistence.api.ProcessInstanceRepository
import io.wellsmith.play.serde.BPMN20Serde
import org.omg.spec.bpmn._20100524.model.TProcess
import org.springframework.stereotype.Service
import java.util.UUID
import java.util.WeakHashMap

@Service
class BPMN20ProcessInstanceService<T: ProcessInstanceEntity>(
    private val bpmn20XMLService: BPMN20XMLService<*>,
    private val bpmn20Serde: BPMN20Serde,
    private val processInstanceRepository: ProcessInstanceRepository<T>,
    private val entityFactory: EntityFactory) {

  /**
   * A map with weak keys used to cache [FlowElementGraph]s.
   * The keys are a pair consisting of a [BPMN20XMLEntity ID][io.wellsmith.play.domain.BPMN20XMLEntity.id]
   * and a [Process ID][org.omg.spec.bpmn._20100524.model.TProcess.id].
   */
  private val bpmn20ProcessGraphCache = WeakHashMap<Pair<UUID, String>, FlowElementGraph>()

  /**
   *
   *
   * @return  A process instance ID
   */
  fun instantiateProcess(bpmn20xmlEntityId: UUID, processId: String): UUID {

    val flowElementGraph = bpmn20ProcessGraphCache.computeIfAbsent(
        bpmn20xmlEntityId to processId) {
      FlowElementGraph(
          bpmn20Serde.deserialize(
              bpmn20XMLService.getDefinitionsXML(bpmn20xmlEntityId)
                  .byteInputStream(Charsets.UTF_8))
              .rootElement
              .find { it.value.id == processId }?.value
              as TProcess?
              ?: throw IllegalStateException(
                  """Process element with ID $processId not found in Definitions element
                    on BPMN20XMLEntity with ID $bpmn20xmlEntityId""".trimIndent()))
    }

    val processInstanceId = UUID.randomUUID()
    @Suppress("UNCHECKED_CAST")
    val processInstanceEntity = processInstanceRepository.save(
        entityFactory.processInstanceEntity(processInstanceId) as T)

    val initialVisitIds = flowElementGraph.allIdsOfFlowNodesToVisitUponProcessInstantiation()
    // todo

    return processInstanceId
  }
}