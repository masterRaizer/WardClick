package com.wardclick.app.ui.screens.details

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.wardclick.app.ui.viewmodels.DetailsState
import com.wardclick.app.ui.viewmodels.DetailsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(
    listingId: String,
    viewModel: DetailsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToBooking: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val detailsState by viewModel.detailsState.collectAsStateWithLifecycle()

    LaunchedEffect(listingId) {
        viewModel.loadDetails(listingId)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Listing Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (detailsState is DetailsState.Success) {
                        val isFavorite = (detailsState as DetailsState.Success).isFavorite
                        IconButton(onClick = { viewModel.toggleFavorite() }) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (detailsState is DetailsState.Success) {
                val listing = (detailsState as DetailsState.Success).listing
                BottomAppBar(
                    actions = {
                        Text(
                            text = "$${listing.pricePerNight} / night",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    },
                    floatingActionButton = {
                        Button(
                            onClick = { onNavigateToBooking(listingId) },
                            modifier = Modifier.padding(end = 16.dp)
                        ) {
                            Text("Book Now")
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        when (val state = detailsState) {
            is DetailsState.Loading -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is DetailsState.Success -> {
                val listing = state.listing
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        AsyncImage(
                            model = listing.imageUrls?.firstOrNull() ?: "",
                            contentDescription = "Listing Image",
                            modifier = Modifier.fillMaxWidth().height(250.dp),
                            contentScale = ContentScale.Crop
                        )
                    }
                    item {
                        Text(text = listing.title ?: "", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text(text = listing.address ?: "", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    item {
                        Text(text = listing.description ?: "", style = MaterialTheme.typography.bodyMedium)
                    }
                    item {
                        Text(text = "Amenities", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        listing.amenities?.forEach { amenity ->
                            Text(text = "• ${amenity.name}", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            is DetailsState.Error -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    Text(text = "Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
