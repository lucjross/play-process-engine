package io.wellsmith.play.engine

import io.wellsmith.play.serde.BPMN20Serde
import org.omg.spec.bpmn._20100524.model.TDefinitions

private val bpmn20Serde = BPMN20Serde()

fun definitionsFromResource(name: String): TDefinitions {
  val xmlInputStream =
      Thread.currentThread().contextClassLoader.getResourceAsStream(name)
  return bpmn20Serde.deserialize(xmlInputStream)
}