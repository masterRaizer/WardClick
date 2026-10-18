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
        val reviews = state.reviews.getOrPut(id) { mutableListOf() }
        reviews.add(review)
        recalcRating(listing, reviews)
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

    /** Число отзывов и средняя оценка (до десятых) — по самим отзывам объекта. */
    private fun recalcRating(listing: ListingDetailsDto, reviews: List<ReviewDto>) {
        listing.reviewsCount = reviews.size
        listing.rating = if (reviews.isEmpty()) 0.0 else
            BigDecimal.valueOf(reviews.sumOf { it.rating })
                .divide(BigDecimal(reviews.size), 1, RoundingMode.HALF_UP).toDouble()
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
            image: String, price: Long, guests: Int, bedrooms: Int,
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
                    180000, 16, 8, 10, 7, listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9), "Анна", "h1", true, 55.7360, 37.2610),
                l("l2", "Особняк на Крестовском острове", "Каменный особняк на тихой Южной дороге в зелёной части острова: в двух шагах Южный пруд, Приморский парк Победы и яхт-клуб. До Невского проспекта — 15 минут. Подходит для семейных праздников и деловых встреч.",
                    1, "Крестовский остров, Южная дорога", 0, DemoPhotos.uri("l2"),
                    150000, 12, 6, 8, 5, listOf(0, 2, 3, 4, 5, 6, 9), "Павел", "h2", true, 59.9683, 30.2453),
                l("l3", "Вилла у моря в Хосте", "Белоснежная вилла в пяти минутах от пляжа: бассейн, субтропический сад с пальмами, большая терраса для ужинов на закате.",
                    2, "Хоста, ул. Платановая, 15", 1, DemoPhotos.uri("l3"),
                    95000, 10, 5, 6, 4, listOf(0, 1, 4, 6, 7, 9), "Михаил", "h3", false, 43.5112, 39.8672),
                l("l4", "Усадьба на берегу Волги", "Просторная усадьба с верандой и садом на высоком правом берегу Волги. Русская баня, тишина и вид на реку — в получасе езды от центра Казани.",
                    3, "с. Верхний Услон, ул. Колхозная, 52", 2, DemoPhotos.uri("l4"),
                    70000, 14, 7, 9, 4, listOf(0, 2, 3, 4, 7, 9), "Анна", "h1", true, 55.7648, 48.9910),
                l("l5", "Вилла с панорамным бассейном", "Современная вилла на холме Бытха для большой компании: десять спален, панорамный бассейн, вид на горы и море. Персонал и вертолётная площадка.",
                    2, "ул. Бытха, 8", 1, DemoPhotos.uri("l5"),
                    250000, 20, 10, 12, 8, listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9), "Олег", "h4", true, 43.5668, 39.7560),
                l("l6", "Особняк у озера Шарташ", "Современный особняк в лесопарке у озера Шарташ: панорамные окна, кинозал и сауна. 15 минут на машине до центра Екатеринбурга.",
                    4, "ул. Отдыха, 11", 0, DemoPhotos.uri("l6"),
                    60000, 10, 5, 6, 4, listOf(0, 2, 3, 4, 5, 9), "Павел", "h2", false, 56.8668, 60.6895),
                l("l7", "Шале в Красной Поляне", "Деревянное шале среди елей, в 500 м от реки Мзымты: камин, баня на участке и терраса с видом на заснеженные вершины. 10 минут до горнолыжных подъёмников — идеально для зимнего отдыха.",
                    2, "Эстосадок, ул. Берёзовая, 45", 3, DemoPhotos.uri("l7"),
                    85000, 8, 4, 5, 3, listOf(0, 2, 4, 7, 9), "Михаил", "h3", true, 43.6870, 40.2650),
                l("l8", "Особняк у Балтийского моря", "Современный особняк в Светлогорске с открытым бассейном и террасой на крыше. До моря и променада — 10 минут пешком, на территории охрана и вертолётная площадка.",
                    6, "Светлогорск, ул. Аптечная, 3", 0, DemoPhotos.uri("l8"),
                    120000, 12, 6, 7, 5, listOf(0, 1, 3, 4, 6, 7, 8, 9), "Елена", "h5", true, 54.9420, 20.1540),
                l("l9", "Стеклянная вилла на Новой Риге", "Вилла в стиле хай-тек: стены-окна от пола до потолка, бассейн прямо у гостиной и летняя кухня с грилем. Коттеджный посёлок у соснового леса, до конноспортивного клуба — 5 минут на машине.",
                    0, "дер. Покровское, ул. Майская", 1, DemoPhotos.uri("l9"),
                    160000, 10, 5, 6, 5, listOf(0, 1, 3, 4, 7, 9), "Ксения", "h6", true, 55.8165, 37.0082),
                l("l10", "Особняк в Жуковке", "Белоснежный особняк с двухуровневой террасой и длинным бассейном с подсветкой. Гостиная со вторым светом, винный погреб, кинозал и спа-зона с хаммамом. Своя вертолётная площадка — до центра Москвы 10 минут по воздуху.",
                    0, "дер. Жуковка, Рублёво-Успенское ш.", 0, DemoPhotos.uri("l10"),
                    220000, 14, 7, 9, 7, listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9), "Олег", "h4", true, 55.7340, 37.2470),
                l("l11", "Финская усадьба в Комарово", "Деревянная усадьба в финском стиле среди сосен Карельского перешейка. До Финского залива — 7 минут пешком, на участке баня и веранда с мангалом. Электричка до Финляндского вокзала идёт меньше часа.",
                    1, "Комарово, ул. Кавалерийская", 2, DemoPhotos.uri("l11"),
                    55000, 8, 4, 5, 2, listOf(0, 2, 4, 7, 9), "Наталья", "h7", false, 60.1782, 29.8051),
                l("l12", "Особняк у Финского залива в Стрельне", "Кирпичный особняк в английском стиле с ухоженным газоном и садом, территория под охраной. До берега Финского залива — 5 минут пешком, до Константиновского дворца — 5 минут на машине, до Петергофа — 10. Кинозал, сауна, камин в гостиной и терраса.",
                    1, "Стрельна, Нижняя дорога", 0, DemoPhotos.uri("l12"),
                    90000, 10, 5, 6, 4, listOf(0, 2, 3, 4, 5, 7, 9), "Павел", "h2", true, 59.8585, 30.0211),
                l("l13", "Вилла-гасиенда в Лоо", "Вилла в испанском стиле с арочной галереей и круглым бассейном во внутреннем дворе. Вечером двор освещают фонари, до галечного пляжа Лоо — 10 минут пешком. Персонал приготовит ужин на террасе.",
                    2, "Лоо, ул. Разина, 20", 1, DemoPhotos.uri("l13"),
                    75000, 10, 5, 6, 4, listOf(0, 1, 4, 6, 7, 9), "Михаил", "h3", false, 43.7060, 39.5880),
                l("l14", "Особняк в Амалиенау", "Немецкая вилла начала XX века в районе Амалиенау: фахверковый фасад, черепичная крыша и отреставрированные интерьеры. Сад с туями и террасой, камин и сауна. До центра Калининграда — 10 минут, до моря — 40.",
                    6, "ул. Кутузова, 24", 0, DemoPhotos.uri("l14"),
                    80000, 10, 5, 6, 3, listOf(0, 2, 4, 7, 9), "Елена", "h5", true, 54.7180, 20.4660),
                l("l15", "Шале в сосновом бору у Обского моря", "Бревенчатое шале в сосновом бору, в 200 м от Обского моря: камин в гостиной, русская баня и открытая веранда с мангалом. Летом — пляж и катамараны, зимой — лыжня прямо от дома. До центра Новосибирска — 40 минут.",
                    5, "Бердск, ул. Морская", 3, DemoPhotos.uri("l15"),
                    40000, 8, 4, 5, 2, listOf(0, 2, 4, 7, 9), "Денис", "h8", false, 54.7760, 83.0623),
                l("l16", "Современный особняк в Академгородке", "Особняк из стекла и дерева в тихом лесном квартале Академгородка: рядом Ботанический сад и Утиное озеро, до пляжа Обского моря — 15 минут пешком. Кинозал, сауна, умный дом и зарядка для электромобиля.",
                    5, "Академгородок, ул. Золотодолинская", 0, DemoPhotos.uri("l16"),
                    55000, 10, 5, 6, 3, listOf(0, 2, 3, 4, 5, 9), "Денис", "h8", false, 54.8277, 83.1028),
                l("l17", "Вилла на Русском острове", "Белоснежная вилла с бассейном и панорамными окнами на пролив Босфор Восточный, с террасы виден Русский мост — до него 1 км. Спуск к морю прямо от участка. Повар и горничная, охрана и вертолётная площадка на территории.",
                    7, "о. Русский, пос. Поспелово", 1, DemoPhotos.uri("l17"),
                    130000, 12, 6, 7, 5, listOf(0, 1, 3, 4, 6, 7, 8, 9), "Артём", "h9", true, 43.0618, 131.8890),
                l("l18", "Белый особняк у бухты Шамора", "Минималистичный особняк в средиземноморском стиле в лесу у бухты Шамора — самого известного пляжа Владивостока. До моря — 3 минуты пешком. Внутренний дворик, терраса на крыше и сауна. До центра города — 40 минут.",
                    7, "бухта Шамора, ул. Лазурная", 0, DemoPhotos.uri("l18"),
                    65000, 8, 4, 5, 3, listOf(0, 2, 4, 7, 9), "Артём", "h9", true, 43.1870, 132.1097),
                l("l19", "Вилла в Боровом Матюшино", "Современная вилла в сосновом бору на берегу Волги: бассейн с подогревом, терраса на крыше и собственный причал. Хамам, кинозал, персонал и охрана посёлка. До Казанского кремля — 45 минут.",
                    3, "Боровое Матюшино, ул. 2-я Грибная", 1, DemoPhotos.uri("l19"),
                    110000, 12, 6, 7, 5, listOf(0, 1, 2, 3, 4, 5, 6, 7, 9), "Рустам", "h10", true, 55.6125, 49.0203),
                l("l20", "Семейный особняк в Салмачах", "Кирпичный особняк в спокойном коттеджном посёлке: гараж на две машины, газон, детская площадка и терраса для барбекю. Удобно для семьи с детьми — до центра Казани и до аэропорта по 25 минут.",
                    3, "пос. Салмачи, ул. Сабантуй", 0, DemoPhotos.uri("l20"),
                    45000, 8, 4, 5, 3, listOf(0, 4, 7, 9), "Гульнара", "h11", false, 55.7358, 49.2719),
                l("l21", "Шале у Уктусских гор", "Деревянное шале с панорамным остеклением у подножия Уктусских гор. Днём — горные лыжи и тюбинг в 5 минутах от дома, вечером — камин, баня и терраса с видом на лес. До центра Екатеринбурга — 20 минут.",
                    4, "Уктус, пер. Кирпичный", 3, DemoPhotos.uri("l21"),
                    50000, 8, 4, 5, 3, listOf(0, 2, 4, 7, 9), "Ирина", "h12", true, 56.7730, 60.6390),
                l("l22", "Вилла на Верх-Исетском пруду", "Современная вилла на берегу Верх-Исетского пруда: бассейн с видом на воду, кинозал, сауна и причал для сапбордов. Охраняемая территория, до центра Екатеринбурга — 20 минут.",
                    4, "ул. Водонасосная", 1, DemoPhotos.uri("l22"),
                    95000, 12, 6, 7, 4, listOf(0, 1, 2, 3, 4, 5, 7, 9), "Ирина", "h12", true, 56.8461, 60.4911)
            )
        )

        // коэффициенты цен VIP-услуг: класс особняка и расположение
        val vipFactors = mapOf(
            "l1" to 1.5, "l2" to 1.3, "l3" to 1.0, "l4" to 0.8, "l5" to 1.6, "l6" to 0.85, "l7" to 1.2, "l8" to 1.1,
            "l9" to 1.4, "l10" to 1.5, "l11" to 0.9, "l12" to 1.15, "l13" to 0.95, "l14" to 1.0, "l15" to 0.7,
            "l16" to 0.8, "l17" to 1.25, "l18" to 0.85, "l19" to 1.1, "l20" to 0.75, "l21" to 0.8, "l22" to 1.0
        )
        s.listings.forEach { it.vipFactor = vipFactors[it.id] ?: 1.0 }

        fun r(listingId: String, id: String, author: String, rating: Int, text: String, date: String) {
            s.reviews.getOrPut(listingId) { mutableListOf() }.add(
                ReviewDto().apply {
                    this.id = id
                    userId = "seed"
                    authorName = author
                    authorAvatarUrl = ""
                    this.rating = rating.toDouble()
                    comment = text
                    createdAt = date
                }
            )
        }
        r("l1", "r1", "Иван", 5, "Отмечали юбилей всей семьёй — места хватило всем, персонал помог с ужином.", "2026-08-10")
        r("l1", "r2", "Мария", 5, "Огромный дом, кинозал и баня. Немного далеко от центра, но это даже плюс.", "2026-07-02")
        r("l1", "r3", "Евгения С.", 5, "Снимали на Новый год большой компанией. Кинозал, баня и подогреваемый бассейн — праздник удался, Анна помогла с ёлкой.", "2026-01-03")
        r("l1", "r4", "Роман Л.", 5, "Охрана на въезде, тишина, сосны вокруг. Повар готовил завтраки — дети были в восторге.", "2025-08-17")
        r("l1", "r5", "Галина П.", 5, "Огромный дом в идеальном порядке. Отдельное спасибо за кинозал — смотрели фильмы до ночи.", "2026-05-11")
        r("l1", "r6", "Тимур А.", 4, "Дом шикарный, но в пятницу вечером по Рублёво-Успенскому шоссе ехали почти час. В остальном всё отлично.", "2026-09-20")
        r("l2", "r7", "Сергей", 5, "Тихое место у пруда, до центра 15 минут. Владелец всегда на связи.", "2026-08-21")
        r("l2", "r8", "Ольга", 5, "Провели здесь встречу на 12 человек — всё прошло отлично.", "2026-06-15")
        r("l2", "r9", "Вера Н.", 5, "Утром бегали вокруг Южного пруда, вечером гуляли по Приморскому парку. Дом красивый, а сауна — спасение после прогулок.", "2026-06-07")
        r("l2", "r10", "Аркадий Б.", 5, "Жили здесь во время деловой поездки: до центра быстро, повар и горничная работали идеально.", "2026-06-19")
        r("l2", "r11", "Людмила Г.", 4, "Дом прекрасный, но летом по выходным у яхт-клуба шумно до полуночи.", "2026-07-26")
        r("l2", "r12", "Станислав Е.", 5, "Отмечали день рождения: кинозал, сауна и ужин от повара. Павел заранее всё подготовил.", "2025-09-14")
        r("l3", "r13", "Дмитрий", 5, "Море рядом, бассейн чистый, владелец приветливый.", "2026-07-25")
        r("l3", "r14", "Алина Ш.", 5, "До пляжа правда пять минут, а вечером ужинали на террасе под пальмами. Михаил подсказал хорошие места в Хосте.", "2026-08-09")
        r("l3", "r15", "Григорий Т.", 4, "Вилла отличная, но рядом железная дорога — поезда слышно. Нам не мешало, но чутко спящим стоит учесть.", "2026-07-15")
        r("l3", "r16", "Лилия К.", 5, "Ездили в сентябре: море тёплое, народу мало. Персонал готовил завтраки, сад с пальмами — как в кино.", "2025-09-28")
        r("l3", "r17", "Владимир Р.", 5, "Чистый бассейн, просторные комнаты, удобная парковка. Сходили в Тисо-самшитовую рощу — она совсем рядом.", "2026-06-01")
        r("l3", "r18", "Полина Д.", 4, "Хороший дом для большой семьи. Минус — крутая лестница к дому, с коляской непросто.", "2026-05-03")
        r("l4", "r19", "Ренат Г.", 5, "Вид на Волгу с веранды потрясающий, особенно на закате. Русская баня — лучшая, в какой мы были.", "2026-07-05")
        r("l4", "r20", "Ольга В.", 5, "Собрались три семьи, места хватило всем. До пляжа дошли пешком за десять минут.", "2026-08-02")
        r("l4", "r21", "Кирилл М.", 4, "Дом уютный, баня отличная, но в июльскую жару комнаты на втором этаже нагреваются.", "2026-07-19")
        r("l4", "r22", "Эльвира Х.", 5, "Анна встретила с пирогами, в саду поспели яблоки. Дети катались на велосипедах, а мы отдыхали на веранде.", "2025-09-07")
        r("l4", "r23", "Борис Ф.", 5, "Приезжали зимой: сад в снегу, баня, тишина. Обязательно вернёмся летом.", "2026-02-22")
        r("l5", "r24", "Алексей", 5, "Бассейн, терраса, шикарный вид — лучший отдых. Прилетели на вертолёте прямо к вилле.", "2026-08-02")
        r("l5", "r25", "Диана Ж.", 5, "Двадцать человек — и всем было где разместиться. Панорамный бассейн с видом на море и горы, персонал работал как часы.", "2026-08-23")
        r("l5", "r26", "Артур К.", 5, "Праздновали свадьбу на террасе, Олег помог с декором и кейтерингом. Вид — как на открытке.", "2026-06-27")
        r("l5", "r27", "Наиля С.", 5, "Бассейн, сауна, кинозал — ни разу не захотелось уезжать с виллы. К морю спускались минут десять.", "2025-10-04")
        r("l5", "r28", "Яна Ч.", 5, "Безупречная чистота и вид на горы по утрам. Лучший дом, в котором мы отдыхали в Сочи.", "2026-04-30")
        r("l6", "r29", "Глеб О.", 5, "Лес прямо за окном, до озера две минуты. Вечером парились в сауне, потом смотрели кино.", "2026-07-11")
        r("l6", "r30", "Марина Ж.", 4, "Хороший дом, но бассейна нет — летом его не хватало. Зато Шарташ рядом, купались в озере.", "2026-07-25")
        r("l6", "r31", "Филипп Я.", 5, "Ездили зимой: катались на лыжах по лесопарку, а дома тепло и уютно. Из панорамных окон — заснеженный лес.", "2026-01-18")
        r("l6", "r32", "Нина Б.", 4, "Удобно добираться из центра, охрана на въезде. Wi‑Fi в дальних комнатах слабоват.", "2025-11-02")
        r("l6", "r33", "Эдуард Ц.", 5, "Провели выездную планёрку на десять человек — тихо, просторно, никто не отвлекает.", "2026-03-14")
        r("l7", "r34", "Алёна Р.", 5, "Катались неделю: до подъёмников десять минут на машине, вечером камин и баня. Идеальное шале для зимы.", "2026-02-08")
        r("l7", "r35", "Вадим Ш.", 5, "Летом тоже отлично: гуляли вдоль Мзымты, ездили на водопады, а на террасе пили чай с видом на горы.", "2025-08-03")
        r("l7", "r36", "Карина Т.", 5, "Уютное деревянное шале, пахнет деревом и хвоей. Михаил оставил подробную инструкцию по бане и маршрутам.", "2026-01-05")
        r("l7", "r37", "Матвей Л.", 4, "Шале замечательное, но в праздники от Адлера добирались почти два часа из-за пробок.", "2026-01-02")
        r("l7", "r38", "Василиса Г.", 5, "Небольшое, но очень продуманное шале на восемь человек. Баня на участке — лучшее завершение лыжного дня.", "2026-03-01")
        r("l7", "r39", "Георгий Н.", 5, "Приезжали на майские: наверху ещё снег, а внизу всё цветёт. Терраса с видом на вершины — восторг.", "2026-05-04")
        r("l7", "r40", "Инна П.", 4, "Хорошее шале, но парковка на одну машину — вторую ставили на улице.", "2025-12-28")
        r("l8", "r41", "Арсений В.", 5, "До моря и променада правда десять минут пешком. Открытый бассейн и терраса на крыше — лучшее, что было в отпуске.", "2026-07-22")
        r("l8", "r42", "Тамара С.", 5, "Елена всё показала и рассказала про город. Днём гуляли по Светлогорску, вечером ужинали на крыше.", "2026-06-14")
        r("l8", "r43", "Леонид К.", 5, "Ездили осенью: на пляже пусто, воздух сосновый, в доме тепло. Персонал очень внимательный.", "2025-10-12")
        r("l8", "r44", "Майя Д.", 4, "Дом красивый, но бассейн открытый — в начале июня вода в нём ещё прохладная.", "2026-06-05")
        r("l8", "r45", "Захар И.", 5, "От аэропорта Храброво — полчаса на машине. Охрана, тишина, двенадцать человек разместились свободно.", "2026-08-16")
        r("l8", "r46", "Ева М.", 5, "Детям очень понравилось: бассейн, сосны, море. Обязательно вернёмся следующим летом.", "2025-08-24")
        r("l9", "r47", "Виктория", 5, "Бассейн прямо у гостиной — дети не вылезали из воды. Утром гуляли в лесу.", "2026-08-30")
        r("l9", "r48", "Пётр Ф.", 5, "Стеклянные стены и лес вокруг — ощущение, что живёшь прямо в лесу. Гриль в летней кухне очень выручил.", "2026-06-21")
        r("l9", "r49", "София Е.", 4, "Красивая вилла, но из-за стен-окон в солнечный день жарко — спасали шторы.", "2026-07-12")
        r("l9", "r50", "Ярослав У.", 5, "Съездили на конную прогулку — клуб в пяти минутах на машине. Ксения заранее всё подсказала.", "2026-05-24")
        r("l9", "r51", "Лариса Ш.", 5, "Отметили годовщину с друзьями. Бассейн у гостиной — главная фишка дома.", "2025-09-06")
        r("l10", "r52", "Константин", 5, "Прилетели на вертолёте прямо на площадку у дома. Хаммам и кинозал — выше всяких похвал.", "2026-09-12")
        r("l10", "r53", "Валерия Б.", 5, "Хаммам, винный погреб и бассейн с подсветкой — настоящий загородный клуб. Обслуживание на высоте.", "2026-08-08")
        r("l10", "r54", "Антон Р.", 5, "Проводили закрытый ужин для партнёров. Олег организовал всё до мелочей.", "2026-04-17")
        r("l10", "r55", "Злата Г.", 4, "Дом великолепный, но рядом железнодорожная ветка — иногда слышны поезда.", "2026-07-03")
        r("l10", "r56", "Семён Л.", 5, "Огромная гостиная со вторым светом и тишина вокруг. Четырнадцать человек — и никакой тесноты.", "2026-01-10")
        r("l11", "r57", "Татьяна", 4, "Настоящая финская атмосфера: сосны, баня и залив рядом. Дом простой, но очень уютный.", "2026-07-19")
        r("l11", "r58", "Ульяна З.", 5, "До залива семь минут через сосновый лес. Баня на участке и веранда — всё, что нужно для выходных.", "2026-07-04")
        r("l11", "r59", "Всеволод Р.", 5, "Приехали на электричке из Петербурга, от станции дошли пешком. Наталья встретила и всё показала.", "2026-05-16")
        r("l11", "r60", "Римма К.", 4, "Атмосферная дача, но бойлера на восемь человек маловато — горячая вода быстро заканчивается.", "2025-08-30")
        r("l11", "r61", "Богдан Т.", 5, "Зимой Комарово — сказка: сосны в снегу, залив подо льдом, а вечером баня.", "2026-02-14")
        r("l12", "r62", "Андрей", 5, "Отличное место для семейного праздника, до Петергофа доехали за 10 минут.", "2026-09-05")
        r("l12", "r63", "Кристина Л.", 5, "Английский дом с камином и садом — как в сериале. До залива дошли пешком за пять минут.", "2026-08-29")
        r("l12", "r64", "Никита С.", 5, "Ездили в Петергоф и Константиновский дворец, а вечером возвращались в тишину. Кинозал детям очень понравился.", "2026-07-18")
        r("l12", "r65", "Жанна Р.", 4, "Хороший дом, но в выходные на Петербургском шоссе пробки — до центра города ехали час.", "2026-06-28")
        r("l12", "r66", "Валентин Д.", 5, "Отмечали юбилей отца: сауна, терраса, ухоженный газон. Павел помог с ужином.", "2025-09-21")
        r("l13", "r67", "Екатерина", 4, "Вечером во дворе с фонарями как в Испании. Пляж близко, но к дому крутой подъём.", "2026-08-14")
        r("l13", "r68", "Мирон В.", 5, "Круглый бассейн во дворе и ужины при фонарях — вилла правда как в Испании.", "2026-08-19")
        r("l13", "r69", "Ангелина Ф.", 4, "Красиво и уютно, но с маленькими детьми до пляжа идти минут пятнадцать.", "2026-07-08")
        r("l13", "r70", "Илья Н.", 5, "Персонал готовил завтраки и ужины, всё вкусно. Лоо — спокойный посёлок без сочинской суеты.", "2025-09-12")
        r("l13", "r71", "Таисия М.", 5, "Ездили в начале октября: море ещё тёплое, на пляже почти никого. Вилла уютная, сад красивый.", "2025-10-06")
        r("l14", "r72", "Николай", 5, "Старинный дом с душой — камин, сад и тихая улица Амалиенау. Хозяйка рассказала историю дома.", "2026-09-20")
        r("l14", "r73", "Светлана", 5, "Красивые интерьеры, до центра рукой подать. Сауна — отдельное удовольствие.", "2026-06-28")
        r("l14", "r74", "Герман А.", 5, "Прогулки по Амалиенау — отдельное удовольствие: старые виллы, каштаны. А дом внутри ещё красивее.", "2026-05-23")
        r("l14", "r75", "Эмилия К.", 5, "Елена рассказала историю дома — ему больше ста лет. Вечером камин и сауна, утром кофе в саду.", "2026-09-06")
        r("l14", "r76", "Лев С.", 4, "Отличная вилла, но во двор помещается только одна машина, вторую оставляли на улице.", "2026-08-01")
        r("l14", "r77", "Фаина Р.", 5, "До моря сорок минут — ездили в Светлогорск и Зеленоградск. Возвращаться в такой дом было приятно.", "2025-07-27")
        r("l15", "r78", "Игорь", 5, "Зимой катались на лыжах прямо от крыльца, потом баня. Тишина и сосны.", "2026-02-21")
        r("l15", "r79", "Дарина В.", 5, "Летом брали катамаран, до воды двести метров через бор. Вечером баня и шашлыки на веранде.", "2026-07-26")
        r("l15", "r80", "Савелий О.", 4, "Шале уютное, но всего две ванные на восемь человек — по утрам очередь.", "2026-08-09")
        r("l15", "r81", "Регина Т.", 5, "Сосны, камин и тишина. Денис оставил дрова и всё объяснил про баню.", "2025-12-20")
        r("l15", "r82", "Тихон Б.", 5, "Ездили с друзьями на выходные из Новосибирска — сорок минут, и ты в лесу у моря.", "2026-05-30")
        r("l16", "r83", "Элеонора П.", 5, "Дом среди сосен, белки прямо у окна. Гуляли в Ботаническом саду и к Утиному озеру.", "2026-09-13")
        r("l16", "r84", "Марат З.", 5, "Умный дом и зарядка для электромобиля — приехали на своей машине, всё работало без сбоев.", "2026-08-22")
        r("l16", "r85", "Олеся Н.", 4, "Красивый современный дом, но до пляжа на Обском море минут пятнадцать пешком — с детьми ездили на машине.", "2026-07-31")
        r("l17", "r86", "Юлия", 5, "Закаты над проливом и вид на мост — ради этого стоит лететь через всю страну.", "2026-09-08")
        r("l17", "r87", "Виталий Г.", 5, "С террасы виден Русский мост, вечером он подсвечен — очень красиво. Повар готовил морепродукты, всё свежее.", "2026-08-12")
        r("l17", "r88", "Снежана М.", 5, "Спускались к морю прямо от участка, вода в августе тёплая. Бассейн и горничная — отдых без забот.", "2026-08-27")
        r("l17", "r89", "Родион С.", 4, "Вилла шикарная, но в июле стоял туман, и мост было видно не каждый день.", "2026-07-09")
        r("l17", "r90", "Варвара Л.", 5, "Прилетели на день рождения подруги — отличный дом на двенадцать человек, всё было готово к приезду.", "2026-09-19")
        r("l18", "r91", "Кира Д.", 5, "До моря три минуты, а дом стоит в лесу: днём пляж, вечером сауна и терраса на крыше.", "2026-08-17")
        r("l18", "r92", "Елисей В.", 4, "Хороший дом, но в выходные на Шаморе много людей, и дорога к бухте забита машинами.", "2026-08-02")
        r("l19", "r93", "Ринат", 5, "Причал, бассейн с подогревом и баня — провели здесь корпоратив, все в восторге.", "2026-08-25")
        r("l19", "r94", "Айгуль Н.", 5, "Сосновый бор, Волга в двух шагах и свой причал — утром пили кофе прямо у воды.", "2026-07-20")
        r("l19", "r95", "Динар Х.", 5, "Бассейн с подогревом работал даже в сентябре. Рустам организовал прогулку на катере.", "2026-09-10")
        r("l19", "r96", "Зарина Г.", 4, "Отличная вилла, но до Казани далековато — в кремль ехали около часа.", "2026-06-13")
        r("l19", "r97", "Тагир С.", 5, "Хамам и кинозал после дня на воде — идеально. Персонал вежливый и незаметный.", "2025-08-31")
        r("l20", "r98", "Алсу Р.", 5, "Ехали с тремя детьми — детская площадка и газон прямо у дома, а мы жарили мясо на террасе.", "2026-06-21")
        r("l20", "r99", "Ильдар М.", 4, "Удобный дом для семьи, гараж на две машины. Но по соседству строят дома, днём бывает шумно.", "2026-08-05")
        r("l20", "r100", "Лейла Б.", 5, "Гульнара встретила с чак-чаком, всё чисто и уютно. До аэропорта правда около двадцати пяти минут.", "2026-05-09")
        r("l20", "r101", "Роберт Ш.", 4, "Тихий посёлок и хороший дом. Не хватает бассейна, но за эти деньги — отлично.", "2026-07-14")
        r("l20", "r102", "Камила Ф.", 5, "Жили неделю, пока шёл ремонт в квартире. Тихо, чисто, до центра Казани добираться удобно.", "2026-02-28")
        r("l21", "r103", "Анастасия", 5, "До подъёмников пять минут, а вечером камин и вид на лес. Обязательно вернёмся зимой.", "2026-03-09")
        r("l21", "r104", "Степан Ю.", 5, "Катались на Уктусе каждый день — до склона пять минут пешком. Вечером камин и баня.", "2026-01-24")
        r("l21", "r105", "Лидия Ф.", 5, "Панорамные окна в лес, тишина, а до центра Екатеринбурга двадцать минут. Ирина очень гостеприимная.", "2026-03-07")
        r("l21", "r106", "Анатолий Е.", 4, "Хорошее шале, но летом у речки много комаров — берите репелленты.", "2026-07-02")
        r("l21", "r107", "Влада К.", 5, "Днём сходили в термы неподалёку, вечером баня в шале — двойное удовольствие.", "2025-12-14")
        r("l21", "r108", "Платон Р.", 5, "Детям понравился тюбинг, взрослым — терраса с видом на лес. Отличное место для зимних выходных.", "2026-02-01")
        r("l22", "r109", "Максим", 5, "Катались на сапбордах прямо от причала. Дом большой, хватило на две семьи.", "2026-07-30")
        r("l22", "r110", "Евгений Ш.", 5, "Бассейн с видом на пруд, вечером закат над водой. Кинозал и сауна — всё в одном доме.", "2026-08-15")
        r("l22", "r111", "Розалия А.", 4, "Красиво и удобно, до центра двадцать минут. Но в выходные с пляжного клуба на берегу доносится музыка.", "2026-07-18")
        r("l22", "r112", "Ян Т.", 5, "Территория охраняется, спокойно отпускали детей к воде. Ирина на связи в любое время.", "2026-06-06")
        r("l22", "r113", "Серафима Д.", 4, "Хорошая вилла, но сапбордов всего два — на двенадцать человек мало.", "2025-08-09")

        // число отзывов и рейтинг — из самих отзывов, чтобы карточка совпадала со списком
        s.listings.forEach { recalcRating(it, s.reviews[it.id].orEmpty()) }

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
        const val KEY_STATE = "state_v10" // v10: рейтинг и число отзывов считаются из отзывов
        const val MAX_NIGHTS = 30
        val DEFAULT_IMAGE = DemoPhotos.uri("l1")
    }
}
