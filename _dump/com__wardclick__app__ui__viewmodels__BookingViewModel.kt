package com.wardclick.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wardclick.app.data.model.CreateBookingRequest
import com.wardclick.app.data.repository.ApiResult
import com.wardclick.app.data.repository.BookingRepository
import com.wardclick.app.data.repository.ListingRepository
import com.wardclick.app.util.PriceCalculator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

sealed class AvailabilityState {
    object Idle : AvailabilityState()
    object Checking : AvailabilityState()
    data class Available(val totalPrice: BigDecimal, val formattedPrice: String) : AvailabilityState()
    data class Unavailable(val reason: String) : AvailabilityState()
    data class Error(val message: String) : AvailabilityState()
}

sealed class BookingActionState {
    object Idle : BookingActionState()
    object Loading : BookingActionState()
    object Success : BookingActionState()
    data class Error(val message: String) : BookingActionState()
}

class BookingViewModel(
    private val listingRepository: ListingRepository,
    private val bookingRepository: BookingRepository
) : ViewModel() {

    private val _availabilityState = MutableStateFlow<AvailabilityState>(AvailabilityState.Idle)
    val availabilityState: StateFlow<AvailabilityState> = _availabilityState.asStateFlow()

    private val _bookingActionState = MutableStateFlow<BookingActionState>(BookingActionState.Idle)
    val bookingActionState: StateFlow<BookingActionState> = _bookingActionState.asStateFlow()

    private var checkJob: Job? = null
    
    fun checkAvailability(listingId: String, checkIn: String, checkOut: String, guests: Int, pricePerNight: BigDecimal) {
        checkJob?.cancel()
        
        // Simple validation
        if (checkIn.isBlank() || checkOut.isBlank() || guests <= 0) {
            _availabilityState.value = AvailabilityState.Idle
            return
        }

        checkJob = viewModelScope.launch {
            _availabilityState.value = AvailabilityState.Checking
            delay(300) // Debounce
            
            // Note: the backend check logic goes here, but we'll use local calculation for price
            when (val result = listingRepository.getListingAvailability(listingId)) {
                is ApiResult.Success -> {
                    // For this mock let's assume it's available if we get success response.
                    // Calculate days
                    try {
                        val formatter = DateTimeFormatter.ISO_LOCAL_DATE
                        val d1 = LocalDate.parse(checkIn, formatter)
                        val d2 = LocalDate.parse(checkOut, formatter)
                        val nights = ChronoUnit.DAYS.between(d1, d2)
                        if (nights > 0) {
                            val totalPrice = PriceCalculator.calculateTotalPrice(pricePerNight, nights)
                            val formattedPrice = PriceCalculator.formatPrice(totalPrice)
                            _availabilityState.value = AvailabilityState.Available(totalPrice, formattedPrice)
                        } else {
                            _availabilityState.value = AvailabilityState.Unavailable("Неверные даты")
                        }
                    } catch (e: Exception) {
                        _availabilityState.value = AvailabilityState.Error("Ошибка расчета дат")
                    }
                }
                is ApiResult.Error -> {
                    _availabilityState.value = AvailabilityState.Unavailable(result.message)
                }
                is ApiResult.Exception -> {
                    _availabilityState.value = AvailabilityState.Error(result.message)
                }
            }
        }
    }

    fun createBooking(listingId: String, checkIn: String, checkOut: String, guests: Int, totalPrice: BigDecimal) {
        _bookingActionState.value = BookingActionState.Loading
        viewModelScope.launch {
            val request = CreateBookingRequest(listingId, checkIn, checkOut, guests, totalPrice)
            when (val result = bookingRepository.createBooking(request)) {
                is ApiResult.Success -> {
                    _bookingActionState.value = BookingActionState.Success
                }
                is ApiResult.Error -> {
                    _bookingActionState.value = BookingActionState.Error(result.message)
                }
                is ApiResult.Exception -> {
                    _bookingActionState.value = BookingActionState.Error(result.message)
                }
            }
        }
    }
}
