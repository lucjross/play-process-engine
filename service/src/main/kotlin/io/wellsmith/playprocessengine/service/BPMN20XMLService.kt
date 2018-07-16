package io.wellsmith.playprocessengine.service

import io.wellsmith.playprocessengine.domain.bpmn.BPMN20XML
import io.wellsmith.playprocessengine.domain.bpmn.BPMN20XMLEntity
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class BPMN20XMLService(val bpmn20BundleCache: MutableCollection<BPMN20XMLEntity>) {

  /**
   * Placeholder for creating a BPMN2.0 XML bundle
   *
   * @return  new bundle ID
   */
  fun createBundle(bundle: Collection<BPMN20XML>): UUID {

    val bundleId = UUID.randomUUID()
    bpmn20BundleCache.addAll(bundle.map {
      BPMN20XMLEntity(UUID.randomUUID(), it.originalFilename, it.document, bundleId)
    })

    return bundleId
  }

  /**
   * Placeholder for retrieving the TDefinitions IDs in a BPMN2.0 XML bundle
   */
  fun getDefinitionsIdsInBundle(bundleId: UUID): Collection<UUID> =
      bpmn20BundleCache.filter { it.bundleId == bundleId }.map { it.id }

  /**
   * Placeholder for retrieving [BPMN20XMLEntity.document] XML by entity ID
   */
  fun getDefinitionsXML(id: UUID) = bpmn20BundleCache.find { it.id == id }?.document
        ?: throw EntityNotFoundException("$id")
}