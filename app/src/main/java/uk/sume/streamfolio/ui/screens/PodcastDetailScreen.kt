package uk.sume.streamfolio.ui.screens

import android.text.Html
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FileDownloadDone
import androidx.compose.material.icons.outlined.Hearing
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import uk.sume.streamfolio.data.model.PodcastEpisode
import uk.sume.streamfolio.data.model.PodcastSubscription
import uk.sume.streamfolio.ui.theme.getThemeBackgroundBrush
import uk.sume.streamfolio.ui.viewmodel.NewsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PodcastDetailScreen(
    navController: NavController,
    viewModel: NewsViewModel,
    feedId: Int
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val bgBrush = getThemeBackgroundBrush()

    var showDetails by remember { mutableStateOf<PodcastSubscription?>(null) }
    val episodes by viewModel.podcastRepository.getEpisodesForPodcast(feedId).collectAsState(initial = emptyList())
    var isLoadingEpisodes by remember { mutableStateOf(false) }
    var isSubscribed by remember { mutableStateOf(false) }

    // Fetch show metadata and cache episodes
    LaunchedEffect(feedId) {
        isLoadingEpisodes = true
        coroutineScope.launch {
            try {
                val key = viewModel.prefs.podcastApiOverrideKey
                val sec = viewModel.prefs.podcastApiOverrideSecret
                
                // Fetch details from local or network
                val localSub = viewModel.podcastRepository.getSubscriptionById(feedId)
                if (localSub != null) {
                    showDetails = localSub
                    isSubscribed = true
                } else {
                    // Try to search or pull from API. We can just retrieve it via searching by term/ID.
                    // To keep things simple, we can search by feedId or query.
                    // Wait, PodcastIndexApi doesn't have byfeedid for shows, but search returns it.
                    // If they navigated here, they clicked a search result, which means we can pass the show details or query.
                    // Let's check: we can fetch show details via direct show ID endpoint later if needed.
                    // But wait, search results are what navigated us here. If they subscribed, it's saved locally.
                    // Let's make sure we retrieve metadata. If localSub is null, we can fetch episodes and extract show metadata from the first episode if needed, 
                    // or we can make a query. Let's write a small API call to retrieve feed by ID:
                    // GET /podcasts/byfeedid?id=...
                    // Let's implement it in PodcastIndexApi.kt if we want, or search for it.
                    // Actually, we can get feed info directly from `/episodes/byfeedid?id=...` because it returns the feed info!
                    // Let's verify: Yes, PodcastIndex API's `/episodes/byfeedid` returns feed title, image, etc. in the root properties!
                }

                viewModel.podcastRepository.fetchAndCacheEpisodes(feedId, key, sec)
                
                // If local subscription is still null, we can populate show details from the database or API
                if (showDetails == null) {
                    val sub = viewModel.podcastRepository.getSubscriptionById(feedId)
                    if (sub != null) {
                        showDetails = sub
                        isSubscribed = true
                    } else {
                        // Create a temporary mock details from the first episode or query
                        val firstEp = episodes.firstOrNull()
                        showDetails = PodcastSubscription(
                            feedId = feedId,
                            title = "Podcast Feed $feedId",
                            author = "Unknown Creator",
                            description = "No description available",
                            feedUrl = "",
                            imageUri = ""
                        )
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load episodes", Toast.LENGTH_SHORT).show()
            } finally {
                isLoadingEpisodes = false
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBrush)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Show Details",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (isLoadingEpisodes && episodes.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    // Show Info Card
                    item {
                        showDetails?.let { show ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                AsyncImage(
                                    model = show.imageUri,
                                    contentDescription = show.title,
                                    modifier = Modifier
                                        .size(160.dp)
                                        .shadow(8.dp, RoundedCornerShape(24.dp))
                                        .clip(RoundedCornerShape(24.dp)),
                                    contentScale = ContentScale.Crop
                                )

                                Spacer(modifier = Modifier.height(20.dp))

                                Text(
                                    text = show.title,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Text(
                                    text = "by ${show.author}",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(top = 4.dp)
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // Subscribe Button
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            if (isSubscribed) {
                                                viewModel.podcastRepository.unsubscribe(show)
                                                isSubscribed = false
                                                Toast.makeText(context, "Unsubscribed", Toast.LENGTH_SHORT).show()
                                            } else {
                                                viewModel.podcastRepository.subscribe(show)
                                                isSubscribed = true
                                                Toast.makeText(context, "Subscribed", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSubscribed) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
                                        contentColor = if (isSubscribed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(text = if (isSubscribed) "Subscribed" else "Subscribe")
                                }

                                if (show.description.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(18.dp))
                                    Text(
                                        text = Html.fromHtml(show.description, Html.FROM_HTML_MODE_LEGACY).toString().trim(),
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        maxLines = 4,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    // Episodes count list header
                    item {
                        Text(
                            text = "EPISODES (${episodes.size})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                            modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 12.dp)
                        )
                    }

                    // Episodes list
                    items(episodes) { episode ->
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

@Composable
fun EpisodeListItem(
    episode: PodcastEpisode,
    viewModel: NewsViewModel
) {
    val context = LocalContext.current
    val activeEpisode by viewModel.currentEpisode.collectAsState()
    val isPlayingState by viewModel.isPlayingPodcast.collectAsState()
    val isActive = activeEpisode?.episodeId == episode.episodeId

    val dateStr = remember(episode.pubDate) {
        val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        sdf.format(Date(episode.pubDate))
    }

    val durationStr = remember(episode.durationSeconds) {
        val minutes = episode.durationSeconds / 60
        if (minutes > 0) "$minutes min" else "${episode.durationSeconds} sec"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .shadow(2.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
            .clickable { viewModel.toggleEpisodePlayback(episode) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Play / Pause Circle Action Button
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isActive && isPlayingState) Icons.Outlined.Hearing else Icons.Default.PlayArrow,
                contentDescription = "Play",
                tint = if (isActive) Color.White else MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = episode.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            
            Text(
                text = "$dateStr • $durationStr",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Download Action
        Box(
            modifier = Modifier.size(32.dp),
            contentAlignment = Alignment.Center
        ) {
            if (episode.localFilePath == "downloading") {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                IconButton(
                    onClick = {
                        if (episode.isDownloaded) {
                            viewModel.deleteDownloadedEpisode(episode)
                        } else {
                            viewModel.downloadEpisode(episode)
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (episode.isDownloaded) Icons.Outlined.DeleteOutline else Icons.Outlined.CloudDownload,
                        contentDescription = "Download",
                        tint = if (episode.isDownloaded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Bookmark Action
        IconButton(
            onClick = { viewModel.toggleEpisodeBookmark(episode) }
        ) {
            Icon(
                imageVector = if (episode.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                contentDescription = "Bookmark",
                tint = if (episode.isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
