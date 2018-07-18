package io.wellsmith.play.serde

import java.io.ByteArrayOutputStream
import java.io.InputStream

interface Serde<T> {

  fun serialize(obj: T): ByteArrayOutputStream
  fun deserialize(inputStream: InputStream): T
}