package com.wardclick.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wardclick.app.data.model.VipServiceDto
import com.wardclick.app.data.repository.ApiResult
import com.wardclick.app.data.repository.ListingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class VipUiState(
    val isLoading: Boolean = true,
    val services: List<VipServiceDto> = emptyList(),
    val error: String? = null
)

/** Каталог VIP-услуг. */
class VipViewModel(private val listingRepository: ListingRepository) : ViewModel() {

    private val _state = MutableStateFlow(VipUiState())
    val state: StateFlow<VipUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.value = VipUiState(isLoading = true)
        viewModelScope.launch {
            _state.value = when (val r = listingRepository.getVipServices()) {
                is ApiResult.Success -> VipUiState(isLoading = false, services = r.data)
                is ApiResult.Error -> VipUiState(isLoading = false, error = r.message)
                is ApiResult.Exception -> VipUiState(isLoading = false, error = r.message)
            }
        }
    }
}
