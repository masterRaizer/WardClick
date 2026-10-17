package com.wardclick.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wardclick.app.data.model.CreateBookingRequest
import com.wardclick.app.data.model.CreateOrderRequest
import com.wardclick.app.data.repository.ApiResult
import com.wardclick.app.data.repository.BookingRepository
import com.wardclick.app.data.repository.CartItem
import com.wardclick.app.data.repository.CartRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal

data class CartSummary(
    val stay: BigDecimal,
    val vip: BigDecimal,
    val allInclusiveDiscount: BigDecimal,
    val multiDiscount: BigDecimal
) {
    val total: BigDecimal get() = stay.add(vip).subtract(allInclusiveDiscount).subtract(multiDiscount)
}

data class CartUiState(
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)

/** Заказ из нескольких особняков. */
class CartViewModel(
    private val cartRepository: CartRepository,
    private val bookingRepository: BookingRepository
) : ViewModel() {

    val items: StateFlow<List<CartItem>> = cartRepository.items

    private val _state = MutableStateFlow(CartUiState())
    val state: StateFlow<CartUiState> = _state.asStateFlow()

    fun summary(list: List<CartItem>): CartSummary {
        val multi = list.size >= 2
        val quotes = list.map { it.quote(multi) }
        return CartSummary(
            stay = quotes.fold(BigDecimal.ZERO) { a, q -> a.add(q.stay) },
            vip = quotes.fold(BigDecimal.ZERO) { a, q -> a.add(q.vip) },
            allInclusiveDiscount = quotes.fold(BigDecimal.ZERO) { a, q -> a.add(q.allInclusiveDiscount) },
            multiDiscount = quotes.fold(BigDecimal.ZERO) { a, q -> a.add(q.multiDiscount) }
        )
    }

    fun remove(key: String) {
        cartRepository.remove(key)
        _state.update { it.copy(error = null) }
    }

    fun checkout() {
        val list = items.value
        if (list.isEmpty() || _state.value.isSubmitting) return
        _state.update { it.copy(isSubmitting = true, error = null) }
        viewModelScope.launch {
            val multi = list.size >= 2
            val request = CreateOrderRequest(
                list.map { item ->
                    CreateBookingRequest(
                        item.listing.id,
                        item.checkIn.toString(),
                        item.checkOut.toString(),
                        item.guests,
                        item.quote(multi).total
                    ).apply { vipServiceIds = item.chosenVip.map { it.id } }
                }
            )
            when (val r = bookingRepository.createOrder(request)) {
                is ApiResult.Success -> {
                    cartRepository.clear()
                    _state.update { it.copy(isSubmitting = false, success = true) }
                }
                is ApiResult.Error -> _state.update { it.copy(isSubmitting = false, error = r.message) }
                is ApiResult.Exception -> _state.update { it.copy(isSubmitting = false, error = r.message) }
            }
        }
    }

    fun consumeSuccess() = _state.update { it.copy(success = false) }
}
