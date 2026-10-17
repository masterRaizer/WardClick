package com.wardclick.app.ui.screens.booking

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wardclick.app.ui.components.DateRangeField
import com.wardclick.app.ui.components.GuestsStepper
import com.wardclick.app.ui.viewmodels.AvailabilityState
import com.wardclick.app.ui.viewmodels.BookingActionState
import com.wardclick.app.ui.viewmodels.BookingViewModel
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingScreen(
    listingId: String,
    viewModel: BookingViewModel,
    onNavigateBack: () -> Unit,
    onBookingSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val availabilityState by viewModel.availabilityState.collectAsStateWithLifecycle()
    val bookingActionState by viewModel.bookingActionState.collectAsStateWithLifecycle()

    var startDateMillis by remember { mutableStateOf<Long?>(null) }
    var endDateMillis by remember { mutableStateOf<Long?>(null) }
    var guests by remember { mutableStateOf(1) }
    
    val pricePerNight = BigDecimal("100.00") // Mock price for now

    LaunchedEffect(startDateMillis, endDateMillis, guests) {
        if (startDateMillis != null && endDateMillis != null) {
            val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val checkIn = formatter.format(Date(startDateMillis!!))
            val checkOut = formatter.format(Date(endDateMillis!!))
            viewModel.checkAvailability(listingId, checkIn, checkOut, guests, pricePerNight)
        }
    }

    LaunchedEffect(bookingActionState) {
        if (bookingActionState is BookingActionState.Success) {
            onBookingSuccess()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Book Listing") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Select Dates", style = MaterialTheme.typography.titleMedium)
            DateRangeField(
                startDateMillis = startDateMillis,
                endDateMillis = endDateMillis,
                onDateRangeSelected = { start, end ->
                    startDateMillis = start
                    endDateMillis = end
                },
                modifier = Modifier.fillMaxWidth()
            )

            Text("Guests", style = MaterialTheme.typography.titleMedium)
            GuestsStepper(
                guests = guests,
                onGuestsChange = { guests = it }
            )

            Spacer(modifier = Modifier.weight(1f))

            when (val state = availabilityState) {
                is AvailabilityState.Checking -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                }
                is AvailabilityState.Available -> {
                    Text("Total Price: ${state.formattedPrice}", style = MaterialTheme.typography.titleLarge)
                    Button(
                        onClick = {
                            val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            val checkIn = formatter.format(Date(startDateMillis!!))
                            val checkOut = formatter.format(Date(endDateMillis!!))
                            viewModel.createBooking(listingId, checkIn, checkOut, guests, state.totalPrice)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = bookingActionState !is BookingActionState.Loading
                    ) {
                        if (bookingActionState is BookingActionState.Loading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        } else {
                            Text("Confirm Booking")
                        }
                    }
                }
                is AvailabilityState.Unavailable -> {
                    Text("Unavailable: ${state.reason}", color = MaterialTheme.colorScheme.error)
                }
                is AvailabilityState.Error -> {
                    Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                }
                is AvailabilityState.Idle -> {
                    // Do nothing
                }
            }
            
            if (bookingActionState is BookingActionState.Error) {
                Text(
                    text = "Error: ${(bookingActionState as BookingActionState.Error).message}",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
