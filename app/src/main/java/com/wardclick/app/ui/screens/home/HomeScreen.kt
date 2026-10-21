package com.wardclick.app.ui.screens.home

import com.wardclick.app.ui.theme.liquidGlass
import com.wardclick.app.ui.theme.LocalBottomBarPadding
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.wardclick.app.data.model.ListingSummaryDto
import com.wardclick.app.ui.components.DateRangeField
import com.wardclick.app.ui.components.EmptyBox
import com.wardclick.app.ui.components.ErrorBox
import com.wardclick.app.ui.components.GuestsStepper
import com.wardclick.app.ui.components.ListingCard
import com.wardclick.app.ui.components.ListingsMap
import com.wardclick.app.ui.components.LoadingBox
import com.wardclick.app.ui.components.RatingBadge
import com.wardclick.app.ui.components.RemoteImage
import com.wardclick.app.ui.viewmodels.CityInfo
import com.wardclick.app.ui.viewmodels.HomeUiState
import com.wardclick.app.ui.viewmodels.HomeViewModel
import com.wardclick.app.ui.viewmodels.SortMode
import com.wardclick.app.ui.viewmodels.ViewMode
import com.wardclick.app.util.rub
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToDetails: (String) -> Unit,
    onNavigateToBooking: (String) -> Unit
) {
    val state by viewModel.state.collectAsState()
    var showFilters by remember { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshFavorites() }

    // Шаг 1: сначала пользователь выбирает город, где хочет арендовать особняк
    if (!state.cityChosen) {
        CityPicker(state = state, onChoose = viewModel::chooseCity, onRetry = viewModel::retry)
        return
    }
    // «Назад» из списка возвращает к выбору города
    BackHandler { viewModel.changeCity() }

    Column(modifier = Modifier.fillMaxSize()) {
        CityHeader(cityName = state.cityName, onChange = viewModel::changeCity)

        SearchBar(
            query = state.query,
            onQueryChange = viewModel::onQueryChange,
            activeFilters = state.activeFilters,
            onFiltersClick = { showFilters = true }
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (state.isLoading) "Ищем…" else "Найдено: ${state.listings.size}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            ViewModeToggle(mode = state.viewMode, onChange = viewModel::setViewMode)
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                state.error != null && state.listings.isEmpty() ->
                    ErrorBox(message = state.error!!, onRetry = viewModel::retry)

                state.viewMode == ViewMode.MAP -> MapContent(
                    state = state,
                    onSelect = viewModel::select,
                    onDetails = onNavigateToDetails,
                    onBook = onNavigateToBooking
                )

                state.isLoading && state.listings.isEmpty() -> LoadingBox()

                state.listings.isEmpty() -> EmptyBox(
                    title = "Ничего не найдено",
                    subtitle = "Измените запрос или сбросьте фильтры",
                    action = { OutlinedButton(onClick = viewModel::resetFilters) { Text("Сбросить фильтры") } }
                )

                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp + LocalBottomBarPadding.current),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.listings, key = { it.id }) { listing ->
                        ListingCard(
                            listing = listing,
                            isFavorite = listing.id in state.favorites,
                            onClick = { onNavigateToDetails(listing.id) },
                            onFavoriteClick = { viewModel.toggleFavorite(listing.id) }
                        )
                    }
                }
            }
        }
    }

    if (showFilters) {
        FiltersSheet(
            state = state,
            onDismiss = { showFilters = false },
            onApply = { type, guests, inD, outD, sort ->
                viewModel.applyFilters(type, guests, inD, outD, sort)
                showFilters = false
            },
            onReset = {
                viewModel.resetFilters()
                showFilters = false
            }
        )
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    activeFilters: Int,
    onFiltersClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            placeholder = { Text("Адрес или название") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = if (query.isNotEmpty()) {
                {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Очистить")
                    }
                }
            } else null
        )
        IconButton(onClick = onFiltersClick) {
            BadgedBox(badge = { if (activeFilters > 0) Badge { Text(activeFilters.toString()) } }) {
                Icon(Icons.Default.FilterList, contentDescription = "Фильтры")
            }
        }
    }
}

@Composable
private fun CityHeader(cityName: String, onChange: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(6.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Особняки в городе", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(cityName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        TextButton(onClick = onChange) { Text("Сменить") }
    }
}

/** Первый экран: выбор города аренды. Показываются только города, где есть особняки. */
@Composable
private fun CityPicker(state: HomeUiState, onChoose: (String?) -> Unit, onRetry: () -> Unit) {
    when {
        state.error != null && state.cityInfos.isEmpty() -> ErrorBox(message = state.error, onRetry = onRetry)
        state.cityInfos.isEmpty() -> LoadingBox()
        else -> LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp + LocalBottomBarPadding.current),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Column(modifier = Modifier.padding(bottom = 4.dp)) {
                    Text("Где хотите отдохнуть?", style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text("Выберите город — покажем особняки, свободные для аренды",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            items(state.cityInfos, key = { it.city.id }) { info ->
                CityCard(info = info, onClick = { onChoose(info.city.id) })
            }
            item {
                OutlinedButton(onClick = { onChoose(null) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Показать все города")
                }
            }
        }
    }
}

@Composable
private fun CityCard(info: CityInfo, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().height(120.dp)
            .liquidGlass(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            RemoteImage(url = info.imageUrl, modifier = Modifier.fillMaxSize())
            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.horizontalGradient(listOf(Color.Black.copy(alpha = 0.72f), Color.Transparent))
                )
            )
            Column(
                modifier = Modifier.align(Alignment.CenterStart).padding(horizontal = 16.dp)
            ) {
                Text(info.city.name, style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold, color = Color.White)
                Text(
                    "${info.count} ${mansionsWord(info.count)} · от ${rub(info.minPrice)} / ночь",
                    style = MaterialTheme.typography.bodyMedium, color = Color.White
                )
            }
        }
    }
}

private fun mansionsWord(n: Int): String {
    val d = n % 10; val dd = n % 100
    return when {
        d == 1 && dd != 11 -> "особняк"
        d in 2..4 && dd !in 12..14 -> "особняка"
        else -> "особняков"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ViewModeToggle(mode: ViewMode, onChange: (ViewMode) -> Unit) {
    SingleChoiceSegmentedButtonRow {
        SegmentedButton(
            selected = mode == ViewMode.LIST,
            onClick = { onChange(ViewMode.LIST) },
            shape = SegmentedButtonDefaults.itemShape(0, 2)
        ) { Text("Список") }
        SegmentedButton(
            selected = mode == ViewMode.MAP,
            onClick = { onChange(ViewMode.MAP) },
            shape = SegmentedButtonDefaults.itemShape(1, 2)
        ) { Text("Карта") }
    }
}

@Composable
private fun MapContent(
    state: HomeUiState,
    onSelect: (String?) -> Unit,
    onDetails: (String) -> Unit,
    onBook: (String) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        ListingsMap(
            listings = state.listings,
            selectedId = state.selectedId,
            onSelect = onSelect,
            modifier = Modifier.fillMaxSize()
        )
        if (state.isLoading) {
            Text(
                "Обновляем…",
                modifier = Modifier.align(Alignment.TopCenter).padding(8.dp),
                style = MaterialTheme.typography.labelMedium
            )
        }
        val selected = state.selected
        if (selected != null) {
            SelectedListingCard(
                listing = selected,
                onDetails = { onDetails(selected.id) },
                onBook = { onBook(selected.id) },
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = LocalBottomBarPadding.current).padding(12.dp)
            )
        } else if (state.listings.isEmpty() && !state.isLoading) {
            Card(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = LocalBottomBarPadding.current).padding(12.dp)) {
                Text("По вашему запросу ничего не найдено", modifier = Modifier.padding(16.dp))
            }
        }
    }
}

@Composable
private fun SelectedListingCard(
    listing: ListingSummaryDto,
    onDetails: () -> Unit,
    onBook: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        // поверх карты: слоя с картой у стекла нет, поэтому просто полупрозрачная плёнка
        modifier = modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(24.dp), backdrop = null),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.clickable(onClick = onDetails)) {
                RemoteImage(
                    url = listing.imageUrl,
                    modifier = Modifier.size(84.dp).clip(RoundedCornerShape(12.dp))
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        listing.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        listing.location.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    RatingBadge(listing.rating, listing.reviewsCount)
                    Text(
                        "${rub(listing.pricePerNight)} / ночь",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onDetails, modifier = Modifier.weight(1f)) { Text("Подробнее") }
                Button(onClick = onBook, modifier = Modifier.weight(1f)) { Text("Арендовать") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FiltersSheet(
    state: HomeUiState,
    onDismiss: () -> Unit,
    onApply: (String?, Int, LocalDate?, LocalDate?, SortMode) -> Unit,
    onReset: () -> Unit
) {
    var typeId by remember { mutableStateOf(state.typeId) }
    var guests by remember { mutableStateOf(state.guests) }
    var checkIn by remember { mutableStateOf(state.checkIn) }
    var checkOut by remember { mutableStateOf(state.checkOut) }
    var sort by remember { mutableStateOf(state.sort) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Фильтры", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            Text("Тип особняка", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = typeId == null, onClick = { typeId = null }, label = { Text("Любой") })
                state.types.forEach { t ->
                    FilterChip(
                        selected = typeId == t.id,
                        onClick = { typeId = if (typeId == t.id) null else t.id },
                        label = { Text(t.name) }
                    )
                }
            }

            GuestsStepper(value = guests, onChange = { guests = it })

            DateRangeField(
                checkIn = checkIn,
                checkOut = checkOut,
                onChange = { a, b -> checkIn = a; checkOut = b },
                label = "Свободные даты",
                clearable = true
            )

            Text("Сортировка", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SortMode.values().forEach { m ->
                    FilterChip(selected = sort == m, onClick = { sort = m }, label = { Text(m.label) })
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(onClick = onReset, modifier = Modifier.weight(1f)) { Text("Сбросить") }
                Button(
                    onClick = { onApply(typeId, guests, checkIn, checkOut, sort) },
                    modifier = Modifier.weight(1f)
                ) { Text("Показать") }
            }
        }
    }
}
