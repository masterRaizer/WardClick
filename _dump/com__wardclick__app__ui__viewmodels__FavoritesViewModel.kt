package com.wardclick.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wardclick.app.data.model.ListingDetailsDto
import com.wardclick.app.data.repository.ApiResult
import com.wardclick.app.data.repository.FavoriteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class FavoritesState {
    object Loading : FavoritesState()
    data class Success(val favorites: List<ListingDetailsDto>) : FavoritesState()
    data class Error(val message: String) : FavoritesState()
}

class FavoritesViewModel(private val favoriteRepository: FavoriteRepository) : ViewModel() {
    private val _favoritesState = MutableStateFlow<FavoritesState>(FavoritesState.Loading)
    val favoritesState: StateFlow<FavoritesState> = _favoritesState.asStateFlow()

    fun loadFavorites() {
        _favoritesState.value = FavoritesState.Loading
        viewModelScope.launch {
            when (val result = favoriteRepository.getFavorites()) {
                is ApiResult.Success -> {
                    _favoritesState.value = FavoritesState.Success(result.data)
                }
                is ApiResult.Error -> {
                    _favoritesState.value = FavoritesState.Error(result.message)
                }
                is ApiResult.Exception -> {
                    _favoritesState.value = FavoritesState.Error(result.message)
                }
            }
        }
    }

    fun removeFavorite(listingId: String) {
        favoriteRepository.toggleFavorite(listingId)
        loadFavorites() // Refresh list
    }
}
