package uk.sume.streamfolio.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FolderSpecial
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uk.sume.streamfolio.data.model.Article
import uk.sume.streamfolio.data.model.BookmarkList
import uk.sume.streamfolio.ui.viewmodel.NewsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaveToListBottomSheet(
    article: Article,
    viewModel: NewsViewModel,
    onDismiss: () -> Unit
) {
    val allLists by viewModel.allBookmarkLists.collectAsState()
    val assignedListIdsFlow by remember(article.link) {
        derivedStateOf { viewModel.getListIdsForArticle(article.link) }
    }
    val assignedListIds by assignedListIdsFlow.collectAsState(initial = emptyList())

    val selectedListIds = remember { mutableStateListOf<Long>() }
    var isInitialized by remember { mutableStateOf(false) }

    LaunchedEffect(assignedListIds, allLists) {
        if (!isInitialized && assignedListIds.isNotEmpty()) {
            selectedListIds.clear()
            selectedListIds.addAll(assignedListIds)
            isInitialized = true
        } else if (!isInitialized && allLists.isNotEmpty()) {
            selectedListIds.clear()
            val lastSavedId = viewModel.prefs.lastSelectedBookmarkListId
            val defaultId = allLists.firstOrNull { it.isDefault }?.id ?: 1L
            val targetInitialId = if (lastSavedId != -1L && allLists.any { it.id == lastSavedId }) {
                lastSavedId
            } else {
                defaultId
            }
            selectedListIds.add(targetInitialId)
            isInitialized = true
        }
    }

    var showCreateField by remember { mutableStateOf(false) }
    var newListName by remember { mutableStateOf("") }

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
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.FolderSpecial,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (article.isBookmarked || assignedListIds.isNotEmpty()) "Manage Collections" else "Save to Collection",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = { animateDismiss() }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Scrollable List of Collections
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items = allLists, key = { it.id }) { list ->
                    val isChecked = selectedListIds.contains(list.id)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                if (isChecked) {
                                    selectedListIds.remove(list.id)
                                } else {
                                    selectedListIds.add(list.id)
                                }
                            },
                        color = if (isChecked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (list.isDefault) Icons.Outlined.FolderSpecial else Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = if (isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = list.name,
                                    fontSize = 15.sp,
                                    fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        selectedListIds.add(list.id)
                                    } else {
                                        selectedListIds.remove(list.id)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Create Inline Collection
            if (showCreateField) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newListName,
                        onValueChange = { newListName = it },
                        label = { Text("New List Name") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            if (newListName.isNotBlank()) {
                                viewModel.createBookmarkList(newListName.trim()) { newId ->
                                    if (!selectedListIds.contains(newId)) {
                                        selectedListIds.add(newId)
                                    }
                                }
                                newListName = ""
                                showCreateField = false
                            }
                        })
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (newListName.isNotBlank()) {
                                viewModel.createBookmarkList(newListName.trim()) { newId ->
                                    if (!selectedListIds.contains(newId)) {
                                        selectedListIds.add(newId)
                                    }
                                }
                                newListName = ""
                                showCreateField = false
                            }
                        },
                        enabled = newListName.isNotBlank(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Add")
                    }
                }
            } else {
                TextButton(
                    onClick = { showCreateField = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Create New Collection",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Done/Save Button
            Button(
                onClick = {
                    animateDismiss {
                        viewModel.saveArticleToLists(article, selectedListIds.toSet())
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = "Done",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
