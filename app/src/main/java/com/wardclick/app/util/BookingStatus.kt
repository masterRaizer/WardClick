package com.wardclick.app.util

import com.wardclick.app.data.model.BookingDto
import java.time.LocalDate

/** Статус брони для интерфейса. COMPLETED вычисляется: подтверждённая бронь с датой выезда в прошлом. */
enum class BookingUiStatus(val label: String) {
    PENDING("Ожидает подтверждения"),
    CONFIRMED("Подтверждено"),
    COMPLETED("Завершено"),
    CANCELLED("Отменено")
}

fun BookingDto.uiStatus(today: LocalDate = LocalDate.now()): BookingUiStatus {
    return when (status) {
        "CANCELLED" -> BookingUiStatus.CANCELLED
        "PENDING" -> {
            val out = checkOutDate.toLocalDateOrNull()
            // неподтверждённая бронь, дата выезда которой прошла, больше не актуальна
            if (out != null && out.isBefore(today)) BookingUiStatus.CANCELLED else BookingUiStatus.PENDING
        }
        "CONFIRMED" -> {
            val out = checkOutDate.toLocalDateOrNull()
            if (out != null && out.isBefore(today)) BookingUiStatus.COMPLETED else BookingUiStatus.CONFIRMED
        }
        else -> BookingUiStatus.PENDING
    }
}
