package io.wellsmith.play.service.command

import java.util.UUID

data class InstantiateProcess(val bpmn20XMLEntityId: UUID,
                              val processId: String) {
}