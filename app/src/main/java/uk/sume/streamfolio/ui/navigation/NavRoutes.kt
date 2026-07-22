package uk.sume.streamfolio.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.runtime.LaunchedEffect
import uk.sume.streamfolio.ui.components.BottomTab
import uk.sume.streamfolio.ui.components.GlassmorphicNavBar
import uk.sume.streamfolio.ui.components.TtsMiniPlayer
import uk.sume.streamfolio.ui.components.TtsLyricsVisualizer
import uk.sume.streamfolio.ui.screens.*
import uk.sume.streamfolio.ui.viewmodel.NewsViewModel
import java.net.URLDecoder
import java.net.URLEncoder

@Composable
fun AppNavigation(viewModel: NewsViewModel) {
    val navController = rememberNavController()

    val pendingUrl by viewModel.pendingArticleUrl.collectAsState()
    LaunchedEffect(pendingUrl) {
        pendingUrl?.let { url ->
            viewModel.setPendingArticleUrl(null)
            val encodedUrl = URLEncoder.encode(url, "UTF-8")
            navController.navigate("detail_screen/$encodedUrl")
        }
    }

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
            @OptIn(ExperimentalSharedTransitionApi::class)
            SharedTransitionLayout {
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
                composable(
                    route = BottomTab.HOME.route,
                    enterTransition = { tabEnterTransition() },
                    exitTransition = { tabExitTransition() }
                ) {
                    HomeScreen(
                        navController = navController,
                        viewModel = viewModel,
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this
                    )
                }
                composable(
                    route = BottomTab.SEARCH.route,
                    enterTransition = { tabEnterTransition() },
                    exitTransition = { tabExitTransition() }
                ) {
                    SearchScreen(
                        navController = navController,
                        viewModel = viewModel,
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this
                    )
                }
                composable(
                    route = BottomTab.BOOKMARKS.route,
                    enterTransition = { tabEnterTransition() },
                    exitTransition = { tabExitTransition() }
                ) {
                    BookmarkScreen(
                        navController = navController,
                        viewModel = viewModel,
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this
                    )
                }
                composable(
                    route = BottomTab.SETTINGS.route,
                    enterTransition = { tabEnterTransition() },
                    exitTransition = { tabExitTransition() }
                ) {
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
                route = "settings_ai",
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
                SettingsAiScreen(navController = navController, viewModel = viewModel)
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
                SettingsManageContentScreen(navController = navController, viewModel = viewModel)
            }
            composable(
                route = "settings_reorder",
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
                SettingsReorderScreen(navController = navController, viewModel = viewModel)
            }
            composable(
                route = "settings_providers",
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
                SettingsProvidersScreen(navController = navController, viewModel = viewModel)
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
                route = "settings_backup",
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
                SettingsBackupScreen(navController = navController, viewModel = viewModel)
            }
            composable(
                route = "settings_gestures",
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
                SettingsGesturesScreen(navController = navController, viewModel = viewModel)
            }
            composable(
                route = "detail_screen/{url}",
                arguments = listOf(navArgument("url") { type = NavType.StringType }),
                enterTransition = {
                    fadeIn(animationSpec = tween(300, easing = FastOutSlowInEasing))
                },
                exitTransition = {
                    fadeOut(animationSpec = tween(300, easing = FastOutSlowInEasing))
                },
                popEnterTransition = {
                    fadeIn(animationSpec = tween(300, easing = FastOutSlowInEasing))
                },
                popExitTransition = {
                    fadeOut(animationSpec = tween(300, easing = FastOutSlowInEasing))
                }
            ) { backStackEntry ->
                val encodedUrl = backStackEntry.arguments?.getString("url") ?: ""
                val decodedUrl = URLDecoder.decode(encodedUrl, "UTF-8")
                DetailScreen(
                    navController = navController,
                    viewModel = viewModel,
                    url = decodedUrl,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this
                )
            }
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
                        } else {
                            viewModel.triggerTabReset(tab.route)
                        }
                    }
                )
            }
        }

        val ttsPlaylist by viewModel.ttsPlaylist.collectAsState()
        val currentTtsArticleIndex by viewModel.currentTtsArticleIndex.collectAsState()
        val showMiniPlayer = currentRoute != null && 
                !currentRoute.startsWith("settings") && 
                currentRoute != "onboarding" &&
                ttsPlaylist.isNotEmpty() &&
                currentTtsArticleIndex != -1 &&
                currentTtsArticleIndex < ttsPlaylist.size


        val animatedBottomOffset by animateDpAsState(
            targetValue = if (showBottomBar) 108.dp else 20.dp,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "miniPlayerBottomOffset"
        )

        AnimatedVisibility(
            visible = showMiniPlayer,
            enter = fadeIn(animationSpec = tween(300)) + slideInVertically(
                initialOffsetY = { it / 2 },
                animationSpec = spring(stiffness = Spring.StiffnessLow)
            ),
            exit = fadeOut(animationSpec = tween(300)) + slideOutVertically(
                targetOffsetY = { it / 2 },
                animationSpec = spring(stiffness = Spring.StiffnessLow)
            ),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            TtsMiniPlayer(
                viewModel = viewModel,
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(bottom = animatedBottomOffset)
            )
        }

        // Immersive Apple Music-style Lyrics Visualizer Overlay
        val showLyricsVisualizer by viewModel.showLyricsVisualizer.collectAsState()
        AnimatedVisibility(
            visible = showLyricsVisualizer,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(400)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(400)),
            modifier = Modifier.fillMaxSize()
        ) {
            TtsLyricsVisualizer(
                viewModel = viewModel,
                navController = navController,
                onDismiss = { viewModel.setShowLyricsVisualizer(false) }
            )
        }
    }
}
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.tabEnterTransition(): EnterTransition {
    val bottomTabs = listOf(
        BottomTab.HOME.route,
        BottomTab.SEARCH.route,
        BottomTab.BOOKMARKS.route,
        BottomTab.SETTINGS.route
    )
    val initialRoute = initialState.destination.route ?: return fadeIn(animationSpec = tween(250))
    val targetRoute = targetState.destination.route ?: return fadeIn(animationSpec = tween(250))
    
    val initialIndex = bottomTabs.indexOf(initialRoute)
    val targetIndex = bottomTabs.indexOf(targetRoute)
    
    if (initialIndex == -1 || targetIndex == -1) return fadeIn(animationSpec = tween(250))
    
    val direction = if (targetIndex > initialIndex) {
        AnimatedContentTransitionScope.SlideDirection.Left
    } else {
        AnimatedContentTransitionScope.SlideDirection.Right
    }
    
    return slideIntoContainer(
        direction,
        animationSpec = tween(300, easing = FastOutSlowInEasing)
    ) + fadeIn(animationSpec = tween(300))
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.tabExitTransition(): ExitTransition {
    val bottomTabs = listOf(
        BottomTab.HOME.route,
        BottomTab.SEARCH.route,
        BottomTab.BOOKMARKS.route,
        BottomTab.SETTINGS.route
    )
    val initialRoute = initialState.destination.route ?: return fadeOut(animationSpec = tween(250))
    val targetRoute = targetState.destination.route ?: return fadeOut(animationSpec = tween(250))
    
    val initialIndex = bottomTabs.indexOf(initialRoute)
    val targetIndex = bottomTabs.indexOf(targetRoute)
    
    if (initialIndex == -1 || targetIndex == -1) return fadeOut(animationSpec = tween(250))
    
    val direction = if (targetIndex > initialIndex) {
        AnimatedContentTransitionScope.SlideDirection.Left
    } else {
        AnimatedContentTransitionScope.SlideDirection.Right
    }
    
    return slideOutOfContainer(
        direction,
        animationSpec = tween(300, easing = FastOutSlowInEasing)
    ) + fadeOut(animationSpec = tween(300))
}

