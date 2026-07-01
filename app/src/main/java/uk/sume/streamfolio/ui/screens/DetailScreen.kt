package uk.sume.streamfolio.ui.screens

import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.*
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Hearing
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import coil.compose.AsyncImage
import uk.sume.streamfolio.ui.theme.DarkGradient
import uk.sume.streamfolio.ui.theme.LightGradient
import uk.sume.streamfolio.ui.theme.EmeraldPrimary
import uk.sume.streamfolio.ui.components.TextSkeletonLoader
import uk.sume.streamfolio.ui.viewmodel.NewsViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun DetailScreen(
    navController: NavController,
    viewModel: NewsViewModel,
    url: String,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val articles by viewModel.articles.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val bookmarkedArticles by viewModel.bookmarkedArticles.collectAsState()

    // Find the article across all cached lists
    val article = remember(articles, searchResults, bookmarkedArticles, url) {
        articles.firstOrNull { it.link == url }
            ?: searchResults.firstOrNull { it.link == url }
            ?: bookmarkedArticles.firstOrNull { it.link == url }
    }

    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val bgBrush = if (isDark) DarkGradient else LightGradient

    var currentTab by remember { mutableStateOf("Reader") } // "Reader" or "Web"
    


    val articleBody by viewModel.articleBody.collectAsState()
    val isLoadingBody by viewModel.isLoadingBody.collectAsState()
    val isTtsPlaying by viewModel.ttsHelper.isPlaying.collectAsState()
    val ttsParagraphIndex by viewModel.ttsHelper.currentParagraphIndex.collectAsState()

    // Trigger load of article body for Reader mode
    LaunchedEffect(url) {
        viewModel.loadArticleBody(url)
    }

    // Stop speech when exiting detail screen
    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopSpeaking()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBrush)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // Top Navigation Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                // Reader / Web Switcher Tabs
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(4.dp)
                ) {
                    listOf("Reader", "Web View").forEach { tab ->
                        val isSelected = (tab == "Reader" && currentTab == "Reader") || (tab == "Web View" && currentTab == "Web")
                        val tabBg = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                        val tabColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(tabBg)
                                .clickable { currentTab = if (tab == "Reader") "Reader" else "Web" }
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tab,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = tabColor
                            )
                        }
                    }
                }

                IconButton(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, article?.title ?: "")
                            putExtra(Intent.EXTRA_TEXT, url)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Article"))
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Article Content Shell
            if (currentTab == "Reader") {
                if (article == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "Article details not found.", color = MaterialTheme.colorScheme.onSurface)
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Hero Thumbnail
                        val thumbnail = article.thumbnailUrl
                        val isGoogleLogo = thumbnail?.let {
                            it.contains("googleusercontent.com") || it.contains("gstatic.com") || it.contains("google.com")
                        } ?: false
                        val hasValidThumbnail = thumbnail != null && thumbnail != "failed" && !isGoogleLogo

                        if (hasValidThumbnail) {
                            with(sharedTransitionScope) {
                                AsyncImage(
                                    model = thumbnail,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(240.dp)
                                        .padding(horizontal = 24.dp)
                                        .clip(RoundedCornerShape(24.dp))
                                        .sharedElement(
                                            rememberSharedContentState(key = "image_${article.link}"),
                                            animatedVisibilityScope = animatedVisibilityScope,
                                            clipInOverlayDuringTransition = remember { OverlayClip(RoundedCornerShape(24.dp)) }
                                        ),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        } else {
                            with(sharedTransitionScope) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .padding(horizontal = 24.dp)
                                        .clip(RoundedCornerShape(24.dp))
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
                                        text = article.sourceName,
                                        color = Color.White,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Meta details
                        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)) {
                            Text(
                                text = article.category.uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            with(sharedTransitionScope) {
                                Text(
                                    text = article.title,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 28.sp,
                                    modifier = Modifier.sharedElement(
                                        rememberSharedContentState(key = "title_${article.link}"),
                                        animatedVisibilityScope = animatedVisibilityScope
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surface)
                                        .padding(4.dp)
                                ) {
                                    val domain = getPublisherDomain(article.sourceName, article.sourceUrl, article.link)
                                    AsyncImage(
                                        model = "https://www.google.com/s2/favicons?sz=64&domain=$domain",
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = article.sourceName,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = formatPubDate(article.pubDate),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }

                        // Audio Player controller card ("Listen to Article")
                        if (articleBody.isNotBlank() && articleBody != "Unable to parse article text. Please open in WebView to read the full story.") {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 8.dp),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.speakArticle() }
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = if (isTtsPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "Listen",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isTtsPlaying) "Pause Listening" else "Listen to Article",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }

                        // Body text loader or formatted output
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            if (isLoadingBody) {
                                TextSkeletonLoader()
                            } else {
                                val bodyToShow = if (articleBody.length < 150 || articleBody.startsWith("Failed to load")) {
                                    article.description.takeIf { it.isNotBlank() } ?: articleBody
                                } else {
                                    articleBody
                                }
                                
                                val showSuggestionBanner = articleBody.length < 150 || articleBody.startsWith("Failed to load")

                                val paragraphs = bodyToShow.split("\n\n").filter { it.isNotBlank() }
                                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    if (showSuggestionBanner) {
                                        Card(
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                            ),
                                            shape = RoundedCornerShape(16.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(16.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Info,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(
                                                    text = "Full-text parsing is restricted by this publisher. Tap 'Web View' above to read the complete article.",
                                                    fontSize = 13.sp,
                                                    lineHeight = 18.sp,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }

                                    paragraphs.forEachIndexed { index, paragraph ->
                                        val isHighlighted = isTtsPlaying && index == ttsParagraphIndex
                                        val textColor = if (isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        val weight = if (isHighlighted) FontWeight.Bold else FontWeight.Normal
                                        val cardAlpha = if (isHighlighted) 0.1f else 0.0f

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = cardAlpha))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = paragraph,
                                                fontSize = 16.sp,
                                                lineHeight = 26.sp,
                                                color = textColor,
                                                fontWeight = weight
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(100.dp))
                    }
                }
            } else {
                // In-App WebView Layout
                var isWebViewLoading by remember { mutableStateOf(true) }
                Box(modifier = Modifier.fillMaxSize()) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                webViewClient = object : WebViewClient() {
                                    override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                        super.onPageStarted(view, url, favicon)
                                        isWebViewLoading = true
                                    }
                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        isWebViewLoading = false
                                    }
                                    override fun onReceivedError(
                                        view: WebView?,
                                        errorCode: Int,
                                        description: String?,
                                        failingUrl: String?
                                    ) {
                                        super.onReceivedError(view, errorCode, description, failingUrl)
                                        isWebViewLoading = false
                                    }
                                }
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                loadUrl(url)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                    
                    if (isWebViewLoading) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter)
                                .height(3.dp),
                            color = EmeraldPrimary,
                            trackColor = Color.Transparent
                        )
                    }
                }
            }
        }

        // Floating Bottom Reaction Bar
        if (article != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 16.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Row(
                    modifier = Modifier
                        .wrapContentWidth()
                        .height(60.dp)
                        .shadow(16.dp, RoundedCornerShape(30.dp))
                        .clip(RoundedCornerShape(30.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(30.dp))
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Save Bookmark Action
                    Row(
                        modifier = Modifier.clickable { viewModel.toggleBookmark(article) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (article.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Save",
                            tint = if (article.isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (article.isBookmarked) "Saved" else "Save",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Share Action
                    Row(
                        modifier = Modifier.clickable {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, article.title)
                                putExtra(Intent.EXTRA_TEXT, url)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Article"))
                        },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Share",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}


