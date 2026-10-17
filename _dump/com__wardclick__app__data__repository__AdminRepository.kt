package com.wardclick.app.data.repository

import com.wardclick.app.data.model.BookingDto
import com.wardclick.app.data.model.ListingDetailsDto
import com.wardclick.app.data.model.ListingRequest
import com.wardclick.app.data.model.StatsDto
import com.wardclick.app.data.remote.ApiService

class AdminRepository(private val apiService: ApiService) {
    suspend fun getStats(): ApiResult<StatsDto> {
        return safeApiCall { apiService.getAdminStats() }
    }

    suspend fun createListing(request: ListingRequest): ApiResult<ListingDetailsDto> {
        return safeApiCall { apiService.createListing(request) }
    }

    suspend fun updateListing(id: String, request: ListingRequest): ApiResult<ListingDetailsDto> {
        return safeApiCall { apiService.updateListing(id, request) }
    }

    suspend fun hideListing(id: String): ApiResult<ListingDetailsDto> {
        return safeApiCall { apiService.hideListing(id) }
    }

    suspend fun getAllBookings(): ApiResult<List<BookingDto>> {
        return safeApiCall { apiService.getAllBookings() }
    }

    suspend fun confirmBooking(id: String): ApiResult<BookingDto> {
        return safeApiCall { apiService.confirmBooking(id) }
    }

    suspend fun cancelAdminBooking(id: String): ApiResult<BookingDto> {
        return safeApiCall { apiService.cancelAdminBooking(id) }
    }
}
