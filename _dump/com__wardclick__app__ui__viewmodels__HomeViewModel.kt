package com.wardclick.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wardclick.app.data.model.ListingSummaryDto
import com.wardclick.app.data.repository.ApiResult
import com.wardclick.app.data.repository.ListingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class HomeState {
    object Idle : HomeState()
    object Loading : HomeState()
    data class Success(val listings: List<ListingSummaryDto>) : HomeState()
    data class Error(val message: String) : HomeState()
}

class HomeViewModel(private val repository: ListingRepository) : ViewModel() {
    private val _homeState = MutableStateFlow<HomeState>(HomeState.Idle)
    val homeState: StateFlow<HomeState> = _homeState.asStateFlow()

    // Pagination and filter states
    private var currentPage = 1
    private var isLastPage = false
    private val currentListings = mutableListOf<ListingSummaryDto>()
    
    private var currentCityId: String? = null
    private var currentTypeId: String? = null
    private var currentGuests: Int? = null

    init {
        loadListings(isRefresh = true)
    }

    fun search(cityId: String?, typeId: String?, guests: Int?) {
        currentCityId = cityId
        currentTypeId = typeId
        currentGuests = guests
        loadListings(isRefresh = true)
    }

    fun loadNextPage() {
        if (!isLastPage && _homeState.value !is HomeState.Loading) {
            currentPage++
            loadListings(isRefresh = false)
        }
    }

    fun loadListings(isRefresh: Boolean) {
        if (isRefresh) {
            currentPage = 1
            isLastPage = false
            currentListings.clear()
        }

        _homeState.value = HomeState.Loading
        viewModelScope.launch {
            val result = repository.getListings(
                cityId = currentCityId,
                typeId = currentTypeId,
                guests = currentGuests
            )
            when (result) {
                is ApiResult.Success -> {
                    // For mock, pagination might not be implemented, so we simulate it
                    val newData = result.data
                    if (newData.isEmpty()) {
                        isLastPage = true
                    } else {
                        currentListings.addAll(newData)
                    }
                    _homeState.value = HomeState.Success(currentListings.toList())
                }
                is ApiResult.Error -> {
                    _homeState.value = HomeState.Error(result.message)
                }
                is ApiResult.Exception -> {
                    _homeState.value = HomeState.Error(result.message)
                }
            }
        }
    }
}
