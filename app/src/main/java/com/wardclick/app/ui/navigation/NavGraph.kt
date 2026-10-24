package com.wardclick.app.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
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
import com.wardclick.app.ui.screens.reviewer.ReviewerProfileScreen
import com.wardclick.app.ui.screens.vip.VipScreen
import com.wardclick.app.ui.screens.cart.CartScreen
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.runtime.collectAsState
import com.wardclick.app.ui.components.AutoSizeText
import androidx.compose.material.icons.filled.Diamond
import kotlin.reflect.KClass
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import com.wardclick.app.ui.components.GlassNavBar
import com.wardclick.app.ui.components.GlassNavItem
import com.wardclick.app.ui.components.GlassPillButton
import com.wardclick.app.ui.theme.GlassBackground
import com.wardclick.app.ui.theme.LocalBackgroundBackdrop
import com.wardclick.app.ui.theme.LocalBottomBarPadding
import com.wardclick.app.ui.theme.LocalScreenBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

data class NavBarItem(
    val route: Any,
    val routeClass: KClass<*>,
    val icon: ImageVector,
    val label: String
)

private val TOP_LEVEL_ROUTES = listOf(
    NavBarItem(HomeRoute, HomeRoute::class, Icons.Default.Search, "Поиск"),
    NavBarItem(VipRoute, VipRoute::class, Icons.Default.Diamond, "VIP"),
    NavBarItem(FavoritesRoute, FavoritesRoute::class, Icons.Default.Favorite, "Избранное"),
    NavBarItem(BookingsRoute, BookingsRoute::class, Icons.Default.DateRange, "Аренды"),
    NavBarItem(ProfileRoute, ProfileRoute::class, Icons.Default.Person, "Профиль")
)

/** Порядок вкладок нижнего меню — для выбора направления сдвига. */
private val TAB_ORDER: List<KClass<*>> = listOf(
    HomeRoute::class, VipRoute::class, FavoritesRoute::class, BookingsRoute::class, ProfileRoute::class
)

// Плавный переход «общая ось X» (Material Motion): экраны сдвигаются на ~30 % ширины
// с мягким замедлением, старый быстро гаснет, новый проявляется с небольшой задержкой.
private const val NAV_DURATION = 380
private val NAV_EASING = CubicBezierEasing(0.2f, 0f, 0f, 1f) // Material «standard»
private const val NAV_SHIFT = 0.3f

private fun navEnter(forward: Boolean): EnterTransition =
    slideInHorizontally(tween(NAV_DURATION, easing = NAV_EASING)) { w ->
        ((if (forward) 1 else -1) * w * NAV_SHIFT).toInt()
    } + fadeIn(tween(durationMillis = NAV_DURATION * 65 / 100, delayMillis = NAV_DURATION * 30 / 100, easing = LinearOutSlowInEasing))

private fun navExit(forward: Boolean): ExitTransition =
    slideOutHorizontally(tween(NAV_DURATION, easing = NAV_EASING)) { w ->
        ((if (forward) -1 else 1) * w * NAV_SHIFT).toInt()
    } + fadeOut(tween(durationMillis = NAV_DURATION * 30 / 100, easing = FastOutLinearInEasing))

/**
 * «Поиск» — стартовый экран графа, поэтому его открываем возвратом к корню стека, без restoreState:
 * при popUpTo(HomeRoute) { saveState = true } Navigation связывает с HomeRoute сохранённый стек
 * вкладки, с которой ушли, и restoreState вернул бы её (например, «Аренды») вместо поиска.
 */
private fun NavHostController.openHomeTab() {
    if (!popBackStack(HomeRoute, inclusive = false)) {
        navigate(HomeRoute) { launchSingleTop = true }
    }
}

/** Переход на вкладку нижнего меню с сохранением состояния остальных вкладок. */
private fun NavHostController.openTab(route: Any) {
    if (route == HomeRoute) {
        openHomeTab()
        return
    }
    navigate(route) {
        popUpTo(HomeRoute) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private fun NavDestination.tabIndex(): Int? =
    TAB_ORDER.indexOfFirst { cls -> hasRoute(cls) }.takeIf { it >= 0 }

/** Вперёд (или на вкладку правее) — сдвиг влево, на вкладку левее — вправо. */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.isForward(): Boolean {
    val from = initialState.destination.tabIndex()
    val to = targetState.destination.tabIndex()
    return if (from != null && to != null) to >= from else true
}

@Composable
fun MainAppScreen(appContainer: AppContainer) {
    val navController = rememberNavController()
    val sessionManager = appContainer.sessionManager
    var isLoggedIn by remember { mutableStateOf(sessionManager.isLoggedIn()) }
    var isAdmin by remember { mutableStateOf(sessionManager.isAdmin()) }

    val factory = remember(appContainer) { ViewModelFactory(appContainer) }
    val cartItems by appContainer.cartRepository.items.collectAsState()

    // вкладки «Админ» в панели нет: администратор открывает её скрытым жестом в профиле
    val topLevelRoutes = TOP_LEVEL_ROUTES

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val isBottomBarVisible = topLevelRoutes.any { item ->
        currentDestination?.hierarchy?.any { it.hasRoute(item.routeClass) } == true
    }

    fun onAuthorized() {
        isLoggedIn = sessionManager.isLoggedIn()
        isAdmin = sessionManager.isAdmin()
        // вернуться туда, откуда пользователь пришёл на экран входа
        if (!navController.popBackStack()) {
            navController.navigate(HomeRoute) { popUpTo(0) { inclusive = true } }
        }
    }

    val showCartFab = cartItems.isNotEmpty() && currentDestination?.hierarchy?.any {
        it.hasRoute(HomeRoute::class) || it.hasRoute(VipRoute::class)
    } == true

    // «Liquid Glass»: стекло преломляет то, что под ним. Пишутся два слоя (Kyant Backdrop):
    // весь экран — для панелей поверх контента, и отдельно фон — для карточек внутри экранов
    val screenBackdrop = rememberLayerBackdrop()
    val backgroundBackdrop = rememberLayerBackdrop()
    val selectedTab = topLevelRoutes.indexOfFirst { item ->
        currentDestination?.hierarchy?.any { it.hasRoute(item.routeClass) } == true
    }

    CompositionLocalProvider(
        LocalScreenBackdrop provides screenBackdrop,
        LocalBackgroundBackdrop provides backgroundBackdrop
    ) {
    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            if (showCartFab) {
                GlassPillButton(
                    icon = Icons.Default.ShoppingCart,
                    text = "Заказ: ${cartItems.size}",
                    onClick = { navController.navigate(CartRoute) }
                )
            }
        },
        bottomBar = {
            if (isBottomBarVisible) {
                GlassNavBar(
                    items = topLevelRoutes.map { GlassNavItem(it.icon, it.label) },
                    selectedIndex = selectedTab,
                    onSelect = { index -> navController.openTab(topLevelRoutes[index].route) }
                )
            }
        }
    ) { innerPadding ->
      // контент уходит под плавающую панель — так её стекло преломляет прокручиваемые списки
      val bottomBarPadding = if (isBottomBarVisible) innerPadding.calculateBottomPadding() else 0.dp
      Box(modifier = Modifier.fillMaxSize().layerBackdrop(screenBackdrop)) {
        GlassBackground(Modifier.fillMaxSize().layerBackdrop(backgroundBackdrop))
        CompositionLocalProvider(LocalBottomBarPadding provides bottomBarPadding) {
        NavHost(
            navController = navController,
            startDestination = HomeRoute,
            modifier = Modifier.padding(
                top = innerPadding.calculateTopPadding(),
                bottom = if (isBottomBarVisible) 0.dp else innerPadding.calculateBottomPadding()
            ),
            // Переходы — плавным сдвигом: вперёд новый экран приходит справа, «назад» — слева.
            // Между вкладками нижнего меню направление зависит от их порядка (вкладка правее — сдвиг влево).
            enterTransition = { navEnter(isForward()) },
            exitTransition = { navExit(isForward()) },
            popEnterTransition = { navEnter(forward = false) },
            popExitTransition = { navExit(forward = false) }
        ) {
            composable<HomeRoute> {
                HomeScreen(
                    viewModel = viewModel(factory = factory),
                    onNavigateToDetails = { id -> navController.navigate(DetailsRoute(id)) },
                    onNavigateToBooking = { id ->
                        if (isLoggedIn) navController.navigate(BookingRoute(id))
                        else navController.navigate(AuthRoute)
                    }
                )
            }
            composable<VipRoute> {
                VipScreen(
                    viewModel = viewModel(factory = factory),
                    onChooseMansion = { navController.openHomeTab() }
                )
            }
            composable<FavoritesRoute> {
                if (!isLoggedIn) {
                    LoginPromptScreen(
                        text = "Войдите, чтобы сохранять особняки в избранное",
                        onNavigateToAuth = { navController.navigate(AuthRoute) }
                    )
                } else {
                    FavoritesScreen(
                        viewModel = viewModel(factory = factory),
                        onNavigateToDetails = { id -> navController.navigate(DetailsRoute(id)) }
                    )
                }
            }
            composable<BookingsRoute> {
                if (!isLoggedIn) {
                    LoginPromptScreen(
                        text = "Войдите, чтобы увидеть свои аренды",
                        onNavigateToAuth = { navController.navigate(AuthRoute) }
                    )
                } else {
                    BookingsScreen(
                        viewModel = viewModel(factory = factory),
                        onNavigateToDetails = { id -> navController.navigate(DetailsRoute(id)) }
                    )
                }
            }
            composable<ProfileRoute> {
                if (!isLoggedIn) {
                    LoginPromptScreen(
                        text = "Войдите или зарегистрируйтесь",
                        onNavigateToAuth = { navController.navigate(AuthRoute) }
                    )
                } else {
                    ProfileScreen(
                        viewModel = viewModel(factory = factory),
                        onLogout = {
                            isLoggedIn = false
                            isAdmin = false
                            navController.navigate(HomeRoute) { popUpTo(0) { inclusive = true } }
                        },
                        onOpenAdmin = { navController.navigate(AdminRoute) { launchSingleTop = true } }
                    )
                }
            }
            composable<AdminRoute> {
                if (!isLoggedIn || !isAdmin) {
                    LoginPromptScreen(
                        text = "Раздел доступен только администратору",
                        onNavigateToAuth = { navController.navigate(AuthRoute) }
                    )
                } else {
                    AdminScreen(viewModel = viewModel(factory = factory))
                }
            }
            composable<AuthRoute> {
                LoginScreen(
                    viewModel = viewModel(factory = factory),
                    onNavigateToRegister = {
                        navController.navigate(RegisterRoute) { popUpTo(AuthRoute) { inclusive = true } }
                    },
                    onLoginSuccess = { onAuthorized() },
                    onBack = { navController.popBackStack() }
                )
            }
            composable<RegisterRoute> {
                RegisterScreen(
                    viewModel = viewModel(factory = factory),
                    onNavigateToLogin = {
                        navController.navigate(AuthRoute) { popUpTo(RegisterRoute) { inclusive = true } }
                    },
                    onRegisterSuccess = { onAuthorized() },
                    onBack = { navController.popBackStack() }
                )
            }
            composable<DetailsRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<DetailsRoute>()
                DetailsScreen(
                    listingId = route.listingId,
                    viewModel = viewModel(factory = factory),
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToBooking = { id ->
                        if (isLoggedIn) navController.navigate(BookingRoute(id))
                        else navController.navigate(AuthRoute)
                    },
                    onOpenReviewer = { r, title ->
                        navController.navigate(
                            ReviewerRoute(
                                name = r.authorName.orEmpty(),
                                userId = r.userId.orEmpty(),
                                listingTitle = title,
                                rating = r.rating,
                                comment = r.comment.orEmpty(),
                                date = r.createdAt.orEmpty()
                            )
                        )
                    }
                )
            }
            composable<ReviewerRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<ReviewerRoute>()
                ReviewerProfileScreen(
                    name = route.name,
                    userId = route.userId,
                    listingTitle = route.listingTitle,
                    rating = route.rating,
                    comment = route.comment,
                    date = route.date,
                    onBack = { navController.popBackStack() }
                )
            }
            composable<BookingRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<BookingRoute>()
                BookingScreen(
                    listingId = route.listingId,
                    viewModel = viewModel(factory = factory),
                    onNavigateBack = { navController.popBackStack() },
                    onBookingSuccess = {
                        navController.navigate(BookingsRoute) {
                            popUpTo(HomeRoute)
                            launchSingleTop = true
                        }
                    },
                    onAddedToCart = {
                        navController.navigate(CartRoute) {
                            popUpTo<BookingRoute> { inclusive = true }
                        }
                    }
                )
            }
            composable<CartRoute> {
                CartScreen(
                    viewModel = viewModel(factory = factory),
                    isLoggedIn = isLoggedIn,
                    onBack = { navController.popBackStack() },
                    onAddMore = {
                        navController.navigate(HomeRoute) {
                            popUpTo(HomeRoute)
                            launchSingleTop = true
                        }
                    },
                    onLogin = { navController.navigate(AuthRoute) },
                    onOrderDone = {
                        navController.navigate(BookingsRoute) {
                            popUpTo(HomeRoute)
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
        } // LocalBottomBarPadding
      } // Box (screenBackdrop)
    } // Scaffold
    } // LocalScreenBackdrop
}

@Composable
fun LoginPromptScreen(text: String, onNavigateToAuth: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(12.dp))
        Text(text, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onNavigateToAuth) { Text("Войти") }
    }
}
