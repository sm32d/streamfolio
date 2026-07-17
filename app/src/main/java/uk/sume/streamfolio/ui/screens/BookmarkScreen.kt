package uk.sume.streamfolio.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import uk.sume.streamfolio.ui.theme.getThemeBackgroundBrush
import uk.sume.streamfolio.ui.viewmodel.NewsViewModel
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.AnimatedVisibilityScope
import java.net.URLEncoder
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.outlined.Hearing

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun BookmarkScreen(
    navController: NavController,
    viewModel: NewsViewModel,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val bookmarkedArticles by viewModel.bookmarkedArticles.collectAsState()
    val bookmarkedEpisodes by viewModel.bookmarkedEpisodes.collectAsState()
    val context = LocalContext.current

    val scrollState = rememberLazyListState()
    val isPodcastsEnabled = viewModel.prefs.isPodcastsEnabled
    
    var selectedTabState by remember { mutableStateOf(0) } // 0 = Articles, 1 = Episodes

    LaunchedEffect(Unit) {
        viewModel.tabResetEvent.collect { route ->
            if (route == "bookmarks_screen") {
                scrollState.animateScrollToItem(0)
            }
        }
    }

    val isDark = isSystemInDarkTheme()
    val bgBrush = getThemeBackgroundBrush()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBrush)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // Header
            Column(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(start = 24.dp, end = 24.dp, top = 24.dp)
            ) {
                Text(
                    text = "Bookmarks",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isPodcastsEnabled) "Your saved articles and episodes" else "Your saved articles and reads",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )
            }

            // Tab bar if podcasts are enabled
            if (isPodcastsEnabled) {
                TabRow(
                    selectedTabIndex = selectedTabState,
                    containerColor = Color.Transparent,
                    divider = {},
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 12.dp)
                ) {
                    Tab(
                        selected = selectedTabState == 0,
                        onClick = { selectedTabState = 0 },
                        text = { Text("Articles", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTabState == 1,
                        onClick = { selectedTabState = 1 },
                        text = { Text("Episodes", fontWeight = FontWeight.Bold) }
                    )
                }
            } else {
                // Force show articles if podcasts are disabled
                selectedTabState = 0
            }

            if (selectedTabState == 0) {
                // Articles List
                if (bookmarkedArticles.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "📰", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No saved articles",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Swipe right on articles in the Home feed to save them for later reading.",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        state = scrollState,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = 96.dp)
                    ) {
                        items(bookmarkedArticles) { article ->
                            ArticleListItem(
                                article = article,
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = animatedVisibilityScope,
                                onTap = {
                                    val encodedUrl = URLEncoder.encode(article.link, "UTF-8")
                                    navController.navigate("detail_screen/$encodedUrl")
                                },
                                onBookmarkToggle = { viewModel.toggleBookmark(article) },
                                onPlayClick = { viewModel.speakArticle(article) },
                                onQueueClick = {
                                    viewModel.addToTtsPlaylist(article)
                                    Toast.makeText(context, "Added to audio playlist", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            } else {
                // Episodes List (only reachable if isPodcastsEnabled == true)
                if (bookmarkedEpisodes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "🎙️", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No saved episodes",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Click the bookmark icon on podcast episodes to save them for later listening.",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        state = scrollState,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = 96.dp)
                    ) {
                        items(bookmarkedEpisodes) { episode ->
                            EpisodeListItem(
                                episode = episode,
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }
        }
    }
}
