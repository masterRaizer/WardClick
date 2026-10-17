package com.wardclick.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wardclick.app.data.model.CreateBookingRequest
import com.wardclick.app.data.model.ListingDetailsDto
import com.wardclick.app.data.model.VipServiceDto
import com.wardclick.app.data.repository.ApiResult
import com.wardclick.app.data.repository.BookingRepository
import com.wardclick.app.data.repository.CartItem
import com.wardclick.app.data.repository.CartRepository
import com.wardclick.app.data.repository.ListingRepository
import com.wardclick.app.util.Pricing
import com.wardclick.app.util.nightsText
import com.wardclick.app.util.toLocalDateOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import java.time.temporal.ChronoUnit

const val MAX_NIGHTS = 30

data class BookingUiState(
    val isLoading: Boolean = true,
    val loadError: String? = null,
    val listing: ListingDetailsDto? = null,
    val blocked: Set<LocalDate> = emptySet(),
    val checkIn: LocalDate? = null,
    val checkOut: LocalDate? = null,
    val guests: Int = 1,
    /** VIP-услуги с ценами ЭТОГО особняка. */
    val vipServices: List<VipServiceDto> = emptyList(),
    val selectedVip: Set<String> = emptySet(),
    val isSubmitting: Boolean = false,
    val submitError: String? = null,
    val success: Boolean = false,
    val addedToCart: Boolean = false
) {
    val nights: Int
        get() = if (checkIn != null && checkOut != null) {
            ChronoUnit.DAYS.between(checkIn, checkOut).toInt()
        } else 0

    fun isVipAvailable(s: VipServiceDto): Boolean = Pricing.isAvailable(listing, s)

    val availableVip: List<VipServiceDto>
        get() = vipServices.filter { isVipAvailable(it) }

    val chosenVip: List<VipServiceDto>
        get() = vipServices.filter { it.id in selectedVip && isVipAvailable(it) }

    /** Выбраны все доступные услуги — пакет «Всё включено». */
    val isAllInclusive: Boolean
        get() = availableVip.isNotEmpty() && availableVip.all { it.id in selectedVip }

    /** Расчёт по правилам ценообразования (одиночная аренда). */
    val quote: Pricing.Quote?
        get() = listing?.let { Pricing.quote(it, nights.toLong(), chosenVip, availableVip, multi = false) }

    val stayTotal: BigDecimal get() = quote?.stay ?: BigDecimal.ZERO
    val vipTotal: BigDecimal get() = quote?.vip ?: BigDecimal.ZERO
    val total: BigDecimal get() = quote?.total ?: BigDecimal.ZERO

    /** Сообщение о том, почему аренду пока нельзя отправить (null — всё в порядке). */
    val validationError: String?
        get() {
            val l = listing ?: return "Данные особняка не загружены"
            if (checkIn == null || checkOut == null) return null // просто кнопка неактивна
            if (nights <= 0) return "Дата выезда должна быть позже даты заезда"
            if (nights > MAX_NIGHTS) return "Максимальный срок аренды — ${nightsText(MAX_NIGHTS)}"
            if (checkIn.isBefore(LocalDate.now())) return "Дата заезда не может быть в прошлом"
            if (guests < 1 || guests > l.maxGuests) return "Количество гостей: от 1 до ${l.maxGuests}"
            var d: LocalDate = checkIn
            while (d.isBefore(checkOut)) {
                if (d in blocked) return "В выбранном периоде есть занятые даты"
                d = d.plusDays(1)
            }
            return null
        }

    val canSubmit: Boolean
        get() = listing != null && checkIn != null && checkOut != null &&
            nights > 0 && validationError == null && !isSubmitting
}

class BookingViewModel(
    private val listingRepository: ListingRepository,
    private val bookingRepository: BookingRepository,
    private val cartRepository: CartRepository
) : ViewModel() {

    private val _state = MutableStateFlow(BookingUiState())
    val state: StateFlow<BookingUiState> = _state.asStateFlow()

    /** Сколько особняков уже в заказе — для подсказки о скидке. */
    val cartItems = cartRepository.items

    private var currentId: String? = null

    fun load(id: String, force: Boolean = false) {
        if (!force && currentId == id && _state.value.listing != null) return
        currentId = id
        _state.value = BookingUiState(isLoading = true)
        viewModelScope.launch {
            when (val details = listingRepository.getListingDetails(id)) {
                is ApiResult.Success -> {
                    val blocked = loadBlocked(id)
                    val vip = (listingRepository.getListingVipServices(id) as? ApiResult.Success)?.data.orEmpty()
                    _state.value = BookingUiState(isLoading = false, listing = details.data, blocked = blocked, vipServices = vip)
                }
                is ApiResult.Error -> _state.value = BookingUiState(isLoading = false, loadError = details.message)
                is ApiResult.Exception -> _state.value = BookingUiState(isLoading = false, loadError = details.message)
            }
        }
    }

    private suspend fun loadBlocked(id: String): Set<LocalDate> {
        val res = listingRepository.getListingAvailability(id)
        return if (res is ApiResult.Success) {
            res.data.bookedDates.orEmpty().mapNotNull { it.toLocalDateOrNull() }.toSet()
        } else emptySet()
    }

    fun setDates(checkIn: LocalDate?, checkOut: LocalDate?) {
        _state.update { it.copy(checkIn = checkIn, checkOut = checkOut, submitError = null) }
    }

    fun setGuests(value: Int) {
        val max = _state.value.listing?.maxGuests ?: 1
        _state.update { it.copy(guests = value.coerceIn(1, max), submitError = null) }
    }

    fun toggleVip(id: String) {
        val s = _state.value
        val service = s.vipServices.find { it.id == id } ?: return
        if (!s.isVipAvailable(service)) return
        _state.update {
            it.copy(
                selectedVip = if (id in it.selectedVip) it.selectedVip - id else it.selectedVip + id,
                submitError = null
            )
        }
    }

    /** Кнопка «Всё включено»: выбрать все доступные услуги (или снять выбор). */
    fun toggleAllInclusive() {
        _state.update { s ->
            if (s.isAllInclusive) s.copy(selectedVip = emptySet())
            else s.copy(selectedVip = s.availableVip.map { it.id }.toSet())
        }
    }

    /** Добавить особняк в общий заказ (можно арендовать несколько сразу). */
    fun addToCart() {
        val s = _state.value
        val listing = s.listing ?: return
        if (!s.canSubmit) return
        val ci = s.checkIn ?: return
        val co = s.checkOut ?: return
        val duplicate = cartRepository.items.value.any {
            it.listing.id == listing.id && ci.isBefore(it.checkOut) && co.isAfter(it.checkIn)
        }
        if (duplicate) {
            _state.update { it.copy(submitError = "Этот особняк уже есть в заказе на пересекающиеся даты") }
            return
        }
        cartRepository.add(
            CartItem(
                listing = listing,
                checkIn = ci,
                checkOut = co,
                guests = s.guests,
                chosenVip = s.chosenVip,
                availableVip = s.availableVip
            )
        )
        _state.update { it.copy(addedToCart = true) }
    }

    fun submit() {
        val s = _state.value
        val listing = s.listing ?: return
        if (!s.canSubmit) return
        _state.update { it.copy(isSubmitting = true, submitError = null) }
        viewModelScope.launch {
            val request = CreateBookingRequest(
                listing.id,
                s.checkIn.toString(),
                s.checkOut.toString(),
                s.guests,
                s.total
            ).apply { vipServiceIds = s.chosenVip.map { it.id } }
            when (val result = bookingRepository.createBooking(request)) {
                is ApiResult.Success -> _state.update { it.copy(isSubmitting = false, success = true) }
                is ApiResult.Error -> {
                    // чаще всего — 409: даты заняли, обновляем календарь занятости
                    val blocked = loadBlocked(listing.id)
                    _state.update { it.copy(isSubmitting = false, submitError = result.message, blocked = blocked) }
                }
                is ApiResult.Exception -> _state.update { it.copy(isSubmitting = false, submitError = result.message) }
            }
        }
    }

    /** Вызывается после перехода на другой экран, чтобы повторное открытие начиналось с нуля. */
    fun reset() {
        currentId = null
        _state.value = BookingUiState()
    }
}
