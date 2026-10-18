package com.wardclick.app.ui.screens.details

import com.wardclick.app.ui.components.vipIcon
import com.wardclick.app.util.unitLabel
import com.wardclick.app.util.Pricing
import com.wardclick.app.data.model.VipServiceDto
import com.wardclick.app.util.hasHelipad
import com.wardclick.app.util.ReviewerProfiles
import androidx.compose.runtime.remember
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.HotTub
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Theaters
import androidx.compose.material.icons.filled.RoomService
import androidx.compose.material.icons.filled.Deck
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wardclick.app.data.model.AmenityDto
import com.wardclick.app.data.model.ListingDetailsDto
import com.wardclick.app.data.model.ReviewDto
import com.wardclick.app.ui.components.ErrorBox
import com.wardclick.app.ui.components.LoadingBox
import com.wardclick.app.ui.components.PointMap
import com.wardclick.app.ui.components.RatingBadge
import com.wardclick.app.ui.components.RatingStars
import com.wardclick.app.ui.components.RemoteImage
import com.wardclick.app.ui.components.openInYandexMaps
import com.wardclick.app.ui.theme.Accent
import com.wardclick.app.ui.viewmodels.DetailsViewModel
import com.wardclick.app.util.bathroomsText
import com.wardclick.app.util.bedroomsText
import com.wardclick.app.util.bedsText
import com.wardclick.app.util.guestsText
import com.wardclick.app.util.isoToDisplay
import com.wardclick.app.util.rub

@Composable
fun DetailsScreen(
    listingId: String,
    viewModel: DetailsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToBooking: (String) -> Unit,
    onOpenReviewer: (review: ReviewDto, listingTitle: String) -> Unit = { _, _ -> }
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(listingId) { viewModel.load(listingId) }

    val listing = state.listing
    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                state.isLoading -> {
                    LoadingBox()
                    BackButton(onNavigateBack, Modifier.align(Alignment.TopStart))
                }
                listing == null -> {
                    ErrorBox(message = state.error ?: "Особняк не найден", onRetry = viewModel::retry)
                    BackButton(onNavigateBack, Modifier.align(Alignment.TopStart))
                }
                else -> DetailsContent(
                    listing = listing,
                    reviews = state.reviews,
                    vipServices = state.vipServices,
                    isFavorite = state.isFavorite,
                    onBack = onNavigateBack,
                    onToggleFavorite = viewModel::toggleFavorite,
                    onOpenReviewer = { r -> onOpenReviewer(r, listing.title.orEmpty()) }
                )
            }
        }
        if (listing != null) {
            BookBar(listing = listing, onBook = { onNavigateToBooking(listing.id) })
        }
    }
}

private val PhotoHeight = 260.dp

@Composable
private fun BackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .padding(8.dp)
            .size(40.dp)
            .background(Color.White.copy(alpha = 0.9f), CircleShape)
    ) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = Color(0xFF222222))
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
private fun DetailsContent(
    listing: ListingDetailsDto,
    reviews: List<ReviewDto>,
    vipServices: List<VipServiceDto> = emptyList(),
    isFavorite: Boolean,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenReviewer: (ReviewDto) -> Unit = {}
) {
    val context = LocalContext.current
    val images = listing.imageUrls.orEmpty().filter { it.isNotBlank() }
    val hasCoords = listing.latitude != 0.0 || listing.longitude != 0.0

    // после прокрутки за фото под кнопками «назад» и «избранное» появляется плотная полоса с названием,
    // иначе текст уезжает под круглые кнопки и обрезается у статус-бара
    val listState = rememberLazyListState()
    val collapseAt = with(LocalDensity.current) { (PhotoHeight - 56.dp).roundToPx() }
    val showBar by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > collapseAt }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(PhotoHeight)) {
                    if (images.isEmpty()) {
                        RemoteImage(url = null, modifier = Modifier.fillMaxSize())
                    } else {
                        val pager = rememberPagerState(pageCount = { images.size })
                        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
                            RemoteImage(
                                url = images[page],
                                contentDescription = listing.title,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        if (images.size > 1) {
                            Surface(
                                modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = Color.Black.copy(alpha = 0.6f)
                            ) {
                                Text(
                                    "${pager.currentPage + 1} / ${images.size}",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }
            }

            item {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(listing.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Place, contentDescription = null,
                            modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "${listing.city.name}, ${listing.address}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    RatingBadge(listing.rating, listing.reviewsCount)

                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        InfoChip(listing.propertyType.name)
                        InfoChip("до ${guestsText(listing.maxGuests)}")
                        if (listing.bedrooms > 0) InfoChip(bedroomsText(listing.bedrooms))
                        if (listing.beds > 0) InfoChip(bedsText(listing.beds))
                        if (listing.bathrooms > 0) InfoChip(bathroomsText(listing.bathrooms))
                    }

                    HorizontalDivider()
                    Text(
                        "Владелец: ${listing.hostName.orEmpty()}" + if (listing.isSuperhost) " · Проверенный владелец" else "",
                        style = MaterialTheme.typography.titleSmall
                    )
                    if (!listing.description.isNullOrBlank()) {
                        Text(listing.description, style = MaterialTheme.typography.bodyMedium)
                    }

                    val amenities = listing.amenities.orEmpty()
                    if (amenities.isNotEmpty()) {
                        HorizontalDivider()
                        Text("Удобства", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            amenities.forEach { a ->
                                AssistChip(
                                    onClick = {},
                                    label = { Text(a.name) },
                                    leadingIcon = { Icon(amenityIcon(a), contentDescription = null, modifier = Modifier.size(18.dp)) }
                                )
                            }
                        }
                    }

                    HorizontalDivider()
                    Text("VIP-услуги", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Цены для этого особняка. Заказываются при оформлении аренды; «Всё включено» — скидка " +
                            "${Pricing.ALL_INCLUSIVE_VIP_PERCENT}% на услуги и ${Pricing.ALL_INCLUSIVE_STAY_PERCENT}% на проживание.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    vipServices.forEach { s ->
                        val ok = Pricing.isAvailable(listing, s)
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Icon(vipIcon(s.iconCode), contentDescription = null, modifier = Modifier.size(18.dp),
                                tint = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                            Spacer(Modifier.width(8.dp))
                            Text(s.name.orEmpty(), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium,
                                color = if (ok) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline)
                            Text(
                                if (ok) "${rub(s.price)} ${s.unitLabel()}" else "нет площадки",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (ok) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (ok) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    HorizontalDivider()
                    Text("Расположение", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (hasCoords) {
                        PointMap(
                            latitude = listing.latitude,
                            longitude = listing.longitude,
                            priceText = rub(listing.pricePerNight),
                            modifier = Modifier.height(200.dp).clip(RoundedCornerShape(16.dp))
                        )
                        OutlinedButton(
                            onClick = { openInYandexMaps(context, listing.latitude, listing.longitude) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Открыть в Яндекс Картах")
                        }
                    } else {
                        Text("Координаты объекта не указаны", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    HorizontalDivider()
                    Text(
                        "Отзывы (${reviews.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (reviews.isEmpty()) {
                        Text(
                            "Пока нет отзывов. Оставить отзыв можно после проживания — в разделе «Аренды».",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            items(reviews, key = { it.id }) { review ->
                ReviewItem(review, onAuthorClick = { onOpenReviewer(review) })
            }
            item { Spacer(Modifier.height(16.dp)) }
        }

        // кнопки поверх фото; после прокрутки — на полосе с названием
        Box(modifier = Modifier.fillMaxWidth()) {
            AnimatedVisibility(visible = showBar, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.matchParentSize()) {
                Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
                    Spacer(Modifier.weight(1f))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BackButton(onBack)
                Text(
                    listing.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).alpha(if (showBar) 1f else 0f)
                )
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(40.dp)
                        .background(Color.White.copy(alpha = 0.9f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (isFavorite) "Убрать из избранного" else "В избранное",
                        tint = if (isFavorite) Accent else Color(0xFF444444)
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoChip(text: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
private fun ReviewItem(review: ReviewDto, onAuthorClick: () -> Unit = {}) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()) {
                // имя автора с аватаром — нажатие открывает его профиль
                val author = remember(review.authorName, review.userId) {
                    ReviewerProfiles.forAuthor(review.authorName.orEmpty(), review.userId.orEmpty())
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onAuthorClick).padding(end = 6.dp)
                ) {
                    Box(
                        modifier = Modifier.size(32.dp).background(Color(author.avatarColor), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(author.initials, color = Color.White, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(review.authorName.orEmpty(), style = MaterialTheme.typography.titleSmall)
                        Text("Профиль ›", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
                Text(
                    review.createdAt.isoToDisplay(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            RatingStars(review.rating)
            if (!review.comment.isNullOrBlank()) {
                Text(review.comment, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun BookBar(listing: ListingDetailsDto, onBook: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shadowElevation = 6.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(rub(listing.pricePerNight), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("за ночь", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(onClick = onBook) { Text("Арендовать") }
        }
    }
}

private fun amenityIcon(a: AmenityDto): ImageVector = when (a.iconCode) {
    "wifi" -> Icons.Default.Wifi
    "kitchen" -> Icons.Default.Kitchen
    "ac" -> Icons.Default.AcUnit
    "pool" -> Icons.Default.Pool
    "parking" -> Icons.Default.LocalParking
    "sauna" -> Icons.Default.HotTub
    "security" -> Icons.Default.Security
    "cinema" -> Icons.Default.Theaters
    "staff" -> Icons.Default.RoomService
    "terrace" -> Icons.Default.Deck
    "helipad" -> Icons.Default.Flight
    else -> Icons.Default.CheckCircle
}
