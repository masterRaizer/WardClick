package com.wardclick.app.ui.screens.bookings

import com.wardclick.app.ui.theme.LocalBottomBarPadding
import com.wardclick.app.ui.components.AutoSizeText
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.wardclick.app.data.model.BookingDto
import com.wardclick.app.ui.components.EmptyBox
import com.wardclick.app.ui.components.ErrorBox
import com.wardclick.app.ui.components.LoadingBox
import com.wardclick.app.ui.components.RatingPicker
import com.wardclick.app.ui.components.RemoteImage
import com.wardclick.app.ui.theme.Danger
import com.wardclick.app.ui.theme.Success
import com.wardclick.app.ui.theme.Warning
import com.wardclick.app.ui.viewmodels.BookingsTab
import com.wardclick.app.ui.viewmodels.BookingsViewModel
import com.wardclick.app.util.BookingUiStatus
import com.wardclick.app.util.guestsText
import com.wardclick.app.util.isoToDisplay
import com.wardclick.app.util.nightsText
import com.wardclick.app.util.rub
import com.wardclick.app.util.toLocalDateOrNull
import com.wardclick.app.util.uiStatus
import java.time.temporal.ChronoUnit

@Composable
fun BookingsScreen(
    viewModel: BookingsViewModel,
    onNavigateToDetails: (String) -> Unit
) {
    val state by viewModel.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var cancelTarget by remember { mutableStateOf<BookingDto?>(null) }
    var reviewTarget by remember { mutableStateOf<BookingDto?>(null) }

    // обновляем список при каждом возвращении на экран (например, после оформления брони)
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.load(silent = state.bookings.isNotEmpty()) }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    Box(modifier = Modifier.fillMaxSize().padding(bottom = LocalBottomBarPadding.current)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                "Мои аренды",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 8.dp)
            )
            TabRow(selectedTabIndex = state.tab.ordinal) {
                BookingsTab.values().forEach { tab ->
                    Tab(
                        selected = state.tab == tab,
                        onClick = { viewModel.selectTab(tab) },
                        text = { AutoSizeText(tab.title, style = MaterialTheme.typography.labelLarge) }
                    )
                }
            }

            when {
                state.isLoading && state.bookings.isEmpty() -> LoadingBox()
                state.error != null && state.bookings.isEmpty() ->
                    ErrorBox(message = state.error!!, onRetry = { viewModel.load() })
                else -> {
                    val items = state.forTab(state.tab)
                    if (items.isEmpty()) {
                        EmptyBox(
                            title = when (state.tab) {
                                BookingsTab.UPCOMING -> "Нет предстоящих аренд"
                                BookingsTab.COMPLETED -> "Нет завершённых поездок"
                                BookingsTab.CANCELLED -> "Нет отменённых аренд"
                            },
                            subtitle = if (state.tab == BookingsTab.UPCOMING) "Найдите особняк на вкладке «Поиск»" else null,
                            icon = Icons.Default.EventBusy
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(items, key = { it.id }) { booking ->
                                BookingCard(
                                    booking = booking,
                                    busy = state.busyId == booking.id,
                                    onOpen = { onNavigateToDetails(booking.listingId) },
                                    onCancel = { cancelTarget = booking },
                                    onReview = { reviewTarget = booking }
                                )
                            }
                        }
                    }
                }
            }
        }
        SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter))
    }

    cancelTarget?.let { b ->
        AlertDialog(
            onDismissRequest = { cancelTarget = null },
            title = { Text("Отменить аренду?") },
            text = { Text("«${b.listingTitle}», ${b.checkInDate.isoToDisplay()} — ${b.checkOutDate.isoToDisplay()}. Это действие нельзя отменить.") },
            confirmButton = {
                TextButton(onClick = { viewModel.cancel(b.id); cancelTarget = null }) {
                    Text("Отменить аренду", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { cancelTarget = null }) { Text("Назад") } }
        )
    }

    reviewTarget?.let { b ->
        ReviewDialog(
            title = b.listingTitle.orEmpty(),
            onDismiss = { reviewTarget = null },
            onSubmit = { rating, comment ->
                viewModel.addReview(b, rating, comment)
                reviewTarget = null
            }
        )
    }
}

@Composable
private fun BookingCard(
    booking: BookingDto,
    busy: Boolean,
    onOpen: () -> Unit,
    onCancel: () -> Unit,
    onReview: () -> Unit
) {
    val status = booking.uiStatus()
    val nights = run {
        val a = booking.checkInDate.toLocalDateOrNull()
        val b = booking.checkOutDate.toLocalDateOrNull()
        if (a != null && b != null) ChronoUnit.DAYS.between(a, b).toInt() else 0
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.clickable(onClick = onOpen), verticalAlignment = Alignment.Top) {
                RemoteImage(
                    url = booking.listingImageUrl,
                    modifier = Modifier.size(84.dp).clip(RoundedCornerShape(12.dp))
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        booking.listingTitle.orEmpty(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        booking.listingLocation.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    StatusChip(status)
                }
            }
            Text(
                "${booking.checkInDate.isoToDisplay()} — ${booking.checkOutDate.isoToDisplay()} · ${nightsText(nights)} · ${guestsText(booking.guestsCount)}",
                style = MaterialTheme.typography.bodyMedium
            )
            if (!booking.vipServiceNames.isNullOrEmpty()) {
                Text(
                    (if (booking.isAllInclusive) "Всё включено: " else "VIP: ") + booking.vipServiceNames.joinToString(", "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            val discount = booking.discount
            if (discount != null && discount.signum() > 0 || !booking.orderId.isNullOrBlank()) {
                Text(
                    listOfNotNull(
                        booking.orderId?.takeIf { it.isNotBlank() }?.let { "Заказ $it" },
                        discount?.takeIf { it.signum() > 0 }?.let { "скидка −${rub(it)}" }
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(rub(booking.totalPrice), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                when {
                    busy -> CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    status == BookingUiStatus.PENDING || status == BookingUiStatus.CONFIRMED ->
                        OutlinedButton(onClick = onCancel) { Text("Отменить") }
                    status == BookingUiStatus.COMPLETED && !booking.isReviewed ->
                        Button(onClick = onReview) { Text("Оставить отзыв") }
                    status == BookingUiStatus.COMPLETED ->
                        Text("Отзыв оставлен", style = MaterialTheme.typography.labelLarge, color = Success)
                }
            }
        }
    }
}

@Composable
fun StatusChip(status: BookingUiStatus) {
    val color = when (status) {
        BookingUiStatus.PENDING -> Warning
        BookingUiStatus.CONFIRMED -> Success
        BookingUiStatus.COMPLETED -> MaterialTheme.colorScheme.primary
        BookingUiStatus.CANCELLED -> Danger
    }
    Surface(shape = RoundedCornerShape(8.dp), color = color.copy(alpha = 0.14f)) {
        Text(
            status.label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ReviewDialog(title: String, onDismiss: () -> Unit, onSubmit: (Int, String) -> Unit) {
    var rating by remember { mutableIntStateOf(5) }
    var comment by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Отзыв о проживании") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                RatingPicker(value = rating, onChange = { rating = it })
                OutlinedTextField(
                    value = comment,
                    onValueChange = { if (it.length <= 500) comment = it },
                    label = { Text("Комментарий (необязательно)") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSubmit(rating, comment) }) { Text("Отправить") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}
