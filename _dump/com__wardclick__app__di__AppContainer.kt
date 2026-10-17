package com.wardclick.app.di

import android.content.Context
import com.wardclick.app.BuildConfig
import com.wardclick.app.data.local.SessionManager
import com.wardclick.app.data.remote.ApiService
import com.wardclick.app.data.remote.MockApiService
import com.wardclick.app.data.remote.RetrofitClient
import com.wardclick.app.data.repository.*

class AppContainer(private val context: Context) {
    val sessionManager: SessionManager by lazy {
        SessionManager(context)
    }

    val apiService: ApiService by lazy {
        if (BuildConfig.USE_MOCK) {
            MockApiService()
        } else {
            RetrofitClient.create(context, sessionManager)
        }
    }

    val authRepository: AuthRepository by lazy {
        AuthRepository(apiService, sessionManager)
    }

    val listingRepository: ListingRepository by lazy {
        ListingRepository(apiService)
    }

    val bookingRepository: BookingRepository by lazy {
        BookingRepository(apiService)
    }

    val favoriteRepository: FavoriteRepository by lazy {
        FavoriteRepository(context, apiService)
    }

    val adminRepository: AdminRepository by lazy {
        AdminRepository(apiService)
    }
}
