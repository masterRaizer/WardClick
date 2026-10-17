package com.wardclick.app.ui.screens.admin

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
import com.wardclick.app.ui.screens.bookings.BookingCard
import com.wardclick.app.ui.viewmodels.AdminBookingsState
import com.wardclick.app.ui.viewmodels.AdminStatsState
import com.wardclick.app.ui.viewmodels.AdminViewModel

@Composable
fun AdminScreen(
    viewModel: AdminViewModel,
    modifier: Modifier = Modifier
) {
    val statsState by viewModel.statsState.collectAsStateWithLifecycle()
    val bookingsState by viewModel.bookingsState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadStats()
        viewModel.loadBookings()
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text("Admin Dashboard", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        // Stats section
        when (val state = statsState) {
            is AdminStatsState.Loading -> {
                CircularProgressIndicator()
            }
            is AdminStatsState.Success -> {
                val stats = state.stats
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Users: ${stats.totalUsers}", style = MaterialTheme.typography.bodyLarge)
                        Text("Listings: ${stats.totalListings}", style = MaterialTheme.typography.bodyLarge)
                        Text("Bookings: ${stats.totalBookings}", style = MaterialTheme.typography.bodyLarge)
                        Text("Revenue: $${stats.totalRevenue}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }
            is AdminStatsState.Error -> {
                Text(text = "Error loading stats: ${state.message}", color = MaterialTheme.colorScheme.error)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("All Bookings", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))

        // Bookings section
        when (val state = bookingsState) {
            is AdminBookingsState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is AdminBookingsState.Success -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.bookings) { booking ->
                        BookingCard(
                            booking = booking,
                            onCancelClick = { viewModel.cancelBooking(booking.id ?: "") },
                            onReviewClick = { /* No review from admin */ },
                            isUpcoming = booking.status == "PENDING" || booking.status == "CONFIRMED",
                            isCompleted = false
                        )
                    }
                }
            }
            is AdminBookingsState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
