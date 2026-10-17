package com.wardclick.app.data.repository

import com.wardclick.app.data.model.AvailabilityDto
import com.wardclick.app.data.model.ListingDetailsDto
import com.wardclick.app.data.model.ListingSummaryDto
import com.wardclick.app.data.remote.ApiService

class ListingRepository(private val apiService: ApiService) {
    
    suspend fun getListings(
        cityId: String? = null,
        typeId: String? = null,
        guests: Int? = null,
        checkIn: String? = null,
        checkOut: String? = null
    ): ApiResult<List<ListingSummaryDto>> {
        return safeApiCall {
            apiService.getListings(cityId, typeId, guests, checkIn, checkOut)
        }
    }
    
    suspend fun getFeaturedListings(): ApiResult<List<ListingSummaryDto>> {
        return safeApiCall {
            apiService.getFeaturedListings()
        }
    }
    
    suspend fun getListingDetails(id: String): ApiResult<ListingDetailsDto> {
        return safeApiCall {
            apiService.getListingDetails(id)
        }
    }
    
    suspend fun getListingAvailability(id: String): ApiResult<AvailabilityDto> {
        return safeApiCall {
            apiService.getListingAvailability(id)
        }
    }
}
