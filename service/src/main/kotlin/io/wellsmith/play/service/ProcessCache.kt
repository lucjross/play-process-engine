package io.wellsmith.play.service

import io.wellsmith.play.engine.FlowElementGraph
import io.wellsmith.play.serde.BPMN20Serde
import org.omg.spec.bpmn._20100524.model.TProcess
import org.springframework.stereotype.Component
import java.util.UUID
import java.util.WeakHashMap

@Component
class ProcessCache(private val bpmn20XMLService: BPMN20XMLService<*>,
                   private val bpmn20Serde: BPMN20Serde) {

  /**
   * A map with weak keys used to cache [FlowElementGraph]s.
   * The keys are a pair consisting of a [BPMN20XMLEntity ID][io.wellsmith.play.domain.BPMN20XMLEntity.id]
   * and a [Process ID][org.omg.spec.bpmn._20100524.model.TProcess.id].
   */
  private val bpmn20ProcessGraphCache = WeakHashMap<Pair<UUID, String>, FlowElementGraph>()

  internal fun getGraph(bpmn20xmlEntityId: UUID, processId: String) =
      bpmn20ProcessGraphCache.computeIfAbsent(
          bpmn20xmlEntityId to processId) {
        FlowElementGraph(
            bpmn20Serde.deserialize(
                bpmn20XMLService.getDefinitionsXML(bpmn20xmlEntityId)
                    .byteInputStream(Charsets.UTF_8))
                .rootElement
                .find { it.value.id == processId }?.value
                as TProcess?
                ?: throw IllegalStateException("""
                  Process element with ID $processId not found in Definitions element
                  on BPMN20XMLEntity with ID $bpmn20xmlEntityId
                  """.trimIndent()))
      }
}