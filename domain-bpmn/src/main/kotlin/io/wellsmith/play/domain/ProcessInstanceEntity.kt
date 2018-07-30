package io.wellsmith.play.domain

import java.util.UUID

interface ProcessInstanceEntity: Entity {

  val bpmn20XMLEntityId: UUID
  val processId: String
}