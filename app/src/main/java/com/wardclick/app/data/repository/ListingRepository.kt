package com.wardclick.app.data.repository

import com.wardclick.app.data.model.AmenityDto
import com.wardclick.app.data.model.AvailabilityDto
import com.wardclick.app.data.model.CityDto
import com.wardclick.app.data.model.ListingDetailsDto
import com.wardclick.app.data.model.ListingSummaryDto
import com.wardclick.app.data.model.PropertyTypeDto
import com.wardclick.app.data.model.ReviewDto
import com.wardclick.app.data.model.VipServiceDto
import com.wardclick.app.data.remote.ApiService

class ListingRepository(private val apiService: ApiService) {

    suspend fun getListings(
        cityId: String? = null,
        typeId: String? = null,
        guests: Int? = null,
        checkIn: String? = null,
        checkOut: String? = null,
        query: String? = null
    ): ApiResult<List<ListingSummaryDto>> {
        return safeApiCall {
            apiService.getListings(cityId, typeId, guests, checkIn, checkOut, query)
        }
    }

    suspend fun getFeaturedListings(): ApiResult<List<ListingSummaryDto>> =
        safeApiCall { apiService.getFeaturedListings() }

    suspend fun getListingDetails(id: String): ApiResult<ListingDetailsDto> =
        safeApiCall { apiService.getListingDetails(id) }

    suspend fun getListingAvailability(id: String): ApiResult<AvailabilityDto> =
        safeApiCall { apiService.getListingAvailability(id) }

    suspend fun getReviews(id: String): ApiResult<List<ReviewDto>> =
        safeApiCall { apiService.getListingReviews(id) }

    suspend fun getCities(): ApiResult<List<CityDto>> =
        safeApiCall { apiService.getCities() }

    suspend fun getPropertyTypes(): ApiResult<List<PropertyTypeDto>> =
        safeApiCall { apiService.getPropertyTypes() }

    suspend fun getAmenities(): ApiResult<List<AmenityDto>> =
        safeApiCall { apiService.getAmenities() }

    suspend fun getVipServices(): ApiResult<List<VipServiceDto>> =
        safeApiCall { apiService.getVipServices() }

    suspend fun getListingVipServices(id: String): ApiResult<List<VipServiceDto>> =
        safeApiCall { apiService.getListingVipServices(id) }
}
