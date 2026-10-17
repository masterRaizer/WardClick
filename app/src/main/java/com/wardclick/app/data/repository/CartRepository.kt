package com.wardclick.app.data.repository

import com.wardclick.app.data.model.ListingDetailsDto
import com.wardclick.app.data.model.VipServiceDto
import com.wardclick.app.util.Pricing
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID

/** Позиция заказа: один особняк со своими датами, гостями и VIP-услугами. */
data class CartItem(
    val key: String = UUID.randomUUID().toString(),
    val listing: ListingDetailsDto,
    val checkIn: LocalDate,
    val checkOut: LocalDate,
    val guests: Int,
    val chosenVip: List<VipServiceDto>,
    val availableVip: List<VipServiceDto>
) {
    val nights: Long get() = ChronoUnit.DAYS.between(checkIn, checkOut)

    /** Расчёт с учётом того, сколько особняков в заказе. */
    fun quote(multi: Boolean): Pricing.Quote =
        Pricing.quote(listing, nights, chosenVip, availableVip, multi)
}

/** Заказ (корзина) из нескольких особняков. Хранится в памяти, пока приложение открыто. */
class CartRepository {
    private val _items = MutableStateFlow<List<CartItem>>(emptyList())
    val items: StateFlow<List<CartItem>> = _items.asStateFlow()

    fun add(item: CartItem) = _items.update { it + item }
    fun remove(key: String) = _items.update { list -> list.filterNot { it.key == key } }
    fun clear() { _items.value = emptyList() }
}
