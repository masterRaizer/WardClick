package com.wardclick.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wardclick.app.data.model.VipServiceDto
import com.wardclick.app.util.rub
import com.wardclick.app.util.unitLabel

fun vipIcon(code: String?): ImageVector = when (code) {
    "helicopter" -> Icons.Default.Flight
    "driver" -> Icons.Default.DirectionsCar
    "concierge" -> Icons.Default.SupportAgent
    "chef" -> Icons.Default.RestaurantMenu
    "maid" -> Icons.Default.CleaningServices
    "guard" -> Icons.Default.Security
    else -> Icons.Default.Star
}

/**
 * Карточка VIP-услуги. Если передан [checked] — показывается галочка (экран оформления аренды),
 * иначе это просто карточка каталога.
 */
@Composable
fun VipServiceItem(
    service: VipServiceDto,
    modifier: Modifier = Modifier,
    checked: Boolean? = null,
    enabled: Boolean = true,
    note: String? = null,
    priceSuffix: String = "",
    onToggle: () -> Unit = {}
) {
    Surface(
        modifier = modifier.fillMaxWidth().alpha(if (enabled) 1f else 0.5f),
        shape = RoundedCornerShape(14.dp),
        color = if (checked == true) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier
                .then(if (checked != null && enabled) Modifier.clickable(onClick = onToggle) else Modifier)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(44.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(vipIcon(service.iconCode), contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(service.name.orEmpty(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(service.description.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "${rub(service.price)} ${service.unitLabel()}$priceSuffix",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                if (note != null) {
                    Text(note, style = MaterialTheme.typography.labelSmall, color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error)
                }
            }
            if (checked != null) {
                Checkbox(checked = checked, onCheckedChange = { if (enabled) onToggle() }, enabled = enabled)
            }
        }
    }
}
