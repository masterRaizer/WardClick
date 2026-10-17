package com.wardclick.app.ui.components

import com.wardclick.app.ui.theme.liquidGlass
import androidx.compose.foundation.background
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.wardclick.app.util.DemoPhotos
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Brush
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.material.icons.filled.Home
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.wardclick.app.data.model.ListingSummaryDto
import com.wardclick.app.ui.theme.Accent
import com.wardclick.app.util.rub

/**
 * Фото объекта. Демо-фото («demo://…») берутся из ресурсов приложения, остальные — из сети.
 * При сетевой ошибке делается до двух повторов, потом показывается аккуратная заглушка
 * (а не пустой серый прямоугольник).
 */
@Composable
fun RemoteImage(url: String?, modifier: Modifier = Modifier, contentDescription: String? = null) {
    val context = LocalContext.current
    val model = remember(url) { DemoPhotos.resolve(context, url) }
    var attempt by remember(model) { mutableIntStateOf(0) }
    var failed by remember(model) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Box(modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant)) {
        if (model == null || failed) {
            PhotoFallback(Modifier.matchParentSize())
        } else {
            key(attempt) {
                // Плавное проявление фото (без «выскакивания» при прокрутке списка)
                val request = remember(model, attempt) {
                    ImageRequest.Builder(context).data(model).crossfade(250).build()
                }
                AsyncImage(
                    model = request,
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize(),
                    onError = {
                        if (model is String && attempt < 2) {
                            scope.launch {
                                delay(1500L * (attempt + 1))
                                attempt++
                            }
                        } else {
                            failed = true
                        }
                    }
                )
            }
        }
    }
}

/** Заглушка, если фото нет или оно не загрузилось. */
@Composable
private fun PhotoFallback(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(
            Brush.linearGradient(listOf(Color(0xFFDCEBF2), Color(0xFFB9D7E5)))
        ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Filled.Home,
            contentDescription = null,
            tint = Color(0xFF0B6E99).copy(alpha = 0.55f),
            modifier = Modifier.size(40.dp)
        )
    }
}

@Composable
fun ListingCard(
    listing: ListingSummaryDto,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
            .liquidGlass(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                RemoteImage(
                    url = listing.imageUrl,
                    contentDescription = listing.title,
                    modifier = Modifier.fillMaxWidth().height(200.dp)
                )
                // метки: «Проверенный владелец», «Новинка»
                Row(
                    modifier = Modifier.align(Alignment.TopStart).padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listing.tags.orEmpty().take(2).forEach { tag ->
                        Surface(shape = RoundedCornerShape(8.dp), color = Color.White.copy(alpha = 0.92f)) {
                            Text(
                                tag,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF222222)
                            )
                        }
                    }
                }
                IconButton(
                    onClick = onFavoriteClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(40.dp)
                        .background(Color.White.copy(alpha = 0.85f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (isFavorite) "Убрать из избранного" else "В избранное",
                        tint = if (isFavorite) Accent else Color(0xFF444444)
                    )
                }
            }
            Column(modifier = Modifier.padding(14.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        listing.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    RatingBadge(listing.rating, listing.reviewsCount)
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    listing.location.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        rub(listing.pricePerNight),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        " / ночь",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
