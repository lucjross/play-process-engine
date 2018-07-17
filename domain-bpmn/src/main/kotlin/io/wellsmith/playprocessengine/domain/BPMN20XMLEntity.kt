package io.wellsmith.playprocessengine.domain

import java.util.UUID

interface BPMN20XMLEntity {

  val id: UUID
  val originalFilename: String
  val document: String
  val bundleId: UUID
}