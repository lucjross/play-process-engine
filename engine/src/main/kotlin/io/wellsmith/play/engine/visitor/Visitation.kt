package io.wellsmith.play.engine.visitor

import java.util.UUID

data class Visitation(val elementKey: String,
                      val originatingVisitorClassSimpleName: String?,
                      val processInstanceEntityId: UUID,
                      val work: () -> Unit)