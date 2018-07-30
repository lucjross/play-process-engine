package io.wellsmith.play.domain

import java.time.Instant
import java.util.UUID

interface ElementVisitEntity: Entity {

  val bpmn20XMLEntityId: UUID
  val processId: String
  val processInstanceEntityId: UUID
  val baseElementId: String?
  val fromFlowNodeId: String?
  val time: Instant
}