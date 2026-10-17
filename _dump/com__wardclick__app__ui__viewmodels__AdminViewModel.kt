package com.wardclick.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wardclick.app.data.model.BookingDto
import com.wardclick.app.data.model.ListingDetailsDto
import com.wardclick.app.data.model.ListingRequest
import com.wardclick.app.data.model.StatsDto
import com.wardclick.app.data.repository.AdminRepository
import com.wardclick.app.data.repository.ApiResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AdminStatsState {
    object Loading : AdminStatsState()
    data class Success(val stats: StatsDto) : AdminStatsState()
    data class Error(val message: String) : AdminStatsState()
}

sealed class AdminBookingsState {
    object Loading : AdminBookingsState()
    data class Success(val bookings: List<BookingDto>) : AdminBookingsState()
    data class Error(val message: String) : AdminBookingsState()
}

class AdminViewModel(private val repository: AdminRepository) : ViewModel() {

    private val _statsState = MutableStateFlow<AdminStatsState>(AdminStatsState.Loading)
    val statsState: StateFlow<AdminStatsState> = _statsState.asStateFlow()

    private val _bookingsState = MutableStateFlow<AdminBookingsState>(AdminBookingsState.Loading)
    val bookingsState: StateFlow<AdminBookingsState> = _bookingsState.asStateFlow()

    fun loadStats() {
        _statsState.value = AdminStatsState.Loading
        viewModelScope.launch {
            when (val result = repository.getStats()) {
                is ApiResult.Success -> {
                    _statsState.value = AdminStatsState.Success(result.data)
                }
                is ApiResult.Error -> {
                    _statsState.value = AdminStatsState.Error(result.message)
                }
                is ApiResult.Exception -> {
                    _statsState.value = AdminStatsState.Error(result.message)
                }
            }
        }
    }

    fun loadBookings() {
        _bookingsState.value = AdminBookingsState.Loading
        viewModelScope.launch {
            when (val result = repository.getAllBookings()) {
                is ApiResult.Success -> {
                    _bookingsState.value = AdminBookingsState.Success(result.data)
                }
                is ApiResult.Error -> {
                    _bookingsState.value = AdminBookingsState.Error(result.message)
                }
                is ApiResult.Exception -> {
                    _bookingsState.value = AdminBookingsState.Error(result.message)
                }
            }
        }
    }

    fun confirmBooking(id: String) {
        viewModelScope.launch {
            when (repository.confirmBooking(id)) {
                is ApiResult.Success -> {
                    loadBookings() // refresh
                }
                else -> {
                    // handle error
                }
            }
        }
    }

    fun cancelBooking(id: String) {
        viewModelScope.launch {
            when (repository.cancelAdminBooking(id)) {
                is ApiResult.Success -> {
                    loadBookings() // refresh
                }
                else -> {
                    // handle error
                }
            }
        }
    }

    fun createListing(request: ListingRequest, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            when (val result = repository.createListing(request)) {
                is ApiResult.Success -> {
                    onResult(true, null)
                }
                is ApiResult.Error -> {
                    onResult(false, result.message)
                }
                is ApiResult.Exception -> {
                    onResult(false, result.message)
                }
            }
        }
    }

    fun updateListing(id: String, request: ListingRequest, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            when (val result = repository.updateListing(id, request)) {
                is ApiResult.Success -> {
                    onResult(true, null)
                }
                is ApiResult.Error -> {
                    onResult(false, result.message)
                }
                is ApiResult.Exception -> {
                    onResult(false, result.message)
                }
            }
        }
    }

    fun hideListing(id: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            when (val result = repository.hideListing(id)) {
                is ApiResult.Success -> {
                    onResult(true, null)
                }
                is ApiResult.Error -> {
                    onResult(false, result.message)
                }
                is ApiResult.Exception -> {
                    onResult(false, result.message)
                }
            }
        }
    }
}
