package com.wardclick.app.ui.screens.vip

import com.wardclick.app.ui.theme.LocalBottomBarPadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wardclick.app.ui.components.ErrorBox
import com.wardclick.app.ui.components.LoadingBox
import com.wardclick.app.ui.components.VipServiceItem
import com.wardclick.app.ui.viewmodels.VipViewModel
import com.wardclick.app.util.Pricing

@Composable
fun VipScreen(
    viewModel: VipViewModel,
    onChooseMansion: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "VIP-услуги",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp)
        )
        when {
            state.isLoading -> LoadingBox()
            state.error != null -> ErrorBox(message = state.error!!, onRetry = viewModel::load)
            else -> LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp + LocalBottomBarPadding.current),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Сервис уровня «всё включено»", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                "Транспорт, персонал, отдых и праздники — от вертолёта и яхты до няни, СПА и фотографа — " +
                                    "заказываются вместе с арендой особняка. " +
                                    "Цена зависит от особняка — от 75% до 160% базовой. " +
                                    "Пакет «Всё включено»: −${Pricing.ALL_INCLUSIVE_VIP_PERCENT}% на услуги и −${Pricing.ALL_INCLUSIVE_STAY_PERCENT}% на проживание.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Button(onClick = onChooseMansion, modifier = Modifier.fillMaxWidth()) { Text("Выбрать особняк") }
                        }
                    }
                }
                items(state.services, key = { it.id }) { s ->
                    VipServiceItem(
                        service = s,
                        note = if (s.isRequiresHelipad) "Для особняков с вертолётной площадкой" else null,
                        priceSuffix = " (базовая)"
                    )
                }
            }
        }
    }
}
