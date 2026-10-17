package com.wardclick.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wardclick.app.data.model.BookingDto
import com.wardclick.app.data.model.ReviewRequest
import com.wardclick.app.data.repository.ApiResult
import com.wardclick.app.data.repository.BookingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class BookingsState {
    object Loading : BookingsState()
    data class Success(
        val upcoming: List<BookingDto>,
        val completed: List<BookingDto>,
        val cancelled: List<BookingDto>
    ) : BookingsState()
    data class Error(val message: String) : BookingsState()
}

class BookingsViewModel(private val bookingRepository: BookingRepository) : ViewModel() {

    private val _bookingsState = MutableStateFlow<BookingsState>(BookingsState.Loading)
    val bookingsState: StateFlow<BookingsState> = _bookingsState.asStateFlow()

    fun loadBookings() {
        _bookingsState.value = BookingsState.Loading
        viewModelScope.launch {
            when (val result = bookingRepository.getMyBookings()) {
                is ApiResult.Success -> {
                    val allBookings = result.data
                    val upcoming = allBookings.filter { it.status == "CONFIRMED" || it.status == "PENDING" }
                    val completed = allBookings.filter { it.status == "COMPLETED" }
                    val cancelled = allBookings.filter { it.status == "CANCELLED" }
                    _bookingsState.value = BookingsState.Success(upcoming, completed, cancelled)
                }
                is ApiResult.Error -> {
                    _bookingsState.value = BookingsState.Error(result.message)
                }
                is ApiResult.Exception -> {
                    _bookingsState.value = BookingsState.Error(result.message)
                }
            }
        }
    }

    fun cancelBooking(id: String) {
        viewModelScope.launch {
            when (bookingRepository.cancelBooking(id)) {
                is ApiResult.Success -> {
                    loadBookings() // refresh
                }
                else -> {
                    // ignore or handle error
                }
            }
        }
    }

    fun addReview(listingId: String, rating: Int, text: String) {
        viewModelScope.launch {
            val request = ReviewRequest(listingId, rating.toDouble(), text)
            bookingRepository.addReview(listingId, request)
        }
    }
}
