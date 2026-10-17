package com.wardclick.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wardclick.app.ui.theme.Star
import com.wardclick.app.util.reviewsText
import java.util.Locale

/** Компактный рейтинг: ★ 4.8 (23 отзыва). Для объектов без отзывов — «Новый». */
@Composable
fun RatingBadge(rating: Double, reviews: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Star, contentDescription = null, tint = Star, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(2.dp))
        if (reviews <= 0) {
            Text("Новый", style = MaterialTheme.typography.labelLarge)
        } else {
            Text(
                String.format(Locale.US, "%.1f", rating),
                style = MaterialTheme.typography.labelLarge
            )
            Spacer(Modifier.width(4.dp))
            Text(
                "(${reviewsText(reviews)})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Выбор оценки 1..5 нажатием на звёзды. */
@Composable
fun RatingPicker(
    value: Int,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    starSize: Dp = 36.dp
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (i in 1..5) {
            Icon(
                imageVector = if (i <= value) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = "Оценка $i",
                tint = Star,
                modifier = Modifier.size(starSize).clickable { onChange(i) }
            )
        }
    }
}

/** Только для показа: заполненные звёзды по округлённому рейтингу. */
@Composable
fun RatingStars(rating: Double, modifier: Modifier = Modifier, starSize: Dp = 14.dp) {
    val full = Math.round(rating).toInt().coerceIn(0, 5)
    Row(modifier = modifier) {
        for (i in 1..5) {
            Icon(
                imageVector = if (i <= full) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = null,
                tint = Star,
                modifier = Modifier.size(starSize)
            )
        }
    }
}
