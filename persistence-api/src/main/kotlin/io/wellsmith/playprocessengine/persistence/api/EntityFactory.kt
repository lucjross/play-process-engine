package io.wellsmith.playprocessengine.persistence.api

import io.wellsmith.playprocessengine.domain.BPMN20XMLEntity
import java.util.UUID

interface EntityFactory {

  fun bpmn20XMLEntity(id: UUID,
                      originalFilename: String,
                      document: String,
                      bundleId: UUID): BPMN20XMLEntity
}