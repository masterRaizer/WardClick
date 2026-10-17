package com.wardclick.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.wardclick.app.di.AppContainer
import com.wardclick.app.di.ViewModelFactory
import com.wardclick.app.ui.screens.admin.AdminScreen
import com.wardclick.app.ui.screens.auth.LoginScreen
import com.wardclick.app.ui.screens.auth.RegisterScreen
import com.wardclick.app.ui.screens.booking.BookingScreen
import com.wardclick.app.ui.screens.bookings.BookingsScreen
import com.wardclick.app.ui.screens.details.DetailsScreen
import com.wardclick.app.ui.screens.favorites.FavoritesScreen
import com.wardclick.app.ui.screens.home.HomeScreen
import com.wardclick.app.ui.screens.profile.ProfileScreen
import kotlin.reflect.KClass

data class NavBarItem(
    val route: Any,
    val routeClass: KClass<*>,
    val icon: ImageVector,
    val description: String
)

private val TOP_LEVEL_ROUTES = listOf(
    NavBarItem(HomeRoute, HomeRoute::class, Icons.Default.Search, "Search"),
    NavBarItem(FavoritesRoute, FavoritesRoute::class, Icons.Default.Favorite, "Favorites"),
    NavBarItem(BookingsRoute, BookingsRoute::class, Icons.Default.List, "Bookings"),
    NavBarItem(ProfileRoute, ProfileRoute::class, Icons.Default.Person, "Profile")
)

@Composable
fun MainAppScreen(appContainer: AppContainer) {
    val navController = rememberNavController()
    val sessionManager = appContainer.sessionManager
    var isLoggedIn by remember { mutableStateOf(sessionManager.isLoggedIn()) }
    var isAdmin by remember { mutableStateOf(sessionManager.isAdmin()) }
    
    val factory = ViewModelFactory(appContainer)

    val topLevelRoutes = remember(isAdmin) {
        val routes = TOP_LEVEL_ROUTES.toMutableList()
        if (isAdmin) {
            routes.add(NavBarItem(AdminRoute, AdminRoute::class, Icons.Default.Settings, "Admin"))
        }
        routes
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val isBottomBarVisible = topLevelRoutes.any { item ->
        currentDestination?.hierarchy?.any { it.hasRoute(item.routeClass) } == true
    }

    Scaffold(
        bottomBar = {
            if (isBottomBarVisible) {
                NavigationBar {
                    topLevelRoutes.forEach { item ->
                        val isSelected = currentDestination?.hierarchy?.any { it.hasRoute(item.routeClass) } == true
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(HomeRoute) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(imageVector = item.icon, contentDescription = item.description) },
                            label = { Text(item.description) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = HomeRoute,
            modifier = padding(innerPadding)
        ) {
            composable<HomeRoute> {
                HomeScreen(
                    viewModel = viewModel(factory = factory),
                    onNavigateToDetails = { id -> navController.navigate(DetailsRoute(id)) }
                )
            }
            composable<FavoritesRoute> {
                if (!isLoggedIn) {
                    LoginPromptScreen(onNavigateToAuth = { navController.navigate(AuthRoute) })
                } else {
                    FavoritesScreen(
                        viewModel = viewModel(factory = factory),
                        onNavigateToDetails = { id -> navController.navigate(DetailsRoute(id)) }
                    )
                }
            }
            composable<BookingsRoute> {
                if (!isLoggedIn) {
                    LoginPromptScreen(onNavigateToAuth = { navController.navigate(AuthRoute) })
                } else {
                    BookingsScreen(
                        viewModel = viewModel(factory = factory)
                    )
                }
            }
            composable<ProfileRoute> {
                if (!isLoggedIn) {
                    LoginPromptScreen(onNavigateToAuth = { navController.navigate(AuthRoute) })
                } else {
                    ProfileScreen(
                        viewModel = viewModel(factory = factory),
                        onLogout = {
                            isLoggedIn = false
                            isAdmin = false
                            navController.navigate(HomeRoute) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
                }
            }
            composable<AdminRoute> {
                if (!isLoggedIn || !isAdmin) {
                    LoginPromptScreen(onNavigateToAuth = { navController.navigate(AuthRoute) })
                } else {
                    AdminScreen(
                        viewModel = viewModel(factory = factory)
                    )
                }
            }
            composable<AuthRoute> {
                LoginScreen(
                    onNavigateToRegister = {
                        navController.navigate(RegisterRoute) {
                            popUpTo(AuthRoute) { inclusive = true }
                        }
                    },
                    onLoginSuccess = {
                        sessionManager.saveSession("mock_token", "user1", "mock@email.com", "Mock User", "USER")
                        isLoggedIn = true
                        isAdmin = sessionManager.isAdmin()
                        navController.navigate(HomeRoute) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
            composable<RegisterRoute> {
                RegisterScreen(
                    onNavigateToLogin = {
                        navController.navigate(AuthRoute) {
                            popUpTo(RegisterRoute) { inclusive = true }
                        }
                    },
                    onRegisterSuccess = {
                        sessionManager.saveSession("mock_token", "user1", "mock@email.com", "Mock User", "USER")
                        isLoggedIn = true
                        isAdmin = sessionManager.isAdmin()
                        navController.navigate(HomeRoute) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
            composable<DetailsRoute> { backStackEntry ->
                val detailsRoute = backStackEntry.toRoute<DetailsRoute>()
                DetailsScreen(
                    listingId = detailsRoute.listingId,
                    viewModel = viewModel(factory = factory),
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToBooking = { id -> 
                        if (isLoggedIn) navController.navigate(BookingRoute(id)) 
                        else navController.navigate(AuthRoute) 
                    }
                )
            }
            composable<BookingRoute> { backStackEntry ->
                val bookingRoute = backStackEntry.toRoute<BookingRoute>()
                BookingScreen(
                    listingId = bookingRoute.listingId,
                    viewModel = viewModel(factory = factory),
                    onNavigateBack = { navController.popBackStack() },
                    onBookingSuccess = {
                        navController.navigate(BookingsRoute) {
                            popUpTo(HomeRoute)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun LoginPromptScreen(onNavigateToAuth: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Please login to view this page", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.padding(8.dp))
            Button(onClick = onNavigateToAuth) {
                Text("Go to Login")
            }
        }
    }
}
