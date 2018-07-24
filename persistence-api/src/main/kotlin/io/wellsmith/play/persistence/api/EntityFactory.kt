package io.wellsmith.play.persistence.api

import io.wellsmith.play.domain.BPMN20XMLEntity
import io.wellsmith.play.domain.ProcessInstanceEntity
import java.util.UUID

interface EntityFactory {

  fun bpmn20XMLEntity(id: UUID,
                      originalFilename: String,
                      document: String,
                      bundleId: UUID): BPMN20XMLEntity

  fun processInstanceEntity(id: UUID): ProcessInstanceEntity
}