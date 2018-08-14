package io.wellsmith.play.engine

import io.wellsmith.play.persistence.api.BPMN20XMLRepository
import io.wellsmith.play.serde.BPMN20Serde
import org.omg.spec.bpmn._20100524.model.TProcess
import java.util.UUID
import java.util.WeakHashMap

class ProcessCache(private val bpmn20XMLRepository: BPMN20XMLRepository<*>,
                   private val bpmn20Serde: BPMN20Serde) {

  /**
   * A map with weak keys used to cache [FlowElementGraph]s.
   * The keys are a pair consisting of a [BPMN20XMLEntity ID][io.wellsmith.play.domain.BPMN20XMLEntity.id]
   * and a [Process ID][org.omg.spec.bpmn._20100524.model.TProcess.id].
   */
  private val bpmn20ProcessGraphCache = WeakHashMap<Pair<UUID, String>, FlowElementGraph>()

  fun getGraph(bpmn20xmlEntityId: UUID, processId: String): FlowElementGraph =
      bpmn20ProcessGraphCache.computeIfAbsent(
          bpmn20xmlEntityId to processId) {
        FlowElementGraph(
            bpmn20Serde.deserialize(
                bpmn20XMLRepository.findById(bpmn20xmlEntityId)
                    .map { it.document }
                    .orElseThrow { IllegalStateException(
                        "Entity not found for bpmn20XMLEntityId $bpmn20xmlEntityId") }
                    .byteInputStream(Charsets.UTF_8))
                .rootElement
                .find { it.value.id == processId }
                ?.value as TProcess?
                ?: throw IllegalStateException("""
                  Process element with ID $processId not found in Definitions element
                  on BPMN20XMLEntity with ID $bpmn20xmlEntityId
                  """.trimIndent()))
      }
}