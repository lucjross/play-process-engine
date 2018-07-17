package io.wellsmith.playprocessengine.service

import io.wellsmith.playprocessengine.domain.BPMN20XMLEntity
import io.wellsmith.playprocessengine.persistence.api.BPMN20XMLRepository
import io.wellsmith.playprocessengine.persistence.api.EntityFactory
import io.wellsmith.playprocessengine.serde.BPMN20Serde
import io.wellsmith.playprocessengine.service.command.BPMN20XML
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
   * Placeholder for creating a BPMN2.0 XML bundle
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
   * Placeholder for retrieving the TDefinitions IDs in a BPMN2.0 XML bundle
   */
  fun getDefinitionsIdsInBundle(bundleId: UUID): Collection<UUID> =
      bpmn20XMLRepository.findByBundleId(bundleId)
          .map { it.id }

  /**
   * Placeholder for retrieving [BPMN20XMLEntity.document] XML by entity ID
   */
  fun getDefinitionsXML(id: UUID): String = bpmn20XMLRepository.findById(id)
      .map { it.document }
      .orElseThrow { EntityNotFoundException("$id") }
}