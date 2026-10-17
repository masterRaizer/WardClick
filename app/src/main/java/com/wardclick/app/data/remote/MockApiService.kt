package com.wardclick.app.data.remote

import com.wardclick.app.util.DemoPhotos
import com.wardclick.app.util.Pricing
import android.content.Context
import com.google.gson.Gson
import com.wardclick.app.data.model.*
import com.wardclick.app.data.repository.ApiException
import kotlinx.coroutines.delay
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * Встроенный "сервер" в памяти (USE_MOCK = true). Состояние (пользователи, объекты,
 * брони, отзывы) сохраняется в SharedPreferences, поэтому переживает перезапуск приложения.
 * Бизнес-правила те же, что и у настоящего REST API.
 */
class MockApiService(
    context: Context,
    private val tokenProvider: () -> String?
) : ApiService {

    private class Snapshot {
        var users: MutableList<AuthResponse> = mutableListOf()
        var passwords: MutableMap<String, String> = mutableMapOf()
        var listings: MutableList<ListingDetailsDto> = mutableListOf()
        var bookings: MutableList<BookingDto> = mutableListOf()
        var reviews: MutableMap<String, MutableList<ReviewDto>> = mutableMapOf()
    }

    private val prefs = context.getSharedPreferences("wardclick_mock", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val cities = listOf(
        CityDto("c1", "Москва"),
        CityDto("c2", "Санкт-Петербург"),
        CityDto("c3", "Сочи"),
        CityDto("c4", "Казань"),
        CityDto("c5", "Екатеринбург"),
        CityDto("c6", "Новосибирск"),
        CityDto("c7", "Калининград"),
        CityDto("c8", "Владивосток")
    )

    private val cityCenters = mapOf(
        "c1" to (55.7558 to 37.6173),
        "c2" to (59.9343 to 30.3351),
        "c3" to (43.5855 to 39.7231),
        "c4" to (55.7887 to 49.1221),
        "c5" to (56.8389 to 60.6057),
        "c6" to (55.0084 to 82.9357),
        "c7" to (54.7104 to 20.4522),
        "c8" to (43.1198 to 131.8869)
    )

    // Типы особняков
    private val propertyTypes = listOf(
        PropertyTypeDto("t1", "Особняк", "mansion"),
        PropertyTypeDto("t2", "Вилла", "villa"),
        PropertyTypeDto("t3", "Усадьба", "estate"),
        PropertyTypeDto("t4", "Шале", "chalet")
    )

    // Удобства особняков (индексы 0..9 используются в демо-данных ниже)
    // VIP-услуги (заказываются вместе с арендой особняка)
    private val vipServices = listOf(
        VipServiceDto("v1", "Вертолёт до особняка", "Перелёт из аэропорта или города прямо на вертолётную площадку особняка.", "helicopter", BigDecimal(350000), VipServiceDto.UNIT_PER_TRIP, true),
        VipServiceDto("v2", "Личный водитель", "Автомобиль бизнес-класса с водителем на всё время отдыха.", "driver", BigDecimal(25000), VipServiceDto.UNIT_PER_DAY, false),
        VipServiceDto("v3", "Консьерж 24/7", "Бронирование ресторанов, билеты, экскурсии и любые поручения в любое время суток.", "concierge", BigDecimal(15000), VipServiceDto.UNIT_PER_DAY, false),
        VipServiceDto("v4", "Шеф-повар", "Завтраки, обеды и ужины в особняке по вашему меню.", "chef", BigDecimal(20000), VipServiceDto.UNIT_PER_DAY, false),
        VipServiceDto("v5", "Горничная", "Ежедневная уборка, смена белья и порядок в доме.", "maid", BigDecimal(8000), VipServiceDto.UNIT_PER_DAY, false),
        VipServiceDto("v6", "Личная охрана", "Сопровождение и охрана гостей во время отдыха.", "guard", BigDecimal(30000), VipServiceDto.UNIT_PER_DAY, false)
    )

    private val allAmenities = listOf(
        AmenityDto("a1", "Wi-Fi", "wifi"),
        AmenityDto("a2", "Бассейн", "pool"),
        AmenityDto("a3", "Баня / сауна", "sauna"),
        AmenityDto("a4", "Охрана", "security"),
        AmenityDto("a5", "Парковка", "parking"),
        AmenityDto("a6", "Кинозал", "cinema"),
        AmenityDto("a7", "Персонал (повар, горничная)", "staff"),
        AmenityDto("a8", "Терраса", "terrace"),
        AmenityDto("a9", "Вертолётная площадка", "helipad"),
        AmenityDto("a10", "Кухня", "kitchen")
    )

    // ВАЖНО: state создаётся ПОСЛЕ справочников выше — seed() использует cities/propertyTypes/amenities
    private val state: Snapshot = loadOrSeed()

    /** Известен ли токен демо-серверу (токены старых версий приложения недействительны). */
    fun isKnownToken(token: String?): Boolean = token != null && state.users.any { it.token == token }

    // ---------------------------------------------------------------- auth

    override suspend fun login(request: LoginRequest): AuthResponse {
        delay(400)
        val email = request.email.orEmpty().trim()
        val user = state.users.find { it.email.equals(email, ignoreCase = true) }
        if (user == null || state.passwords[user.email.lowercase()] != request.password) {
            throw ApiException(401, "Неверный email или пароль")
        }
        return user
    }

    override suspend fun register(request: RegisterRequest): AuthResponse {
        delay(400)
        val email = request.email.orEmpty().trim()
        if (email.isEmpty() || request.password.isNullOrEmpty()) {
            throw ApiException(400, "Заполните email и пароль")
        }
        if (state.users.any { it.email.equals(email, ignoreCase = true) }) {
            throw ApiException(409, "Пользователь с таким email уже существует")
        }
        val user = AuthResponse(
            "mock-token-${UUID.randomUUID()}",
            "user-${UUID.randomUUID().toString().take(8)}",
            email,
            request.fullName.orEmpty().trim(),
            "USER"
        )
        state.users.add(user)
        state.passwords[email.lowercase()] = request.password
        persist()
        return user
    }

    // ---------------------------------------------------------------- справочники

    override suspend fun getCities(): List<CityDto> {
        delay(150)
        return cities
    }

    override suspend fun getPropertyTypes(): List<PropertyTypeDto> {
        delay(150)
        return propertyTypes
    }

    override suspend fun getAmenities(): List<AmenityDto> {
        delay(150)
        return allAmenities
    }

    override suspend fun getVipServices(): List<VipServiceDto> {
        delay(150)
        return vipServices
    }

    // ---------------------------------------------------------------- объекты

    override suspend fun getListings(
        cityId: String?,
        typeId: String?,
        guests: Int?,
        checkIn: String?,
        checkOut: String?,
        query: String?
    ): List<ListingSummaryDto> {
        delay(350)
        var result = state.listings.filter { it.isActive }
        if (!cityId.isNullOrEmpty()) result = result.filter { it.city.id == cityId }
        if (!typeId.isNullOrEmpty()) result = result.filter { it.propertyType.id == typeId }
        if (guests != null) result = result.filter { it.maxGuests >= guests }
        if (!query.isNullOrBlank()) {
            val q = query.trim().lowercase()
            result = result.filter {
                it.title.lowercase().contains(q) ||
                    it.address.lowercase().contains(q) ||
                    it.city.name.lowercase().contains(q) ||
                    it.description.orEmpty().lowercase().contains(q)
            }
        }
        if (!checkIn.isNullOrEmpty() && !checkOut.isNullOrEmpty()) {
            val inD = parseDate(checkIn)
            val outD = parseDate(checkOut)
            result = result.filter { !overlaps(it.id, inD, outD) }
        }
        return result.map { toSummary(it) }
    }

    override suspend fun getFeaturedListings(): List<ListingSummaryDto> {
        delay(250)
        return state.listings.filter { it.isActive }
            .sortedByDescending { it.rating }
            .take(5)
            .map { toSummary(it) }
    }

    override suspend fun getListingDetails(id: String): ListingDetailsDto {
        delay(250)
        return state.listings.find { it.id == id && it.isActive }
            ?: throw ApiException(404, "Особняк не найден")
    }

    override suspend fun getListingAvailability(id: String): AvailabilityDto {
        delay(150)
        val booked = linkedSetOf<String>()
        state.bookings
            .filter { it.listingId == id && (it.status == "PENDING" || it.status == "CONFIRMED") }
            .forEach { b ->
                var d = parseDate(b.checkInDate)
                val end = parseDate(b.checkOutDate)
                while (d.isBefore(end)) { // занятые ночи: [заезд, выезд)
                    booked.add(d.toString())
                    d = d.plusDays(1)
                }
            }
        return AvailabilityDto().apply {
            listingId = id
            bookedDates = booked.toList()
        }
    }

    override suspend fun getListingReviews(id: String): List<ReviewDto> {
        delay(250)
        return state.reviews[id].orEmpty().sortedByDescending { it.createdAt }
    }

    override suspend fun addReview(id: String, request: ReviewRequest): ReviewDto {
        delay(350)
        val user = currentUser()
        val listing = state.listings.find { it.id == id } ?: throw ApiException(404, "Особняк не найден")
        if (request.rating < 1.0 || request.rating > 5.0) {
            throw ApiException(400, "Оценка должна быть от 1 до 5")
        }
        val today = LocalDate.now()
        val booking = state.bookings.firstOrNull {
            it.userId == user.userId && it.listingId == id && it.status == "CONFIRMED" &&
                parseDate(it.checkOutDate).isBefore(today) && !it.isReviewed
        } ?: throw ApiException(
            409,
            "Отзыв можно оставить один раз после завершённого проживания"
        )
        booking.isReviewed = true

        val review = ReviewDto().apply {
            this.id = UUID.randomUUID().toString()
            userId = user.userId
            authorName = user.fullName.takeUnless { it.isNullOrBlank() } ?: user.email
            authorAvatarUrl = ""
            rating = request.rating
            comment = request.comment.orEmpty().trim()
            createdAt = today.toString()
        }
        state.reviews.getOrPut(id) { mutableListOf() }.add(review)

        val oldCount = listing.reviewsCount
        listing.rating = BigDecimal(listing.rating * oldCount + request.rating)
            .divide(BigDecimal(oldCount + 1), 1, RoundingMode.HALF_UP).toDouble()
        listing.reviewsCount = oldCount + 1
        persist()
        return review
    }

    // ---------------------------------------------------------------- брони

    override suspend fun createBooking(request: CreateBookingRequest): BookingDto {
        delay(500)
        val user = currentUser()
        val booking = prepareBooking(request, user, multi = false, orderId = null, pending = emptyList())
        state.bookings.add(booking)
        persist()
        return booking
    }

    /** Заказ из нескольких особняков: проверяем все позиции, и только потом сохраняем (всё или ничего). */
    override suspend fun createOrder(request: CreateOrderRequest): List<BookingDto> {
        delay(700)
        val user = currentUser()
        val items = request.items.orEmpty()
        if (items.isEmpty()) throw ApiException(400, "Заказ пуст")
        if (items.size > 10) throw ApiException(400, "В одном заказе — не больше 10 особняков")
        val orderId = "W" + (100000..999999).random()
        val multi = items.size >= 2
        val prepared = mutableListOf<BookingDto>()
        for (item in items) {
            prepared += prepareBooking(item, user, multi, orderId, pending = prepared)
        }
        state.bookings.addAll(prepared)
        persist()
        return prepared
    }

    override suspend fun getListingVipServices(id: String): List<VipServiceDto> {
        delay(150)
        val listing = state.listings.find { it.id == id } ?: throw ApiException(404, "Особняк не найден")
        return Pricing.pricedFor(listing, vipServices)
    }

    /** Проверка и расчёт одной аренды по правилам ценообразования (см. util/Pricing.kt). */
    private fun prepareBooking(
        request: CreateBookingRequest,
        user: AuthResponse,
        multi: Boolean,
        orderId: String?,
        pending: List<BookingDto>
    ): BookingDto {
        val listing = state.listings.find { it.id == request.listingId && it.isActive }
            ?: throw ApiException(404, "Особняк не найден")

        val inD = parseDate(request.checkInDate)
        val outD = parseDate(request.checkOutDate)
        if (inD.isBefore(LocalDate.now())) {
            throw ApiException(400, "Дата заезда не может быть в прошлом")
        }
        val nights = ChronoUnit.DAYS.between(inD, outD)
        if (nights <= 0) throw ApiException(400, "Дата выезда должна быть позже даты заезда")
        if (nights > MAX_NIGHTS) throw ApiException(400, "Максимальный срок аренды — $MAX_NIGHTS ночей")
        if (request.guestsCount < 1 || request.guestsCount > listing.maxGuests) {
            throw ApiException(400, "${listing.title}: количество гостей — от 1 до ${listing.maxGuests}")
        }
        if (overlaps(listing.id, inD, outD)) {
            throw ApiException(409, "«${listing.title}»: выбранные даты уже заняты")
        }
        // пересечение с другим особняком этого же заказа (один и тот же дом на те же даты)
        val clash = pending.any {
            it.listingId == listing.id && inD.isBefore(parseDate(it.checkOutDate)) && outD.isAfter(parseDate(it.checkInDate))
        }
        if (clash) throw ApiException(409, "«${listing.title}» добавлен в заказ дважды на пересекающиеся даты")

        // VIP-услуги с ценами этого особняка
        val priced = Pricing.pricedFor(listing, vipServices)
        val chosen = request.vipServiceIds.orEmpty().distinct().map { id ->
            priced.find { it.id == id } ?: throw ApiException(400, "Неизвестная VIP-услуга")
        }
        chosen.firstOrNull { !Pricing.isAvailable(listing, it) }?.let {
            throw ApiException(400, "«${it.name}» доступен только для особняков с вертолётной площадкой")
        }
        val available = priced.filter { Pricing.isAvailable(listing, it) }
        val q = Pricing.quote(listing, nights, chosen, available, multi)

        return BookingDto().apply {
            id = UUID.randomUUID().toString()
            listingId = listing.id
            listingTitle = listing.title
            listingImageUrl = listing.imageUrls.firstOrNull() ?: ""
            listingLocation = "${listing.city.name}, ${listing.address}"
            checkInDate = request.checkInDate
            checkOutDate = request.checkOutDate
            guestsCount = request.guestsCount
            // цена всегда считается на "сервере"
            vipServiceNames = chosen.map { it.name }
            vipTotal = q.vip
            discount = q.discount
            isAllInclusive = q.allInclusive
            this.orderId = orderId
            totalPrice = q.total
            status = "PENDING"
            userId = user.userId
            guestName = user.fullName
            guestEmail = user.email
            isReviewed = false
        }
    }

    override suspend fun getMyBookings(): List<BookingDto> {
        delay(300)
        val user = currentUser()
        return state.bookings.filter { it.userId == user.userId }.reversed()
    }

    override suspend fun cancelBooking(id: String): BookingDto {
        delay(300)
        val user = currentUser()
        val booking = state.bookings.find { it.id == id && it.userId == user.userId }
            ?: throw ApiException(404, "Аренда не найдена")
        ensureCancellable(booking)
        booking.status = "CANCELLED"
        persist()
        return booking
    }

    // ---------------------------------------------------------------- админ

    override suspend fun createListing(request: ListingRequest): ListingDetailsDto {
        delay(400)
        requireAdmin()
        val city = cities.find { it.id == request.cityId } ?: cities[0]
        val type = propertyTypes.find { it.id == request.propertyTypeId } ?: propertyTypes[0]
        if (request.title.isNullOrBlank()) throw ApiException(400, "Укажите название")
        if (request.pricePerNight == null || request.pricePerNight.signum() <= 0) {
            throw ApiException(400, "Укажите цену за ночь")
        }
        val center = cityCenters[city.id] ?: (55.75 to 37.62)
        val listing = ListingDetailsDto().apply {
            id = "l-${UUID.randomUUID().toString().take(8)}"
            title = request.title.trim()
            description = request.description.orEmpty()
            this.city = city
            address = request.address.orEmpty()
            propertyType = type
            imageUrls = request.imageUrls?.filter { it.isNotBlank() }?.ifEmpty { null } ?: listOf(DEFAULT_IMAGE)
            pricePerNight = request.pricePerNight
            rating = 0.0
            reviewsCount = 0
            maxGuests = if (request.maxGuests > 0) request.maxGuests else 2
            bedrooms = request.bedrooms
            beds = request.beds
            bathrooms = request.bathrooms
            this.amenities = allAmenities.filter { request.amenityIds?.contains(it.id) == true }
            hostId = "admin-1"
            hostName = "Администратор"
            hostAvatarUrl = ""
            isSuperhost = false
            latitude = if (request.latitude != 0.0) request.latitude else center.first
            longitude = if (request.longitude != 0.0) request.longitude else center.second
            isActive = true
        }
        state.listings.add(listing)
        persist()
        return listing
    }

    override suspend fun updateListing(id: String, request: ListingRequest): ListingDetailsDto {
        delay(300)
        requireAdmin()
        val listing = state.listings.find { it.id == id } ?: throw ApiException(404, "Особняк не найден")
        val city = cities.find { it.id == request.cityId } ?: listing.city
        val type = propertyTypes.find { it.id == request.propertyTypeId } ?: listing.propertyType
        listing.apply {
            if (!request.title.isNullOrBlank()) title = request.title
            description = request.description ?: description
            this.city = city
            address = request.address ?: address
            propertyType = type
            if (request.pricePerNight != null && request.pricePerNight.signum() > 0) {
                pricePerNight = request.pricePerNight
            }
            if (request.maxGuests > 0) maxGuests = request.maxGuests
            if (request.latitude != 0.0) latitude = request.latitude
            if (request.longitude != 0.0) longitude = request.longitude
        }
        persist()
        return listing
    }

    override suspend fun hideListing(id: String): ListingDetailsDto {
        delay(300)
        requireAdmin()
        val listing = state.listings.find { it.id == id } ?: throw ApiException(404, "Особняк не найден")
        listing.isActive = false
        persist()
        return listing
    }

    override suspend fun getAllBookings(): List<BookingDto> {
        delay(300)
        requireAdmin()
        return state.bookings.reversed()
    }

    override suspend fun confirmBooking(id: String): BookingDto {
        delay(300)
        requireAdmin()
        val booking = state.bookings.find { it.id == id } ?: throw ApiException(404, "Аренда не найдена")
        if (booking.status != "PENDING") {
            throw ApiException(409, "Подтвердить можно только аренду в статусе «Ожидает подтверждения»")
        }
        booking.status = "CONFIRMED"
        persist()
        return booking
    }

    override suspend fun cancelAdminBooking(id: String): BookingDto {
        delay(300)
        requireAdmin()
        val booking = state.bookings.find { it.id == id } ?: throw ApiException(404, "Аренда не найдена")
        ensureCancellable(booking)
        booking.status = "CANCELLED"
        persist()
        return booking
    }

    override suspend fun getAdminStats(): StatsDto {
        delay(250)
        requireAdmin()
        return StatsDto().apply {
            totalUsers = state.users.size
            totalListings = state.listings.count { it.isActive }
            totalBookings = state.bookings.size
            totalRevenue = state.bookings.filter { it.status == "CONFIRMED" }
                .fold(BigDecimal.ZERO) { acc, b -> acc.add(b.totalPrice ?: BigDecimal.ZERO) }
        }
    }

    // ---------------------------------------------------------------- helpers

    private fun currentUser(): AuthResponse {
        val token = tokenProvider()
        return state.users.find { it.token == token }
            ?: throw ApiException(401, "Необходима авторизация")
    }

    private fun requireAdmin() {
        if (!currentUser().role.equals("ADMIN", ignoreCase = true)) {
            throw ApiException(403, "Доступ запрещен")
        }
    }

    private fun ensureCancellable(booking: BookingDto) {
        if (booking.status == "CANCELLED") throw ApiException(409, "Аренда уже отменена")
        if (parseDate(booking.checkOutDate).isBefore(LocalDate.now())) {
            throw ApiException(409, "Нельзя отменить завершённую аренду")
        }
    }

    private fun parseDate(value: String?): LocalDate {
        try {
            return LocalDate.parse(value)
        } catch (e: DateTimeParseException) {
            throw ApiException(400, "Неверный формат даты")
        } catch (e: NullPointerException) {
            throw ApiException(400, "Неверный формат даты")
        }
    }

    /** Пересечение: newIn < oldOut && newOut > oldIn среди PENDING/CONFIRMED. */
    private fun overlaps(listingId: String, inD: LocalDate, outD: LocalDate): Boolean {
        return state.bookings.any { b ->
            b.listingId == listingId && (b.status == "PENDING" || b.status == "CONFIRMED") &&
                inD.isBefore(parseDate(b.checkOutDate)) && outD.isAfter(parseDate(b.checkInDate))
        }
    }

    private fun toSummary(d: ListingDetailsDto): ListingSummaryDto {
        return ListingSummaryDto().apply {
            id = d.id
            title = d.title
            location = "${d.city.name}, ${d.address}"
            imageUrl = d.imageUrls.firstOrNull() ?: ""
            pricePerNight = d.pricePerNight
            rating = d.rating
            reviewsCount = d.reviewsCount
            cityId = d.city.id
            propertyTypeName = d.propertyType.name
            latitude = d.latitude
            longitude = d.longitude
            val t = mutableListOf<String>()
            if (d.isSuperhost) t.add("Проверенный владелец")
            if (d.reviewsCount < 5) t.add("Новинка")
            tags = t
        }
    }

    private fun persist() {
        prefs.edit().putString(KEY_STATE, gson.toJson(state)).apply()
    }

    private fun loadOrSeed(): Snapshot {
        val json = prefs.getString(KEY_STATE, null)
        if (json != null) {
            try {
                val loaded = gson.fromJson(json, Snapshot::class.java)
                if (loaded != null && loaded.users.isNotEmpty() && loaded.listings.isNotEmpty()) return loaded
            } catch (e: Exception) {
                // повреждённое состояние — пересоздаём
            }
        }
        val seeded = seed()
        prefs.edit().putString(KEY_STATE, gson.toJson(seeded)).apply()
        return seeded
    }

    // ---------------------------------------------------------------- начальные данные

    private fun seed(): Snapshot {
        val s = Snapshot()
        s.users.add(AuthResponse("mock-admin-token", "admin-1", "admin@wardclick.ru", "Администратор", "ADMIN"))
        s.users.add(AuthResponse("mock-user-token", "user-1", "user@wardclick.ru", "Тестовый Пользователь", "USER"))
        s.passwords["admin@wardclick.ru"] = "123456"
        s.passwords["user@wardclick.ru"] = "123456"

        fun l(
            id: String, title: String, desc: String, cityIdx: Int, address: String, typeIdx: Int,
            image: String, price: Long, rating: Double, reviews: Int, guests: Int, bedrooms: Int,
            beds: Int, baths: Int, amenityIdx: List<Int>, host: String, hostId: String,
            superhost: Boolean, lat: Double, lon: Double
        ) = ListingDetailsDto().apply {
            this.id = id
            this.title = title
            description = desc
            city = cities[cityIdx]
            this.address = address
            propertyType = propertyTypes[typeIdx]
            imageUrls = listOf(image)
            pricePerNight = BigDecimal(price)
            this.rating = rating
            reviewsCount = reviews
            maxGuests = guests
            this.bedrooms = bedrooms
            this.beds = beds
            bathrooms = baths
            this.amenities = amenityIdx.map { allAmenities[it] }
            this.hostId = hostId
            hostName = host
            hostAvatarUrl = ""
            isSuperhost = superhost
            latitude = lat
            longitude = lon
            isActive = true
        }

        s.listings.addAll(
            listOf(
                l("l1", "Особняк на Рублёвке", "Классический особняк в закрытом посёлке Барвиха: восемь спален, собственный кинозал, баня и бассейн. Территория под охраной, на участке есть вертолётная площадка. До МКАД — 20 минут по Рублёво-Успенскому шоссе.",
                    0, "пос. Барвиха, 29", 0, DemoPhotos.uri("l1"),
                    180000, 4.9, 24, 16, 8, 10, 7, listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9), "Анна", "h1", true, 55.7360, 37.2610),
                l("l2", "Особняк на Крестовском острове", "Каменный особняк на тихой Южной дороге в зелёной части острова: в двух шагах Южный пруд, Приморский парк Победы и яхт-клуб. До Невского проспекта — 15 минут. Подходит для семейных праздников и деловых встреч.",
                    1, "Крестовский остров, Южная дорога", 0, DemoPhotos.uri("l2"),
                    150000, 4.9, 31, 12, 6, 8, 5, listOf(0, 2, 3, 4, 5, 6, 9), "Павел", "h2", true, 59.9683, 30.2453),
                l("l3", "Вилла у моря в Хосте", "Белоснежная вилла в пяти минутах от пляжа: бассейн, субтропический сад с пальмами, большая терраса для ужинов на закате.",
                    2, "Хоста, ул. Платановая, 15", 1, DemoPhotos.uri("l3"),
                    95000, 4.7, 35, 10, 5, 6, 4, listOf(0, 1, 4, 6, 7, 9), "Михаил", "h3", false, 43.5112, 39.8672),
                l("l4", "Усадьба на берегу Волги", "Просторная усадьба с верандой и садом на высоком правом берегу Волги. Русская баня, тишина и вид на реку — в получасе езды от центра Казани.",
                    3, "с. Верхний Услон, ул. Колхозная, 52", 2, DemoPhotos.uri("l4"),
                    70000, 4.8, 18, 14, 7, 9, 4, listOf(0, 2, 3, 4, 7, 9), "Анна", "h1", true, 55.7648, 48.9910),
                l("l5", "Вилла с панорамным бассейном", "Современная вилла на холме Бытха для большой компании: десять спален, панорамный бассейн, вид на горы и море. Персонал и вертолётная площадка.",
                    2, "ул. Бытха, 8", 1, DemoPhotos.uri("l5"),
                    250000, 5.0, 12, 20, 10, 12, 8, listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9), "Олег", "h4", true, 43.5668, 39.7560),
                l("l6", "Особняк у озера Шарташ", "Современный особняк в лесопарке у озера Шарташ: панорамные окна, кинозал и сауна. 15 минут на машине до центра Екатеринбурга.",
                    4, "ул. Отдыха, 11", 0, DemoPhotos.uri("l6"),
                    60000, 4.6, 20, 10, 5, 6, 4, listOf(0, 2, 3, 4, 5, 9), "Павел", "h2", false, 56.8668, 60.6895),
                l("l7", "Шале в Красной Поляне", "Деревянное шале среди елей, в 500 м от реки Мзымты: камин, баня на участке и терраса с видом на заснеженные вершины. 10 минут до горнолыжных подъёмников — идеально для зимнего отдыха.",
                    2, "Эстосадок, ул. Берёзовая, 45", 3, DemoPhotos.uri("l7"),
                    85000, 4.8, 50, 8, 4, 5, 3, listOf(0, 2, 4, 7, 9), "Михаил", "h3", true, 43.6870, 40.2650),
                l("l8", "Особняк у Балтийского моря", "Современный особняк в Светлогорске с открытым бассейном и террасой на крыше. До моря и променада — 10 минут пешком, на территории охрана и вертолётная площадка.",
                    6, "Светлогорск, ул. Аптечная, 3", 0, DemoPhotos.uri("l8"),
                    120000, 4.9, 40, 12, 6, 7, 5, listOf(0, 1, 3, 4, 6, 7, 8, 9), "Елена", "h5", true, 54.9420, 20.1540),
                l("l9", "Стеклянная вилла на Новой Риге", "Вилла в стиле хай-тек: стены-окна от пола до потолка, бассейн прямо у гостиной и летняя кухня с грилем. Коттеджный посёлок у соснового леса, до конноспортивного клуба — 5 минут на машине.",
                    0, "дер. Покровское, ул. Майская", 1, DemoPhotos.uri("l9"),
                    160000, 4.8, 15, 10, 5, 6, 5, listOf(0, 1, 3, 4, 7, 9), "Ксения", "h6", true, 55.8165, 37.0082),
                l("l10", "Особняк в Жуковке", "Белоснежный особняк с двухуровневой террасой и длинным бассейном с подсветкой. Гостиная со вторым светом, винный погреб, кинозал и спа-зона с хаммамом. Своя вертолётная площадка — до центра Москвы 10 минут по воздуху.",
                    0, "дер. Жуковка, Рублёво-Успенское ш.", 0, DemoPhotos.uri("l10"),
                    220000, 4.9, 9, 14, 7, 9, 7, listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9), "Олег", "h4", true, 55.7340, 37.2470),
                l("l11", "Финская усадьба в Комарово", "Деревянная усадьба в финском стиле среди сосен Карельского перешейка. До Финского залива — 7 минут пешком, на участке баня и веранда с мангалом. Электричка до Финляндского вокзала идёт меньше часа.",
                    1, "Комарово, ул. Кавалерийская", 2, DemoPhotos.uri("l11"),
                    55000, 4.7, 27, 8, 4, 5, 2, listOf(0, 2, 4, 7, 9), "Наталья", "h7", false, 60.1782, 29.8051),
                l("l12", "Особняк у Финского залива в Стрельне", "Кирпичный особняк в английском стиле с ухоженным газоном и садом, территория под охраной. До берега Финского залива — 5 минут пешком, до Константиновского дворца — 5 минут на машине, до Петергофа — 10. Кинозал, сауна, камин в гостиной и терраса.",
                    1, "Стрельна, Нижняя дорога", 0, DemoPhotos.uri("l12"),
                    90000, 4.8, 22, 10, 5, 6, 4, listOf(0, 2, 3, 4, 5, 7, 9), "Павел", "h2", true, 59.8585, 30.0211),
                l("l13", "Вилла-гасиенда в Лоо", "Вилла в испанском стиле с арочной галереей и круглым бассейном во внутреннем дворе. Вечером двор освещают фонари, до галечного пляжа Лоо — 10 минут пешком. Персонал приготовит ужин на террасе.",
                    2, "Лоо, ул. Разина, 20", 1, DemoPhotos.uri("l13"),
                    75000, 4.6, 19, 10, 5, 6, 4, listOf(0, 1, 4, 6, 7, 9), "Михаил", "h3", false, 43.7060, 39.5880),
                l("l14", "Особняк в Амалиенау", "Немецкая вилла начала XX века в районе Амалиенау: фахверковый фасад, черепичная крыша и отреставрированные интерьеры. Сад с туями и террасой, камин и сауна. До центра Калининграда — 10 минут, до моря — 40.",
                    6, "ул. Кутузова, 24", 0, DemoPhotos.uri("l14"),
                    80000, 4.9, 33, 10, 5, 6, 3, listOf(0, 2, 4, 7, 9), "Елена", "h5", true, 54.7180, 20.4660),
                l("l15", "Шале в сосновом бору у Обского моря", "Бревенчатое шале в сосновом бору, в 200 м от Обского моря: камин в гостиной, русская баня и открытая веранда с мангалом. Летом — пляж и катамараны, зимой — лыжня прямо от дома. До центра Новосибирска — 40 минут.",
                    5, "Бердск, ул. Морская", 3, DemoPhotos.uri("l15"),
                    40000, 4.7, 26, 8, 4, 5, 2, listOf(0, 2, 4, 7, 9), "Денис", "h8", false, 54.7760, 83.0623),
                l("l16", "Современный особняк в Академгородке", "Особняк из стекла и дерева в тихом лесном квартале Академгородка: рядом Ботанический сад и Утиное озеро, до пляжа Обского моря — 15 минут пешком. Кинозал, сауна, умный дом и зарядка для электромобиля.",
                    5, "Академгородок, ул. Золотодолинская", 0, DemoPhotos.uri("l16"),
                    55000, 4.8, 4, 10, 5, 6, 3, listOf(0, 2, 3, 4, 5, 9), "Денис", "h8", false, 54.8277, 83.1028),
                l("l17", "Вилла на Русском острове", "Белоснежная вилла с бассейном и панорамными окнами на пролив Босфор Восточный, с террасы виден Русский мост — до него 1 км. Спуск к морю прямо от участка. Повар и горничная, охрана и вертолётная площадка на территории.",
                    7, "о. Русский, пос. Поспелово", 1, DemoPhotos.uri("l17"),
                    130000, 4.9, 11, 12, 6, 7, 5, listOf(0, 1, 3, 4, 6, 7, 8, 9), "Артём", "h9", true, 43.0618, 131.8890),
                l("l18", "Белый особняк у бухты Шамора", "Минималистичный особняк в средиземноморском стиле в лесу у бухты Шамора — самого известного пляжа Владивостока. До моря — 3 минуты пешком. Внутренний дворик, терраса на крыше и сауна. До центра города — 40 минут.",
                    7, "бухта Шамора, ул. Лазурная", 0, DemoPhotos.uri("l18"),
                    65000, 4.6, 3, 8, 4, 5, 3, listOf(0, 2, 4, 7, 9), "Артём", "h9", true, 43.1870, 132.1097),
                l("l19", "Вилла в Боровом Матюшино", "Современная вилла в сосновом бору на берегу Волги: бассейн с подогревом, терраса на крыше и собственный причал. Хамам, кинозал, персонал и охрана посёлка. До Казанского кремля — 45 минут.",
                    3, "Боровое Матюшино, ул. 2-я Грибная", 1, DemoPhotos.uri("l19"),
                    110000, 4.9, 17, 12, 6, 7, 5, listOf(0, 1, 2, 3, 4, 5, 6, 7, 9), "Рустам", "h10", true, 55.6125, 49.0203),
                l("l20", "Семейный особняк в Салмачах", "Кирпичный особняк в спокойном коттеджном посёлке: гараж на две машины, газон, детская площадка и терраса для барбекю. Удобно для семьи с детьми — до центра Казани и до аэропорта по 25 минут.",
                    3, "пос. Салмачи, ул. Сабантуй", 0, DemoPhotos.uri("l20"),
                    45000, 4.5, 8, 8, 4, 5, 3, listOf(0, 4, 7, 9), "Гульнара", "h11", false, 55.7358, 49.2719),
                l("l21", "Шале у Уктусских гор", "Деревянное шале с панорамным остеклением у подножия Уктусских гор. Днём — горные лыжи и тюбинг в 5 минутах от дома, вечером — камин, баня и терраса с видом на лес. До центра Екатеринбурга — 20 минут.",
                    4, "Уктус, пер. Кирпичный", 3, DemoPhotos.uri("l21"),
                    50000, 4.8, 29, 8, 4, 5, 3, listOf(0, 2, 4, 7, 9), "Ирина", "h12", true, 56.7730, 60.6390),
                l("l22", "Вилла на Верх-Исетском пруду", "Современная вилла на берегу Верх-Исетского пруда: бассейн с видом на воду, кинозал, сауна и причал для сапбордов. Охраняемая территория, до центра Екатеринбурга — 20 минут.",
                    4, "ул. Водонасосная", 1, DemoPhotos.uri("l22"),
                    95000, 4.7, 13, 12, 6, 7, 4, listOf(0, 1, 2, 3, 4, 5, 7, 9), "Ирина", "h12", true, 56.8461, 60.4911)
            )
        )

        // коэффициенты цен VIP-услуг: класс особняка и расположение
        val vipFactors = mapOf(
            "l1" to 1.5, "l2" to 1.3, "l3" to 1.0, "l4" to 0.8, "l5" to 1.6, "l6" to 0.85, "l7" to 1.2, "l8" to 1.1,
            "l9" to 1.4, "l10" to 1.5, "l11" to 0.9, "l12" to 1.15, "l13" to 0.95, "l14" to 1.0, "l15" to 0.7,
            "l16" to 0.8, "l17" to 1.25, "l18" to 0.85, "l19" to 1.1, "l20" to 0.75, "l21" to 0.8, "l22" to 1.0
        )
        s.listings.forEach { it.vipFactor = vipFactors[it.id] ?: 1.0 }

        fun r(listingId: String, id: String, author: String, rating: Double, text: String, date: String) {
            s.reviews.getOrPut(listingId) { mutableListOf() }.add(
                ReviewDto().apply {
                    this.id = id
                    userId = "seed"
                    authorName = author
                    authorAvatarUrl = ""
                    this.rating = rating
                    comment = text
                    createdAt = date
                }
            )
        }
        r("l1", "r1", "Иван", 5.0, "Отмечали юбилей всей семьёй — места хватило всем, персонал помог с ужином.", "2026-08-10")
        r("l1", "r2", "Мария", 4.5, "Огромный дом, кинозал и баня. Немного далеко от центра, но это даже плюс.", "2026-07-02")
        r("l2", "r3", "Сергей", 5.0, "Тихое место у пруда, до центра 15 минут. Владелец всегда на связи.", "2026-08-21")
        r("l2", "r4", "Ольга", 4.8, "Провели здесь встречу на 12 человек — всё прошло отлично.", "2026-06-15")
        r("l3", "r5", "Дмитрий", 4.7, "Море рядом, бассейн чистый, владелец приветливый.", "2026-07-25")
        r("l5", "r6", "Алексей", 5.0, "Бассейн, терраса, шикарный вид — лучший отдых. Прилетели на вертолёте прямо к вилле.", "2026-08-02")
        r("l9", "r7", "Виктория", 4.8, "Бассейн прямо у гостиной — дети не вылезали из воды. Утром гуляли в лесу.", "2026-08-30")
        r("l10", "r8", "Константин", 5.0, "Прилетели на вертолёте прямо на площадку у дома. Хаммам и кинозал — выше всяких похвал.", "2026-09-12")
        r("l11", "r9", "Татьяна", 4.6, "Настоящая финская атмосфера: сосны, баня и залив рядом. Дом простой, но очень уютный.", "2026-07-19")
        r("l12", "r10", "Андрей", 4.8, "Отличное место для семейного праздника, до Петергофа доехали за 10 минут.", "2026-09-05")
        r("l13", "r11", "Екатерина", 4.5, "Вечером во дворе с фонарями как в Испании. Пляж близко, но к дому крутой подъём.", "2026-08-14")
        r("l14", "r12", "Николай", 5.0, "Старинный дом с душой — камин, сад и тихая улица Амалиенау. Хозяйка рассказала историю дома.", "2026-09-20")
        r("l14", "r13", "Светлана", 4.8, "Красивые интерьеры, до центра рукой подать. Сауна — отдельное удовольствие.", "2026-06-28")
        r("l15", "r14", "Игорь", 4.7, "Зимой катались на лыжах прямо от крыльца, потом баня. Тишина и сосны.", "2026-02-21")
        r("l17", "r15", "Юлия", 5.0, "Закаты над проливом и вид на мост — ради этого стоит лететь через всю страну.", "2026-09-08")
        r("l19", "r16", "Ринат", 4.9, "Причал, бассейн с подогревом и баня — провели здесь корпоратив, все в восторге.", "2026-08-25")
        r("l21", "r17", "Анастасия", 4.8, "До подъёмников пять минут, а вечером камин и вид на лес. Обязательно вернёмся зимой.", "2026-03-09")
        r("l22", "r18", "Максим", 4.6, "Катались на сапбордах прямо от причала. Дом большой, хватило на две семьи.", "2026-07-30")

        // завершённая бронь тестового пользователя — чтобы можно было проверить вкладку «Завершённые» и отзыв
        val today = LocalDate.now()
        val l7 = s.listings.first { it.id == "l7" }
        s.bookings.add(
            BookingDto().apply {
                id = UUID.randomUUID().toString()
                listingId = l7.id
                listingTitle = l7.title
                listingImageUrl = l7.imageUrls.first()
                listingLocation = "${l7.city.name}, ${l7.address}"
                checkInDate = today.minusDays(14).toString()
                checkOutDate = today.minusDays(10).toString()
                guestsCount = 2
                vipServiceNames = listOf("Личный водитель", "Шеф-повар")
                vipTotal = BigDecimal((30000 + 24000) * 4) // водитель и повар с коэффициентом 1.2
                totalPrice = l7.pricePerNight.multiply(BigDecimal.valueOf(4)).add(vipTotal)
                status = "CONFIRMED"
                userId = "user-1"
                guestName = "Тестовый Пользователь"
                guestEmail = "user@wardclick.ru"
                isReviewed = false
            }
        )
        return s
    }

    private companion object {
        const val KEY_STATE = "state_v9" // v9: координаты и адреса сверены с картой
        const val MAX_NIGHTS = 30
        val DEFAULT_IMAGE = DemoPhotos.uri("l1")
    }
}
