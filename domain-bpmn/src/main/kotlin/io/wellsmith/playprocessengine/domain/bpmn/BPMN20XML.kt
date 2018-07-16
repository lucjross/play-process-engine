package io.wellsmith.playprocessengine.domain.bpmn

import java.util.UUID

data class BPMN20XML(val originalFilename: String,
                     val document: String,
                     val bundleId: UUID?)