package uk.sume.streamfolio.ui.screens

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.absoluteValue
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
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Hearing
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import android.content.Intent
import android.widget.Toast
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
import uk.sume.streamfolio.data.model.Article
import uk.sume.streamfolio.ui.components.SkeletonLoader
import uk.sume.streamfolio.ui.components.SwipeableCard
import uk.sume.streamfolio.ui.theme.DarkGradient
import uk.sume.streamfolio.ui.theme.LightGradient
import uk.sume.streamfolio.ui.theme.EmeraldPrimary
import androidx.compose.ui.draw.shadow
import uk.sume.streamfolio.ui.viewmodel.NewsViewModel
import uk.sume.streamfolio.data.network.DefaultFeedsConfig
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: NewsViewModel,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val articles by viewModel.articles.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val customFeeds by viewModel.customFeeds.collectAsState()
    val selectedPublisher by viewModel.selectedPublisher.collectAsState()
    val context = LocalContext.current

    val activeRegion = remember(viewModel.prefs.region) { viewModel.prefs.region.uppercase() }
    val hasCuratedFeeds = remember(activeRegion, selectedCategory) {
        DefaultFeedsConfig.getFeedsFor(
            region = activeRegion,
            category = selectedCategory,
            disabledFeedUrls = emptySet(),
            enabledCrossRegionFeeds = emptySet()
        ).isNotEmpty()
    }
    
    val enabledFeeds = remember(activeRegion, selectedCategory, viewModel.prefs.disabledFeedUrls, viewModel.prefs.enabledCrossRegionFeeds) {
        DefaultFeedsConfig.getFeedsFor(
            region = activeRegion,
            category = selectedCategory,
            disabledFeedUrls = viewModel.prefs.disabledFeedUrls,
            enabledCrossRegionFeeds = viewModel.prefs.enabledCrossRegionFeeds
        )
    }
    
    val emptyFeedDescription = remember(hasCuratedFeeds, enabledFeeds, selectedCategory, activeRegion) {
        when {
            !hasCuratedFeeds -> "There are no default curated feeds for the \"$selectedCategory\" category in your active region ($activeRegion). You can enable sources from other regions or add a custom feed."
            enabledFeeds.isEmpty() -> "All curation sources for the \"$selectedCategory\" category are currently disabled. Tap below to enable them."
            else -> "No articles found for \"$selectedCategory\". Drag down to refresh or verify your connection."
        }
    }

    val selectedCategoriesPref = remember { viewModel.prefs.selectedCategories }
    val isDefaultFeedsEnabled = remember { viewModel.prefs.isDefaultFeedsEnabled }
    val categoryOrder = viewModel.prefs.categoryOrder
    val categories = remember(customFeeds, selectedCategoriesPref, isDefaultFeedsEnabled, categoryOrder) {
        val defaultCategories = listOf("Top Stories", "World", "Business", "Technology", "Science", "Sports", "Health", "Entertainment")
        val googleCategories = if (isDefaultFeedsEnabled) {
            val filteredDefaults = defaultCategories.filter { selectedCategoriesPref.contains(it) }
            if (filteredDefaults.isEmpty()) listOf("Top Stories") else filteredDefaults
        } else {
            emptyList()
        }
        val rawCategories = (googleCategories + customFeeds.map { it.category }).distinct()
        rawCategories.sortedWith(compareBy { cat ->
            val index = categoryOrder.indexOf(cat)
            if (index == -1) Int.MAX_VALUE else index
        })
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
                    text = "StreamFolio",
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
                    val tabBg = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    val tabColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)

                    Tab(
                        selected = isSelected,
                        onClick = {
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
                    if (publishers.size > 1) {
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
                                            }
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(56.dp)
                                                .shadow(3.dp, CircleShape)
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
                                            text = name,
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

                    // Horizontal Carousel for Trending Highlights
                    if (trendingArticles.isNotEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp)
                            ) {
                                Text(
                                    text = "Trending Highlights",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 12.dp)
                                )

                                val pagerState = rememberPagerState(pageCount = { trendingArticles.size })

                                HorizontalPager(
                                    state = pagerState,
                                    contentPadding = PaddingValues(horizontal = 24.dp),
                                    pageSpacing = 12.dp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(260.dp)
                                ) { page ->
                                    val article = trendingArticles[page]
                                    val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                                    val scale = (1f - (pageOffset.absoluteValue * 0.15f)).coerceIn(0.85f, 1f)
                                    val alpha = (1f - (pageOffset.absoluteValue * 0.3f)).coerceIn(0.5f, 1f)

                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .graphicsLayer {
                                                scaleX = scale
                                                scaleY = scale
                                                this.alpha = alpha
                                            }
                                            .shadow(
                                                elevation = 8.dp,
                                                shape = RoundedCornerShape(24.dp),
                                                clip = false
                                            )
                                    ) {
                                         TrendingCard(
                                             article = article,
                                             sharedTransitionScope = sharedTransitionScope,
                                             animatedVisibilityScope = animatedVisibilityScope,
                                             onBookmarkClick = {
                                                 viewModel.toggleBookmark(article)
                                             },
                                             onPlayClick = {
                                                 viewModel.speakArticle(article)
                                             },
                                             onQueueClick = {
                                                 viewModel.addToTtsPlaylist(article)
                                                 Toast.makeText(context, "Added to audio playlist", Toast.LENGTH_SHORT).show()
                                             },
                                             onTap = {
                                                 val encodedUrl = URLEncoder.encode(article.link, "UTF-8")
                                                 navController.navigate("detail_screen/$encodedUrl")
                                             }
                                         )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.CenterHorizontally),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    for (i in 0 until trendingArticles.size) {
                                        val isActive = pagerState.currentPage == i
                                        val indicatorWidth = if (isActive) 24.dp else 8.dp
                                        val indicatorAlpha = if (isActive) 1f else 0.4f
                                        val indicatorColor = if (isActive) EmeraldPrimary else MaterialTheme.colorScheme.onSurface

                                        Box(
                                            modifier = Modifier
                                                .padding(horizontal = 4.dp)
                                                .height(8.dp)
                                                .width(indicatorWidth)
                                                .clip(CircleShape)
                                                .background(indicatorColor.copy(alpha = indicatorAlpha))
                                                .animateContentSize()
                                        )
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

                    if (listArticles.isEmpty() && trendingArticles.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 32.dp, vertical = 64.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "No Articles Available",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = emptyFeedDescription,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Button(
                                    onClick = {
                                        viewModel.filterCategoryOnSettings = selectedCategory
                                        navController.navigate("settings_providers")
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                                    modifier = Modifier.fillMaxWidth(0.85f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bookmark,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Manage Curation Sources", fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedButton(
                                    onClick = {
                                        navController.navigate("settings_feeds")
                                    },
                                    border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldPrimary),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                                    modifier = Modifier.fillMaxWidth(0.85f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.BookmarkAdd,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = EmeraldPrimary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Add Custom RSS Feed", fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                                }
                            }
                        }
                    } else {
                        items(listArticles) { article ->
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
}
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun TrendingCard(
    article: Article,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onBookmarkClick: () -> Unit,
    onPlayClick: () -> Unit,
    onQueueClick: () -> Unit,
    onTap: () -> Unit
) {
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
            with(sharedTransitionScope) {
                AsyncImage(
                    model = thumbnail,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .sharedElement(
                            rememberSharedContentState(key = "image_${article.link}"),
                            animatedVisibilityScope = animatedVisibilityScope,
                            clipInOverlayDuringTransition = remember { OverlayClip(RoundedCornerShape(24.dp)) }
                        ),
                    contentScale = ContentScale.Crop
                )
            }
        } else {
            // Fallback gradient background with publisher's initial watermark
            with(sharedTransitionScope) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .sharedElement(
                            rememberSharedContentState(key = "image_${article.link}"),
                            animatedVisibilityScope = animatedVisibilityScope,
                            clipInOverlayDuringTransition = remember { OverlayClip(RoundedCornerShape(24.dp)) }
                        )
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
            with(sharedTransitionScope) {
                Text(
                    text = article.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.sharedElement(
                        rememberSharedContentState(key = "title_${article.link}"),
                        animatedVisibilityScope = animatedVisibilityScope
                    )
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = article.sourceName,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formatPubDate(article.pubDate),
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }

                // Primary Play Button establishing hierarchy
                IconButton(
                    onClick = onPlayClick,
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Play Now",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        // Floating actions button row at the top-right (secondary actions only)
        var isBookmarked by remember(article.isBookmarked) { mutableStateOf(article.isBookmarked) }
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onQueueClick,
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.Black.copy(alpha = 0.4f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Filled.PlaylistAdd,
                    contentDescription = "Add to Queue",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = {
                    onBookmarkClick()
                    isBookmarked = !isBookmarked
                },
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.Black.copy(alpha = 0.4f), CircleShape)
            ) {
                Icon(
                    imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkAdd,
                    contentDescription = "Bookmark",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ArticleListItem(
    article: Article,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onTap: () -> Unit,
    onBookmarkToggle: () -> Unit,
    onPlayClick: () -> Unit,
    onQueueClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.EndToStart -> {
                    onBookmarkToggle()
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    false
                }
                SwipeToDismissBoxValue.StartToEnd -> {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, article.title)
                        putExtra(Intent.EXTRA_TEXT, "${article.title}\n\nRead more at: ${article.link}")
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share Article"))
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    false
                }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val color = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Color(0xFF00B0FF).copy(alpha = 0.2f)
                SwipeToDismissBoxValue.EndToStart -> {
                    if (article.isBookmarked) {
                        Color(0xFFEF5350).copy(alpha = 0.2f)
                    } else {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    }
                }
                else -> Color.Transparent
            }
            val contentColor = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Color(0xFF00B0FF)
                SwipeToDismissBoxValue.EndToStart -> {
                    if (article.isBookmarked) {
                        Color(0xFFEF5350)
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                }
                else -> Color.Transparent
            }
            val alignment = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                else -> Alignment.Center
            }
            val icon = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Icons.Default.Share
                SwipeToDismissBoxValue.EndToStart -> if (article.isBookmarked) Icons.Default.Delete else Icons.Outlined.BookmarkAdd
                else -> null
            }
            val label = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> "Share"
                SwipeToDismissBoxValue.EndToStart -> if (article.isBookmarked) "Remove" else "Save"
                else -> ""
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = alignment
            ) {
                if (icon != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (direction == SwipeToDismissBoxValue.StartToEnd) {
                            Icon(icon, contentDescription = null, tint = contentColor)
                            Text(label, color = contentColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        } else {
                            Text(label, color = contentColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Icon(icon, contentDescription = null, tint = contentColor)
                        }
                    }
                }
            }
        },
        content = {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                        RoundedCornerShape(20.dp)
                    )
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
                    val thumbnail = article.thumbnailUrl
                    val isGoogleLogo = thumbnail?.let {
                        it.contains("googleusercontent.com") || it.contains("gstatic.com") || it.contains("google.com")
                    } ?: false
                    val hasValidThumbnail = thumbnail != null && thumbnail != "failed" && !isGoogleLogo

                    // Left Thumbnail Area with Overlaid Play Button
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(RoundedCornerShape(16.dp))
                    ) {
                        if (hasValidThumbnail) {
                            with(sharedTransitionScope) {
                                AsyncImage(
                                    model = thumbnail,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .sharedElement(
                                            rememberSharedContentState(key = "image_${article.link}"),
                                            animatedVisibilityScope = animatedVisibilityScope,
                                            clipInOverlayDuringTransition = remember { OverlayClip(RoundedCornerShape(16.dp)) }
                                        ),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        } else {
                            with(sharedTransitionScope) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .sharedElement(
                                            rememberSharedContentState(key = "image_${article.link}"),
                                            animatedVisibilityScope = animatedVisibilityScope,
                                            clipInOverlayDuringTransition = remember { OverlayClip(RoundedCornerShape(16.dp)) }
                                        )
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
                        }

                        // Overlaid Play Button
                        IconButton(
                            onClick = onPlayClick,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(36.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = "Play Now",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Text & Actions Column
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = article.sourceName,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        with(sharedTransitionScope) {
                            Text(
                                text = article.title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.sharedElement(
                                    rememberSharedContentState(key = "title_${article.link}"),
                                    animatedVisibilityScope = animatedVisibilityScope
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Bottom Row: Date on Left, Secondary Actions on Right
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = onQueueClick,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PlaylistAdd,
                                        contentDescription = "Add to Queue",
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(12.dp))

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
        }
    )
}

// Extract publisher domain helper
fun getPublisherDomain(sourceName: String, sourceUrl: String, articleLink: String = ""): String {
    if (sourceUrl.isNotBlank()) {
        try {
            val uri = Uri.parse(sourceUrl)
            var host = uri.host
            if (!host.isNullOrEmpty() && !host.contains("google.com")) {
                if (host.startsWith("www.")) host = host.substring(4)
                return host
            }
        } catch (e: Exception) {
            // ignore
        }
    }
    if (articleLink.isNotBlank()) {
        try {
            val uri = Uri.parse(articleLink)
            var host = uri.host
            if (!host.isNullOrEmpty() && !host.contains("google.com")) {
                if (host.startsWith("www.")) host = host.substring(4)
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
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd HH:mm:ss"
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
