package uk.sume.streamfolio.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import uk.sume.streamfolio.data.model.CustomFeed
import uk.sume.streamfolio.ui.components.CardDivider
import uk.sume.streamfolio.ui.components.ConfirmationModalSheet
import uk.sume.streamfolio.ui.components.InfoNote
import uk.sume.streamfolio.ui.components.SectionLabel
import uk.sume.streamfolio.ui.components.SettingsCard
import uk.sume.streamfolio.ui.components.SettingsRow
import uk.sume.streamfolio.ui.components.SubPageTopBar
import uk.sume.streamfolio.ui.theme.getThemeBackgroundBrush
import uk.sume.streamfolio.ui.viewmodel.NewsViewModel
import uk.sume.streamfolio.util.OpmlFeed
import uk.sume.streamfolio.util.OpmlHelper
import uk.sume.streamfolio.util.UrlSecurityValidator
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBackupScreen(navController: NavController, viewModel: NewsViewModel) {
    val customFeeds by viewModel.customFeeds.collectAsState()
    val context = LocalContext.current
    val bgBrush = getThemeBackgroundBrush()
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    var parsedFeedsToImport by remember { mutableStateOf<List<OpmlFeed>?>(null) }
    var fullBackupJsonToRestore by remember { mutableStateOf<String?>(null) }

    val opmlPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val parsed = OpmlHelper.parseOpml(inputStream)
                    if (parsed.isNotEmpty()) {
                        parsedFeedsToImport = parsed
                    } else {
                        Toast.makeText(context, "No RSS feeds found in this OPML file.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to parse OPML: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val backupPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val jsonText = inputStream.bufferedReader().use { it.readText() }
                    if (jsonText.contains("custom_feeds") && jsonText.contains("preferences")) {
                        fullBackupJsonToRestore = jsonText
                    } else {
                        scope.launch {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            kotlinx.coroutines.delay(200)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        Toast.makeText(context, "Invalid backup file.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                scope.launch {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    kotlinx.coroutines.delay(200)
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                Toast.makeText(context, "Failed to read backup: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun exportSubscriptions(feeds: List<CustomFeed>) {
        if (feeds.isEmpty()) {
            scope.launch {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                kotlinx.coroutines.delay(200)
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            Toast.makeText(context, "You have no custom feeds to export.", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val xmlContent = OpmlHelper.exportFeedsToOpml(feeds)
            val tempFile = File(context.cacheDir, "streamfolio_subscriptions.opml")
            tempFile.writeText(xmlContent)
            
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, tempFile)
            
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/xml"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            context.startActivity(Intent.createChooser(shareIntent, "Export OPML Subscriptions"))
        } catch (e: Exception) {
            scope.launch {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                kotlinx.coroutines.delay(200)
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            Toast.makeText(context, "Failed to export OPML: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    fun exportFullBackup(backupJson: String) {
        try {
            val tempFile = File(context.cacheDir, "streamfolio_backup.json")
            tempFile.writeText(backupJson)
            
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, tempFile)
            
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            context.startActivity(Intent.createChooser(shareIntent, "Export StreamFolio Backup"))
        } catch (e: Exception) {
            scope.launch {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                kotlinx.coroutines.delay(200)
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            Toast.makeText(context, "Failed to export backup: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    // OPML Import Confirmation Bottom Sheet
    if (parsedFeedsToImport != null) {
        val feeds = remember(parsedFeedsToImport, customFeeds) {
            val existingUrls = customFeeds.map { it.url.trim().lowercase() }.toSet()
            parsedFeedsToImport!!.mapNotNull { opmlFeed ->
                UrlSecurityValidator.sanitizeUrl(opmlFeed.xmlUrl, requireHttps = true)?.let { safeUrl ->
                    if (existingUrls.contains(safeUrl.lowercase())) null else opmlFeed.copy(xmlUrl = safeUrl)
                }
            }
        }

        if (feeds.isEmpty()) {
            LaunchedEffect(Unit) {
                Toast.makeText(context, "All feeds in this OPML are already imported!", Toast.LENGTH_LONG).show()
                parsedFeedsToImport = null
            }
        } else {
            val uniqueCategories = remember(feeds) { feeds.map { it.category }.distinct() }
            val categoryMapping = remember(feeds) {
                mutableStateMapOf<String, String>().apply {
                    uniqueCategories.forEach { put(it, it) }
                }
            }
            val selectedCategories = remember(feeds) {
                mutableStateOf(uniqueCategories.toSet())
            }

            ModalBottomSheet(
                onDismissRequest = { parsedFeedsToImport = null },
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
                    Text(
                        text = "Import Feeds (${feeds.size} found)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Map OPML folders to StreamFolio tabs, or uncheck categories you don't want to import.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        uniqueCategories.forEach { originalCategory ->
                            val mappedVal = categoryMapping[originalCategory] ?: originalCategory
                            val isChecked = selectedCategories.value.contains(originalCategory)
                            val categoryFeeds = remember(feeds) { feeds.filter { it.category == originalCategory } }
                            val feedCount = categoryFeeds.size
                            
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                ),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Checkbox(
                                                checked = isChecked,
                                                onCheckedChange = { checked ->
                                                    if (checked) {
                                                        selectedCategories.value = selectedCategories.value + originalCategory
                                                    } else {
                                                        selectedCategories.value = selectedCategories.value - originalCategory
                                                    }
                                                },
                                                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = originalCategory,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Text(
                                            text = "$feedCount feed${if (feedCount > 1) "s" else ""}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    
                                    if (isChecked) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Column(
                                            modifier = Modifier.padding(start = 12.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "Feeds to import:",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                            )
                                            categoryFeeds.forEach { f ->
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(5.dp)
                                                            .clip(CircleShape)
                                                            .background(MaterialTheme.colorScheme.primary)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = f.title,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        OutlinedTextField(
                                            value = mappedVal,
                                            onValueChange = { newVal ->
                                                categoryMapping[originalCategory] = newVal
                                            },
                                            label = { Text("StreamFolio Category Tab Name") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(10.dp),
                                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { parsedFeedsToImport = null },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                viewModel.importCustomRssFeeds(
                                    feeds = feeds,
                                    categoryMapping = categoryMapping.toMap(),
                                    selectedCategories = selectedCategories.value
                                )
                                parsedFeedsToImport = null
                                Toast.makeText(context, "Feeds imported successfully!", Toast.LENGTH_SHORT).show()
                            },
                            enabled = selectedCategories.value.isNotEmpty(),
                            modifier = Modifier.weight(1.5f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Import Selected", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Settings Restore Confirmation Dialog
    if (fullBackupJsonToRestore != null) {
        val backupJson = fullBackupJsonToRestore!!
        ConfirmationModalSheet(
            title = "Restore Settings",
            message = "This will overwrite all of your current custom RSS feeds, swipe action choices, categories, and translation preferences. Are you sure you want to proceed?",
            icon = Icons.Default.Warning,
            primaryButtonText = "Restore Settings",
            primaryButtonColor = MaterialTheme.colorScheme.error,
            onPrimaryClick = {
                viewModel.restoreSettingsBackup(
                    backupJson = backupJson,
                    onSuccess = {
                        scope.launch {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            kotlinx.coroutines.delay(100)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        Toast.makeText(context, "Backup restored successfully!", Toast.LENGTH_SHORT).show()
                    },
                    onError = { e ->
                        scope.launch {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            kotlinx.coroutines.delay(200)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        Toast.makeText(context, "Restore failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                )
                fullBackupJsonToRestore = null
            },
            secondaryButtonText = "Cancel",
            onSecondaryClick = { fullBackupJsonToRestore = null },
            onDismiss = { fullBackupJsonToRestore = null }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBrush)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 120.dp)
        ) {
            SubPageTopBar(title = "Backup & Restore", onBack = { navController.popBackStack() })

            Spacer(modifier = Modifier.height(20.dp))

            // 👑 Master Hero Master Banner Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            )
                        )
                    )
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                        RoundedCornerShape(24.dp)
                    )
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Backup,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Backup & Restore Hub",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Export/Import preferences, custom feeds, and OPML data",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            SectionLabel("FULL SETTINGS BACKUP")
            Spacer(modifier = Modifier.height(10.dp))

            SettingsCard {
                SettingsRow(
                    icon = Icons.Default.SettingsBackupRestore,
                    iconBg = MaterialTheme.colorScheme.primary,
                    title = "Import Settings",
                    subtitle = "Restore app preferences, feeds & gestures from JSON",
                    onClick = { backupPickerLauncher.launch("*/*") }
                )
                CardDivider()
                SettingsRow(
                    icon = Icons.Default.Save,
                    iconBg = MaterialTheme.colorScheme.secondary,
                    title = "Export Settings",
                    subtitle = "Backup preferences & feeds to standard JSON file",
                    onClick = {
                        scope.launch {
                            try {
                                val backupJson = viewModel.generateBackupJson()
                                exportFullBackup(backupJson)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Failed to create backup: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            SectionLabel("OPML SUBSCRIPTION OUTLINE")
            Spacer(modifier = Modifier.height(10.dp))

            SettingsCard {
                SettingsRow(
                    icon = Icons.Default.CloudUpload,
                    iconBg = MaterialTheme.colorScheme.tertiary,
                    title = "Import OPML",
                    subtitle = "Upload RSS subscription list outline file",
                    onClick = { opmlPickerLauncher.launch("*/*") }
                )
                CardDivider()
                SettingsRow(
                    icon = Icons.Default.CloudDownload,
                    iconBg = MaterialTheme.colorScheme.primary,
                    title = "Export OPML",
                    subtitle = "Save RSS subscription list outline file",
                    onClick = { exportSubscriptions(customFeeds) }
                )
            }

            Spacer(modifier = Modifier.height(28.dp))
            InfoNote("Full Settings Backups generate a standard text JSON file that securely restores feeds, category orders, UI gestures, and translation choices.")
        }
    }
}
