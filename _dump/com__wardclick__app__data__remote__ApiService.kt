package com.wardclick.app.data.remote

import com.wardclick.app.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): AuthResponse

    @GET("meta/cities")
    suspend fun getCities(): List<CityDto>

    @GET("meta/property-types")
    suspend fun getPropertyTypes(): List<PropertyTypeDto>

    @GET("meta/amenities")
    suspend fun getAmenities(): List<AmenityDto>

    @GET("listings")
    suspend fun getListings(
        @Query("cityId") cityId: String? = null,
        @Query("typeId") typeId: String? = null,
        @Query("guests") guests: Int? = null,
        @Query("checkIn") checkIn: String? = null,
        @Query("checkOut") checkOut: String? = null
    ): List<ListingSummaryDto>

    @GET("listings/featured")
    suspend fun getFeaturedListings(): List<ListingSummaryDto>

    @GET("listings/{id}")
    suspend fun getListingDetails(@Path("id") id: String): ListingDetailsDto

    @GET("listings/{id}/availability")
    suspend fun getListingAvailability(@Path("id") id: String): AvailabilityDto

    @GET("listings/{id}/reviews")
    suspend fun getListingReviews(@Path("id") id: String): List<ReviewDto>

    @POST("listings/{id}/reviews")
    suspend fun addReview(@Path("id") id: String, @Body request: ReviewRequest): ReviewDto

    @POST("bookings")
    suspend fun createBooking(@Body request: CreateBookingRequest): BookingDto

    @GET("bookings/my")
    suspend fun getMyBookings(): List<BookingDto>

    @PUT("bookings/{id}/cancel")
    suspend fun cancelBooking(@Path("id") id: String): BookingDto

    // Admin endpoints
    @POST("admin/listings")
    suspend fun createListing(@Body request: ListingRequest): ListingDetailsDto

    @PUT("admin/listings/{id}")
    suspend fun updateListing(@Path("id") id: String, @Body request: ListingRequest): ListingDetailsDto

    @DELETE("admin/listings/{id}")
    suspend fun hideListing(@Path("id") id: String): ListingDetailsDto

    @GET("admin/bookings")
    suspend fun getAllBookings(): List<BookingDto>

    @PUT("admin/bookings/{id}/confirm")
    suspend fun confirmBooking(@Path("id") id: String): BookingDto

    @PUT("admin/bookings/{id}/cancel")
    suspend fun cancelAdminBooking(@Path("id") id: String): BookingDto

    @GET("admin/stats")
    suspend fun getAdminStats(): StatsDto
}
