package com.wardclick.app.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

private val DISPLAY = DateTimeFormatter.ofPattern("dd.MM.yyyy")

/** DatePicker Material 3 хранит дату как миллисекунды UTC (полночь по UTC). */
fun Long.utcMillisToLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

fun LocalDate.toUtcMillis(): Long =
    atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

fun LocalDate.toDisplay(): String = format(DISPLAY)

fun String?.toLocalDateOrNull(): LocalDate? {
    if (this.isNullOrBlank()) return null
    return try {
        LocalDate.parse(this.trim())
    } catch (e: DateTimeParseException) {
        null
    }
}

/** "2025-06-01" -> "01.06.2025" */
fun String?.isoToDisplay(): String = this.toLocalDateOrNull()?.toDisplay() ?: (this ?: "")
