package io.wellsmith.play.domain

import java.time.Instant
import java.util.UUID

interface ProcessInstanceElementVisitEntity {

  val id: UUID
  val bpmn20XMLEntityId: UUID
  val processId: String
  val processInstanceEntityId: UUID
  val elementId: String
  val time: Instant
}