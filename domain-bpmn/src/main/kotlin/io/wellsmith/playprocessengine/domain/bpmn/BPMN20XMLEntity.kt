package io.wellsmith.playprocessengine.domain.bpmn

import org.springframework.hateoas.ResourceSupport
import java.util.UUID

data class BPMN20XMLEntity(
    val id: UUID,
    val originalFilename: String,
    val document: String,
    val bundleId: UUID
): ResourceSupport()