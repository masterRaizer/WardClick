package com.wardclick.app.data.repository

import com.wardclick.app.data.model.BookingDto
import com.wardclick.app.data.model.CreateBookingRequest
import com.wardclick.app.data.model.CreateOrderRequest
import com.wardclick.app.data.model.ReviewDto
import com.wardclick.app.data.model.ReviewRequest
import com.wardclick.app.data.remote.ApiService

class BookingRepository(private val apiService: ApiService) {
    suspend fun createBooking(request: CreateBookingRequest): ApiResult<BookingDto> {
        return safeApiCall { apiService.createBooking(request) }
    }
    
    suspend fun createOrder(request: CreateOrderRequest): ApiResult<List<BookingDto>> {
        return safeApiCall { apiService.createOrder(request) }
    }

    suspend fun getMyBookings(): ApiResult<List<BookingDto>> {
        return safeApiCall { apiService.getMyBookings() }
    }
    
    suspend fun cancelBooking(id: String): ApiResult<BookingDto> {
        return safeApiCall { apiService.cancelBooking(id) }
    }
    
    suspend fun addReview(listingId: String, request: ReviewRequest): ApiResult<ReviewDto> {
        return safeApiCall { apiService.addReview(listingId, request) }
    }
}
