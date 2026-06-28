package uk.sume.news.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import uk.sume.news.ui.theme.DarkGradient
import uk.sume.news.ui.theme.LightGradient
import uk.sume.news.ui.viewmodel.NewsViewModel
import java.net.URLEncoder

@Composable
fun BookmarkScreen(navController: NavController, viewModel: NewsViewModel) {
    val bookmarkedArticles by viewModel.bookmarkedArticles.collectAsState()

    val isDark = isSystemInDarkTheme()
    val bgBrush = if (isDark) DarkGradient else LightGradient

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBrush)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // Header
            Text(
                text = "Bookmarks",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 48.dp, bottom = 16.dp)
            )

            if (bookmarkedArticles.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No saved bookmarks. Swipe right on cards to save them!",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(24.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    items(bookmarkedArticles) { article ->
                        ArticleListItem(
                            article = article,
                            onTap = {
                                val encodedUrl = URLEncoder.encode(article.link, "UTF-8")
                                navController.navigate("detail_screen/$encodedUrl")
                            },
                            onBookmarkToggle = { viewModel.toggleBookmark(article) }
                        )
                    }
                }
            }
        }
    }
}
