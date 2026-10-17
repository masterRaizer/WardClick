package com.wardclick.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wardclick.app.data.model.BookingDto
import com.wardclick.app.data.model.CityDto
import com.wardclick.app.data.model.ListingRequest
import com.wardclick.app.data.model.ListingSummaryDto
import com.wardclick.app.data.model.PropertyTypeDto
import com.wardclick.app.data.model.StatsDto
import com.wardclick.app.data.repository.AdminRepository
import com.wardclick.app.data.repository.ApiResult
import com.wardclick.app.data.repository.ListingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val stats: StatsDto? = null,
    val bookings: List<BookingDto> = emptyList(),
    val listings: List<ListingSummaryDto> = emptyList(),
    val cities: List<CityDto> = emptyList(),
    val types: List<PropertyTypeDto> = emptyList(),
    val busyId: String? = null,
    val message: String? = null
)

class AdminViewModel(
    private val adminRepository: AdminRepository,
    private val listingRepository: ListingRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AdminUiState())
    val state: StateFlow<AdminUiState> = _state.asStateFlow()

    fun loadAll(silent: Boolean = false) {
        if (!silent) _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            var firstError: String? = null
            val stats = adminRepository.getStats()
            val bookings = adminRepository.getAllBookings()
            val listings = listingRepository.getListings()
            val cities = listingRepository.getCities()
            val types = listingRepository.getPropertyTypes()

            fun errorOf(r: ApiResult<*>): String? = when (r) {
                is ApiResult.Error -> r.message
                is ApiResult.Exception -> r.message
                else -> null
            }
            firstError = errorOf(stats) ?: errorOf(bookings) ?: errorOf(listings)

            _state.update {
                it.copy(
                    isLoading = false,
                    error = firstError,
                    stats = (stats as? ApiResult.Success)?.data ?: it.stats,
                    bookings = (bookings as? ApiResult.Success)?.data ?: it.bookings,
                    listings = (listings as? ApiResult.Success)?.data ?: it.listings,
                    cities = (cities as? ApiResult.Success)?.data ?: it.cities,
                    types = (types as? ApiResult.Success)?.data ?: it.types
                )
            }
        }
    }

    fun consumeMessage() = _state.update { it.copy(message = null) }

    fun confirmBooking(id: String) = runAction(id, "Аренда подтверждена") { adminRepository.confirmBooking(id) }

    fun cancelBooking(id: String) = runAction(id, "Аренда отменена") { adminRepository.cancelAdminBooking(id) }

    fun hideListing(id: String) = runAction(id, "Особняк скрыт из поиска") { adminRepository.hideListing(id) }

    fun createListing(request: ListingRequest, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            when (val result = adminRepository.createListing(request)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(message = "Особняк добавлен") }
                    loadAll(silent = true)
                    onDone(true)
                }
                is ApiResult.Error -> {
                    _state.update { it.copy(message = result.message) }
                    onDone(false)
                }
                is ApiResult.Exception -> {
                    _state.update { it.copy(message = result.message) }
                    onDone(false)
                }
            }
        }
    }

    private fun <T> runAction(id: String, okMessage: String, call: suspend () -> ApiResult<T>) {
        if (_state.value.busyId != null) return
        _state.update { it.copy(busyId = id) }
        viewModelScope.launch {
            when (val result = call()) {
                is ApiResult.Success -> {
                    _state.update { it.copy(busyId = null, message = okMessage) }
                    loadAll(silent = true)
                }
                is ApiResult.Error -> _state.update { it.copy(busyId = null, message = result.message) }
                is ApiResult.Exception -> _state.update { it.copy(busyId = null, message = result.message) }
            }
        }
    }
}
