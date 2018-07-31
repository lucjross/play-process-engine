package io.wellsmith.play.domain

interface FlowNodeVisitEntity: ElementVisitEntity {

  val baseElementId: String?

  override fun elementKey() = baseElementId!!
}