package com.wardclick.app.ui.navigation

import kotlinx.serialization.Serializable

/** Профиль автора отзыва. Вместе с именем передаётся сам отзыв, чтобы показать его в профиле. */
@Serializable
data class ReviewerRoute(
    val name: String,
    val userId: String = "",
    val listingTitle: String = "",
    val rating: Double = 0.0,
    val comment: String = "",
    val date: String = ""
)
