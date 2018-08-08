package io.wellsmith.play.engine

fun String.cleanMultiline() = this.trimIndent().replace('\n', ' ')