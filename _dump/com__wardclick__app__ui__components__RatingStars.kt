package com.wardclick.app.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.StarHalf
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlin.math.floor

@Composable
fun RatingStars(
    rating: Double,
    modifier: Modifier = Modifier,
    starColor: Color = MaterialTheme.colorScheme.primary,
    maxStars: Int = 5
) {
    Row(modifier = modifier) {
        val fullStars = floor(rating).toInt()
        val hasHalfStar = (rating - fullStars) >= 0.5
        val emptyStars = maxStars - fullStars - if (hasHalfStar) 1 else 0

        for (i in 0 until fullStars) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = starColor
            )
        }
        if (hasHalfStar) {
            Icon(
                imageVector = Icons.Default.StarHalf,
                contentDescription = null,
                tint = starColor
            )
        }
        for (i in 0 until emptyStars) {
            Icon(
                imageVector = Icons.Default.StarBorder,
                contentDescription = null,
                tint = starColor
            )
        }
    }
}
