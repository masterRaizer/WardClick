package com.wardclick.app.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
data object AuthRoute

@Serializable
data object RegisterRoute

@Serializable
data object HomeRoute

@Serializable
data class DetailsRoute(val listingId: String)

@Serializable
data class BookingRoute(val listingId: String)

@Serializable
data object BookingsRoute

@Serializable
data object FavoritesRoute

@Serializable
data object ProfileRoute

@Serializable
data object AdminRoute
