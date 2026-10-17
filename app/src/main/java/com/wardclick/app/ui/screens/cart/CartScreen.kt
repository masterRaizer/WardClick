package com.wardclick.app.ui.screens.cart

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import com.wardclick.app.data.repository.CartItem
import com.wardclick.app.ui.components.EmptyBox
import com.wardclick.app.ui.components.RemoteImage
import com.wardclick.app.ui.viewmodels.CartViewModel
import com.wardclick.app.util.Pricing
import com.wardclick.app.util.guestsText
import com.wardclick.app.util.nightsText
import com.wardclick.app.util.rub
import com.wardclick.app.util.toDisplay
import java.math.BigDecimal

@Composable
fun CartScreen(
    viewModel: CartViewModel,
    isLoggedIn: Boolean,
    onBack: () -> Unit,
    onAddMore: () -> Unit,
    onLogin: () -> Unit,
    onOrderDone: () -> Unit
) {
    val items by viewModel.items.collectAsState()
    val state by viewModel.state.collectAsState()
    LaunchedEffect(state.success) {
        if (state.success) {
            viewModel.consumeSuccess()
            onOrderDone()
        }
    }
    val multi = items.size >= 2
    val sum = viewModel.summary(items)

    Column(modifier = Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(4.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад") }
            Text("Заказ", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        if (items.isEmpty()) {
            Column(modifier = Modifier.weight(1f)) {
                EmptyBox(title = "В заказе пока нет особняков", subtitle = "Выберите особняк и нажмите «В заказ»")
            }
            Button(onClick = onAddMore, modifier = Modifier.fillMaxWidth().padding(16.dp)) { Text("Выбрать особняк") }
            return@Column
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(items, key = { it.key }) { item ->
                CartItemCard(item, multi, onRemove = { viewModel.remove(item.key) })
            }
            item {
                OutlinedButton(onClick = onAddMore, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Добавить ещё особняк")
                }
                if (!multi) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Добавьте второй особняк — скидка ${Pricing.MULTI_STAY_PERCENT}% на проживание в каждом.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            item {
                Card {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Итого по заказу", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        SumRow("Проживание", sum.stay)
                        if (sum.vip.signum() > 0) SumRow("VIP-услуги", sum.vip)
                        if (sum.allInclusiveDiscount.signum() > 0) SumRow("Скидка «Всё включено»", sum.allInclusiveDiscount.negate())
                        if (sum.multiDiscount.signum() > 0) SumRow("Скидка за ${items.size} особняка (−${Pricing.MULTI_STAY_PERCENT}%)", sum.multiDiscount.negate())
                        HorizontalDivider()
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("К оплате", fontWeight = FontWeight.Bold)
                            Text(rub(sum.total), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }

        Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shadowElevation = 6.dp) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
                if (state.error != null) {
                    Text(state.error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(4.dp))
                }
                Button(
                    onClick = { if (isLoggedIn) viewModel.checkout() else onLogin() },
                    enabled = !state.isSubmitting,
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    if (state.isSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text(
                            if (isLoggedIn) "Оформить заказ за ${rub(sum.total)}" else "Войдите, чтобы оформить заказ",
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SumRow(label: String, value: BigDecimal) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(
            if (value.signum() < 0) "−" + rub(value.negate()) else rub(value),
            style = MaterialTheme.typography.bodyMedium,
            color = if (value.signum() < 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun CartItemCard(item: CartItem, multi: Boolean, onRemove: () -> Unit) {
    val q = item.quote(multi)
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RemoteImage(
                    url = item.listing.imageUrls.orEmpty().firstOrNull(),
                    modifier = Modifier.size(64.dp).clip(RoundedCornerShape(10.dp))
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.listing.title.orEmpty(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${item.checkIn.toDisplay()} — ${item.checkOut.toDisplay()} · ${nightsText(item.nights.toInt())} · ${guestsText(item.guests)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onRemove) { Icon(Icons.Default.Delete, contentDescription = "Убрать из заказа") }
            }
            if (item.chosenVip.isNotEmpty()) {
                Text(
                    (if (q.allInclusive) "Всё включено: " else "VIP: ") + item.chosenVip.joinToString(", ") { it.name.orEmpty() },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    if (q.discount.signum() > 0) "Скидка −${rub(q.discount)}" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(rub(q.total), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}
