package com.wardclick.app.util

import com.wardclick.app.data.model.ListingDetailsDto
import com.wardclick.app.data.model.ListingSummaryDto
import com.wardclick.app.data.model.VipServiceDto
import java.math.BigDecimal

/** Склонение русских существительных: 1 ночь, 2 ночи, 5 ночей. */
fun pluralRu(n: Int, one: String, few: String, many: String): String {
    val abs = Math.abs(n) % 100
    val last = abs % 10
    return when {
        abs in 11..19 -> many
        last == 1 -> one
        last in 2..4 -> few
        else -> many
    }
}

fun nightsText(n: Int): String = "$n ${pluralRu(n, "ночь", "ночи", "ночей")}"
fun guestsText(n: Int): String = "$n ${pluralRu(n, "гость", "гостя", "гостей")}"
fun reviewsText(n: Int): String = "$n ${pluralRu(n, "отзыв", "отзыва", "отзывов")}"
fun bedroomsText(n: Int): String = "$n ${pluralRu(n, "спальня", "спальни", "спален")}"
fun bedsText(n: Int): String = "$n ${pluralRu(n, "кровать", "кровати", "кроватей")}"
fun bathroomsText(n: Int): String = "$n ${pluralRu(n, "ванная", "ванные", "ванных")}"

fun rub(amount: BigDecimal?): String = PriceCalculator.formatPrice(amount)

fun ListingDetailsDto.toSummary(): ListingSummaryDto {
    val d = this
    return ListingSummaryDto().apply {
        id = d.id
        title = d.title
        location = "${d.city.name}, ${d.address}"
        imageUrl = d.imageUrls?.firstOrNull() ?: ""
        pricePerNight = d.pricePerNight
        rating = d.rating
        reviewsCount = d.reviewsCount
        tags = if (d.isSuperhost) listOf("Проверенный владелец") else emptyList()
        cityId = d.city.id
        propertyTypeName = d.propertyType.name
        latitude = d.latitude
        longitude = d.longitude
    }
}

/** Подпись единицы цены VIP-услуги: «в сутки», «за полёт», «за услугу». */
fun VipServiceDto.unitLabel(): String = when (unit) {
    VipServiceDto.UNIT_PER_DAY -> "в сутки"
    VipServiceDto.UNIT_PER_TRIP -> "за полёт"
    else -> "за услугу"
}

/** Стоимость VIP-услуги на весь срок: посуточные умножаются на число ночей. */
fun VipServiceDto.costFor(nights: Long): BigDecimal {
    val p = price ?: BigDecimal.ZERO
    return if (unit == VipServiceDto.UNIT_PER_DAY) p.multiply(BigDecimal.valueOf(maxOf(nights, 1L))) else p
}

/** Есть ли у особняка вертолётная площадка. */
fun ListingDetailsDto.hasHelipad(): Boolean = amenities.orEmpty().any { it.iconCode == "helipad" }
