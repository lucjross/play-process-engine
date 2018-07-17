package io.wellsmith.playprocessengine.restapp.entity

import org.springframework.hateoas.ResourceSupport
import java.util.UUID

data class BPMN20XMLEntity(
    override val id: UUID,
    override val originalFilename: String,
    override val document: String,
    override val bundleId: UUID
): ResourceSupport(), io.wellsmith.playprocessengine.domain.BPMN20XMLEntity