package io.wellsmith.play.service

import io.wellsmith.play.domain.BPMN20XMLEntity
import io.wellsmith.play.persistence.api.BPMN20XMLRepository
import io.wellsmith.play.persistence.api.EntityFactory
import io.wellsmith.play.serde.BPMN20Serde
import io.wellsmith.play.service.command.BPMN20XML
import io.wellsmith.play.service.response.ProcessIdToBPMN20XMLEntityId
import org.omg.spec.bpmn._20100524.model.TProcess
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class BPMN20XMLService<T: BPMN20XMLEntity>(val bpmn20XMLRepository: BPMN20XMLRepository<T>,
                                           val entityFactory: EntityFactory,
                                           val bpmn20Serde: BPMN20Serde) {

  companion object {
    private val logger = LoggerFactory.getLogger(BPMN20XMLService::class.java)
  }

  /**
   * Creates a BPMN2.0 XML bundle
   *
   * @return  new bundle ID
   * @throws  org.springframework.oxm.XmlMappingException
   */
  fun createBundle(bundle: Collection<BPMN20XML>): UUID {

    val bundleId = UUID.randomUUID()
    bpmn20XMLRepository.saveAll(bundle.map {

      // validate xml is BPMN
      val tDefinitions = bpmn20Serde.deserialize(it.document.byteInputStream(Charsets.UTF_8))
      if (logger.isDebugEnabled) {
        val xml = bpmn20Serde.serialize(tDefinitions).toString(Charsets.UTF_8.name())
        logger.debug("deserialized, then serialized:\n$xml")
      }

      @Suppress("UNCHECKED_CAST")
      entityFactory.bpmn20XMLEntity(
          UUID.randomUUID(), it.originalFilename, it.document, bundleId) as T
    })

    return bundleId
  }

  /**
   * Gets the [entity IDs][BPMN20XMLEntity.id] in a BPMN2.0 XML bundle
   */
  fun getDefinitionsIdsInBundle(bundleId: UUID): Collection<UUID> =
      bpmn20XMLRepository.findByBundleId(bundleId)
          .map { it.id }

  fun getProcessIdsByBPMN20XMLEntityId(bundleId: UUID): Collection<ProcessIdToBPMN20XMLEntityId> =
      bpmn20XMLRepository.findByBundleId(bundleId)
          .flatMap { bpmn20XmlEntity ->
            bpmn20Serde.deserialize(bpmn20XmlEntity.document.byteInputStream(Charsets.UTF_8))
                .rootElement
                .filter { it.value is TProcess }
                .map { ProcessIdToBPMN20XMLEntityId(it.value.id, bpmn20XmlEntity.id) }
          }

  /**
   * Gets [Definitions XML][BPMN20XMLEntity.document] by entity ID
   *
   * @throws  EntityNotFoundException
   */
  fun getDefinitionsXML(id: UUID): String = bpmn20XMLRepository.findById(id)
      .map { it.document }
      .orElseThrow { EntityNotFoundException("$id") }
}