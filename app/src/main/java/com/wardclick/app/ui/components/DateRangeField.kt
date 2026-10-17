package com.wardclick.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wardclick.app.util.nightsText
import com.wardclick.app.util.toDisplay
import com.wardclick.app.util.toUtcMillis
import com.wardclick.app.util.utcMillisToLocalDate
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Поле выбора диапазона дат. Все даты — java.time.LocalDate (без смещения часовых поясов:
 * DatePicker отдаёт полночь UTC, она переводится в LocalDate тоже по UTC).
 * Прошедшие даты и занятые ночи (blocked) выбрать нельзя.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangeField(
    checkIn: LocalDate?,
    checkOut: LocalDate?,
    onChange: (LocalDate?, LocalDate?) -> Unit,
    modifier: Modifier = Modifier,
    blocked: Set<LocalDate> = emptySet(),
    label: String = "Даты",
    clearable: Boolean = false
) {
    var showDialog by remember { mutableStateOf(false) }

    val text = if (checkIn != null && checkOut != null) {
        val nights = ChronoUnit.DAYS.between(checkIn, checkOut).toInt()
        "${checkIn.toDisplay()} — ${checkOut.toDisplay()} (${nightsText(nights)})"
    } else {
        "Выберите даты"
    }

    OutlinedTextField(
        value = text,
        onValueChange = {},
        modifier = modifier.fillMaxWidth().clickable { showDialog = true },
        enabled = false,
        readOnly = true,
        singleLine = true,
        label = { Text(label) },
        colors = OutlinedTextFieldDefaults.colors(
            disabledTextColor = MaterialTheme.colorScheme.onSurface,
            disabledBorderColor = MaterialTheme.colorScheme.outline,
            disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
        trailingIcon = if (clearable && checkIn != null) {
            {
                IconButton(onClick = { onChange(null, null) }) {
                    Icon(Icons.Default.Close, contentDescription = "Сбросить даты")
                }
            }
        } else null
    )

    if (showDialog) {
        val today = remember { LocalDate.now() }
        val pickerState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = checkIn?.toUtcMillis(),
            initialSelectedEndDateMillis = checkOut?.toUtcMillis(),
            selectableDates = remember(blocked) {
                object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                        val d = utcTimeMillis.utcMillisToLocalDate()
                        return !d.isBefore(today) && d !in blocked
                    }

                    override fun isSelectableYear(year: Int): Boolean = year >= today.year
                }
            }
        )
        val start = pickerState.selectedStartDateMillis
        val end = pickerState.selectedEndDateMillis

        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(
                    enabled = start != null && end != null && end > start,
                    onClick = {
                        if (start != null && end != null) {
                            onChange(start.utcMillisToLocalDate(), end.utcMillisToLocalDate())
                        }
                        showDialog = false
                    }
                ) { Text("Готово") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Отмена") }
            }
        ) {
            Column {
                DateRangePicker(
                    state = pickerState,
                    modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp),
                    title = null,
                    showModeToggle = false
                )
            }
        }
    }
}
