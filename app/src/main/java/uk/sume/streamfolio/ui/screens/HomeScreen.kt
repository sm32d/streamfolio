package uk.sume.streamfolio.ui.screens

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.outlined.Hearing
import androidx.compose.material.icons.outlined.Gesture
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import android.content.Intent
import android.widget.Toast
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import coil.request.ImageRequest
import uk.sume.streamfolio.data.model.Article
import uk.sume.streamfolio.ui.components.EmptyState
import uk.sume.streamfolio.ui.components.OfflineIndicator
import uk.sume.streamfolio.ui.components.SkeletonLoader
import uk.sume.streamfolio.ui.components.SwipeableCard
import uk.sume.streamfolio.ui.theme.getThemeBackgroundBrush
import uk.sume.streamfolio.util.NetworkMonitor
import uk.sume.streamfolio.util.UrlSecurityValidator


import androidx.compose.ui.draw.shadow
import androidx.compose.ui.zIndex
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.BorderStroke
import kotlinx.coroutines.launch
import uk.sume.streamfolio.ui.viewmodel.NewsViewModel
import uk.sume.streamfolio.data.network.DefaultFeedsConfig
import okhttp3.Headers
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
    val categoryArticlesState by viewModel.categoryArticlesState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val customFeeds by viewModel.customFeeds.collectAsState()
    val selectedPublisher by viewModel.selectedPublisher.collectAsState()
    val haptic = LocalHapticFeedback.current
    
    val isAiEnabled by viewModel.isAiEnabled.collectAsState()
    val isSmartTagsEnabled by viewModel.isSmartTagsEnabled.collectAsState()
    val hasSeenAiSpotlight by viewModel.hasSeenAiSpotlight.collectAsState()
    val swipeLeftAction by viewModel.swipeLeftAction.collectAsState()
    val swipeRightAction by viewModel.swipeRightAction.collectAsState()

    val ttsPlaylist by viewModel.ttsPlaylist.collectAsState()
    val currentTtsIndex by viewModel.currentTtsArticleIndex.collectAsState()
    val showMiniPlayer = ttsPlaylist.isNotEmpty() && currentTtsIndex != -1 && currentTtsIndex < ttsPlaylist.size
    val emptyStateBottomPadding = if (showMiniPlayer) 160.dp else 96.dp

    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    val articlesMap = remember { mutableStateMapOf<String, List<uk.sume.streamfolio.data.model.ArticleGroup>>() }

    // Cache category articles so switching back is instant; always reflect live updates (e.g. bookmarks).
    LaunchedEffect(categoryArticlesState) {
        if (categoryArticlesState.category.isNotBlank()) {
            articlesMap[categoryArticlesState.category] = categoryArticlesState.groups
        }
    }

    LaunchedEffect(Unit) {
        viewModel.tabResetEvent.collect { route ->
            if (route == "home_screen") {
                viewModel.scrollStates[selectedCategory]?.animateScrollToItem(0)
            }
        }
    }

    val context = LocalContext.current

    val activeRegion = remember(viewModel.prefs.region) { viewModel.prefs.region.uppercase() }


    val selectedCategoriesPref = remember { viewModel.prefs.selectedCategories }
    val isDefaultFeedsEnabled = remember { viewModel.prefs.isDefaultFeedsEnabled }
    val categoryOrder = viewModel.prefs.categoryOrder
    val selectedDynamicTag by viewModel.selectedDynamicTag.collectAsState()

    val categories = remember(customFeeds, selectedCategoriesPref, isDefaultFeedsEnabled, categoryOrder, selectedDynamicTag) {
        val defaultCategories = listOf("Top Stories", "World", "Business", "Technology", "Science", "Sports", "Health", "Entertainment")
        val googleCategories = if (isDefaultFeedsEnabled) {
            val filteredDefaults = defaultCategories.filter { selectedCategoriesPref.contains(it) }
            if (filteredDefaults.isEmpty()) listOf("Top Stories") else filteredDefaults
        } else {
            emptyList()
        }
        val rawCategories = (googleCategories + customFeeds.map { it.category }).distinct()
        val sorted = rawCategories.sortedWith(compareBy { cat ->
            val index = categoryOrder.indexOf(cat)
            if (index == -1) Int.MAX_VALUE else index
        }).toMutableList()
        selectedDynamicTag?.let { tag ->
            if (!sorted.contains(tag)) {
                sorted.add(tag)
            }
        }
        sorted.toList()
    }

    LaunchedEffect(categories) {
        if (categories.isNotEmpty() && !categories.contains(selectedCategory)) {
            viewModel.selectCategory(categories.first())
        }
    }

    val isDark = isSystemInDarkTheme()
    val bgBrush = getThemeBackgroundBrush()

    val isOnline by NetworkMonitor.observe(context).collectAsState(initial = NetworkMonitor.isOnline(context))
    val isOffline = !isOnline
    val hasSeenSwipeHint by viewModel.hasSeenSwipeHint.collectAsState()

    val dateFormatter = remember { SimpleDateFormat("EEEE, MMM d", Locale.getDefault()) }
    val formattedDate = remember(dateFormatter) { dateFormatter.format(Date()) }

    val pageSize = 12

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
                    text = formattedDate,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            OfflineIndicator(isOffline = isOffline)

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
                    val tabColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)

                    Tab(
                        selected = isSelected,
                        onClick = {
                            if (category.startsWith("#")) {
                                viewModel.setDynamicTagFilter(category)
                                viewModel.selectCategory(category)
                            } else {
                                viewModel.setDynamicTagFilter(null)
                                viewModel.selectCategory(category)
                            }
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

            // Refresh indicator or Shimmer Loader or content
            Box(modifier = Modifier.weight(1f)) {
                val targetCategory = selectedCategory
                val liveCategoryArticles = categoryArticlesState.groups.takeIf { categoryArticlesState.category == targetCategory }
                val currentCategoryArticles = liveCategoryArticles
                    ?: articlesMap[targetCategory]
                    ?: emptyList()
                val currentScrollState = viewModel.scrollStates.getOrPut(targetCategory) { LazyListState() }

                val currentFilteredArticles = remember(currentCategoryArticles, selectedPublisher) {
                    if (selectedPublisher == null) {
                        currentCategoryArticles
                    } else {
                        currentCategoryArticles.filter { it.primary.sourceName == selectedPublisher }
                    }
                }

                val currentPublishers = remember(currentCategoryArticles) {
                    currentCategoryArticles.map { it.primary.sourceName to getPublisherDomain(it.primary.sourceName, it.primary.sourceUrl, it.primary.link) }
                        .distinctBy { it.first }
                        .take(10)
                }

                val currentActiveTags = remember(currentCategoryArticles) {
                    currentCategoryArticles.flatMap { group ->
                        group.primary.tags?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
                    }.distinct().take(10)
                }

                val currentTrendingArticles = remember(currentFilteredArticles) {
                    currentFilteredArticles.take(3)
                }

                val currentListArticles = remember(currentFilteredArticles) {
                    currentFilteredArticles.drop(3)
                }

                var currentVisibleListCount by remember(targetCategory) {
                    mutableIntStateOf(viewModel.visibleListCounts[targetCategory] ?: pageSize)
                }

                LaunchedEffect(currentVisibleListCount) {
                    viewModel.visibleListCounts[targetCategory] = currentVisibleListCount
                }

                LaunchedEffect(currentListArticles.size) {
                    val minimumVisible = minOf(pageSize, currentListArticles.size)
                    if (currentVisibleListCount < minimumVisible) {
                        currentVisibleListCount = minimumVisible
                    }
                    if (currentVisibleListCount > currentListArticles.size) {
                        currentVisibleListCount = currentListArticles.size
                    }
                }

                val currentPagedListArticles = remember(currentListArticles, currentVisibleListCount) {
                    currentListArticles.take(currentVisibleListCount)
                }

                val shouldLoadMore by remember(currentScrollState, currentListArticles.size) {
                    derivedStateOf {
                        val hasMore = currentVisibleListCount < currentListArticles.size
                        val total = currentScrollState.layoutInfo.totalItemsCount
                        val lastVisible = currentScrollState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                        hasMore && total > 0 && lastVisible >= total - 4
                    }
                }

                var isPaging by remember(currentListArticles) { mutableStateOf(false) }

                LaunchedEffect(shouldLoadMore) {
                    if (shouldLoadMore && !isPaging) {
                        isPaging = true
                        currentVisibleListCount = (currentVisibleListCount + pageSize).coerceAtMost(currentListArticles.size)
                        isPaging = false
                    }
                }

                val hasCuratedFeedsForTarget = remember(activeRegion, targetCategory) {
                    DefaultFeedsConfig.getFeedsFor(
                        region = activeRegion,
                        category = targetCategory,
                        disabledFeedUrls = emptySet(),
                        enabledCrossRegionFeeds = emptySet()
                    ).isNotEmpty()
                }

                val enabledFeedsForTarget = remember(activeRegion, targetCategory, viewModel.prefs.disabledFeedUrls, viewModel.prefs.enabledCrossRegionFeeds) {
                    DefaultFeedsConfig.getFeedsFor(
                        region = activeRegion,
                        category = targetCategory,
                        disabledFeedUrls = viewModel.prefs.disabledFeedUrls,
                        enabledCrossRegionFeeds = viewModel.prefs.enabledCrossRegionFeeds
                    )
                }

                val emptyFeedDescriptionForTarget = remember(hasCuratedFeedsForTarget, enabledFeedsForTarget, targetCategory, activeRegion) {
                    when {
                        !hasCuratedFeedsForTarget -> "There are no default curated feeds for the \"$targetCategory\" category in your active region ($activeRegion). You can enable sources from other regions or add a custom feed."
                        enabledFeedsForTarget.isEmpty() -> "All curation sources for the \"$targetCategory\" category are currently disabled. Tap below to enable them."
                        else -> "No articles found for \"$targetCategory\". Drag down to refresh or verify your connection."
                    }
                }

                if (categories.isEmpty()) {
                    EmptyState(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = emptyStateBottomPadding),
                        icon = Icons.Outlined.Newspaper,
                        title = "No feeds active",
                        description = "Please enable default feeds or add a custom RSS feed in Settings to start reading."
                    )
                } else if (currentCategoryArticles.isEmpty() && isRefreshing) {
                    SkeletonLoader(modifier = Modifier.fillMaxSize())
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        PullToRefreshBox(
                            isRefreshing = isRefreshing,
                            onRefresh = { viewModel.refreshCurrentFeed() },
                            modifier = Modifier.fillMaxSize()
                        ) {
                        LazyColumn(
                            state = currentScrollState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 96.dp)
                        ) {
                            if (!hasSeenAiSpotlight && !isAiEnabled) {
                                item {
                                    AiSpotlightBanner(
                                        onEnable = {
                                            viewModel.setHasSeenAiSpotlight(true)
                                            navController.navigate("settings_ai")
                                        },
                                        onDismiss = { viewModel.setHasSeenAiSpotlight(true) }
                                    )
                                }
                            }

                            if (isAiEnabled && isSmartTagsEnabled && currentActiveTags.isNotEmpty()) {
                                item {
                                    TagFilterPillsRow(
                                        tags = currentActiveTags,
                                        selectedTag = selectedDynamicTag,
                                        onTagClick = { tag ->
                                            if (selectedDynamicTag == tag) {
                                                viewModel.setDynamicTagFilter(null)
                                                viewModel.selectCategory(categories.first())
                                            } else {
                                                viewModel.setDynamicTagFilter(tag)
                                                viewModel.selectCategory(tag)
                                            }
                                        }
                                    )
                                }
                            }

                            if (!hasSeenSwipeHint && currentFilteredArticles.isNotEmpty()) {
                                item {
                                    SwipeHintBanner(
                                        swipeLeftAction = swipeLeftAction,
                                        swipeRightAction = swipeRightAction,
                                        onDismiss = { viewModel.setHasSeenSwipeHint(true) }
                                    )
                                }
                            }

                            // Publisher Favicon filter carousel
                            if (currentPublishers.size > 1) {
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
                                        items(
                                            items = currentPublishers,
                                            key = { it.first }
                                        ) { (name, domain) ->
                                            val isPubSelected = selectedPublisher == name
                                            val borderAlpha = if (isPubSelected) 1f else 0.1f
                                            val borderColor = if (isPubSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface

                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier
                                                    .clickable {
                                                        viewModel.selectPublisher(if (isPubSelected) null else name)
                                                    }
                                                    .width(64.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(56.dp)
                                                        .clip(CircleShape)
                                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
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
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .clip(CircleShape),
                                                        contentScale = ContentScale.Crop
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
                            if (currentTrendingArticles.isNotEmpty()) {
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

                                        val pagerState = rememberPagerState(pageCount = { currentTrendingArticles.size })

                                        HorizontalPager(
                                            state = pagerState,
                                            contentPadding = PaddingValues(horizontal = 24.dp),
                                            pageSpacing = 16.dp,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(280.dp)
                                        ) { page ->
                                            val group = currentTrendingArticles[page]
                                            val article = group.primary
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
                                                    viewModel = viewModel,
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
                                                        viewModel.selectArticleForDetail(article)
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
                                            for (i in 0 until currentTrendingArticles.size) {
                                                val isActive = pagerState.currentPage == i
                                                val indicatorWidth = if (isActive) 24.dp else 8.dp
                                                val indicatorAlpha = if (isActive) 1f else 0.4f
                                                val indicatorColor = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface

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

                            if (categoryArticlesState.category == targetCategory && !isRefreshing && currentListArticles.isEmpty() && currentTrendingArticles.isEmpty()) {
                                item {
                                    EmptyState(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 32.dp, vertical = 64.dp)
                                            .padding(bottom = emptyStateBottomPadding),
                                        illustration = painterResource(id = uk.sume.streamfolio.R.drawable.otter_no_articles),
                                        title = "No Articles Available",
                                        scrollable = false,
                                        description = emptyFeedDescriptionForTarget,
                                        actions = {
                                            Spacer(modifier = Modifier.height(24.dp))
                                            Button(
                                                onClick = {
                                                    viewModel.filterCategoryOnSettings = targetCategory
                                                    navController.navigate("settings_providers")
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.primary
                                                ),
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
                                                Text(
                                                    text = "Manage Curation Sources",
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(12.dp))
                                            OutlinedButton(
                                                onClick = { navController.navigate("settings_feeds") },
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                                                shape = RoundedCornerShape(12.dp),
                                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                                                modifier = Modifier.fillMaxWidth(0.85f)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.BookmarkAdd,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "Add Custom RSS Feed",
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    )
                                }
                            } else {
                                items(
                                    items = currentPagedListArticles,
                                    key = { it.primary.link }
                                ) { group ->
                                    val article = group.primary
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
                                    val onSecondaryTap = remember(navController) {
                                        { secArticle: Article ->
                                            viewModel.selectArticleForDetail(secArticle)
                                            val encodedUrl = URLEncoder.encode(secArticle.link, "UTF-8")
                                            navController.navigate("detail_screen/$encodedUrl")
                                        }
                                    }

                                    ArticleListItem(
                                        article = article,
                                        secondaryArticles = group.secondary,
                                        viewModel = viewModel,
                                        sharedTransitionScope = sharedTransitionScope,
                                        animatedVisibilityScope = animatedVisibilityScope,
                                        onTap = onTap,
                                        onBookmarkToggle = onBookmarkToggle,
                                        onPlayClick = onPlayClick,
                                        onQueueClick = onQueueClick,
                                        onPlayNextClick = onPlayNextClick,
                                        onSecondaryTap = onSecondaryTap
                                    )
                                }

                                if (currentVisibleListCount < currentListArticles.size) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 24.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(28.dp),
                                                color = MaterialTheme.colorScheme.primary,
                                                strokeWidth = 3.dp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    androidx.compose.animation.AnimatedVisibility(
                        visible = isRefreshing && currentCategoryArticles.isNotEmpty(),
                        enter = fadeIn(animationSpec = tween(200)) + expandVertically(animationSpec = tween(200)),
                        exit = fadeOut(animationSpec = tween(200)) + shrinkVertically(animationSpec = tween(200)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                    ) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = Color.Transparent
                        )
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
    viewModel: NewsViewModel? = null,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onBookmarkClick: () -> Unit,
    onPlayClick: () -> Unit,
    onQueueClick: () -> Unit,
    onTap: () -> Unit
) {
    val context = LocalContext.current
    val thumbnail = article.thumbnailUrl
    val isGoogleLogo = thumbnail?.let {
        it.contains("googleusercontent.com") || it.contains("gstatic.com") || it.contains("google.com")
    } ?: false
    val hasValidThumbnail = thumbnail != null && thumbnail != "failed" && !isGoogleLogo

    LaunchedEffect(article.link, thumbnail) {
        if (!hasValidThumbnail && viewModel != null) {
            viewModel.requestThumbnailOnDemand(article)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onTap)
    ) {
        if (hasValidThumbnail) {
            // Thumbnail Image
            with(sharedTransitionScope) {
                val request = remember(thumbnail) {
                    ImageRequest.Builder(context)
                        .data(thumbnail)
                        .headers(
                            Headers.Builder()
                                .add(
                                    "User-Agent",
                                    "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                                )
                                .build()
                        )
                        .size(600, 400)
                        .build()
                }
                AsyncImage(
                    model = request,
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
                        color = MaterialTheme.colorScheme.onPrimary
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = formatPubDate(article.pubDate),
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "•",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = calculateReadingTime(article),
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
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
                        tint = MaterialTheme.colorScheme.onPrimary,
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
    secondaryArticles: List<Article> = emptyList(),
    viewModel: NewsViewModel? = null,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onTap: () -> Unit,
    onBookmarkToggle: () -> Unit,
    onPlayClick: () -> Unit,
    onQueueClick: () -> Unit,
    onPlayNextClick: () -> Unit,
    onSecondaryTap: (Article) -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    val currentArticle by rememberUpdatedState(article)
    val currentOnBookmarkToggle by rememberUpdatedState(onBookmarkToggle)
    val showRemoveConfirmationState = remember { mutableStateOf(false) }
    var showLongPressModal by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val swipeLeftAction = viewModel?.swipeLeftAction?.collectAsState()?.value ?: "bookmark"
    val swipeRightAction = viewModel?.swipeRightAction?.collectAsState()?.value ?: "share"

    fun handleBookmarkToggle() {
        if (currentArticle.isBookmarked) {
            showRemoveConfirmationState.value = true
        } else {
            currentOnBookmarkToggle()
        }
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    fun executeSwipeAction(action: String): Boolean {
        when (action) {
            "bookmark" -> {
                handleBookmarkToggle()
            }
            "share" -> {
                val shareUrl = UrlSecurityValidator.normalizeToHttps(currentArticle.link) ?: currentArticle.link
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, currentArticle.title)
                    putExtra(Intent.EXTRA_TEXT, "${currentArticle.title}\n\nRead more at: $shareUrl")
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share Article"))
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            "read" -> {
                viewModel?.toggleReadStatus(currentArticle)
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            "none" -> {
                // do nothing
            }
        }
        return false
    }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.EndToStart -> {
                    executeSwipeAction(swipeLeftAction)
                }
                SwipeToDismissBoxValue.StartToEnd -> {
                    executeSwipeAction(swipeRightAction)
                }
                else -> false
            }
        },
        positionalThreshold = { distance -> distance * 0.75f }
    )

    LaunchedEffect(dismissState.targetValue) {
        if (dismissState.targetValue != SwipeToDismissBoxValue.Settled) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val action = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> swipeRightAction
                SwipeToDismissBoxValue.EndToStart -> swipeLeftAction
                else -> "none"
            }
            
            if (action == "none") {
                Box(Modifier.fillMaxSize())
            } else {
                val color = when (action) {
                    "bookmark" -> if (article.isBookmarked) Color(0xFFEF5350).copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    "share" -> Color(0xFF00B0FF).copy(alpha = 0.2f)
                    "read" -> Color(0xFF8B5CF6).copy(alpha = 0.2f)
                    else -> Color.Transparent
                }
                val contentColor = when (action) {
                    "bookmark" -> if (article.isBookmarked) Color(0xFFEF5350) else MaterialTheme.colorScheme.primary
                    "share" -> Color(0xFF00B0FF)
                    "read" -> Color(0xFF8B5CF6)
                    else -> Color.Transparent
                }
                val alignment = when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                    SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                    else -> Alignment.Center
                }
                val icon = when (action) {
                    "bookmark" -> if (article.isBookmarked) Icons.Default.Delete else Icons.Outlined.BookmarkAdd
                    "share" -> Icons.Default.Share
                    "read" -> if (article.isRead) Icons.Default.Close else Icons.Default.Done
                    else -> null
                }
                val label = when (action) {
                    "bookmark" -> if (article.isBookmarked) "Remove" else "Save"
                    "share" -> "Share"
                    "read" -> if (article.isRead) "Unread" else "Read"
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
        }
    },
        content = {
            val cardShape = remember(secondaryArticles) {
                if (secondaryArticles.isNotEmpty()) {
                    RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
                } else {
                    RoundedCornerShape(20.dp)
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 24.dp,
                        end = 24.dp,
                        top = 8.dp,
                        bottom = if (secondaryArticles.isNotEmpty()) 0.dp else 8.dp
                    )
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                        cardShape
                    )
                    .graphicsLayer(alpha = if (article.isRead) 0.55f else 1.0f)
                    .combinedClickable(
                        onClick = onTap,
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showLongPressModal = true
                        }
                    ),
                shape = cardShape,
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

                    LaunchedEffect(article.link, thumbnail) {
                        if (!hasValidThumbnail && viewModel != null) {
                            viewModel.requestThumbnailOnDemand(article)
                        }
                    }

                    // Left Thumbnail Area with Overlaid Play Button
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(RoundedCornerShape(16.dp))
                    ) {
                        if (hasValidThumbnail) {
                            with(sharedTransitionScope) {
                                val request = remember(thumbnail) {
                                    ImageRequest.Builder(context)
                                        .data(thumbnail)
                                        .headers(
                                            Headers.Builder()
                                                .add(
                                                    "User-Agent",
                                                    "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                                                )
                                                .build()
                                        )
                                        .size(256, 256)
                                        .build()
                                }
                                AsyncImage(
                                    model = request,
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = formatPubDate(article.pubDate),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "•",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = calculateReadingTime(article),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                )
                            }
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
                                    onClick = { handleBookmarkToggle() },
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

        if (secondaryArticles.isNotEmpty()) {
            var isExpanded by remember { mutableStateOf(false) }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 24.dp, end = 24.dp, top = 0.dp, bottom = 8.dp)
                            .zIndex(-1f)
                            .offset(y = (-1).dp)
                            .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                        RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
                    )
                    .animateContentSize()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isExpanded = !isExpanded }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .padding(top = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Icon(
                            imageVector = Icons.Default.LibraryBooks,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Also covered by ${secondaryArticles.size} other ${if (secondaryArticles.size == 1) "source" else "sources"}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand coverage",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (isExpanded) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        secondaryArticles.forEach { secArticle ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSecondaryTap(secArticle) }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = secArticle.sourceName,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = secArticle.title,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showRemoveConfirmationState.value) {
        ModalBottomSheet(
            onDismissRequest = { showRemoveConfirmationState.value = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            dragHandle = { BottomSheetDefaults.DragHandle() },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFFEF5350).copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = Color(0xFFEF5350),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Remove Bookmark",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Are you sure you want to remove this article from your saved bookmarks?",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { showRemoveConfirmationState.value = false },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                    ) {
                        Text("Cancel", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            showRemoveConfirmationState.value = false
                            currentOnBookmarkToggle()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Remove", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }

    if (showLongPressModal) {
        ModalBottomSheet(
            onDismissRequest = { showLongPressModal = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            dragHandle = { BottomSheetDefaults.DragHandle() },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp)
            ) {
                // Header: Article Title & Source
                Text(
                    text = article.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                article.sourceName.let { source ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = source,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                Spacer(modifier = Modifier.height(16.dp))

                // List of Actions:
                // 1. Play Next
                BottomSheetActionRow(
                    icon = Icons.Default.PlayArrow,
                    iconBgColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    iconTint = MaterialTheme.colorScheme.primary,
                    title = "Play Next",
                    subtitle = "Insert next in the audio playlist queue",
                    onClick = {
                        showLongPressModal = false
                        onPlayNextClick()
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 2. Add to Queue
                BottomSheetActionRow(
                    icon = Icons.Default.PlaylistAdd,
                    iconBgColor = Color(0xFF10B981).copy(alpha = 0.1f),
                    iconTint = Color(0xFF10B981),
                    title = "Add to Queue",
                    subtitle = "Queue to end of the audio playlist",
                    onClick = {
                        showLongPressModal = false
                        onQueueClick()
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 3. Bookmark Action
                val isBookmarked = article.isBookmarked
                BottomSheetActionRow(
                    icon = if (isBookmarked) Icons.Default.Delete else Icons.Outlined.BookmarkAdd,
                    iconBgColor = (if (isBookmarked) Color(0xFFEF5350) else MaterialTheme.colorScheme.secondary).copy(alpha = 0.1f),
                    iconTint = if (isBookmarked) Color(0xFFEF5350) else MaterialTheme.colorScheme.secondary,
                    title = if (isBookmarked) "Remove Bookmark" else "Save Bookmark",
                    subtitle = if (isBookmarked) "Remove from your saved articles list" else "Save to read offline later",
                    onClick = {
                        showLongPressModal = false
                        onBookmarkToggle()
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 4. Read Status Action
                val isRead = article.isRead
                BottomSheetActionRow(
                    icon = if (isRead) Icons.Default.Close else Icons.Default.Done,
                    iconBgColor = Color(0xFF00B0FF).copy(alpha = 0.1f),
                    iconTint = Color(0xFF00B0FF),
                    title = if (isRead) "Mark as Unread" else "Mark as Read",
                    subtitle = if (isRead) "Keep in feed to read later" else "Mark this article as completed",
                    onClick = {
                        showLongPressModal = false
                        viewModel?.toggleReadStatus(article)
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 5. Share Action
                BottomSheetActionRow(
                    icon = Icons.Default.Share,
                    iconBgColor = Color(0xFFF59E0B).copy(alpha = 0.1f),
                    iconTint = Color(0xFFF59E0B),
                    title = "Share Article",
                    subtitle = "Send this article link to other apps",
                    onClick = {
                        showLongPressModal = false
                        val shareUrl = UrlSecurityValidator.normalizeToHttps(article.link) ?: article.link
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, article.title)
                            putExtra(Intent.EXTRA_TEXT, "${article.title}\n\nRead more at: $shareUrl")
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Article"))
                    }
                )
            }
        }
    }
}

@Composable
private fun BottomSheetActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBgColor: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(iconBgColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


// Extract publisher domain helper
fun getPublisherDomain(sourceName: String, sourceUrl: String, articleLink: String = ""): String {
    if (articleLink.isNotBlank()) {
        try {
            val uri = Uri.parse(articleLink)
            var host = uri.host
            if (!host.isNullOrEmpty() && !host.contains("google.com")) {
                host = normalizePublisherHost(host)
                return host
            }
        } catch (e: Exception) {
            // ignore
        }
    }
    if (sourceUrl.isNotBlank()) {
        try {
            val uri = Uri.parse(sourceUrl)
            var host = uri.host
            if (!host.isNullOrEmpty() && !host.contains("google.com")) {
                host = normalizePublisherHost(host)
                return host
            }
        } catch (e: Exception) {
            // ignore
        }
    }
    return when (sourceName.lowercase()) {
        else -> {
            val lower = sourceName.lowercase()
            when {
                lower.contains("bbc") -> "bbc.co.uk"
                lower.contains("npr") -> "npr.org"
                lower.contains("straits times") || lower == "st" -> "straitstimes.com"
                lower.contains("cna") || lower.contains("channel newsasia") -> "channelnewsasia.com"
                lower.contains("science daily") || lower.contains("sciencedaily") -> "sciencedaily.com"
                lower.contains("yahoo") -> "yahoo.com"
                lower.contains("reuters") -> "reuters.com"
                lower.contains("associated press") || lower == "ap" -> "apnews.com"
                lower.contains("new york times") || lower.contains("nytimes") -> "nytimes.com"
                lower.contains("bloomberg") -> "bloomberg.com"
                lower.contains("nbc") -> "nbcnews.com"
                lower.contains("guardian") -> "theguardian.com"
                else -> "google.com"
            }
        }
    }
}

private fun normalizePublisherHost(host: String): String {
    var h = host.lowercase()
    if (h.startsWith("www.")) h = h.substring(4)
    if (h.startsWith("feeds.")) h = h.substring(6)
    if (h.startsWith("rss.")) h = h.substring(4)
    if (h.startsWith("m.")) h = h.substring(2)
    if (h.startsWith("mobile.")) h = h.removePrefix("mobile.")
    if (h.endsWith("bbci.co.uk")) return "bbc.co.uk"
    return h
}

fun calculateReadingTime(article: Article): String {
    val textToCount = when {
        !article.fullText.isNullOrBlank() && !article.fullText.startsWith("Failed") && !article.fullText.startsWith("Unable") -> article.fullText
        !article.description.isNullOrBlank() -> article.description
        else -> article.title
    }
    val words = textToCount.split(Regex("\\s+")).filter { it.isNotBlank() }.size
    val wpm = 200
    val minutes = kotlin.math.max(1, (words + wpm - 1) / wpm)
    return "$minutes min read"
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

@Composable
fun AiSpotlightBanner(
    onEnable: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF6366F1), // Premium Indigo
                            Color(0xFF0D9488)  // Premium Teal
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "Upgrade to On-Device AI",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Text(
                    text = "Translate foreign feeds offline, read dynamic key summaries, and browse semantic tags. Private and run 100% locally.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = Color.White.copy(alpha = 0.85f)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onEnable,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF6366F1)
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text("Enable AI", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    TextButton(
                        onClick = onDismiss
                    ) {
                        Text(
                            text = "Dismiss",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TagFilterPillsRow(
    tags: List<String>,
    selectedTag: String?,
    onTagClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = "Smart Tags",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 8.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tags) { tag ->
                val isSelected = selectedTag == tag
                val bg = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                val textCol = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(bg)
                        .clickable { onTagClick(tag) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tag,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = textCol
                    )
                }
            }
        }
    }
}

@Composable
private fun SwipeHintBanner(
    swipeLeftAction: String,
    swipeRightAction: String,
    onDismiss: () -> Unit
) {
    if (swipeLeftAction == "none" && swipeRightAction == "none") return

    val actionLabel = { action: String ->
        when (action) {
            "bookmark" -> "save"
            "share" -> "share"
            "read" -> "mark as read"
            else -> null
        }
    }

    val leftLabel = actionLabel(swipeLeftAction)
    val rightLabel = actionLabel(swipeRightAction)
    val message = buildString {
        append("Swipe cards to act quickly: ")
        val parts = listOfNotNull(
            leftLabel?.let { "left to $it" },
            rightLabel?.let { "right to $it" }
        )
        append(parts.joinToString(", "))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Gesture,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}
