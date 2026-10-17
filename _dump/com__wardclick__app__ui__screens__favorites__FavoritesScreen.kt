package com.wardclick.app.ui.screens.favorites

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wardclick.app.data.model.ListingSummaryDto
import com.wardclick.app.ui.components.ListingCard
import com.wardclick.app.ui.viewmodels.FavoritesState
import com.wardclick.app.ui.viewmodels.FavoritesViewModel

@Composable
fun FavoritesScreen(
    viewModel: FavoritesViewModel,
    onNavigateToDetails: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val favoritesState by viewModel.favoritesState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadFavorites()
    }

    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = "My Favorites",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(16.dp)
        )

        when (val state = favoritesState) {
            is FavoritesState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is FavoritesState.Success -> {
                if (state.favorites.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No favorites yet.", style = MaterialTheme.typography.bodyLarge)
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(state.favorites) { listing ->
                            val summary = ListingSummaryDto().apply {
                                id = listing.id
                                title = listing.title
                                imageUrl = listing.imageUrls?.firstOrNull()
                                location = listing.city?.name
                                pricePerNight = listing.pricePerNight
                                rating = listing.rating
                                reviewsCount = listing.reviewsCount
                            }
                            ListingCard(
                                listing = summary,
                                onClick = { onNavigateToDetails(listing.id ?: "") },
                                onFavoriteClick = { viewModel.removeFavorite(listing.id ?: "") }
                            )
                        }
                    }
                }
            }
            is FavoritesState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
