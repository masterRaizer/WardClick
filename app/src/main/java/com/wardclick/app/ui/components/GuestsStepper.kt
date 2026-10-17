package com.wardclick.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wardclick.app.util.guestsText

@Composable
fun GuestsStepper(
    value: Int,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    min: Int = 1,
    max: Int = 30
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Гости", style = MaterialTheme.typography.bodyLarge)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalIconButton(onClick = { onChange((value - 1).coerceAtLeast(min)) }, enabled = value > min) {
                Icon(Icons.Default.Remove, contentDescription = "Меньше")
            }
            Text(
                guestsText(value),
                modifier = Modifier.widthIn(min = 80.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleSmall
            )
            FilledTonalIconButton(onClick = { onChange((value + 1).coerceAtMost(max)) }, enabled = value < max) {
                Icon(Icons.Default.Add, contentDescription = "Больше")
            }
        }
    }
}
