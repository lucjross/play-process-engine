package io.wellsmith.play.domain

interface SequenceFlowVisitEntity: ElementVisitEntity {

  val sourceRefId: String?
  val targetRefId: String?

  override fun elementKey() = elementKeyOf(sourceRefId!!, targetRefId!!)
}

const val ELEMENT_KEY_DELIMITER = "-\uD83C\uDF2E-"
val elementKeyOf = { s1: String, s2: String ->
  s1 + ELEMENT_KEY_DELIMITER + s2
}