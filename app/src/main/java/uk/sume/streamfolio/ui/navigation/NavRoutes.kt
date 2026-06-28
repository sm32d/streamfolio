package uk.sume.streamfolio.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import uk.sume.streamfolio.ui.components.BottomTab
import uk.sume.streamfolio.ui.components.GlassmorphicNavBar
import uk.sume.streamfolio.ui.screens.*
import uk.sume.streamfolio.ui.viewmodel.NewsViewModel
import java.net.URLDecoder

@Composable
fun AppNavigation(viewModel: NewsViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomBarRoutes = listOf(
        BottomTab.HOME.route,
        BottomTab.SEARCH.route,
        BottomTab.BOOKMARKS.route,
        BottomTab.SETTINGS.route
    )

    val showBottomBar = currentRoute in bottomBarRoutes

    val startDestination = if (viewModel.prefs.isCompletedOnboarding) {
        BottomTab.HOME.route
    } else {
        "onboarding"
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp)
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier.fillMaxSize(),
                enterTransition = { fadeIn(animationSpec = tween(250)) },
                exitTransition = { fadeOut(animationSpec = tween(250)) }
            ) {
            composable("onboarding") {
                OnboardingScreen(navController = navController, viewModel = viewModel)
            }
            composable(BottomTab.HOME.route) {
                HomeScreen(navController = navController, viewModel = viewModel)
            }
            composable(BottomTab.SEARCH.route) {
                SearchScreen(navController = navController, viewModel = viewModel)
            }
            composable(BottomTab.BOOKMARKS.route) {
                BookmarkScreen(navController = navController, viewModel = viewModel)
            }
            composable(BottomTab.SETTINGS.route) {
                SettingsScreen(navController = navController, viewModel = viewModel)
            }
            composable(
                route = "settings_preferences",
                enterTransition = {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(300, easing = FastOutSlowInEasing)
                    )
                },
                exitTransition = {
                    slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(300, easing = FastOutSlowInEasing)
                    )
                },
                popEnterTransition = {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(300, easing = FastOutSlowInEasing)
                    )
                },
                popExitTransition = {
                    slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(300, easing = FastOutSlowInEasing)
                    )
                }
            ) {
                SettingsPreferencesScreen(navController = navController, viewModel = viewModel)
            }
            composable(
                route = "settings_categories",
                enterTransition = {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(300, easing = FastOutSlowInEasing)
                    )
                },
                exitTransition = {
                    slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(300, easing = FastOutSlowInEasing)
                    )
                },
                popEnterTransition = {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(300, easing = FastOutSlowInEasing)
                    )
                },
                popExitTransition = {
                    slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(300, easing = FastOutSlowInEasing)
                    )
                }
            ) {
                SettingsCategoriesScreen(navController = navController, viewModel = viewModel)
            }
            composable(
                route = "settings_feeds",
                enterTransition = {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(300, easing = FastOutSlowInEasing)
                    )
                },
                exitTransition = {
                    slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(300, easing = FastOutSlowInEasing)
                    )
                },
                popEnterTransition = {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(300, easing = FastOutSlowInEasing)
                    )
                },
                popExitTransition = {
                    slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(300, easing = FastOutSlowInEasing)
                    )
                }
            ) {
                SettingsFeedsScreen(navController = navController, viewModel = viewModel)
            }
            composable(
                route = "detail_screen/{url}",
                arguments = listOf(navArgument("url") { type = NavType.StringType }),
                enterTransition = {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(300, easing = FastOutSlowInEasing)
                    )
                },
                exitTransition = {
                    slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(300, easing = FastOutSlowInEasing)
                    )
                },
                popEnterTransition = {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(300, easing = FastOutSlowInEasing)
                    )
                },
                popExitTransition = {
                    slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(300, easing = FastOutSlowInEasing)
                    )
                }
            ) { backStackEntry ->
                val encodedUrl = backStackEntry.arguments?.getString("url") ?: ""
                val decodedUrl = URLDecoder.decode(encodedUrl, "UTF-8")
                DetailScreen(navController = navController, viewModel = viewModel, url = decodedUrl)
            }
        }

        if (showBottomBar) {
            val selectedTab = when (currentRoute) {
                BottomTab.SEARCH.route -> BottomTab.SEARCH
                BottomTab.BOOKMARKS.route -> BottomTab.BOOKMARKS
                BottomTab.SETTINGS.route -> BottomTab.SETTINGS
                else -> BottomTab.HOME
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(bottom = 8.dp)
            ) {
                GlassmorphicNavBar(
                    selectedTab = selectedTab,
                    onTabSelected = { tab ->
                        if (currentRoute != tab.route) {
                            navController.navigate(tab.route) {
                                popUpTo(BottomTab.HOME.route) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    }
}
}
