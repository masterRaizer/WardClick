package com.wardclick.app.util

import com.wardclick.app.data.model.ListingDetailsDto
import com.wardclick.app.data.model.VipServiceDto
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Ценообразование «ВардКлик».
 *
 * 1. Цена VIP-услуги зависит от особняка: базовая цена × коэффициент особняка (vipFactor),
 *    округлённая до 500 ₽. Коэффициент учитывает класс особняка и расположение
 *    (Рублёвка дороже, шале под Новосибирском дешевле).
 * 2. Посуточные услуги умножаются на число ночей, «за полёт» и разовые — нет.
 * 3. «Всё включено» — выбраны все VIP-услуги, доступные в особняке:
 *    −15 % на VIP-услуги и −5 % на проживание.
 * 4. Заказ из двух и более особняков: −7 % на проживание в каждом.
 */
object Pricing {
    const val ALL_INCLUSIVE_VIP_PERCENT = 15
    const val ALL_INCLUSIVE_STAY_PERCENT = 5
    const val MULTI_STAY_PERCENT = 7

    /** Цена услуги в конкретном особняке. */
    fun servicePrice(base: BigDecimal?, factor: Double): BigDecimal {
        val raw = (base ?: BigDecimal.ZERO).multiply(BigDecimal.valueOf(if (factor > 0) factor else 1.0))
        // округление до 500 ₽
        return raw.divide(BigDecimal(500), 0, RoundingMode.HALF_UP).multiply(BigDecimal(500))
    }

    /** Копия каталога с ценами для особняка. */
    fun pricedFor(listing: ListingDetailsDto, catalog: List<VipServiceDto>): List<VipServiceDto> =
        catalog.map { s ->
            VipServiceDto(s.id, s.name, s.description, s.iconCode, servicePrice(s.price, listing.vipFactor), s.unit, s.isRequiresHelipad)
        }

    fun isAvailable(listing: ListingDetailsDto?, s: VipServiceDto): Boolean =
        !s.isRequiresHelipad || (listing?.hasHelipad() == true)

    data class Quote(
        val stay: BigDecimal,
        val vip: BigDecimal,
        val allInclusive: Boolean,
        val allInclusiveDiscount: BigDecimal,
        val multiDiscount: BigDecimal
    ) {
        val discount: BigDecimal get() = allInclusiveDiscount.add(multiDiscount)
        val total: BigDecimal get() = stay.add(vip).subtract(discount)
    }

    /**
     * Расчёт одной аренды.
     * @param services цены уже для этого особняка (см. [pricedFor]); [available] — все доступные в нём услуги.
     * @param multi в заказе два и более особняка.
     */
    fun quote(
        listing: ListingDetailsDto,
        nights: Long,
        chosen: List<VipServiceDto>,
        available: List<VipServiceDto>,
        multi: Boolean
    ): Quote {
        val stay = (listing.pricePerNight ?: BigDecimal.ZERO).multiply(BigDecimal.valueOf(nights))
        val vip = chosen.fold(BigDecimal.ZERO) { acc, s -> acc.add(s.costFor(nights)) }
        val chosenIds = chosen.map { it.id }.toSet()
        val allInclusive = available.isNotEmpty() && available.all { it.id in chosenIds }
        val aiDiscount = if (allInclusive) {
            percent(vip, ALL_INCLUSIVE_VIP_PERCENT).add(percent(stay, ALL_INCLUSIVE_STAY_PERCENT))
        } else BigDecimal.ZERO
        val multiDiscount = if (multi) percent(stay, MULTI_STAY_PERCENT) else BigDecimal.ZERO
        return Quote(stay, vip, allInclusive, aiDiscount, multiDiscount)
    }

    private fun percent(v: BigDecimal, p: Int): BigDecimal =
        v.multiply(BigDecimal(p)).divide(BigDecimal(100), 0, RoundingMode.HALF_UP)
}
