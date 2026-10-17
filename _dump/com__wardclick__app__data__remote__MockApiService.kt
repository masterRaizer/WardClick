package com.wardclick.app.data.remote

import com.wardclick.app.data.model.*
import kotlinx.coroutines.delay
import java.math.BigDecimal
import java.util.UUID

class MockApiService : ApiService {

    private val users = mutableListOf(
        AuthResponse("mock-admin-token", "admin-1", "admin@wardclick.ru", "Admin", "ADMIN"),
        AuthResponse("mock-user-token", "user-1", "user@wardclick.ru", "Test User", "USER")
    )

    private val cities = mutableListOf(
        CityDto("c1", "Москва"),
        CityDto("c2", "Санкт-Петербург"),
        CityDto("c3", "Сочи"),
        CityDto("c4", "Казань"),
        CityDto("c5", "Екатеринбург"),
        CityDto("c6", "Новосибирск"),
        CityDto("c7", "Калининград"),
        CityDto("c8", "Владивосток")
    )

    private val propertyTypes = mutableListOf(
        PropertyTypeDto("t1", "Квартира", "apartment"),
        PropertyTypeDto("t2", "Дом", "house"),
        PropertyTypeDto("t3", "Вилла", "villa"),
        PropertyTypeDto("t4", "Отель", "hotel")
    )

    private val amenities = mutableListOf(
        AmenityDto("a1", "Wi-Fi", "wifi"),
        AmenityDto("a2", "Кухня", "kitchen"),
        AmenityDto("a3", "Кондиционер", "ac"),
        AmenityDto("a4", "Бассейн", "pool"),
        AmenityDto("a5", "Бесплатная парковка", "parking")
    )

    private val listings = mutableListOf(
        ListingDetailsDto().apply {
            id = "l1"
            title = "Светлая студия в центре"
            description = "Отличная студия в самом сердце города."
            city = cities[0]
            address = "Москва, Тверская 12"
            propertyType = propertyTypes[0]
            imageUrls = listOf("https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?w=800")
            pricePerNight = BigDecimal("3500")
            rating = 4.8
            reviewsCount = 42
            maxGuests = 2
            bedrooms = 1
            beds = 1
            bathrooms = 1
            amenities = listOf(this@MockApiService.amenities[0], this@MockApiService.amenities[2])
            hostId = "h1"
            hostName = "Анна"
            hostAvatarUrl = ""
            isSuperhost = true
        },
        ListingDetailsDto().apply {
            id = "l2"
            title = "Лофт с видом на Неву"
            description = "Просторный лофт для ценителей искусства."
            city = cities[1]
            address = "Санкт-Петербург, Невский проспект 40"
            propertyType = propertyTypes[0]
            imageUrls = listOf("https://images.unsplash.com/photo-1502672260266-1c1de2d96674?w=800")
            pricePerNight = BigDecimal("5500")
            rating = 4.9
            reviewsCount = 120
            maxGuests = 4
            bedrooms = 2
            beds = 2
            bathrooms = 2
            amenities = this@MockApiService.amenities.toList()
            hostId = "h2"
            hostName = "Павел"
            hostAvatarUrl = ""
            isSuperhost = true
        },
        ListingDetailsDto().apply {
            id = "l3"
            title = "Уютный домик у моря"
            description = "Идеальное место для отдыха с семьей."
            city = cities[2]
            address = "Сочи, Приморская 5"
            propertyType = propertyTypes[1]
            imageUrls = listOf("https://images.unsplash.com/photo-1564013799919-ab600027ffc6?w=800")
            pricePerNight = BigDecimal("8000")
            rating = 4.7
            reviewsCount = 35
            maxGuests = 6
            bedrooms = 3
            beds = 4
            bathrooms = 2
            amenities = listOf(this@MockApiService.amenities[0], this@MockApiService.amenities[1], this@MockApiService.amenities[4])
            hostId = "h3"
            hostName = "Михаил"
            hostAvatarUrl = ""
            isSuperhost = false
        },
        ListingDetailsDto().apply {
            id = "l4"
            title = "Апартаменты в Кремле"
            description = "Жизнь как в сказке."
            city = cities[3]
            address = "Казань, Кремлевская 1"
            propertyType = propertyTypes[0]
            imageUrls = listOf("https://images.unsplash.com/photo-1522771739844-6a9f6d5f14af?w=800")
            pricePerNight = BigDecimal("4500")
            rating = 4.6
            reviewsCount = 15
            maxGuests = 3
            bedrooms = 1
            beds = 2
            bathrooms = 1
            amenities = listOf(this@MockApiService.amenities[0], this@MockApiService.amenities[1])
            hostId = "h1"
            hostName = "Анна"
            hostAvatarUrl = ""
            isSuperhost = true
        },
        ListingDetailsDto().apply {
            id = "l5"
            title = "Вилла с бассейном"
            description = "Роскошный отдых."
            city = cities[2]
            address = "Сочи, Курортный проспект 100"
            propertyType = propertyTypes[2]
            imageUrls = listOf("https://images.unsplash.com/photo-1512917774080-9991f1c4c750?w=800")
            pricePerNight = BigDecimal("25000")
            rating = 5.0
            reviewsCount = 8
            maxGuests = 10
            bedrooms = 5
            beds = 6
            bathrooms = 4
            amenities = this@MockApiService.amenities.toList()
            hostId = "h4"
            hostName = "Олег"
            hostAvatarUrl = ""
            isSuperhost = true
        },
        ListingDetailsDto().apply {
            id = "l6"
            title = "Квартира с террасой"
            description = "Панорамный вид на горы."
            city = cities[4]
            address = "Екатеринбург, Ленина 50"
            propertyType = propertyTypes[0]
            imageUrls = listOf("https://images.unsplash.com/photo-1501183638710-841dd1904471?w=800")
            pricePerNight = BigDecimal("4000")
            rating = 4.5
            reviewsCount = 20
            maxGuests = 2
            bedrooms = 1
            beds = 1
            bathrooms = 1
            amenities = listOf(this@MockApiService.amenities[0], this@MockApiService.amenities[1])
            hostId = "h2"
            hostName = "Павел"
            hostAvatarUrl = ""
            isSuperhost = false
        },
        ListingDetailsDto().apply {
            id = "l7"
            title = "Шале в лесу"
            description = "Единение с природой."
            city = cities[5]
            address = "Новосибирск, Лесная 10"
            propertyType = propertyTypes[1]
            imageUrls = listOf("https://images.unsplash.com/photo-1542314831-c53cd4b85af4?w=800")
            pricePerNight = BigDecimal("6000")
            rating = 4.8
            reviewsCount = 50
            maxGuests = 4
            bedrooms = 2
            beds = 2
            bathrooms = 1
            amenities = listOf(this@MockApiService.amenities[1], this@MockApiService.amenities[4])
            hostId = "h3"
            hostName = "Михаил"
            hostAvatarUrl = ""
            isSuperhost = true
        },
        ListingDetailsDto().apply {
            id = "l8"
            title = "Исторический особняк"
            description = "Почувствуйте дух старины."
            city = cities[6]
            address = "Калининград, Тельмана 15"
            propertyType = propertyTypes[1]
            imageUrls = listOf("https://images.unsplash.com/photo-1510798831971-661eb04b3739?w=800")
            pricePerNight = BigDecimal("12000")
            rating = 4.9
            reviewsCount = 60
            maxGuests = 8
            bedrooms = 4
            beds = 5
            bathrooms = 3
            amenities = this@MockApiService.amenities.toList()
            hostId = "h5"
            hostName = "Елена"
            hostAvatarUrl = ""
            isSuperhost = true
        }
    )

    private val reviews = mutableListOf(
        ReviewDto().apply {
            id = "r1"
            authorName = "Иван"
            authorAvatarUrl = ""
            rating = 5.0
            comment = "Отличное место, все очень понравилось!"
            createdAt = "2023-10-01"
        }
    )

    private val bookings = mutableListOf<BookingDto>()

    override suspend fun login(request: LoginRequest): AuthResponse {
        delay(500)
        val user = users.find { it.email == request.email }
        if (user != null && request.password == "123456") {
            return user
        }
        throw Exception("Invalid credentials")
    }

    override suspend fun register(request: RegisterRequest): AuthResponse {
        delay(500)
        val newUser = AuthResponse(
            "mock-token-${UUID.randomUUID()}",
            "user-${users.size + 1}",
            request.email,
            request.fullName,
            "USER"
        )
        users.add(newUser)
        return newUser
    }

    override suspend fun getCities(): List<CityDto> {
        delay(300)
        return cities
    }

    override suspend fun getPropertyTypes(): List<PropertyTypeDto> {
        delay(300)
        return propertyTypes
    }

    override suspend fun getAmenities(): List<AmenityDto> {
        delay(300)
        return amenities
    }

    override suspend fun getListings(
        cityId: String?,
        typeId: String?,
        guests: Int?,
        checkIn: String?,
        checkOut: String?
    ): List<ListingSummaryDto> {
        delay(800)
        var result = listings.toList()
        if (!cityId.isNullOrEmpty()) {
            result = result.filter { it.city.id == cityId }
        }
        if (!typeId.isNullOrEmpty()) {
            result = result.filter { it.propertyType.id == typeId }
        }
        if (guests != null) {
            result = result.filter { it.maxGuests >= guests }
        }
        return result.map { toSummary(it) }
    }

    override suspend fun getFeaturedListings(): List<ListingSummaryDto> {
        delay(500)
        return listings.take(5).map { toSummary(it) }
    }

    override suspend fun getListingDetails(id: String): ListingDetailsDto {
        delay(500)
        return listings.find { it.id == id } ?: throw Exception("Not found")
    }

    override suspend fun getListingAvailability(id: String): AvailabilityDto {
        delay(300)
        val listingBookings = bookings.filter { it.listingId == id && it.status != "CANCELLED" }
        val bookedDates = mutableListOf<String>()
        // Simplistic date mock
        listingBookings.forEach { b ->
            bookedDates.add(b.checkInDate)
            bookedDates.add(b.checkOutDate)
        }
        return AvailabilityDto().apply {
            listingId = id
            this.bookedDates = bookedDates
        }
    }

    override suspend fun getListingReviews(id: String): List<ReviewDto> {
        delay(500)
        return reviews // Return same mock reviews for all for simplicity
    }

    override suspend fun addReview(id: String, request: ReviewRequest): ReviewDto {
        delay(500)
        val newReview = ReviewDto().apply {
            this.id = UUID.randomUUID().toString()
            authorName = "Test User"
            authorAvatarUrl = ""
            rating = request.rating
            comment = request.comment
            createdAt = "2024-01-01"
        }
        reviews.add(newReview)
        return newReview
    }

    override suspend fun createBooking(request: CreateBookingRequest): BookingDto {
        delay(800)
        val listing = listings.find { it.id == request.listingId } ?: throw Exception("Not found")
        
        // Conflict checking
        val hasConflict = bookings.any { b ->
            b.listingId == request.listingId && b.status != "CANCELLED" &&
            !(request.checkOutDate <= b.checkInDate || request.checkInDate >= b.checkOutDate)
        }
        if (hasConflict) {
            throw Exception("Даты уже заняты")
        }

        val booking = BookingDto().apply {
            id = UUID.randomUUID().toString()
            listingId = request.listingId
            listingTitle = listing.title
            listingImageUrl = listing.imageUrls.firstOrNull() ?: ""
            listingLocation = "${listing.city.name}, ${listing.address}"
            checkInDate = request.checkInDate
            checkOutDate = request.checkOutDate
            guestsCount = request.guestsCount
            totalPrice = request.totalPrice
            status = "PENDING"
        }
        bookings.add(booking)
        return booking
    }

    override suspend fun getMyBookings(): List<BookingDto> {
        delay(600)
        return bookings // Mocking for current user
    }

    override suspend fun cancelBooking(id: String): BookingDto {
        delay(500)
        val booking = bookings.find { it.id == id } ?: throw Exception("Not found")
        booking.status = "CANCELLED"
        return booking
    }

    override suspend fun createListing(request: ListingRequest): ListingDetailsDto {
        delay(800)
        val city = cities.find { it.id == request.cityId } ?: cities[0]
        val type = propertyTypes.find { it.id == request.propertyTypeId } ?: propertyTypes[0]
        val selAmenities = amenities.filter { request.amenityIds?.contains(it.id) == true }

        val newListing = ListingDetailsDto().apply {
            id = "l${listings.size + 1}"
            title = request.title
            description = request.description
            this.city = city
            address = request.address
            propertyType = type
            imageUrls = request.imageUrls ?: listOf()
            pricePerNight = request.pricePerNight
            rating = 0.0
            reviewsCount = 0
            maxGuests = request.maxGuests
            bedrooms = request.bedrooms
            beds = request.beds
            bathrooms = request.bathrooms
            this.amenities = selAmenities
            hostId = "admin-1"
            hostName = "Admin"
            hostAvatarUrl = ""
            isSuperhost = true
        }
        listings.add(newListing)
        return newListing
    }

    override suspend fun updateListing(id: String, request: ListingRequest): ListingDetailsDto {
        delay(500)
        val listing = listings.find { it.id == id } ?: throw Exception("Not found")
        val city = cities.find { it.id == request.cityId } ?: cities[0]
        val type = propertyTypes.find { it.id == request.propertyTypeId } ?: propertyTypes[0]
        val selAmenities = amenities.filter { request.amenityIds?.contains(it.id) == true }
        
        listing.apply {
            title = request.title
            description = request.description
            this.city = city
            address = request.address
            propertyType = type
            imageUrls = request.imageUrls ?: listOf()
            pricePerNight = request.pricePerNight
            maxGuests = request.maxGuests
            bedrooms = request.bedrooms
            beds = request.beds
            bathrooms = request.bathrooms
            this.amenities = selAmenities
        }
        return listing
    }

    override suspend fun hideListing(id: String): ListingDetailsDto {
        delay(500)
        val listing = listings.find { it.id == id } ?: throw Exception("Not found")
        // No explicit hide property in DTO, we might just return it
        return listing
    }

    override suspend fun getAllBookings(): List<BookingDto> {
        delay(500)
        return bookings
    }

    override suspend fun confirmBooking(id: String): BookingDto {
        delay(500)
        val booking = bookings.find { it.id == id } ?: throw Exception("Not found")
        booking.status = "CONFIRMED"
        return booking
    }

    override suspend fun cancelAdminBooking(id: String): BookingDto {
        delay(500)
        val booking = bookings.find { it.id == id } ?: throw Exception("Not found")
        booking.status = "CANCELLED"
        return booking
    }

    override suspend fun getAdminStats(): StatsDto {
        delay(500)
        return StatsDto().apply {
            totalUsers = users.size
            totalListings = listings.size
            totalBookings = bookings.size
            totalRevenue = bookings.filter { it.status == "CONFIRMED" }
                .fold(BigDecimal.ZERO) { acc, b -> acc.add(b.totalPrice) }
        }
    }

    private fun toSummary(details: ListingDetailsDto): ListingSummaryDto {
        return ListingSummaryDto().apply {
            id = details.id
            title = details.title
            location = "${details.city.name}, ${details.address}"
            imageUrl = details.imageUrls.firstOrNull() ?: ""
            pricePerNight = details.pricePerNight
            rating = details.rating
            reviewsCount = details.reviewsCount
            val currentTags = mutableListOf<String>()
            if (details.isSuperhost) currentTags.add("Суперхозяин")
            if (details.reviewsCount < 5) currentTags.add("Новинка")
            tags = currentTags
        }
    }
}
