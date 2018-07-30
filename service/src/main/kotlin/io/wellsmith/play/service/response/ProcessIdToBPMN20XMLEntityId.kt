package io.wellsmith.play.service.response

import java.util.UUID

data class ProcessIdToBPMN20XMLEntityId(val processId: String,
                                        val bpmn20XMLEntityId: UUID)