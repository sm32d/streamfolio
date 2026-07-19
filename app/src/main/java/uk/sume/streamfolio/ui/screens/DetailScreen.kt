package uk.sume.streamfolio.ui.screens

import android.content.Intent
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import coil.compose.AsyncImage
import uk.sume.streamfolio.ui.theme.getThemeBackgroundBrush


import uk.sume.streamfolio.ui.components.TextSkeletonLoader
import uk.sume.streamfolio.ui.viewmodel.NewsViewModel
import androidx.compose.foundation.BorderStroke
import uk.sume.streamfolio.ui.viewmodel.AiSummaryState
import uk.sume.streamfolio.util.UrlSecurityValidator

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun DetailScreen(
    navController: NavController,
    viewModel: NewsViewModel,
    url: String,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val articleState = viewModel.currentArticleDetail.collectAsState()
    val article = articleState.value

    val isAiEnabled by viewModel.isAiEnabled.collectAsState()
    val isTranslationEnabled by viewModel.isTranslationEnabled.collectAsState()
    val isSummaryEnabled by viewModel.isSummaryEnabled.collectAsState()
    val isSmartTagsEnabled by viewModel.isSmartTagsEnabled.collectAsState()
    val isGeminiSupported by viewModel.isGeminiSupported.collectAsState()

    val translatedTitle by viewModel.translatedTitle.collectAsState()
    val translatedBody by viewModel.translatedBody.collectAsState()
    val isTranslationLoading by viewModel.isTranslationLoading.collectAsState()
    val translationError by viewModel.translationError.collectAsState()
    val articleLanguage by viewModel.articleLanguage.collectAsState()
    val targetLang by viewModel.translationTargetLanguage.collectAsState()

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearTranslation()
        }
    }

    LaunchedEffect(translationError) {
        translationError?.let { err ->
            Toast.makeText(context, "Translation error: $err", Toast.LENGTH_LONG).show()
        }
    }
    val isDark = isSystemInDarkTheme()
    val bgBrush = getThemeBackgroundBrush()

    var currentTab by remember { mutableStateOf("Reader") } // "Reader" or "Web"
    


    val articleBody by viewModel.articleBody.collectAsState()
    val isLoadingBody by viewModel.isLoadingBody.collectAsState()
    val isTtsPlaying by viewModel.ttsHelper.isPlaying.collectAsState()
    val ttsParagraphIndex by viewModel.ttsHelper.currentParagraphIndex.collectAsState()

    val fontFamily by viewModel.readerFontFamily.collectAsState()
    val fontSize by viewModel.readerFontSize.collectAsState()
    val lineSpacing by viewModel.readerLineSpacing.collectAsState()
    var showTypographyPanel by remember { mutableStateOf(false) }

    val playlist by viewModel.ttsPlaylist.collectAsState()
    val currentIndex by viewModel.currentTtsArticleIndex.collectAsState()
    val hasActiveTrack = playlist.isNotEmpty() && currentIndex != -1

    // Trigger load of article body for Reader mode
    LaunchedEffect(url) {
        viewModel.loadArticleBody(url)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBrush)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Navigation Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                // Back Button (Start aligned)
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        navController.popBackStack()
                    },
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                // Reader / Web Switcher Tabs (Center aligned)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(4.dp)
                ) {
                    listOf("Reader", "Web View").forEach { tab ->
                        val isSelected = (tab == "Reader" && currentTab == "Reader") || (tab == "Web View" && currentTab == "Web")
                        val tabBg = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                        val tabColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        
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
                
                // Action Buttons Row (End aligned)
                Row(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentTab == "Reader") {
                        IconButton(onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showTypographyPanel = !showTypographyPanel
                        }) {
                            Icon(
                                imageVector = Icons.Default.TextFields,
                                contentDescription = "Text Settings",
                                tint = if (showTypographyPanel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        article?.let { art ->
                            IconButton(onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.addToTtsPlaylist(art)
                                Toast.makeText(context, "Added to audio playlist", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(
                                    imageVector = Icons.Filled.PlaylistAdd,
                                    contentDescription = "Queue in Playlist",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            if (currentTab == "Reader" && showTypographyPanel) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Font Family", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Row {
                                listOf("Sans-Serif", "Serif").forEach { opt ->
                                    val isSel = (opt == "Sans-Serif" && fontFamily == "sans_serif") || (opt == "Serif" && fontFamily == "serif")
                                    FilterChip(
                                        selected = isSel,
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            viewModel.updateReaderFontFamily(if (opt == "Serif") "serif" else "sans_serif")
                                        },
                                        label = { Text(opt, fontSize = 12.sp) },
                                        modifier = Modifier.padding(start = 4.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Text Size (${fontSize.toInt()}sp)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        if (fontSize > 12f) viewModel.updateReaderFontSize(fontSize - 2f)
                                    },
                                    enabled = fontSize > 12f
                                ) {
                                    Text("A-", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }
                                IconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        if (fontSize < 30f) viewModel.updateReaderFontSize(fontSize + 2f)
                                    },
                                    enabled = fontSize < 30f
                                ) {
                                    Text("A+", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Line Spacing", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Row {
                                listOf("1.2x" to 1.2f, "1.5x" to 1.5f, "1.8x" to 1.8f).forEach { (label, value) ->
                                    val isSel = lineSpacing == value
                                    FilterChip(
                                        selected = isSel,
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            viewModel.updateReaderLineSpacing(value)
                                        },
                                        label = { Text(label, fontSize = 12.sp) },
                                        modifier = Modifier.padding(start = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            val similarArticles by viewModel.similarArticlesForDetail.collectAsState()
            if (currentTab == "Reader" && similarArticles.isNotEmpty()) {
                var isExpanded by remember { mutableStateOf(false) }
                AnimatedVisibility(
                    visible = true,
                    enter = expandVertically(),
                    exit = shrinkVertically(),
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f))
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                RoundedCornerShape(16.dp)
                            )
                            .animateContentSize()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isExpanded = !isExpanded }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.LibraryBooks,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Also covered by ${similarArticles.size} other ${if (similarArticles.size == 1) "source" else "sources"}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                )
                            }
                            Icon(
                                imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                contentDescription = "Toggle similar coverage",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        if (isExpanded) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                            )
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 150.dp)
                                    .verticalScroll(rememberScrollState())
                                    .padding(bottom = 6.dp)
                            ) {
                                similarArticles.forEach { secArticle ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                isExpanded = false
                                                val encodedUrl = java.net.URLEncoder.encode(secArticle.link, "UTF-8")
                                                navController.navigate("detail_screen/$encodedUrl") {
                                                    popUpTo("detail_screen/{url}") { inclusive = true }
                                                }
                                            }
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
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
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
                                val titleSize = (fontSize * 1.3f).sp
                                Text(
                                    text = translatedTitle.ifEmpty { article.title },
                                    fontSize = titleSize,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = (fontSize * 1.3f * 1.25f).sp,
                                    fontFamily = if (fontFamily == "serif") FontFamily(android.graphics.Typeface.SERIF) else FontFamily(android.graphics.Typeface.SANS_SERIF),
                                    modifier = Modifier.sharedElement(
                                        rememberSharedContentState(key = "title_${article.link}"),
                                        animatedVisibilityScope = animatedVisibilityScope
                                    )
                                )
                            }

                            if (isAiEnabled && isSmartTagsEnabled && !article.tags.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val tagsList = article.tags.split(",").map { it.trim() }.filter { it.isNotBlank() }
                                    tagsList.forEach { tag ->
                                        Text(
                                            text = tag,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                                .clickable {
                                                    viewModel.setDynamicTagFilter(tag)
                                                    viewModel.selectCategory(tag)
                                                    navController.popBackStack()
                                                }
                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
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
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
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
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = formatPubDate(article.pubDate),
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "•",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = calculateReadingTime(article),
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }
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
                                        .clickable { viewModel.speakArticle(article) }
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

                        // AI Summary Card
                        if (isAiEnabled && isSummaryEnabled && articleBody.isNotBlank() && articleBody != "Unable to parse article text. Please open in WebView to read the full story.") {
                            val aiSummaryState by viewModel.aiSummaryState.collectAsState()
                            
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 8.dp),
                                shape = RoundedCornerShape(24.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = "AI",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "AI Key Insights",
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontSize = 14.sp
                                            )
                                        }
                                        
                                        // On-Device AI Badge
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                                        ) {
                                            Text(
                                                text = "On-Device",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(12.dp))

                                    val isGeminiOk = isGeminiSupported != false
                                    if (!isGeminiOk) {
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.05f),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(modifier = Modifier.padding(12.dp)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(
                                                            imageVector = Icons.Default.Warning,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.error,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(
                                                            text = "Summary Unavailable",
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 12.sp,
                                                            color = MaterialTheme.colorScheme.error
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = "AI insights require Gemini Nano (AICore), which is not available on this device. The standard article preview is shown below instead.",
                                                        fontSize = 11.sp,
                                                        lineHeight = 15.sp,
                                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                                    )
                                                }
                                            }
                                            
                                            if (article != null && article.description.isNotBlank()) {
                                                Text(
                                                    text = "Standard RSS Preview",
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                                )
                                                Surface(
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text(
                                                        text = android.text.Html.fromHtml(article.description, android.text.Html.FROM_HTML_MODE_LEGACY).toString().trim(),
                                                        fontSize = 12.sp,
                                                        lineHeight = 17.sp,
                                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                                                        modifier = Modifier.padding(12.dp)
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        when (val state = aiSummaryState) {
                                            is AiSummaryState.Idle -> {
                                                Button(
                                                    onClick = {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        viewModel.generateAiSummary(articleBody)
                                                    },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    shape = RoundedCornerShape(16.dp),
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = MaterialTheme.colorScheme.primary
                                                    )
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.AutoAwesome,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text("Generate Summary", fontWeight = FontWeight.Bold)
                                                }
                                            }
                                            is AiSummaryState.Loading -> {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                                    horizontalArrangement = Arrangement.Center,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(20.dp),
                                                        strokeWidth = 2.dp,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Text(
                                                        text = "Gemini is summarizing...",
                                                        fontSize = 13.sp,
                                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                    )
                                                }
                                            }
                                            is AiSummaryState.DownloadingModel -> {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                                    horizontalArrangement = Arrangement.Center,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(20.dp),
                                                        strokeWidth = 2.dp,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Text(
                                                        text = "Initializing local AI model (this may take a while depending on your internet connection)...",
                                                        fontSize = 13.sp,
                                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                    )
                                                }
                                            }
                                            is AiSummaryState.Success -> {
                                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    val bullets = state.summary.split("\n")
                                                        .map { it.trim().removePrefix("*").trim() }
                                                        .filter { it.isNotBlank() }
                                                    
                                                    if (bullets.isNotEmpty()) {
                                                        for (bullet in bullets) {
                                                            Row(modifier = Modifier.fillMaxWidth()) {
                                                                Text(
                                                                    text = "•",
                                                                    color = MaterialTheme.colorScheme.primary,
                                                                    fontWeight = FontWeight.Bold,
                                                                    fontSize = 15.sp,
                                                                    modifier = Modifier.padding(end = 8.dp)
                                                                )
                                                                Text(
                                                                    text = bullet,
                                                                    fontSize = 13.sp,
                                                                    lineHeight = 18.sp,
                                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                                                )
                                                            }
                                                        }
                                                    } else {
                                                        Text(
                                                            text = state.summary,
                                                            fontSize = 13.sp,
                                                            lineHeight = 18.sp,
                                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                                        )
                                                    }
                                                }
                                            }
                                            is AiSummaryState.Error -> {
                                                Column(
                                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    val isPolicyCheck = state.message.contains("policy check", ignoreCase = true)
                                                    
                                                    Surface(
                                                        shape = RoundedCornerShape(12.dp),
                                                        color = if (isPolicyCheck) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f) else MaterialTheme.colorScheme.error.copy(alpha = 0.05f),
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Column(modifier = Modifier.padding(12.dp)) {
                                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                                Icon(
                                                                    imageVector = if (isPolicyCheck) Icons.Default.Info else Icons.Default.Warning,
                                                                    contentDescription = null,
                                                                    tint = if (isPolicyCheck) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                                                    modifier = Modifier.size(16.dp)
                                                                )
                                                                Spacer(modifier = Modifier.width(8.dp))
                                                                Text(
                                                                    text = if (isPolicyCheck) "On-Device Safety Guardrails" else "Summarization Error",
                                                                    fontWeight = FontWeight.Bold,
                                                                    fontSize = 12.sp,
                                                                    color = if (isPolicyCheck) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                                                )
                                                            }
                                                            Spacer(modifier = Modifier.height(4.dp))
                                                            Text(
                                                                text = if (isPolicyCheck) {
                                                                    "This article touches upon sensitive topics that trigger Gemini Nano's built-in local safety policies. We have provided the standard article preview below."
                                                                } else {
                                                                    state.message
                                                                },
                                                                fontSize = 11.sp,
                                                                lineHeight = 15.sp,
                                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                                            )
                                                        }
                                                    }
                                                    
                                                    if (article != null && article.description.isNotBlank()) {
                                                        Text(
                                                            text = "Standard RSS Preview",
                                                            fontWeight = FontWeight.SemiBold,
                                                            fontSize = 12.sp,
                                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                                        )
                                                        
                                                        Surface(
                                                            shape = RoundedCornerShape(12.dp),
                                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f),
                                                            modifier = Modifier.fillMaxWidth()
                                                        ) {
                                                            Text(
                                                                text = android.text.Html.fromHtml(article.description, android.text.Html.FROM_HTML_MODE_LEGACY).toString().trim(),
                                                                fontSize = 12.sp,
                                                                lineHeight = 17.sp,
                                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                                                                modifier = Modifier.padding(12.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
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
                                val rawBody = translatedBody.ifEmpty { articleBody }
                                val bodyToShow = if (rawBody.length < 150 || rawBody.startsWith("Failed to load")) {
                                    if (translatedBody.isNotEmpty()) rawBody else (article.description.takeIf { it.isNotBlank() } ?: rawBody)
                                } else {
                                    rawBody
                                }
                                
                                val showSuggestionBanner = (translatedBody.isEmpty() && (articleBody.length < 150 || articleBody.startsWith("Failed to load")))

                                val paragraphs = bodyToShow.split("\n\n").filter { it.isNotBlank() }
                                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    if (translatedBody.isNotEmpty()) {
                                        Card(
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Translate,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "Automatically translated to your target language",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
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
                                            val currentFont = if (fontFamily == "serif") FontFamily(android.graphics.Typeface.SERIF) else FontFamily(android.graphics.Typeface.SANS_SERIF)
                                            Text(
                                                text = paragraph,
                                                fontSize = fontSize.sp,
                                                lineHeight = (fontSize * lineSpacing).sp,
                                                fontFamily = currentFont,
                                                color = textColor,
                                                fontWeight = weight
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        
                        val bottomSpacerHeight = if (hasActiveTrack) 200.dp else 120.dp
                        Spacer(modifier = Modifier.height(bottomSpacerHeight))
                    }
                }
            } else {
                // In-App WebView Layout
                var isWebViewLoading by remember { mutableStateOf(true) }
                var webViewError by remember { mutableStateOf<String?>(null) }
                val secureUrl = remember(url) {
                    UrlSecurityValidator.normalizeToHttps(url) ?: url
                }
                Box(modifier = Modifier.fillMaxSize()) {
                    if (webViewError != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = webViewError ?: "",
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        AndroidView(
                            factory = { ctx ->
                                WebView(ctx).apply {
                                    webViewClient = object : WebViewClient() {
                                        override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                            super.onPageStarted(view, url, favicon)
                                            isWebViewLoading = true
                                            webViewError = null
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
                                            webViewError = description ?: "Unable to load this article."
                                        }

                                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                            val targetUrl = request?.url?.toString() ?: return false
                                            val scheme = request.url?.scheme?.lowercase() ?: return false
                                            // Block non-http/https schemes (javascript:, file:, intent:, etc.)
                                            if (scheme != "http" && scheme != "https") {
                                                return true
                                            }
                                            // Let the WebView handle http/https navigation normally
                                            return false
                                        }
                                    }
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    settings.allowFileAccess = false
                                    settings.allowContentAccess = false
                                    loadUrl(secureUrl)
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    
                    if (isWebViewLoading) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter)
                                .height(3.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = Color.Transparent
                        )
                    }
                }
            }
        }

        // Floating Bottom Reaction Bar
        if (article != null) {
            val bottomPaddingVal = if (hasActiveTrack) 100.dp else 12.dp

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(bottom = bottomPaddingVal),
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
                        modifier = Modifier.clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (article != null) {
                                viewModel.toggleBookmark(article)
                            }
                        },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (article != null && article.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Save",
                            tint = if (article != null && article.isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (article != null && article.isBookmarked) "Saved" else "Save",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (article != null && article.isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Share Action
                    Row(
                        modifier = Modifier.clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val shareUrl = UrlSecurityValidator.normalizeToHttps(url) ?: url
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, article.title)
                                putExtra(Intent.EXTRA_TEXT, shareUrl)
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

                    val showTranslateOption = isAiEnabled && isTranslationEnabled && (articleLanguage == null || articleLanguage != targetLang)
                    if (showTranslateOption) {
                        val isTranslated = translatedTitle.isNotEmpty()
                        Row(
                            modifier = Modifier.clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (isTranslated) {
                                    viewModel.clearTranslation()
                                } else {
                                    val body = if (currentTab == "Web") "" else (articleBody.takeIf { it.isNotBlank() } ?: article.description)
                                    viewModel.translateArticleText(article.title, body)
                                }
                            },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isTranslationLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Translate,
                                    contentDescription = "Translate",
                                    tint = if (isTranslated) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isTranslated) "Original" else "Translate",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isTranslated) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}


