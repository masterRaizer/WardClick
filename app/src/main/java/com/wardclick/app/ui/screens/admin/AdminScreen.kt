package com.wardclick.app.ui.screens.admin

import com.wardclick.app.ui.theme.GlassCard
import com.wardclick.app.ui.theme.LocalBottomBarPadding
import com.wardclick.app.ui.components.AutoSizeText
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.wardclick.app.data.model.BookingDto
import com.wardclick.app.data.model.ListingRequest
import com.wardclick.app.data.model.ListingSummaryDto
import com.wardclick.app.ui.components.EmptyBox
import com.wardclick.app.ui.components.ErrorBox
import com.wardclick.app.ui.components.LoadingBox
import com.wardclick.app.ui.components.RemoteImage
import com.wardclick.app.ui.screens.bookings.StatusChip
import com.wardclick.app.ui.viewmodels.AdminUiState
import com.wardclick.app.ui.viewmodels.AdminViewModel
import com.wardclick.app.util.BookingUiStatus
import com.wardclick.app.util.guestsText
import com.wardclick.app.util.isoToDisplay
import com.wardclick.app.util.rub
import com.wardclick.app.util.uiStatus
import java.math.BigDecimal

private val TABS = listOf("Обзор", "Аренды", "Особняки")

/** Отступы кнопок в карточках: две кнопки делят ширину пополам и не должны переносить текст. */
private val ActionPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)

@Composable
fun AdminScreen(viewModel: AdminViewModel) {
    val state by viewModel.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var tab by remember { mutableIntStateOf(0) }
    var showCreate by remember { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.loadAll(silent = state.stats != null) }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    Box(modifier = Modifier.fillMaxSize().padding(bottom = LocalBottomBarPadding.current)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                "Панель администратора",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 8.dp)
            )
            TabRow(selectedTabIndex = tab) {
                TABS.forEachIndexed { i, title ->
                    val badge = if (i == 1) state.bookings.count { it.uiStatus() == BookingUiStatus.PENDING } else 0
                    Tab(
                        selected = tab == i,
                        onClick = { tab = i },
                        text = { AutoSizeText(if (badge > 0) "$title ($badge)" else title, style = MaterialTheme.typography.labelLarge) }
                    )
                }
            }
            when {
                state.isLoading && state.stats == null -> LoadingBox()
                state.error != null && state.stats == null -> ErrorBox(state.error!!, onRetry = { viewModel.loadAll() })
                tab == 0 -> OverviewTab(state)
                tab == 1 -> BookingsTab(state, viewModel::confirmBooking, viewModel::cancelBooking)
                else -> ListingsTab(state, viewModel::hideListing)
            }
        }

        if (tab == 2) {
            ExtendedFloatingActionButton(
                onClick = { showCreate = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Добавить особняк") },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
            )
        }
        SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter))
    }

    if (showCreate) {
        CreateListingDialog(
            state = state,
            onDismiss = { showCreate = false },
            onCreate = { request ->
                viewModel.createListing(request) { ok -> if (ok) showCreate = false }
            }
        )
    }
}

@Composable
private fun OverviewTab(state: AdminUiState) {
    val s = state.stats
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (s == null) {
            Text("Нет данных")
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Пользователи", s.totalUsers.toString(), Modifier.weight(1f))
                StatCard("Особняки", s.totalListings.toString(), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Аренды", s.totalBookings.toString(), Modifier.weight(1f))
                StatCard("Выручка", rub(s.totalRevenue), Modifier.weight(1f))
            }
            Text(
                "Выручка считается по подтверждённым арендам.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Spacer(Modifier.height(6.dp))
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun BookingsTab(
    state: AdminUiState,
    onConfirm: (String) -> Unit,
    onCancel: (String) -> Unit
) {
    if (state.bookings.isEmpty()) {
        EmptyBox(title = "Аренд пока нет")
        return
    }
    var cancelTarget by remember { mutableStateOf<BookingDto?>(null) }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(state.bookings, key = { it.id }) { b ->
            val status = b.uiStatus()
            GlassCard(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        b.listingTitle.orEmpty(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    StatusChip(status)
                }
                if (!b.orderId.isNullOrBlank()) {
                    Text("Заказ ${b.orderId}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
                Text(
                    "Гость: ${b.guestName.orEmpty().ifBlank { "—" }} (${b.guestEmail.orEmpty()})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "${b.checkInDate.isoToDisplay()} — ${b.checkOutDate.isoToDisplay()} · ${guestsText(b.guestsCount)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (!b.vipServiceNames.isNullOrEmpty()) {
                    Text(
                        (if (b.isAllInclusive) "Всё включено: " else "VIP: ") + b.vipServiceNames.joinToString(", ") + " (${rub(b.vipTotal)})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Итого",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Text(rub(b.totalPrice), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                // кнопки — отдельной строкой на всю ширину, чтобы не сжимались рядом с ценой
                val canConfirm = status == BookingUiStatus.PENDING
                val canCancel = status == BookingUiStatus.PENDING || status == BookingUiStatus.CONFIRMED
                if (state.busyId == b.id) {
                    Box(modifier = Modifier.fillMaxWidth().height(40.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                    }
                } else if (canConfirm || canCancel) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (canCancel) {
                            OutlinedButton(
                                onClick = { cancelTarget = b },
                                modifier = Modifier.weight(1f),
                                contentPadding = ActionPadding,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) { Text("Отменить", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        }
                        if (canConfirm) {
                            Button(
                                onClick = { onConfirm(b.id) },
                                modifier = Modifier.weight(1f),
                                contentPadding = ActionPadding
                            ) { Text("Подтвердить", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        }
                    }
                }
            }
        }
    }
    cancelTarget?.let { b ->
        AlertDialog(
            onDismissRequest = { cancelTarget = null },
            title = { Text("Отменить аренду?") },
            text = { Text("«${b.listingTitle}» для гостя ${b.guestName.orEmpty()}.") },
            confirmButton = {
                TextButton(onClick = { onCancel(b.id); cancelTarget = null }) {
                    Text("Отменить аренду", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { cancelTarget = null }) { Text("Назад") } }
        )
    }
}

@Composable
private fun ListingsTab(state: AdminUiState, onHide: (String) -> Unit) {
    var hideTarget by remember { mutableStateOf<ListingSummaryDto?>(null) }
    if (state.listings.isEmpty()) {
        EmptyBox(title = "Особняков пока нет", subtitle = "Нажмите «Добавить особняк»")
    } else {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(state.listings, key = { it.id }) { l ->
                GlassCard(contentPadding = PaddingValues(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RemoteImage(
                            url = l.imageUrl,
                            modifier = Modifier.size(72.dp).clip(RoundedCornerShape(12.dp))
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(l.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(l.location.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${rub(l.pricePerNight)} / ночь", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                        if (state.busyId == l.id) {
                            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            TextButton(onClick = { hideTarget = l }) { Text("Скрыть", color = MaterialTheme.colorScheme.error) }
                        }
                    }
                }
            }
        }
    }
    hideTarget?.let { l ->
        AlertDialog(
            onDismissRequest = { hideTarget = null },
            title = { Text("Скрыть особняк?") },
            text = { Text("«${l.title}» перестанет отображаться в поиске. Существующие аренды сохранятся.") },
            confirmButton = {
                TextButton(onClick = { onHide(l.id); hideTarget = null }) {
                    Text("Скрыть", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { hideTarget = null }) { Text("Отмена") } }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CreateListingDialog(
    state: AdminUiState,
    onDismiss: () -> Unit,
    onCreate: (ListingRequest) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var guests by remember { mutableStateOf("2") }
    var imageUrl by remember { mutableStateOf("") }
    var cityId by remember { mutableStateOf(state.cities.firstOrNull()?.id) }
    var typeId by remember { mutableStateOf(state.types.firstOrNull()?.id) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новый особняк") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(title, { title = it }, label = { Text("Название") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Text("Город", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    state.cities.forEach { c ->
                        FilterChip(selected = cityId == c.id, onClick = { cityId = c.id }, label = { Text(c.name) })
                    }
                }
                Text("Тип особняка", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    state.types.forEach { t ->
                        FilterChip(selected = typeId == t.id, onClick = { typeId = t.id }, label = { Text(t.name) })
                    }
                }
                OutlinedTextField(address, { address = it }, label = { Text("Адрес (улица, дом)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    price, { price = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Цена за ночь, ₽") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    guests, { guests = it.filter { ch -> ch.isDigit() }.take(2) },
                    label = { Text("Максимум гостей") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(imageUrl, { imageUrl = it }, label = { Text("Ссылка на фото (необязательно)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(description, { description = it }, label = { Text("Описание") }, minLines = 2, maxLines = 4, modifier = Modifier.fillMaxWidth())
                Text(
                    "Точка на карте будет поставлена в центр выбранного города.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val p = price.toLongOrNull()
                val g = guests.toIntOrNull()
                when {
                    title.isBlank() -> error = "Укажите название"
                    cityId == null -> error = "Выберите город"
                    p == null || p <= 0 -> error = "Укажите цену за ночь"
                    g == null || g < 1 -> error = "Укажите число гостей"
                    else -> onCreate(
                        ListingRequest().apply {
                            this.title = title.trim()
                            this.description = description.trim()
                            this.cityId = cityId
                            this.propertyTypeId = typeId
                            this.address = address.trim()
                            this.pricePerNight = BigDecimal.valueOf(p)
                            this.maxGuests = g
                            this.bedrooms = 1
                            this.beds = 1
                            this.bathrooms = 1
                            this.imageUrls = if (imageUrl.isBlank()) emptyList() else listOf(imageUrl.trim())
                            this.amenityIds = emptyList()
                        }
                    )
                }
            }) { Text("Добавить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}
