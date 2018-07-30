package io.wellsmith.play.domain

import java.util.UUID

interface BPMN20XMLEntity: Entity {

  val originalFilename: String
  val document: String
  val bundleId: UUID
}