package io.wellsmith.play.domain

import java.util.UUID

interface SequenceFlowVisitEntity: ElementVisitEntity {

  val sourceRefId: String?
  val targetRefId: String?
  val splitCorrelationId: UUID?

  override fun elementKey() = elementKeyOf(sourceRefId!!, targetRefId!!)
}

const val ELEMENT_KEY_DELIMITER = "-\uD83C\uDF2E-"
val elementKeyOf = { s1: String, s2: String ->
  s1 + ELEMENT_KEY_DELIMITER + s2
}