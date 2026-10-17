package com.wardclick.app.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.wardclick.app.ui.viewmodels.*

class ViewModelFactory(private val appContainer: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(AuthViewModel::class.java) ->
                AuthViewModel(appContainer.authRepository) as T
            modelClass.isAssignableFrom(HomeViewModel::class.java) ->
                HomeViewModel(appContainer.listingRepository, appContainer.favoriteRepository) as T
            modelClass.isAssignableFrom(DetailsViewModel::class.java) ->
                DetailsViewModel(appContainer.listingRepository, appContainer.favoriteRepository) as T
            modelClass.isAssignableFrom(BookingViewModel::class.java) ->
                BookingViewModel(appContainer.listingRepository, appContainer.bookingRepository, appContainer.cartRepository) as T
            modelClass.isAssignableFrom(BookingsViewModel::class.java) ->
                BookingsViewModel(appContainer.bookingRepository) as T
            modelClass.isAssignableFrom(FavoritesViewModel::class.java) ->
                FavoritesViewModel(appContainer.favoriteRepository) as T
            modelClass.isAssignableFrom(ProfileViewModel::class.java) ->
                ProfileViewModel(appContainer.authRepository, appContainer.sessionManager) as T
            modelClass.isAssignableFrom(AdminViewModel::class.java) ->
                AdminViewModel(appContainer.adminRepository, appContainer.listingRepository) as T
            modelClass.isAssignableFrom(VipViewModel::class.java) ->
                VipViewModel(appContainer.listingRepository) as T
            modelClass.isAssignableFrom(CartViewModel::class.java) ->
                CartViewModel(appContainer.cartRepository, appContainer.bookingRepository) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
