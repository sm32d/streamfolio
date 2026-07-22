package uk.sume.streamfolio.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import uk.sume.streamfolio.ui.components.EmptyState
import uk.sume.streamfolio.ui.components.OfflineIndicator
import uk.sume.streamfolio.ui.theme.getThemeBackgroundBrush
import uk.sume.streamfolio.ui.viewmodel.NewsViewModel
import uk.sume.streamfolio.util.NetworkMonitor
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.AnimatedVisibilityScope
import java.net.URLEncoder
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Hearing
import androidx.compose.material.icons.outlined.BookmarkBorder

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun BookmarkScreen(
    navController: NavController,
    viewModel: NewsViewModel,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val bookmarkedArticles by viewModel.bookmarkedArticles.collectAsState()
    val context = LocalContext.current
    val isOnline by NetworkMonitor.observe(context).collectAsState(initial = NetworkMonitor.isOnline(context))
    val isOffline = !isOnline

    val ttsPlaylist by viewModel.ttsPlaylist.collectAsState()
    val currentTtsIndex by viewModel.currentTtsArticleIndex.collectAsState()
    val showMiniPlayer = ttsPlaylist.isNotEmpty() && currentTtsIndex != -1 && currentTtsIndex < ttsPlaylist.size
    val emptyStateBottomPadding = if (showMiniPlayer) 160.dp else 96.dp

    val scrollState = rememberLazyListState()

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
                    text = "Your saved articles and reads",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                )
            }

            OfflineIndicator(isOffline = isOffline)

            if (bookmarkedArticles.isEmpty()) {
                EmptyState(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(bottom = emptyStateBottomPadding),
                    illustration = painterResource(id = uk.sume.streamfolio.R.drawable.otter_no_bookmarks),
                    title = "No saved bookmarks",
                    description = "Swipe right on articles in the Home feed to save them for later reading."
                )
            } else {
                LazyColumn(
                    state = scrollState,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    items(
                        items = bookmarkedArticles,
                        key = { it.link }
                    ) { article ->
                        val onTap = remember(article.link) {
                            {
                                viewModel.selectArticleForDetail(article)
                                val encodedUrl = URLEncoder.encode(article.link, "UTF-8")
                                navController.navigate("detail_screen/$encodedUrl")
                            }
                        }
                        val articleState = rememberUpdatedState(article)
                        val onBookmarkToggle = remember { { viewModel.toggleBookmark(articleState.value) } }
                        val onPlayClick = remember(article.link) { { viewModel.speakArticle(article) } }
                        val onQueueClick = remember(article.link, context) {
                            {
                                viewModel.addToTtsPlaylist(article)
                                Toast.makeText(context, "Added to audio playlist", Toast.LENGTH_SHORT).show()
                            }
                        }

                        val onPlayNextClick = remember(article.link, context) {
                            {
                                viewModel.playNextInTtsPlaylist(article)
                                Toast.makeText(context, "Added to play next", Toast.LENGTH_SHORT).show()
                            }
                        }

                        ArticleListItem(
                            article = article,
                            viewModel = viewModel,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            onTap = onTap,
                            onBookmarkToggle = onBookmarkToggle,
                            onPlayClick = onPlayClick,
                            onQueueClick = onQueueClick,
                            onPlayNextClick = onPlayNextClick
                        )
                    }
                }
            }
        }
    }
}
