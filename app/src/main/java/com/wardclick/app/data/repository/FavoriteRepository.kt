package com.wardclick.app.data.repository

import android.content.Context
import com.wardclick.app.data.model.ListingDetailsDto
import com.wardclick.app.data.remote.ApiService

class FavoriteRepository(private val context: Context, private val apiService: ApiService) {
    private val prefs = context.getSharedPreferences("favorites", Context.MODE_PRIVATE)

    fun getFavoriteIds(): Set<String> {
        return prefs.getStringSet("favorite_ids", emptySet()) ?: emptySet()
    }

    suspend fun getFavorites(): ApiResult<List<ListingDetailsDto>> {
        val ids = getFavoriteIds()
        val favorites = mutableListOf<ListingDetailsDto>()
        for (id in ids) {
            val result = safeApiCall { apiService.getListingDetails(id) }
            if (result is ApiResult.Success) {
                favorites.add(result.data)
            }
        }
        return ApiResult.Success(favorites)
    }

    fun toggleFavorite(listingId: String): Boolean {
        val ids = getFavoriteIds().toMutableSet()
        val isNowFavorite = if (ids.contains(listingId)) {
            ids.remove(listingId)
            false
        } else {
            ids.add(listingId)
            true
        }
        prefs.edit().putStringSet("favorite_ids", ids).apply()
        return isNowFavorite
    }
    
    fun isFavorite(listingId: String): Boolean {
        return getFavoriteIds().contains(listingId)
    }
}
