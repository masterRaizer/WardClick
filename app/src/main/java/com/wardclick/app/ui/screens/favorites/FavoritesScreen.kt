package com.wardclick.app.ui.screens.favorites

import com.wardclick.app.ui.theme.LocalBottomBarPadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.wardclick.app.ui.components.EmptyBox
import com.wardclick.app.ui.components.ErrorBox
import com.wardclick.app.ui.components.ListingCard
import com.wardclick.app.ui.components.LoadingBox
import com.wardclick.app.ui.viewmodels.FavoritesState
import com.wardclick.app.ui.viewmodels.FavoritesViewModel
import com.wardclick.app.util.toSummary

@Composable
fun FavoritesScreen(
    viewModel: FavoritesViewModel,
    onNavigateToDetails: (String) -> Unit
) {
    val state by viewModel.favoritesState.collectAsState()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.loadFavorites() }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "Избранное",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 8.dp)
        )
        when (val s = state) {
            is FavoritesState.Loading -> LoadingBox()
            is FavoritesState.Error -> ErrorBox(message = s.message, onRetry = viewModel::loadFavorites)
            is FavoritesState.Success -> {
                if (s.favorites.isEmpty()) {
                    EmptyBox(
                        title = "В избранном пока пусто",
                        subtitle = "Нажмите на сердечко у понравившегося особняка",
                        icon = Icons.Default.FavoriteBorder
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp + LocalBottomBarPadding.current),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(s.favorites, key = { it.id }) { listing ->
                            ListingCard(
                                listing = listing.toSummary(),
                                isFavorite = true,
                                onClick = { onNavigateToDetails(listing.id) },
                                onFavoriteClick = { viewModel.removeFavorite(listing.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}
