package com.wardclick.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wardclick.app.data.model.ListingDetailsDto
import com.wardclick.app.data.model.ReviewDto
import com.wardclick.app.data.repository.ApiResult
import com.wardclick.app.data.repository.FavoriteRepository
import com.wardclick.app.data.repository.ListingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DetailsUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val listing: ListingDetailsDto? = null,
    val reviews: List<ReviewDto> = emptyList(),
    val vipServices: List<com.wardclick.app.data.model.VipServiceDto> = emptyList(),
    val isFavorite: Boolean = false
)

class DetailsViewModel(
    private val listingRepository: ListingRepository,
    private val favoriteRepository: FavoriteRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DetailsUiState())
    val state: StateFlow<DetailsUiState> = _state.asStateFlow()

    private var currentId: String? = null

    fun load(id: String, force: Boolean = false) {
        if (!force && currentId == id && _state.value.listing != null) {
            _state.update { it.copy(isFavorite = favoriteRepository.isFavorite(id)) }
            return
        }
        currentId = id
        _state.value = DetailsUiState(isLoading = true)
        viewModelScope.launch {
            when (val result = listingRepository.getListingDetails(id)) {
                is ApiResult.Success -> {
                    val reviews = (listingRepository.getReviews(id) as? ApiResult.Success)?.data.orEmpty()
                    val vip = (listingRepository.getListingVipServices(id) as? ApiResult.Success)?.data.orEmpty()
                    _state.value = DetailsUiState(
                        isLoading = false,
                        listing = result.data,
                        reviews = reviews,
                        vipServices = vip,
                        isFavorite = favoriteRepository.isFavorite(id)
                    )
                }
                is ApiResult.Error -> _state.value = DetailsUiState(isLoading = false, error = result.message)
                is ApiResult.Exception -> _state.value = DetailsUiState(isLoading = false, error = result.message)
            }
        }
    }

    fun retry() {
        currentId?.let { load(it, force = true) }
    }

    fun toggleFavorite() {
        val id = currentId ?: return
        val now = favoriteRepository.toggleFavorite(id)
        _state.update { it.copy(isFavorite = now) }
    }
}
