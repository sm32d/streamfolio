package uk.sume.news.ui.screens

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import uk.sume.news.data.model.Article
import uk.sume.news.ui.components.SkeletonLoader
import uk.sume.news.ui.components.SwipeableCard
import uk.sume.news.ui.theme.DarkGradient
import uk.sume.news.ui.theme.LightGradient
import uk.sume.news.ui.viewmodel.NewsViewModel
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, viewModel: NewsViewModel) {
    val articles by viewModel.articles.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val customFeeds by viewModel.customFeeds.collectAsState()
    val selectedPublisher by viewModel.selectedPublisher.collectAsState()

    val selectedCategoriesPref = remember { viewModel.prefs.selectedCategories }
    val isGoogleNewsEnabled = remember { viewModel.prefs.isGoogleNewsEnabled }
    val categories = remember(customFeeds, selectedCategoriesPref, isGoogleNewsEnabled) {
        val defaultCategories = listOf("Top Stories", "Business", "Technology", "Science", "Sports", "Health", "Entertainment")
        val googleCategories = if (isGoogleNewsEnabled) {
            val filteredDefaults = defaultCategories.filter { selectedCategoriesPref.contains(it) }
            if (filteredDefaults.isEmpty()) listOf("Top Stories") else filteredDefaults
        } else {
            emptyList()
        }
        googleCategories + customFeeds.map { it.category }
    }

    LaunchedEffect(categories) {
        if (categories.isNotEmpty() && !categories.contains(selectedCategory)) {
            viewModel.selectCategory(categories.first())
        }
    }

    val isDark = isSystemInDarkTheme()
    val bgBrush = if (isDark) DarkGradient else LightGradient

    // Derive publisher domains from current articles for publisher filter carousel
    val publishers = remember(articles) {
        articles.map { it.sourceName to getPublisherDomain(it.sourceName, it.sourceUrl, it.link) }
            .distinctBy { it.first }
            .take(10)
    }

    val filteredArticles = remember(articles, selectedPublisher) {
        if (selectedPublisher == null) articles else articles.filter { it.sourceName == selectedPublisher }
    }

    // Split trending (first 3 articles) and regular list
    val trendingArticles = remember(filteredArticles) {
        filteredArticles.take(3)
    }
    
    // Remaining articles
    val listArticles = remember(filteredArticles) {
        filteredArticles.drop(3)
    }

    // Track active swipe deck index (if swiped away, index increases)
    var swipeIndex by remember(trendingArticles) { mutableStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBrush)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PrimeFeed",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date()),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            // Scrollable Categories Tabs
            ScrollableTabRow(
                selectedTabIndex = categories.indexOf(selectedCategory).coerceAtLeast(0),
                edgePadding = 24.dp,
                divider = {},
                indicator = {},
                containerColor = Color.Transparent,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                categories.forEach { category ->
                    val isSelected = category == selectedCategory
                    val tabBg = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    val tabColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)

                    Tab(
                        selected = isSelected,
                        onClick = {
                            swipeIndex = 0
                            viewModel.selectCategory(category)
                        },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(tabBg)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = category,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = tabColor
                        )
                    }
                }
            }

            // Refresh indicator or Shimmer Loader
            if (categories.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = "No feeds active",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Please enable Google News or add a custom RSS feed in Settings to start reading.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else if (isRefreshing && articles.isEmpty()) {
                SkeletonLoader(modifier = Modifier.weight(1f))
            } else {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = { viewModel.refreshCurrentFeed() },
                    modifier = Modifier.weight(1f)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 96.dp)
                    ) {
                    
                    // Publisher Favicon filter carousel
                    if (publishers.isNotEmpty()) {
                        item {
                            Text(
                                text = "Publishers",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(publishers) { (name, domain) ->
                                    val isPubSelected = selectedPublisher == name
                                    val borderAlpha = if (isPubSelected) 1f else 0.1f
                                    val borderColor = if (isPubSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .clickable {
                                                viewModel.selectPublisher(if (isPubSelected) null else name)
                                                swipeIndex = 0
                                            }
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(56.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.surface)
                                                .border(
                                                    2.dp,
                                                    borderColor.copy(alpha = borderAlpha),
                                                    CircleShape
                                                )
                                                .padding(6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            AsyncImage(
                                                model = "https://www.google.com/s2/favicons?sz=64&domain=$domain",
                                                contentDescription = name,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Fit
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = name.take(10),
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Stacked card news deck (Trending Highlights)
                    if (trendingArticles.isNotEmpty() && swipeIndex < trendingArticles.size) {
                        item {
                            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                                Text(
                                    text = "Trending Highlights",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(260.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Render stacked cards bottom-up
                                    val topCardIndex = swipeIndex
                                    for (i in (trendingArticles.size - 1) downTo topCardIndex) {
                                        val article = trendingArticles[i]
                                        val isTop = i == topCardIndex
                                        val cardScale = if (isTop) 1f else (1f - (i - topCardIndex) * 0.05f)
                                        val cardOffsetY = if (isTop) 0.dp else ((i - topCardIndex) * 12).dp

                                        if (isTop) {
                                            SwipeableCard(
                                                onSwipeRight = {
                                                    viewModel.toggleBookmark(article)
                                                    swipeIndex++
                                                },
                                                onSwipeLeft = {
                                                    swipeIndex++
                                                }
                                            ) {
                                                TrendingCard(
                                                    article = article,
                                                    onTap = {
                                                        val encodedUrl = URLEncoder.encode(article.link, "UTF-8")
                                                        navController.navigate("detail_screen/$encodedUrl")
                                                    }
                                                )
                                            }
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .offset(y = cardOffsetY)
                                                    .scale(cardScale)
                                            ) {
                                                TrendingCard(article = article, onTap = {})
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // List of regular news stories
                    item {
                        Text(
                            text = "Latest Stories",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                        )
                    }

                    if (listArticles.isEmpty() && (trendingArticles.isEmpty() || swipeIndex >= trendingArticles.size)) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No articles found. Drag to refresh or select another category.",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }
                    } else {
                        items(listArticles) { article ->
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
}
}

@Composable
fun TrendingCard(article: Article, onTap: () -> Unit) {
    val thumbnail = article.thumbnailUrl
    val isGoogleLogo = thumbnail?.let {
        it.contains("googleusercontent.com") || it.contains("gstatic.com") || it.contains("google.com")
    } ?: false
    val hasValidThumbnail = thumbnail != null && thumbnail != "failed" && !isGoogleLogo

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onTap)
    ) {
        if (hasValidThumbnail) {
            // Thumbnail Image
            AsyncImage(
                model = thumbnail,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            // Fallback gradient background with publisher's initial watermark
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF3B82F6), Color(0xFF10B981))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = article.sourceName.firstOrNull()?.toString() ?: "?",
                    color = Color.White.copy(alpha = 0.15f),
                    fontSize = 120.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        // Gradient overlay for readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
                        startY = 100f
                    )
                )
        )

        // Card Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Trending",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = article.category,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = article.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = article.sourceName,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = formatPubDate(article.pubDate),
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
fun ArticleListItem(
    article: Article,
    onTap: () -> Unit,
    onBookmarkToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .clickable(onClick = onTap),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail with fallback gradient helper
            val thumbnail = article.thumbnailUrl
            val isGoogleLogo = thumbnail?.let {
                it.contains("googleusercontent.com") || it.contains("gstatic.com") || it.contains("google.com")
            } ?: false
            val hasValidThumbnail = thumbnail != null && thumbnail != "failed" && !isGoogleLogo

            if (hasValidThumbnail) {
                AsyncImage(
                    model = thumbnail,
                    contentDescription = null,
                    modifier = Modifier
                        .size(96.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF3B82F6), Color(0xFF10B981))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = article.sourceName.firstOrNull()?.toString() ?: "?",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = article.sourceName,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = article.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatPubDate(article.pubDate),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                    IconButton(
                        onClick = onBookmarkToggle,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (article.isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkAdd,
                            contentDescription = "Bookmark",
                            tint = if (article.isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

// Extract publisher domain helper
fun getPublisherDomain(sourceName: String, sourceUrl: String, articleLink: String = ""): String {
    if (sourceUrl.isNotBlank()) {
        try {
            val uri = Uri.parse(sourceUrl)
            val host = uri.host
            if (!host.isNullOrEmpty() && !host.contains("google.com")) {
                return host
            }
        } catch (e: Exception) {
            // ignore
        }
    }
    if (articleLink.isNotBlank()) {
        try {
            val uri = Uri.parse(articleLink)
            val host = uri.host
            if (!host.isNullOrEmpty() && !host.contains("google.com")) {
                return host
            }
        } catch (e: Exception) {
            // ignore
        }
    }
    return when (sourceName.lowercase()) {
        "bbc news", "bbc" -> "bbc.co.uk"
        "cnn" -> "cnn.com"
        "reuters" -> "reuters.com"
        "associated press", "ap" -> "apnews.com"
        "the new york times", "nytimes" -> "nytimes.com"
        "bloomberg" -> "bloomberg.com"
        "nbc news" -> "nbcnews.com"
        "guardian" -> "theguardian.com"
        else -> "google.com" // generic fallback for favicon
    }
}

// Convert pubDate format to relative time helper
fun formatPubDate(pubDateStr: String): String {
    if (pubDateStr.isBlank()) return ""
    val formats = listOf(
        "EEE, dd MMM yyyy HH:mm:ss z",
        "EEE, dd MMM yyyy HH:mm:ss Z",
        "yyyy-MM-dd'T'HH:mm:ss'Z'"
    )
    for (format in formats) {
        try {
            val sdf = SimpleDateFormat(format, Locale.US)
            val date = sdf.parse(pubDateStr)
            if (date != null) {
                val diff = Date().time - date.time
                val minutes = (diff / (1000 * 60)).toInt()
                val hours = (diff / (1000 * 60 * 60)).toInt()
                val days = (diff / (1000 * 60 * 60 * 24)).toInt()

                return when {
                    minutes < 60 -> "${minutes.coerceAtLeast(1)} min ago"
                    hours < 24 -> "$hours hours ago"
                    else -> "$days days ago"
                }
            }
        } catch (e: Exception) {
            // try next format
        }
    }
    return pubDateStr // fallback
}
