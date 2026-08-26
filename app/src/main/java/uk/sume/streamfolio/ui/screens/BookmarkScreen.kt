package uk.sume.streamfolio.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.outlined.FolderSpecial
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uk.sume.streamfolio.ui.components.ConfirmationModalSheet
import androidx.navigation.NavController
import coil.compose.AsyncImage
import uk.sume.streamfolio.data.model.Article
import uk.sume.streamfolio.data.model.BookmarkList
import uk.sume.streamfolio.ui.components.EmptyState
import uk.sume.streamfolio.ui.components.OfflineIndicator
import uk.sume.streamfolio.ui.components.SaveToListBottomSheet
import uk.sume.streamfolio.ui.theme.getThemeBackgroundBrush
import uk.sume.streamfolio.ui.viewmodel.NewsViewModel
import uk.sume.streamfolio.util.NetworkMonitor
import java.net.URLEncoder

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun BookmarkScreen(
    navController: NavController,
    viewModel: NewsViewModel,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val context = LocalContext.current
    val bookmarkedArticles by viewModel.bookmarkedArticles.collectAsState()
    val allLists by viewModel.allBookmarkLists.collectAsState()
    val saveToListArticle by viewModel.articleForSaveToListSheet.collectAsState()

    val isOnline by NetworkMonitor.observe(context).collectAsState(initial = NetworkMonitor.isOnline(context))
    val isOffline = !isOnline

    val ttsPlaylist by viewModel.ttsPlaylist.collectAsState()
    val currentTtsIndex by viewModel.currentTtsArticleIndex.collectAsState()
    val showMiniPlayer = ttsPlaylist.isNotEmpty() && currentTtsIndex != -1 && currentTtsIndex < ttsPlaylist.size
    val emptyStateBottomPadding = if (showMiniPlayer) 160.dp else 96.dp

    val scrollState = rememberLazyListState()

    var isGridView by remember { mutableStateOf(viewModel.prefs.bookmarkListViewModeIsGrid) }
    var showCreateListModal by remember { mutableStateOf(false) }

    val bgBrush = getThemeBackgroundBrush()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBrush)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Header Section
            Column(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Saved",
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Your saved collections and reads",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // View Mode Switcher (Grid / List)
                        IconButton(
                            onClick = {
                                val newMode = !isGridView
                                isGridView = newMode
                                viewModel.prefs.bookmarkListViewModeIsGrid = newMode
                            },
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                                contentDescription = if (isGridView) "Switch to List View" else "Switch to Grid View",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Add Collection Button
                        IconButton(
                            onClick = { showCreateListModal = true },
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Create Collection",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            OfflineIndicator(isOffline = isOffline)

            // COLLECTIONS ROOT VIEW (GRID OR LIST MODE)
            if (bookmarkedArticles.isEmpty() && allLists.isEmpty()) {
                EmptyState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = emptyStateBottomPadding),
                    illustration = painterResource(id = uk.sume.streamfolio.R.drawable.otter_no_bookmarks),
                    title = "No saved bookmarks",
                    description = "Swipe right on articles in the Home feed to save them to collections."
                )
            } else if (isGridView) {
                // GRID VIEW
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // "All Saved" Card
                    item {
                        CollectionGridCard(
                            title = "All Saved",
                            count = bookmarkedArticles.size,
                            previewThumbnail = bookmarkedArticles.firstOrNull { !it.thumbnailUrl.isNullOrBlank() && it.thumbnailUrl != "failed" }?.thumbnailUrl,
                            isDefault = true,
                            onClick = { navController.navigate("bookmark_collection_detail/-1") }
                        )
                    }

                    // Custom Lists Cards
                    items(items = allLists, key = { it.id }) { list ->
                        val countFlow by remember(list.id) { derivedStateOf { viewModel.getArticleCountForList(list.id) } }
                        val articleCount by countFlow.collectAsState(initial = 0)
                        val thumbFlow by remember(list.id) { derivedStateOf { viewModel.getPreviewThumbnailForList(list.id) } }
                        val thumbUrl by thumbFlow.collectAsState(initial = null)

                        CollectionGridCard(
                            title = list.name,
                            count = articleCount,
                            previewThumbnail = thumbUrl,
                            isDefault = list.isDefault,
                            onClick = { navController.navigate("bookmark_collection_detail/${list.id}") }
                        )
                    }

                    // Add New Collection Card
                    item {
                        AddCollectionGridCard(onClick = { showCreateListModal = true })
                    }
                }
            } else {
                // LIST VIEW
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        CollectionListRow(
                            title = "All Saved Articles",
                            count = bookmarkedArticles.size,
                            previewThumbnail = bookmarkedArticles.firstOrNull { !it.thumbnailUrl.isNullOrBlank() && it.thumbnailUrl != "failed" }?.thumbnailUrl,
                            isDefault = true,
                            onClick = { navController.navigate("bookmark_collection_detail/-1") }
                        )
                    }

                    items(items = allLists, key = { it.id }) { list ->
                        val countFlow by remember(list.id) { derivedStateOf { viewModel.getArticleCountForList(list.id) } }
                        val articleCount by countFlow.collectAsState(initial = 0)
                        val thumbFlow by remember(list.id) { derivedStateOf { viewModel.getPreviewThumbnailForList(list.id) } }
                        val thumbUrl by thumbFlow.collectAsState(initial = null)

                        CollectionListRow(
                            title = list.name,
                            count = articleCount,
                            previewThumbnail = thumbUrl,
                            isDefault = list.isDefault,
                            onClick = { navController.navigate("bookmark_collection_detail/${list.id}") }
                        )
                    }

                    item {
                        AddCollectionListRow(onClick = { showCreateListModal = true })
                    }
                }
            }
        }

        // Save to List Bottom Sheet
        saveToListArticle?.let { art ->
            SaveToListBottomSheet(
                article = art,
                viewModel = viewModel,
                onDismiss = { viewModel.closeSaveToListSheet() }
            )
        }

        // Create Collection Modal Bottom Sheet
        if (showCreateListModal) {
            CreateCollectionModalSheet(
                onDismiss = { showCreateListModal = false },
                onCreate = { name ->
                    viewModel.createBookmarkList(name)
                    showCreateListModal = false
                }
            )
        }
    }
}

// COLLECTION DETAIL SCREEN (First-Class Route for Full System Predictive Back Support)
@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun BookmarkCollectionDetailScreen(
    navController: NavController,
    viewModel: NewsViewModel,
    listId: Long,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val context = LocalContext.current
    val allLists by viewModel.allBookmarkLists.collectAsState()
    val currentList = allLists.firstOrNull { it.id == listId }
    val listName = if (listId == -1L) "All Saved Articles" else (currentList?.name ?: "Collection")

    val articlesForListFlow = remember(listId) {
        if (listId == -1L) {
            viewModel.bookmarkedArticles
        } else {
            viewModel.getArticlesForBookmarkList(listId)
        }
    }
    val listArticles by articlesForListFlow.collectAsState(initial = emptyList())
    val saveToListArticle by viewModel.articleForSaveToListSheet.collectAsState()

    val ttsPlaylist by viewModel.ttsPlaylist.collectAsState()
    val currentTtsIndex by viewModel.currentTtsArticleIndex.collectAsState()
    val showMiniPlayer = ttsPlaylist.isNotEmpty() && currentTtsIndex != -1 && currentTtsIndex < ttsPlaylist.size
    val emptyStateBottomPadding = if (showMiniPlayer) 160.dp else 96.dp

    val scrollState = rememberLazyListState()
    var showDeleteListModal by remember { mutableStateOf(false) }

    val bgBrush = getThemeBackgroundBrush()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBrush)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { navController.popBackStack() }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = listName,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (listArticles.size == 1) "1 saved article" else "${listArticles.size} saved articles",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
                if (currentList != null && !currentList.isDefault) {
                    IconButton(
                        onClick = { showDeleteListModal = true },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Collection",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            if (listArticles.isEmpty()) {
                EmptyState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = emptyStateBottomPadding),
                    illustration = painterResource(id = uk.sume.streamfolio.R.drawable.otter_no_bookmarks),
                    title = "This collection is empty",
                    description = "Add articles to this collection when saving news."
                )
            } else {
                LazyColumn(
                    state = scrollState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    items(
                        items = listArticles,
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
                        val onBookmarkToggle = remember(listId) { { viewModel.toggleBookmark(articleState.value, listId) } }
                        val onPlayClick = remember(article.link) { { viewModel.speakArticle(article) } }
                        val onQueueClick = remember(article.link) {
                            {
                                viewModel.addToTtsPlaylist(article)
                                viewModel.showInfoToast("Added to audio playlist", Icons.Default.PlaylistAdd)
                            }
                        }

                        val onPlayNextClick = remember(article.link) {
                            {
                                viewModel.playNextInTtsPlaylist(article)
                                viewModel.showInfoToast("Added to play next", Icons.Default.QueueMusic)
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

        // Save to List Bottom Sheet
        saveToListArticle?.let { art ->
            SaveToListBottomSheet(
                article = art,
                viewModel = viewModel,
                onDismiss = { viewModel.closeSaveToListSheet() }
            )
        }

        val allLists by viewModel.allBookmarkLists.collectAsState()
        var showMoveDestinationPicker by remember { mutableStateOf(false) }

        // Delete Collection Modal Bottom Sheet
        if (showDeleteListModal && currentList != null) {
            DeleteCollectionModalSheet(
                listName = currentList.name,
                isEmpty = listArticles.isEmpty(),
                onDismiss = { showDeleteListModal = false },
                onConfirmMove = {
                    showDeleteListModal = false
                    showMoveDestinationPicker = true
                },
                onConfirmDeleteAll = {
                    viewModel.deleteBookmarkList(currentList.id, moveArticlesToDefault = false)
                    showDeleteListModal = false
                    navController.popBackStack()
                }
            )
        }

        // Destination Picker Sheet when deleting non-empty collection
        if (showMoveDestinationPicker && currentList != null) {
            MoveArticlesDestinationModalSheet(
                allLists = allLists,
                deletingListId = currentList.id,
                onDismiss = { showMoveDestinationPicker = false },
                onListSelected = { targetListId ->
                    viewModel.deleteBookmarkList(currentList.id, targetMoveListId = targetListId)
                    showMoveDestinationPicker = false
                    navController.popBackStack()
                }
            )
        }
    }
}

// GRID MODE COLLECTION CARD
@Composable
private fun CollectionGridCard(
    title: String,
    count: Int,
    previewThumbnail: String?,
    isDefault: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(145.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
        onClick = onClick
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!previewThumbnail.isNullOrBlank() && previewThumbnail != "failed") {
                AsyncImage(
                    model = previewThumbnail,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.25f), Color.Black.copy(alpha = 0.85f))
                            )
                        )
                )
            } else {
                // Uses the saturated primary/secondary brand colors, not their *Container
                // counterparts — this card puts a plain white icon/badge/text directly on top
                // (below), which needs a vivid background for contrast. primaryContainer /
                // secondaryContainer are intentionally pale tints (correct for e.g. the
                // save-to-collection sheet's selected-row highlight) and washed out to near
                // invisibility here against the light theme's equally pale page background.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            )
                        )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.22f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isDefault) Icons.Outlined.FolderSpecial else Icons.Default.Folder,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Black.copy(alpha = 0.45f)
                    ) {
                        Text(
                            text = "$count",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Column {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (count == 1) "1 item" else "$count items",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

// GRID MODE ADD COLLECTION CARD
@Composable
private fun AddCollectionGridCard(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(145.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "New Collection",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// LIST MODE COLLECTION ROW
@Composable
private fun CollectionListRow(
    title: String,
    count: Int,
    previewThumbnail: String?,
    isDefault: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                if (!previewThumbnail.isNullOrBlank() && previewThumbnail != "failed") {
                    AsyncImage(
                        model = previewThumbnail,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.2f))
                    )
                } else {
                    Icon(
                        imageVector = if (isDefault) Icons.Outlined.FolderSpecial else Icons.Default.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (count == 1) "1 item" else "$count items",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// LIST MODE ADD COLLECTION ROW
@Composable
private fun AddCollectionListRow(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = "Create New Collection",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// CREATE COLLECTION MODAL SHEET
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateCollectionModalSheet(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    fun animateDismiss(action: () -> Unit = {}) {
        coroutineScope.launch {
            sheetState.hide()
        }.invokeOnCompletion {
            action()
            onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FolderSpecial,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Create New Collection",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Organize your saved articles into custom lists for quick access.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Collection Name") },
                placeholder = { Text("e.g. Technology, Reading List") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("create_collection_name_field"),
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (name.isNotBlank()) {
                        animateDismiss { onCreate(name) }
                    }
                })
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { animateDismiss() },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
                ) {
                    Text("Cancel", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            animateDismiss { onCreate(name) }
                        }
                    },
                    enabled = name.isNotBlank(),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Create", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DeleteCollectionModalSheet(
    listName: String,
    isEmpty: Boolean = false,
    onDismiss: () -> Unit,
    onConfirmMove: () -> Unit,
    onConfirmDeleteAll: () -> Unit
) {
    if (isEmpty) {
        ConfirmationModalSheet(
            title = "Delete Collection",
            message = "Are you sure you want to delete \"$listName\"? This action cannot be undone.",
            icon = Icons.Default.Delete,
            primaryButtonText = "Delete Collection",
            primaryButtonColor = MaterialTheme.colorScheme.error,
            onPrimaryClick = { onConfirmDeleteAll() },
            secondaryButtonText = "Cancel",
            onDismiss = onDismiss
        )
    } else {
        ConfirmationModalSheet(
            title = "Delete Collection",
            message = "Are you sure you want to delete \"$listName\"? Choose what happens to the saved articles in this collection.",
            icon = Icons.Default.Delete,
            primaryButtonText = "Move Articles To...",
            primaryButtonColor = MaterialTheme.colorScheme.primary,
            onPrimaryClick = { onConfirmMove() },
            secondaryButtonText = "Remove Bookmarks",
            secondaryButtonColor = MaterialTheme.colorScheme.error,
            isSecondaryOutlined = true,
            onSecondaryClick = { onConfirmDeleteAll() },
            tertiaryButtonText = "Cancel",
            onTertiaryClick = { },
            onDismiss = onDismiss
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoveArticlesDestinationModalSheet(
    allLists: List<uk.sume.streamfolio.data.model.BookmarkList>,
    deletingListId: Long,
    onDismiss: () -> Unit,
    onListSelected: (targetListId: Long) -> Unit
) {
    val availableLists = remember(allLists, deletingListId) {
        allLists.filter { it.id != deletingListId }
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    fun animateDismiss(action: () -> Unit = {}) {
        coroutineScope.launch {
            sheetState.hide()
        }.invokeOnCompletion {
            action()
            onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Move Articles To...",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Select destination collection for saved articles",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(availableLists, key = { it.id }) { list ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .clickable {
                                animateDismiss { onListSelected(list.id) }
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (list.isDefault) Icons.Outlined.FolderSpecial else Icons.Default.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = list.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
