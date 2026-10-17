package com.wardclick.app.ui.screens.booking

import com.wardclick.app.util.Pricing
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.FilterChip
import com.wardclick.app.ui.components.VipServiceItem
import com.wardclick.app.util.costFor
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wardclick.app.ui.components.DateRangeField
import com.wardclick.app.ui.components.ErrorBox
import com.wardclick.app.ui.components.GuestsStepper
import com.wardclick.app.ui.components.LoadingBox
import com.wardclick.app.ui.components.RemoteImage
import com.wardclick.app.ui.viewmodels.BookingViewModel
import com.wardclick.app.ui.viewmodels.MAX_NIGHTS
import com.wardclick.app.util.nightsText
import com.wardclick.app.util.rub

@Composable
fun BookingScreen(
    listingId: String,
    viewModel: BookingViewModel,
    onNavigateBack: () -> Unit,
    onBookingSuccess: () -> Unit,
    onAddedToCart: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val cartItems by viewModel.cartItems.collectAsState()
    LaunchedEffect(listingId) { viewModel.load(listingId) }
    LaunchedEffect(state.success) { if (state.success) onBookingSuccess() }
    LaunchedEffect(state.addedToCart) {
        if (state.addedToCart) {
            viewModel.reset()
            onAddedToCart()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
            }
            Text("Аренда особняка", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        val listing = state.listing
        when {
            state.isLoading -> LoadingBox()
            listing == null -> ErrorBox(
                message = state.loadError ?: "Особняк не найден",
                onRetry = { viewModel.load(listingId, force = true) }
            )
            else -> Column(modifier = Modifier.weight(1f)) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        RemoteImage(
                            url = listing.imageUrls.orEmpty().firstOrNull(),
                            modifier = Modifier.size(80.dp).clip(RoundedCornerShape(12.dp))
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                listing.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "${listing.city.name}, ${listing.address}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "${rub(listing.pricePerNight)} / ночь",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                DateRangeField(
                    checkIn = state.checkIn,
                    checkOut = state.checkOut,
                    onChange = { a, b -> viewModel.setDates(a, b) },
                    blocked = state.blocked,
                    label = "Даты проживания"
                )
                Text(
                    "Занятые даты в календаре недоступны. Максимум — ${nightsText(MAX_NIGHTS)}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                GuestsStepper(
                    value = state.guests,
                    onChange = viewModel::setGuests,
                    max = listing.maxGuests
                )

                if (state.vipServices.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("VIP-услуги", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        FilterChip(
                            selected = state.isAllInclusive,
                            onClick = viewModel::toggleAllInclusive,
                            label = { Text("Всё включено") }
                        )
                    }
                    Text(
                        "Цены услуг рассчитаны для этого особняка. Пакет «Всё включено» (все доступные услуги) — " +
                            "скидка ${Pricing.ALL_INCLUSIVE_VIP_PERCENT}% на VIP-услуги и ${Pricing.ALL_INCLUSIVE_STAY_PERCENT}% на проживание.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    state.vipServices.forEach { s ->
                        val available = state.isVipAvailable(s)
                        VipServiceItem(
                            service = s,
                            checked = s.id in state.selectedVip,
                            enabled = available,
                            note = if (available) null else "Нет вертолётной площадки у этого особняка",
                            onToggle = { viewModel.toggleVip(s.id) }
                        )
                    }
                }

                if (state.nights > 0) {
                    Card {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Стоимость", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${rub(listing.pricePerNight)} × ${nightsText(state.nights)}")
                                Text(rub(state.stayTotal))
                            }
                            state.chosenVip.forEach { s ->
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(s.name.orEmpty(), modifier = Modifier.weight(1f))
                                    Text(rub(s.costFor(state.nights.toLong())))
                                }
                            }
                            val q = state.quote
                            if (q != null && q.allInclusiveDiscount.signum() > 0) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Скидка «Всё включено»", modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.primary)
                                    Text("−" + rub(q.allInclusiveDiscount), color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            HorizontalDivider()
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Итого", fontWeight = FontWeight.Bold)
                                Text(rub(state.total), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }

                val error = state.validationError ?: state.submitError
                if (error != null) {
                    Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }

                Text(
                    "После отправки аренда получит статус «Ожидает подтверждения» — администратор подтвердит её в ближайшее время. Оплата при заселении.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(8.dp))
            }
            // нижняя панель: итог и кнопка всегда на виду
            Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shadowElevation = 6.dp) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
                    if (state.chosenVip.isNotEmpty() && state.nights > 0) {
                        Text(
                            (if (state.isAllInclusive) "Всё включено · " else "") + "VIP-услуги: ${rub(state.vipTotal)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        if (cartItems.isEmpty()) "Можно арендовать несколько особняков одним заказом — от двух скидка ${Pricing.MULTI_STAY_PERCENT}% на проживание."
                        else "В заказе уже ${cartItems.size} — добавьте этот особняк и получите скидку ${Pricing.MULTI_STAY_PERCENT}% на проживание.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = viewModel::addToCart,
                            enabled = state.canSubmit,
                            modifier = Modifier.height(52.dp)
                        ) { Text("В заказ") }
                        Button(
                            onClick = viewModel::submit,
                            enabled = state.canSubmit,
                            modifier = Modifier.weight(1f).height(52.dp)
                        ) {
                            if (state.isSubmitting) {
                                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                            } else {
                                Text(
                                    if (state.nights > 0) "Арендовать за ${rub(state.total)}" else "Выберите даты",
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                        }
                    }
                    if (state.submitError != null) {
                        Text(state.submitError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            }
        }
    }
}
