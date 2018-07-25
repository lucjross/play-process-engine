package io.wellsmith.play.domain

import java.time.Instant
import java.util.UUID

interface FlowNodeVisitEntity {

  val id: UUID
  val bpmn20XMLEntityId: UUID
  val processId: String
  val processInstanceEntityId: UUID
  val flowNodeId: String
  val fromFlowNodeId: String
  val time: Instant
}