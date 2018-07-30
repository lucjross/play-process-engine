package io.wellsmith.play.service.response

import java.util.UUID

data class InstantiatedProcess(val bpmn20XMLEntityId: UUID,
                               val processId: String,
                               val processInstanceEntityId: UUID)