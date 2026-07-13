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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
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
                        Text(text = "🔖", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No saved bookmarks",
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
        }
    }
}
