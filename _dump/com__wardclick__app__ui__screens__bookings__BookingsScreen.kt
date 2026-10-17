package com.wardclick.app.ui.screens.bookings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wardclick.app.data.model.BookingDto
import com.wardclick.app.ui.viewmodels.BookingsState
import com.wardclick.app.ui.viewmodels.BookingsViewModel

@Composable
fun BookingsScreen(
    viewModel: BookingsViewModel,
    modifier: Modifier = Modifier
) {
    val bookingsState by viewModel.bookingsState.collectAsStateWithLifecycle()
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Upcoming", "Completed", "Cancelled")

    LaunchedEffect(Unit) {
        viewModel.loadBookings()
    }

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTabIndex) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title) }
                )
            }
        }

        when (val state = bookingsState) {
            is BookingsState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is BookingsState.Success -> {
                val listToShow = when (selectedTabIndex) {
                    0 -> state.upcoming
                    1 -> state.completed
                    else -> state.cancelled
                }

                if (listToShow.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No bookings found.", style = MaterialTheme.typography.bodyLarge)
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(listToShow) { booking ->
                            BookingCard(
                                booking = booking,
                                onCancelClick = { viewModel.cancelBooking(booking.id ?: "") },
                                onReviewClick = { /* Handle review */ },
                                isUpcoming = selectedTabIndex == 0,
                                isCompleted = selectedTabIndex == 1
                            )
                        }
                    }
                }
            }
            is BookingsState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun BookingCard(
    booking: BookingDto,
    onCancelClick: () -> Unit,
    onReviewClick: () -> Unit,
    isUpcoming: Boolean,
    isCompleted: Boolean,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Booking ID: ${booking.id}", style = MaterialTheme.typography.labelSmall)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Listing ID: ${booking.listingId}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Dates: ${booking.checkInDate} to ${booking.checkOutDate}", style = MaterialTheme.typography.bodyMedium)
            Text("Status: ${booking.status}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (isUpcoming) {
                    OutlinedButton(onClick = onCancelClick) {
                        Text("Cancel")
                    }
                }
                if (isCompleted) {
                    Button(onClick = onReviewClick) {
                        Text("Write Review")
                    }
                }
            }
        }
    }
}
