package com.wardclick.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wardclick.app.data.model.BookingDto
import com.wardclick.app.data.model.ReviewRequest
import com.wardclick.app.data.repository.ApiResult
import com.wardclick.app.data.repository.BookingRepository
import com.wardclick.app.util.BookingUiStatus
import com.wardclick.app.util.toLocalDateOrNull
import com.wardclick.app.util.uiStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class BookingsTab(val title: String) {
    UPCOMING("Предстоящие"),
    COMPLETED("Завершённые"),
    CANCELLED("Отменённые")
}

data class BookingsUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val bookings: List<BookingDto> = emptyList(),
    val tab: BookingsTab = BookingsTab.UPCOMING,
    val busyId: String? = null,
    val message: String? = null
) {
    fun forTab(t: BookingsTab): List<BookingDto> {
        val list = when (t) {
            BookingsTab.UPCOMING -> bookings.filter {
                val s = it.uiStatus()
                s == BookingUiStatus.PENDING || s == BookingUiStatus.CONFIRMED
            }
            BookingsTab.COMPLETED -> bookings.filter { it.uiStatus() == BookingUiStatus.COMPLETED }
            BookingsTab.CANCELLED -> bookings.filter { it.uiStatus() == BookingUiStatus.CANCELLED }
        }
        return list.sortedBy { it.checkInDate.toLocalDateOrNull() }.let {
            if (t == BookingsTab.UPCOMING) it else it.reversed()
        }
    }
}

class BookingsViewModel(private val bookingRepository: BookingRepository) : ViewModel() {

    private val _state = MutableStateFlow(BookingsUiState())
    val state: StateFlow<BookingsUiState> = _state.asStateFlow()

    fun load(silent: Boolean = false) {
        if (!silent) _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = bookingRepository.getMyBookings()) {
                is ApiResult.Success -> _state.update {
                    it.copy(isLoading = false, error = null, bookings = result.data)
                }
                is ApiResult.Error -> _state.update { it.copy(isLoading = false, error = result.message) }
                is ApiResult.Exception -> _state.update { it.copy(isLoading = false, error = result.message) }
            }
        }
    }

    fun selectTab(tab: BookingsTab) = _state.update { it.copy(tab = tab) }

    fun consumeMessage() = _state.update { it.copy(message = null) }

    fun cancel(id: String) {
        if (_state.value.busyId != null) return
        _state.update { it.copy(busyId = id) }
        viewModelScope.launch {
            when (val result = bookingRepository.cancelBooking(id)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(busyId = null, message = "Аренда отменена") }
                    load(silent = true)
                }
                is ApiResult.Error -> _state.update { it.copy(busyId = null, message = result.message) }
                is ApiResult.Exception -> _state.update { it.copy(busyId = null, message = result.message) }
            }
        }
    }

    fun addReview(booking: BookingDto, rating: Int, comment: String) {
        if (_state.value.busyId != null) return
        _state.update { it.copy(busyId = booking.id) }
        viewModelScope.launch {
            val request = ReviewRequest(booking.listingId, rating.toDouble(), comment.trim())
            when (val result = bookingRepository.addReview(booking.listingId, request)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(busyId = null, message = "Спасибо! Отзыв опубликован") }
                    load(silent = true)
                }
                is ApiResult.Error -> _state.update { it.copy(busyId = null, message = result.message) }
                is ApiResult.Exception -> _state.update { it.copy(busyId = null, message = result.message) }
            }
        }
    }
}
