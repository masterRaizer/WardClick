package com.wardclick.app.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun GuestsStepper(
    guests: Int,
    onGuestsChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    minGuests: Int = 1,
    maxGuests: Int = 10
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        IconButton(
            onClick = { if (guests > minGuests) onGuestsChange(guests - 1) },
            enabled = guests > minGuests
        ) {
            Icon(Icons.Default.Remove, contentDescription = "Decrease guests")
        }
        
        Text(
            text = guests.toString(),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        
        IconButton(
            onClick = { if (guests < maxGuests) onGuestsChange(guests + 1) },
            enabled = guests < maxGuests
        ) {
            Icon(Icons.Default.Add, contentDescription = "Increase guests")
        }
    }
}
