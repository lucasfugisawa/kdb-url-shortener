package dev.kotlinbr.utlshortener.testutils

import java.time.Clock
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

object TestClockUtils {
    val fixedClock: Clock = Clock.fixed(Instant.parse("2020-01-01T00:00:00Z"), ZoneOffset.UTC)

    fun now(): OffsetDateTime = OffsetDateTime.now(fixedClock)
}
