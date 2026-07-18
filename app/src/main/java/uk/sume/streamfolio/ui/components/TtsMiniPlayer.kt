package uk.sume.streamfolio.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Hearing
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import uk.sume.streamfolio.data.model.Article
import androidx.navigation.NavController
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import uk.sume.streamfolio.ui.viewmodel.NewsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TtsMiniPlayer(
    viewModel: NewsViewModel,
    modifier: Modifier = Modifier
) {
    val playlist by viewModel.ttsPlaylist.collectAsState()
    val currentIndex by viewModel.currentTtsArticleIndex.collectAsState()
    val isPlaying by viewModel.ttsHelper.isPlaying.collectAsState()
    val sleepTimerRemainingMillis by viewModel.sleepTimerRemainingMillis.collectAsState()

    if (playlist.isEmpty() || currentIndex == -1 || currentIndex >= playlist.size) {
        return
    }

    val article = playlist[currentIndex]
    val haptic = LocalHapticFeedback.current

    var showDismissConfirmDialog by remember { mutableStateOf(false) }
    var pendingDismissOffset by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val offsetY = remember { Animatable(0f) }
    val threshold = 150f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        if (dragAmount.y > 0 || offsetY.value > 0) {
                            change.consume()
                            coroutineScope.launch {
                                offsetY.snapTo((offsetY.value + dragAmount.y).coerceAtLeast(0f))
                            }
                        }
                    },
                    onDragEnd = {
                        coroutineScope.launch {
                            if (offsetY.value > threshold) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                pendingDismissOffset = true
                                showDismissConfirmDialog = true
                            } else {
                                offsetY.animateTo(0f, animationSpec = tween(150))
                            }
                        }
                    },
                    onDragCancel = {
                        coroutineScope.launch {
                            offsetY.animateTo(0f, animationSpec = tween(150))
                        }
                    }
                )
            }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .height(64.dp)
                .offset { IntOffset(0, offsetY.value.roundToInt()) }
                .graphicsLayer {
                    alpha = (1f - (offsetY.value / 300f)).coerceIn(0f, 1f)
                }
                .shadow(16.dp, RoundedCornerShape(32.dp))
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(32.dp)
                )
                .clickable { viewModel.setShowLyricsVisualizer(true) }, // Primary CTA opens visualizer/full-player
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Article Thumbnail / Watermark
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                ) {
                    if (article.thumbnailUrl != null && article.thumbnailUrl != "failed" && !article.thumbnailUrl.contains("google")) {
                        AsyncImage(
                            model = article.thumbnailUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.Hearing,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(20.dp)
                                .align(Alignment.Center)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Title & Source Info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = article.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = article.sourceName,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        sleepTimerRemainingMillis?.let { remaining ->
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.Bedtime,
                                contentDescription = "Sleep timer active",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = formatSleepTimerLabel(remaining),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1
                            )
                        }
                    }
                }

                // Play / Pause Button
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.playOrPausePlaylist()
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Skip Next Button
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.advanceTtsPlaylist()
                    },
                    enabled = currentIndex < playlist.size - 1,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = if (currentIndex < playlist.size - 1) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        if (showDismissConfirmDialog) {
            ModalBottomSheet(
                onDismissRequest = {
                    showDismissConfirmDialog = false
                    pendingDismissOffset = false
                    coroutineScope.launch {
                        offsetY.animateTo(0f, animationSpec = tween(150))
                    }
                },
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
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Stop audio playback?",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Dismissing the player will stop the current article and clear the audio queue.",
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
                            onClick = {
                                showDismissConfirmDialog = false
                                pendingDismissOffset = false
                                coroutineScope.launch {
                                    offsetY.animateTo(0f, animationSpec = tween(150))
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                        ) {
                            Text("Keep Playing", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                showDismissConfirmDialog = false
                                coroutineScope.launch {
                                    if (pendingDismissOffset) {
                                        offsetY.animateTo(300f, animationSpec = tween(200))
                                    }
                                    viewModel.clearTtsPlaylist()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Stop", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TtsLyricsVisualizer(
    viewModel: NewsViewModel,
    navController: NavController,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val playlist by viewModel.ttsPlaylist.collectAsState()
    val currentIndex by viewModel.currentTtsArticleIndex.collectAsState()
    val isPlaying by viewModel.ttsHelper.isPlaying.collectAsState()
    val articleBody by viewModel.ttsArticleBody.collectAsState()
    val ttsParagraphIndex by viewModel.ttsHelper.currentParagraphIndex.collectAsState()
    val currentWordRange by viewModel.currentWordRange.collectAsState()
    val currentSpeed by viewModel.ttsSpeechRate.collectAsState()
    val sleepTimerRemainingMillis by viewModel.sleepTimerRemainingMillis.collectAsState()

    if (playlist.isEmpty() || currentIndex == -1 || currentIndex >= playlist.size) {
        onDismiss()
        return
    }

    val article = playlist[currentIndex]
    val paragraphs = remember(articleBody) { articleBody.split("\n\n").filter { it.isNotBlank() } }
    val lazyListState = rememberLazyListState()

    var showQueueInsideVisualizer by remember { mutableStateOf(false) }
    var showTranscriptView by remember { mutableStateOf(true) }
    var isSpeedMenuExpanded by remember { mutableStateOf(false) }
    var isSleepMenuExpanded by remember { mutableStateOf(false) }

    // Auto-scroll centering
    LaunchedEffect(ttsParagraphIndex, showQueueInsideVisualizer) {
        if (!showQueueInsideVisualizer && ttsParagraphIndex >= 0 && ttsParagraphIndex < paragraphs.size) {
            lazyListState.animateScrollToItem(ttsParagraphIndex, scrollOffset = -250)
        }
    }

    BackHandler(enabled = true) {
        onDismiss()
    }

    val isDark = isSystemInDarkTheme()
    val visualizerBg = if (isDark) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.surface
    val activeTextColor = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
    val radialSpotColor = MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.15f else 0.08f)
    val controlBtnBg = if (isDark) Color.White.copy(alpha = 0.08f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
    val iconTint = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
    val queueActiveBg = if (isDark) Color.White.copy(alpha = 0.08f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
    val queueInactiveBg = if (isDark) Color.White.copy(alpha = 0.03f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f)
    val bottomDockBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)
    val dockIconTint = if (isDark) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(visualizerBg)
    ) {
        // Blur light ambient spot
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            radialSpotColor,
                            Color.Transparent
                        ),
                        center = Offset(200f, 300f),
                        radius = 1000f
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 24.dp)
        ) {
            // Header Area: Symmetric and Polished
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left close button (chevron down matches up/down transition)
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDismiss()
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .background(controlBtnBg, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Close",
                        modifier = Modifier.size(24.dp),
                        tint = iconTint
                    )
                }
                
                // Centered article metadata: Clickable CTA to Reader
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            onDismiss()
                            val encodedUrl = java.net.URLEncoder.encode(article.link, "UTF-8")
                            navController.navigate("detail_screen/$encodedUrl")
                        }
                        .padding(horizontal = 12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = article.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = activeTextColor,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = "Open Reader",
                            tint = activeTextColor.copy(alpha = 0.6f),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = article.sourceName + " • Tap to Read",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                IconButton(
                    onClick = { showQueueInsideVisualizer = !showQueueInsideVisualizer },
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            if (showQueueInsideVisualizer) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            else controlBtnBg,
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = if (showQueueInsideVisualizer) Icons.Default.MenuBook else Icons.Default.QueueMusic,
                        contentDescription = "Toggle Queue",
                        tint = if (showQueueInsideVisualizer) MaterialTheme.colorScheme.primary else iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Immersive Crossfading Body Content
            AnimatedContent(
                targetState = Pair(showQueueInsideVisualizer, showTranscriptView),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                },
                label = "VisualizerContent"
            ) { (showQueue, showTranscript) ->
                if (showQueue) {
                    // Apple Music Drag to Reorder Queue List
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 16.dp)
                    ) {
                        Text(
                            text = "Hold & drag to reorder queue",
                            fontSize = 12.sp,
                            color = activeTextColor.copy(alpha = 0.4f),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        var draggingIndex by remember { mutableStateOf<Int?>(null) }
                        var dragOffset by remember { mutableStateOf(0f) }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentPadding = PaddingValues(bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            itemsIndexed(playlist, key = { _, item -> item.link }) { index, queuedArticle ->
                                val isActive = index == currentIndex
                                val itemBg = if (isActive) queueActiveBg else queueInactiveBg
                                val activeColor = if (isActive) MaterialTheme.colorScheme.primary else activeTextColor

                                var itemHeightPx by remember { mutableStateOf(0) }
                                
                                // Stale-proof state lookups for uninterrupted drag gestures
                                val currentIndexState = rememberUpdatedState(index)
                                val playlistSizeState = rememberUpdatedState(playlist.size)
                                val itemHeightPxState = rememberUpdatedState(itemHeightPx)

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .onGloballyPositioned { coordinates ->
                                            itemHeightPx = coordinates.size.height
                                        }
                                        .graphicsLayer {
                                            if (draggingIndex == index) {
                                                translationY = dragOffset
                                                alpha = 0.85f
                                                shadowElevation = 8f
                                            }
                                        }
                                        .pointerInput(queuedArticle.link) {
                                            detectDragGesturesAfterLongPress(
                                                onDragStart = {
                                                    draggingIndex = currentIndexState.value
                                                    dragOffset = 0f
                                                },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    dragOffset += dragAmount.y
                                                    val currentIdx = currentIndexState.value
                                                    val height = itemHeightPxState.value
                                                    if (height == 0) return@detectDragGesturesAfterLongPress
                                                    val threshold = height / 2f
                                                    val totalSize = playlistSizeState.value

                                                    if (dragOffset > threshold && currentIdx < totalSize - 1) {
                                                        viewModel.moveTtsPlaylistItem(currentIdx, currentIdx + 1)
                                                        dragOffset -= height
                                                        draggingIndex = currentIdx + 1
                                                    } else if (dragOffset < -threshold && currentIdx > 0) {
                                                        viewModel.moveTtsPlaylistItem(currentIdx, currentIdx - 1)
                                                        dragOffset += height
                                                        draggingIndex = currentIdx - 1
                                                    }
                                                },
                                                onDragEnd = {
                                                    draggingIndex = null
                                                    dragOffset = 0f
                                                },
                                                onDragCancel = {
                                                    draggingIndex = null
                                                    dragOffset = 0f
                                                }
                                            )
                                        },
                                    colors = CardDefaults.cardColors(containerColor = itemBg),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Left Card Thumbnail
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(activeTextColor.copy(alpha = 0.05f))
                                        ) {
                                            if (queuedArticle.thumbnailUrl != null && queuedArticle.thumbnailUrl != "failed" && !queuedArticle.thumbnailUrl.contains("google")) {
                                                AsyncImage(
                                                    model = queuedArticle.thumbnailUrl,
                                                    contentDescription = null,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Outlined.Hearing,
                                                    contentDescription = null,
                                                    tint = activeTextColor.copy(alpha = 0.3f),
                                                    modifier = Modifier.size(20.dp).align(Alignment.Center)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Row(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { viewModel.playTtsPlaylist(index) },
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = queuedArticle.title,
                                                    fontSize = 13.sp,
                                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis,
                                                    color = activeColor
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = queuedArticle.sourceName,
                                                    fontSize = 11.sp,
                                                    color = activeTextColor.copy(alpha = 0.5f)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        // Grab Handle
                                        Icon(
                                            imageVector = Icons.Default.Reorder,
                                            contentDescription = "Drag to Reorder",
                                            tint = activeTextColor.copy(alpha = 0.3f),
                                            modifier = Modifier.size(24.dp)
                                        )

                                        Spacer(modifier = Modifier.width(12.dp))

                                        IconButton(
                                            onClick = { viewModel.removeFromTtsPlaylist(queuedArticle) },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Remove",
                                                tint = activeTextColor.copy(alpha = 0.4f),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (showTranscript) {
                    // Scrolling Lyrics List
                    if (paragraphs.isEmpty() || paragraphs[0].startsWith("Failed to load") || paragraphs[0].startsWith("Unable to parse")) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (paragraphs.isEmpty()) "Parsing article body..." else "This publisher restricts word-by-word viewing.",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                color = activeTextColor.copy(alpha = 0.5f)
                            )
                        }
                    } else {
                        LazyColumn(
                            state = lazyListState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = 100.dp, bottom = 220.dp),
                            verticalArrangement = Arrangement.spacedBy(28.dp)
                        ) {
                            itemsIndexed(paragraphs) { index, paragraph ->
                                val isActive = index == ttsParagraphIndex
                                val alpha = if (isActive) 1f else 0.35f
                                val size = if (isActive) 23.sp else 19.sp
                                val weight = if (isActive) FontWeight.Bold else FontWeight.Medium
                                val color = if (isActive) activeTextColor else activeTextColor.copy(alpha = alpha)
                                val highlightColor = MaterialTheme.colorScheme.primary

                                val annotatedText = remember(paragraph, isActive, currentWordRange, highlightColor) {
                                    buildAnnotatedString {
                                        if (isActive && currentWordRange != null) {
                                            val (start, end) = currentWordRange!!
                                            if (start >= 0 && end <= paragraph.length && start <= end) {
                                                append(paragraph.substring(0, start))
                                                withStyle(
                                                    SpanStyle(
                                                        color = highlightColor,
                                                        fontWeight = FontWeight.Black
                                                    )
                                                ) {
                                                    append(paragraph.substring(start, end))
                                                }
                                                append(paragraph.substring(end))
                                            } else {
                                                append(paragraph)
                                            }
                                        } else {
                                            append(paragraph)
                                        }
                                    }
                                }

                                Text(
                                    text = annotatedText,
                                    fontSize = size,
                                    lineHeight = (size.value * 1.5).sp,
                                    fontWeight = weight,
                                    color = color,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.seekTtsToParagraph(index) }
                                        .padding(vertical = 4.dp),
                                    textAlign = TextAlign.Start
                                )
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 32.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(240.dp)
                                    .clip(RoundedCornerShape(28.dp))
                                    .background(controlBtnBg)
                            ) {
                                if (article.thumbnailUrl != null && article.thumbnailUrl != "failed" && !article.thumbnailUrl.contains("google")) {
                                    AsyncImage(
                                        model = article.thumbnailUrl,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Outlined.Hearing,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .size(64.dp)
                                            .align(Alignment.Center)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Text(
                                text = article.title,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                color = activeTextColor
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = article.sourceName,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = activeTextColor.copy(alpha = 0.55f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Timeline Progress & Audio controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
            ) {
                // Progress timeline
                if (paragraphs.isNotEmpty()) {
                    val progressValue = (ttsParagraphIndex.toFloat() + 1) / paragraphs.size.coerceAtLeast(1)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LinearProgressIndicator(
                            progress = { progressValue },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = activeTextColor.copy(alpha = 0.1f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Article ${currentIndex + 1} of ${playlist.size}",
                                fontSize = 11.sp,
                                color = activeTextColor.copy(alpha = 0.5f),
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Paragraph ${ttsParagraphIndex + 1} of ${paragraphs.size}",
                                fontSize = 11.sp,
                                color = activeTextColor.copy(alpha = 0.5f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Player Control row: Premium Capsule Shape
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .width(300.dp)
                            .height(72.dp)
                            .shadow(16.dp, CircleShape)
                            .border(
                                1.dp,
                                if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f),
                                CircleShape
                            ),
                        shape = CircleShape,
                        colors = CardDefaults.cardColors(
                            containerColor = bottomDockBg
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Previous Button
                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.playPreviousTtsArticle()
                                },
                                enabled = currentIndex > 0,
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(
                                        if (currentIndex > 0) dockIconTint.copy(alpha = 0.04f) else Color.Transparent,
                                        CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipPrevious,
                                    contentDescription = "Previous Article",
                                    tint = if (currentIndex > 0) dockIconTint else dockIconTint.copy(alpha = 0.2f),
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            // Play / Pause Button
                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.playOrPausePlaylist()
                                },
                                modifier = Modifier
                                    .size(54.dp)
                                    .shadow(4.dp, CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.secondary
                                            )
                                        ),
                                        CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            // Next Button
                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.advanceTtsPlaylist()
                                },
                                enabled = currentIndex < playlist.size - 1,
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(
                                        if (currentIndex < playlist.size - 1) dockIconTint.copy(alpha = 0.04f) else Color.Transparent,
                                        CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipNext,
                                    contentDescription = "Next Article",
                                    tint = if (currentIndex < playlist.size - 1) dockIconTint else dockIconTint.copy(alpha = 0.2f),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Top
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        BottomUtilityIconButton(
                            icon = Icons.Default.Speed,
                            contentDescription = "Playback speed ${currentSpeed}x",
                            isActive = currentSpeed != 1.0f,
                            tint = if (currentSpeed != 1.0f) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                dockIconTint.copy(alpha = 0.82f)
                            },
                            onClick = { isSpeedMenuExpanded = true }
                        )

                        DropdownMenu(
                            expanded = isSpeedMenuExpanded,
                            onDismissRequest = { isSpeedMenuExpanded = false },
                            modifier = Modifier.width(260.dp),
                            shape = RoundedCornerShape(20.dp),
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 8.dp,
                            shadowElevation = 16.dp,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                            )
                        ) {
                            UtilityMenuHeader(
                                title = "Playback speed",
                                subtitle = "Current: ${formatSpeedOptionLabel(currentSpeed)}"
                            )
                            HorizontalDivider()

                            val speeds = listOf(
                                SpeedOption(0.75f, "0.75x", "Relaxed"),
                                SpeedOption(1.0f, "1.0x", "Normal"),
                                SpeedOption(1.25f, "1.25x", "Steady"),
                                SpeedOption(1.5f, "1.5x", "Faster"),
                                SpeedOption(1.75f, "1.75x", "Quick"),
                                SpeedOption(2.0f, "2.0x", "Fastest")
                            )
                            speeds.forEach { speedOption ->
                                UtilityMenuItem(
                                    title = speedOption.label,
                                    subtitle = speedOption.subtitle,
                                    leadingIcon = Icons.Default.Speed,
                                    selected = speedOption.rate == currentSpeed,
                                    selectedColor = MaterialTheme.colorScheme.primary,
                                    onClick = {
                                        viewModel.setTtsSpeechRate(speedOption.rate)
                                        isSpeedMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Box(contentAlignment = Alignment.Center) {
                        BottomUtilityIconButton(
                            icon = Icons.Default.Bedtime,
                            contentDescription = sleepTimerRemainingMillis?.let {
                                "Sleep timer active, ${formatSleepTimerLabel(it)}"
                            } ?: "Sleep timer",
                            isActive = sleepTimerRemainingMillis != null,
                            tint = if (sleepTimerRemainingMillis != null) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                dockIconTint.copy(alpha = 0.82f)
                            },
                            onClick = { isSleepMenuExpanded = true }
                        )

                        DropdownMenu(
                            expanded = isSleepMenuExpanded,
                            onDismissRequest = { isSleepMenuExpanded = false },
                            modifier = Modifier.width(280.dp),
                            shape = RoundedCornerShape(20.dp),
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 8.dp,
                            shadowElevation = 16.dp,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                            )
                        ) {
                            UtilityMenuHeader(
                                title = "Sleep timer",
                                subtitle = sleepTimerRemainingMillis?.let(::formatSleepTimerLabel)
                                    ?: "Stops playback automatically"
                            )
                            HorizontalDivider()

                            listOf(15, 30, 45, 60).forEach { minutes ->
                                UtilityMenuItem(
                                    title = "$minutes min",
                                    subtitle = "Stop playback after $minutes minutes",
                                    leadingIcon = Icons.Default.Bedtime,
                                    selected = false,
                                    selectedColor = MaterialTheme.colorScheme.primary,
                                    onClick = {
                                        viewModel.setSleepTimer(minutes)
                                        isSleepMenuExpanded = false
                                    }
                                )
                            }
                            if (sleepTimerRemainingMillis != null) {
                                HorizontalDivider()
                                UtilityMenuItem(
                                    title = "Turn off timer",
                                    subtitle = "Continue playback without auto-stop",
                                    leadingIcon = Icons.Default.Close,
                                    selected = false,
                                    selectedColor = MaterialTheme.colorScheme.error,
                                    onClick = {
                                        viewModel.cancelSleepTimer()
                                        isSleepMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    BottomUtilityIconButton(
                        icon = Icons.Default.FormatQuote,
                        contentDescription = if (showTranscriptView) {
                            "Show thumbnail"
                        } else {
                            "Show transcript"
                        },
                        tint = if (showTranscriptView) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            dockIconTint.copy(alpha = 0.82f)
                        },
                        onClick = {
                            showTranscriptView = !showTranscriptView
                            showQueueInsideVisualizer = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomUtilityIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    tint: Color,
    isActive: Boolean = false,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
            .padding(vertical = 6.dp, horizontal = 18.dp)
            .background(
                color = if (isActive) tint.copy(alpha = 0.14f) else Color.Transparent,
                shape = RoundedCornerShape(18.dp)
            )
            .padding(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun UtilityMenuHeader(
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = subtitle,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun UtilityMenuItem(
    title: String,
    subtitle: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    DropdownMenuItem(
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (selected) selectedColor.copy(alpha = 0.10f) else Color.Transparent,
                RoundedCornerShape(14.dp)
            ),
        text = {
            Column {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) selectedColor else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f)
                )
            }
        },
        leadingIcon = {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = if (selected) selectedColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
            )
        },
        trailingIcon = {
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = selectedColor
                )
            }
        },
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        }
    )
}

private data class SpeedOption(
    val rate: Float,
    val label: String,
    val subtitle: String
)

private fun formatSleepTimerLabel(remainingMillis: Long): String {
    val totalSeconds = (remainingMillis / 1_000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return if (minutes > 0L) {
        "${minutes}m left"
    } else {
        "${seconds}s left"
    }
}

private fun formatSpeedOptionLabel(rate: Float): String {
    return if (rate == 1.0f) "1.0x (Normal)" else "${rate}x"
}
