package com.wardclick.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wardclick.app.data.model.CityDto
import com.wardclick.app.data.model.ListingSummaryDto
import com.wardclick.app.data.model.PropertyTypeDto
import com.wardclick.app.data.repository.ApiResult
import com.wardclick.app.data.repository.FavoriteRepository
import com.wardclick.app.data.repository.ListingRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class ViewMode { LIST, MAP }

enum class SortMode(val label: String) {
    DEFAULT("Рекомендуемые"),
    PRICE_ASC("Сначала дешевле"),
    PRICE_DESC("Сначала дороже"),
    RATING("По рейтингу")
}

/** Сводка по городу для экрана выбора города. */
data class CityInfo(
    val city: CityDto,
    val count: Int,
    val minPrice: java.math.BigDecimal?,
    val imageUrl: String?
)

data class HomeUiState(
    /** Пользователь уже выбрал город (или «Все города») — показываем список особняков. */
    val cityChosen: Boolean = false,
    val cityInfos: List<CityInfo> = emptyList(),
    val query: String = "",
    val cityId: String? = null,
    val typeId: String? = null,
    val guests: Int = 1,
    val checkIn: LocalDate? = null,
    val checkOut: LocalDate? = null,
    val sort: SortMode = SortMode.DEFAULT,
    val viewMode: ViewMode = ViewMode.LIST,
    val selectedId: String? = null,
    val cities: List<CityDto> = emptyList(),
    val types: List<PropertyTypeDto> = emptyList(),
    val listings: List<ListingSummaryDto> = emptyList(),
    val favorites: Set<String> = emptySet(),
    val isLoading: Boolean = true,
    val error: String? = null
) {
    val selected: ListingSummaryDto? get() = listings.firstOrNull { it.id == selectedId }

    val cityName: String get() = cities.firstOrNull { it.id == cityId }?.name ?: "Все города"

    /** Сколько фильтров (кроме текстового поиска и города) включено. */
    val activeFilters: Int
        get() = listOf(
            typeId != null,
            guests > 1,
            checkIn != null && checkOut != null,
            sort != SortMode.DEFAULT
        ).count { it }
}

class HomeViewModel(
    private val listingRepository: ListingRepository,
    private val favoriteRepository: FavoriteRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState(favorites = favoriteRepository.getFavoriteIds().toSet()))
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadMeta()
    }

    private fun loadMeta() {
        viewModelScope.launch {
            val cities = listingRepository.getCities()
            if (cities is ApiResult.Success) _state.update { it.copy(cities = cities.data) }
            val types = listingRepository.getPropertyTypes()
            if (types is ApiResult.Success) _state.update { it.copy(types = types.data) }
            loadCityInfos()
        }
    }

    /** Считает, сколько особняков в каждом городе и от какой цены — для экрана выбора города. */
    private suspend fun loadCityInfos() {
        _state.update { it.copy(isLoading = true, error = null) }
        when (val all = listingRepository.getListings(null, null, null, null, null, null)) {
            is ApiResult.Success -> {
                val byCity = all.data.groupBy { it.cityId }
                val infos = _state.value.cities.mapNotNull { city ->
                    val list = byCity[city.id].orEmpty()
                    if (list.isEmpty()) null // города без особняков не показываем
                    else CityInfo(
                        city = city,
                        count = list.size,
                        minPrice = list.mapNotNull { it.pricePerNight }.minOrNull(),
                        imageUrl = list.maxByOrNull { it.rating }?.imageUrl
                    )
                }
                _state.update { it.copy(cityInfos = infos, isLoading = false) }
            }
            is ApiResult.Error -> _state.update { it.copy(isLoading = false, error = all.message) }
            is ApiResult.Exception -> _state.update { it.copy(isLoading = false, error = all.message) }
        }
    }

    /** Первый шаг: выбор города (null — все города). */
    fun chooseCity(cityId: String?) {
        _state.update { it.copy(cityChosen = true, cityId = cityId, selectedId = null, query = "") }
        load()
    }

    /** Вернуться к выбору города. */
    fun changeCity() {
        _state.update { it.copy(cityChosen = false, selectedId = null) }
        viewModelScope.launch { loadCityInfos() }
    }

    fun refreshFavorites() {
        _state.update { it.copy(favorites = favoriteRepository.getFavoriteIds().toSet()) }
    }

    fun toggleFavorite(id: String) {
        favoriteRepository.toggleFavorite(id)
        refreshFavorites()
    }

    fun onQueryChange(value: String) {
        _state.update { it.copy(query = value) }
        load(delayMs = 400) // не дёргаем «сервер» на каждую букву
    }

    fun onCitySelected(cityId: String?) {
        _state.update { it.copy(cityId = cityId, selectedId = null) }
        load()
    }

    fun applyFilters(
        typeId: String?,
        guests: Int,
        checkIn: LocalDate?,
        checkOut: LocalDate?,
        sort: SortMode
    ) {
        _state.update {
            it.copy(
                typeId = typeId,
                guests = guests,
                checkIn = checkIn,
                checkOut = checkOut,
                sort = sort,
                selectedId = null
            )
        }
        load()
    }

    fun resetFilters() {
        _state.update {
            it.copy(
                query = "", typeId = null, guests = 1,
                checkIn = null, checkOut = null, sort = SortMode.DEFAULT, selectedId = null
            )
        }
        load()
    }

    fun setViewMode(mode: ViewMode) {
        _state.update { it.copy(viewMode = mode) }
    }

    fun select(id: String?) {
        _state.update { it.copy(selectedId = id) }
    }

    fun retry() {
        if (_state.value.cityChosen) load() else if (_state.value.cities.isEmpty()) loadMeta()
        else viewModelScope.launch { loadCityInfos() }
    }

    private fun sorted(list: List<ListingSummaryDto>, sort: SortMode): List<ListingSummaryDto> = when (sort) {
        SortMode.DEFAULT -> list
        SortMode.PRICE_ASC -> list.sortedBy { it.pricePerNight }
        SortMode.PRICE_DESC -> list.sortedByDescending { it.pricePerNight }
        SortMode.RATING -> list.sortedByDescending { it.rating }
    }

    private fun load(delayMs: Long = 0) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            if (delayMs > 0) delay(delayMs)
            _state.update { it.copy(isLoading = true, error = null) }
            val s = _state.value
            val result = listingRepository.getListings(
                cityId = s.cityId,
                typeId = s.typeId,
                guests = s.guests.takeIf { it > 1 },
                checkIn = s.checkIn?.toString(),
                checkOut = s.checkOut?.toString(),
                query = s.query.trim().ifBlank { null }
            )
            when (result) {
                is ApiResult.Success -> _state.update {
                    it.copy(
                        listings = sorted(result.data, it.sort),
                        isLoading = false,
                        error = null,
                        selectedId = it.selectedId?.takeIf { sel -> result.data.any { l -> l.id == sel } }
                    )
                }
                is ApiResult.Error -> _state.update { it.copy(isLoading = false, error = result.message) }
                is ApiResult.Exception -> _state.update { it.copy(isLoading = false, error = result.message) }
            }
        }
    }
}
