package com.wardclick.app.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wardclick.app.ui.components.ListingCard
import com.wardclick.app.ui.viewmodels.HomeState
import com.wardclick.app.ui.viewmodels.HomeViewModel

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToDetails: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val homeState by viewModel.homeState.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    
    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search by city id...") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
        
        Button(
            onClick = { viewModel.search(if (searchQuery.isNotBlank()) searchQuery else null, null, null) },
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Text("Search")
        }

        when (val state = homeState) {
            is HomeState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is HomeState.Success -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.listings) { listing ->
                        ListingCard(
                            listing = listing,
                            onClick = { onNavigateToDetails(listing.id ?: "") },
                            onFavoriteClick = { /* Handle favorite */ }
                        )
                    }
                }
            }
            is HomeState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                }
            }
            is HomeState.Idle -> {
                // Initial state
            }
        }
    }
}
