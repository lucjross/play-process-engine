package io.wellsmith.play.engine

import java.time.Clock
import java.time.Instant
import java.time.ZoneId


class NanoClock(private val clock: Clock = Clock.systemUTC()): Clock() {

  private val initialInstant = clock.instant()
  private val initialNanos = System.nanoTime()

  override fun withZone(zone: ZoneId?) = NanoClock(clock.withZone(zone))

  override fun getZone(): ZoneId = clock.zone

  override fun instant(): Instant =
      initialInstant.plusNanos(System.nanoTime() - initialNanos)
}

val defaultNanoClock = NanoClock()

fun now(): Instant = Instant.now(defaultNanoClock)