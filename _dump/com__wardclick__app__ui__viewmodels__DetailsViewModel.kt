package com.wardclick.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wardclick.app.data.model.ListingDetailsDto
import com.wardclick.app.data.repository.ApiResult
import com.wardclick.app.data.repository.FavoriteRepository
import com.wardclick.app.data.repository.ListingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class DetailsState {
    object Loading : DetailsState()
    data class Success(val listing: ListingDetailsDto, val isFavorite: Boolean) : DetailsState()
    data class Error(val message: String) : DetailsState()
}

class DetailsViewModel(
    private val listingRepository: ListingRepository,
    private val favoriteRepository: FavoriteRepository
) : ViewModel() {

    private val _detailsState = MutableStateFlow<DetailsState>(DetailsState.Loading)
    val detailsState: StateFlow<DetailsState> = _detailsState.asStateFlow()

    fun loadDetails(id: String) {
        _detailsState.value = DetailsState.Loading
        viewModelScope.launch {
            when (val result = listingRepository.getListingDetails(id)) {
                is ApiResult.Success -> {
                    val isFav = favoriteRepository.isFavorite(id)
                    _detailsState.value = DetailsState.Success(result.data, isFav)
                }
                is ApiResult.Error -> {
                    _detailsState.value = DetailsState.Error(result.message)
                }
                is ApiResult.Exception -> {
                    _detailsState.value = DetailsState.Error(result.message)
                }
            }
        }
    }

    fun toggleFavorite() {
        val currentState = _detailsState.value
        if (currentState is DetailsState.Success) {
            val id = currentState.listing.id
            val newFavStatus = favoriteRepository.toggleFavorite(id)
            _detailsState.value = currentState.copy(isFavorite = newFavStatus)
        }
    }
}
