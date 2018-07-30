package io.wellsmith.play.restapp.controller

import io.wellsmith.play.serde.BPMN20Serde
import org.omg.spec.bpmn._20100524.model.TDefinitions
import java.io.ByteArrayOutputStream

private val bpmn20Serde = BPMN20Serde()

fun definitionsFromResource(name: String): TDefinitions {
  val xmlInputStream =
      Thread.currentThread().contextClassLoader.getResourceAsStream(name)
  return bpmn20Serde.deserialize(xmlInputStream)
}

fun byteArrayOutputStreamOf(s: String) =
    s.toByteArray(Charsets.UTF_8)
        .let { byteArray -> ByteArrayOutputStream(byteArray.size)
            .apply { write(byteArray, 0, byteArray.size) }
        }