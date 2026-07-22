package uk.sume.streamfolio.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import uk.sume.streamfolio.ui.theme.getThemeBackgroundBrush



import uk.sume.streamfolio.ui.viewmodel.NewsViewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material.icons.filled.Backup
import uk.sume.streamfolio.util.OpmlHelper
import uk.sume.streamfolio.util.OpmlFeed
import uk.sume.streamfolio.util.UrlSecurityValidator
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.OutlinedButton
import java.util.Locale

/**
 * Premium full-screen onboarding wizard with step-by-step flow.
 * Design is consistent with the rest of the app's glassmorphic aesthetic.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(navController: NavController, viewModel: NewsViewModel) {
    val systemLocale = Locale.getDefault()
    val defaultLang = systemLocale.language.takeIf { it in listOf("en", "es", "fr", "de", "hi", "zh") } ?: "en"
    val defaultRegion = systemLocale.country.takeIf { it in listOf("US", "GB", "CA", "FR", "DE", "IN", "AU", "SG", "HK", "KR", "JP", "BR", "ZA", "AE") } ?: "US"

    var selectedLang by rememberSaveable { mutableStateOf(defaultLang) }
    var selectedRegion by rememberSaveable { mutableStateOf(defaultRegion) }
    var selectedCacheDays by rememberSaveable { mutableStateOf("36500") }
    var isDefaultFeedsEnabled by rememberSaveable { mutableStateOf(true) }
    var selectedCats by rememberSaveable {
        mutableStateOf(setOf("Top Stories", "World", "Business", "Technology", "Science", "Sports", "Health", "Entertainment"))
    }
    
    val isGeminiSupported by viewModel.isGeminiSupported.collectAsState()
    
    // AI Toggles
    var isAiEnabled by rememberSaveable { mutableStateOf(false) }
    var isTranslationEnabled by rememberSaveable { mutableStateOf(true) }
    var isSummaryEnabled by rememberSaveable { mutableStateOf(true) }
    var isSmartTagsEnabled by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(isGeminiSupported) {
        if (isGeminiSupported == false) {
            isSummaryEnabled = false
            isSmartTagsEnabled = false
        }
    }

    var currentStep by rememberSaveable { mutableStateOf(0) }
    val showPreferences = isDefaultFeedsEnabled || (isAiEnabled && isTranslationEnabled)
    val totalSteps = if (showPreferences) 5 else 4

    LaunchedEffect(totalSteps) {
        if (currentStep >= totalSteps) {
            currentStep = totalSteps - 1
        }
    }

    // Pulse animation for final button
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val buttonScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.04f,
        animationSpec = InfiniteRepeatableSpec(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val isDark = isSystemInDarkTheme()
    val bgBrush = getThemeBackgroundBrush()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBrush)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Steps indicator
            StepIndicator(currentStep = currentStep, totalSteps = totalSteps)

            // Dynamic Step Content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.TopStart
            ) {
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> -width } + fadeOut()
                            )
                        } else {
                            (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> width } + fadeOut()
                            )
                        }
                    },
                    label = "step_transition"
                ) { step ->
                    when (step) {
                        0 -> WelcomeStep(systemLocale)
                        1 -> ImportBackupStep(
                            viewModel = viewModel,
                            onImportSuccess = {
                                viewModel.prefs.isCompletedOnboarding = true
                                navController.navigate("home_screen") {
                                    popUpTo("onboarding") { inclusive = true }
                                }
                            },
                            onOpmlImportSuccess = {
                                currentStep++
                            }
                        )
                        2 -> DefaultFeedsAndCacheStep(
                            isEnabled = isDefaultFeedsEnabled,
                            onToggle = { isDefaultFeedsEnabled = it },
                            selectedCats = selectedCats,
                            onCatsChanged = { selectedCats = it },
                            selectedCacheDays = selectedCacheDays,
                            onCacheDaysSelected = { selectedCacheDays = it }
                        )
                        3 -> AiFeaturesStep(
                            isAiEnabled = isAiEnabled,
                            onAiToggle = { isAiEnabled = it },
                            isTranslationEnabled = isTranslationEnabled,
                            onTranslationToggle = { isTranslationEnabled = it },
                            isSummaryEnabled = isSummaryEnabled,
                            onSummaryToggle = { isSummaryEnabled = it },
                            isSmartTagsEnabled = isSmartTagsEnabled,
                            onSmartTagsToggle = { isSmartTagsEnabled = it },
                            isGeminiSupported = isGeminiSupported
                        )
                        4 -> TranslationRegionStep(
                            showLang = isAiEnabled && isTranslationEnabled,
                            selectedLang = selectedLang,
                            onLangSelected = { selectedLang = it },
                            showRegion = isDefaultFeedsEnabled,
                            selectedRegion = selectedRegion,
                            onRegionSelected = { selectedRegion = it }
                        )
                    }
                }
            }

            // Bottom navigation
            BottomNavigation(
                currentStep = currentStep,
                totalSteps = totalSteps,
                buttonScale = if (currentStep == totalSteps - 1) buttonScale else 1f,
                onBack = { currentStep-- },
                onNext = {
                    if (currentStep < totalSteps - 1) {
                        currentStep++
                    } else {
                        viewModel.updatePreferences(selectedLang, selectedRegion)
                        viewModel.prefs.cacheHistoryDays = selectedCacheDays.toInt()
                        viewModel.prefs.isDefaultFeedsEnabled = isDefaultFeedsEnabled
                        viewModel.prefs.selectedCategories = selectedCats
                        
                        // Save AI preferences
                        viewModel.setAiEnabled(isAiEnabled)
                        viewModel.setTranslationEnabled(isTranslationEnabled)
                        viewModel.setSummaryEnabled(isSummaryEnabled)
                        viewModel.setSmartTagsEnabled(isSmartTagsEnabled)
                        viewModel.setHasSeenAiSpotlight(true)
                        
                        viewModel.prefs.isCompletedOnboarding = true
                        viewModel.refreshCurrentFeed()
                        viewModel.syncAllCategoriesInBackground()
                        navController.navigate("home_screen") {
                            popUpTo("onboarding") { inclusive = true }
                        }
                    }
                }
            )
        }
    }
}

/** Top step dots indicator */
@Composable
private fun StepIndicator(currentStep: Int, totalSteps: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalSteps) { index ->
            val isActive = index == currentStep
            val isPassed = index < currentStep
            val dotWidth by animateFloatAsState(
                targetValue = if (isActive) 28f else 8f,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "dotWidth"
            )
            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(dotWidth.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isActive -> MaterialTheme.colorScheme.primary
                            isPassed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                        }
                    )
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "${currentStep + 1} / $totalSteps",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            fontWeight = FontWeight.Medium
        )
    }
}

/** Bottom back/next navigation row */
@Composable
private fun BottomNavigation(
    currentStep: Int,
    totalSteps: Int,
    buttonScale: Float,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    val isFinalStep = currentStep == totalSteps - 1
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedVisibility(visible = currentStep > 0) {
            TextButton(
                onClick = onBack,
                modifier = Modifier.height(52.dp)
            ) {
                Text(
                    text = "Back",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }

        Button(
            onClick = onNext,
            modifier = Modifier
                .weight(1f)
                .height(52.dp)
                .scale(buttonScale),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (isFinalStep) "Start Reading" else "Continue",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun ImportBackupStep(
    viewModel: NewsViewModel,
    onImportSuccess: () -> Unit,
    onOpmlImportSuccess: () -> Unit
) {
    val context = LocalContext.current
    var parsedFeedsToImport by remember { mutableStateOf<List<OpmlFeed>?>(null) }

    val backupPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val jsonText = inputStream.bufferedReader().use { it.readText() }
                    if (jsonText.contains("custom_feeds") && jsonText.contains("preferences")) {
                        viewModel.restoreSettingsBackup(
                            backupJson = jsonText,
                            onSuccess = {
                                Toast.makeText(context, "Welcome back! Backup restored successfully.", Toast.LENGTH_LONG).show()
                                onImportSuccess()
                            },
                            onError = { e ->
                                Toast.makeText(context, "Restore failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                        )
                    } else {
                        Toast.makeText(context, "Invalid backup file structure.", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to read backup file: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

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

    // OPML Import Confirmation Bottom Sheet (within Onboarding screen)
    if (parsedFeedsToImport != null) {
        val customFeeds by viewModel.customFeeds.collectAsState()
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
                                Toast.makeText(context, "Feeds imported successfully! Continuing setup...", Toast.LENGTH_LONG).show()
                                onOpmlImportSuccess()
                            },
                            enabled = selectedCategories.value.isNotEmpty(),
                            modifier = Modifier.weight(1.5f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Import Selected", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.linearGradient(
                        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Backup,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Already have a backup?",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 38.sp,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "StreamFolio allows you to easily restore subscriptions or configuration backups from other devices. Choose an import source below.",
            fontSize = 15.sp,
            lineHeight = 22.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(36.dp))

        // Option 1: Full App Settings Backup
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { backupPickerLauncher.launch("*/*") },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SettingsBackupRestore,
                        contentDescription = "Restore Full Backup",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Import Settings Backup",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Restores all feeds & preferences. Skips setup wizard.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Option 2: Standard OPML Subscriptions
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { opmlPickerLauncher.launch("*/*") },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = "Import OPML",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Import OPML Subscriptions",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Imports feeds only. Continue setup wizard.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Otherwise, click Continue below to proceed with normal setup.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
    }
}

/** Step 0 – Welcome */
@Composable
private fun WelcomeStep(systemLocale: Locale) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.Start
    ) {
        // App icon badge
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.linearGradient(
                        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.RssFeed,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Welcome to\nStreamFolio",
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 42.sp,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Your personalized, modern, and free news reader. Let's get you set up in just a few steps.",
            fontSize = 15.sp,
            lineHeight = 22.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(36.dp))

        FeatureRow(icon = Icons.Default.Public, title = "Global Coverage", subtitle = "News from top sources worldwide")
        Spacer(modifier = Modifier.height(16.dp))
        FeatureRow(icon = Icons.Default.RssFeed, title = "Custom Feeds", subtitle = "Add your own RSS feeds")
        Spacer(modifier = Modifier.height(16.dp))
        FeatureRow(icon = Icons.Default.Hearing, title = "Text-to-Speech", subtitle = "Listen to articles hands-free")
        Spacer(modifier = Modifier.height(16.dp))
        FeatureRow(icon = Icons.Default.Bookmark, title = "Bookmarks", subtitle = "Save stories to read later")

        Spacer(modifier = Modifier.height(28.dp))

        // Locale detection chip
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Detected: ${systemLocale.displayLanguage} · ${systemLocale.displayCountry}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun FeatureRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

/** Step 1 – Default Feeds & Offline Cache Configuration */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DefaultFeedsAndCacheStep(
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    selectedCats: Set<String>,
    onCatsChanged: (Set<String>) -> Unit,
    selectedCacheDays: String,
    onCacheDaysSelected: (String) -> Unit
) {
    val availableCategories = listOf(
        "Top Stories" to "Top Stories",
        "World" to "World",
        "Business" to "Business",
        "Technology" to "Technology",
        "Science" to "Science",
        "Sports" to "Sports",
        "Health" to "Health",
        "Entertainment" to "Entertainment"
    )
    val cacheOptions = mapOf(
        "1" to "1 Day",
        "3" to "3 Days",
        "7" to "7 Days",
        "14" to "14 Days",
        "30" to "30 Days",
        "36500" to "Keep All"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Personalise\nYour Feed",
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 42.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Enable default curated categories, choose your interests, and set database retention.",
            fontSize = 15.sp,
            lineHeight = 22.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(36.dp))

        // Default Feeds toggle card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(
                    if (isEnabled)
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                    else
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
                .border(
                    1.dp,
                    if (isEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else Color.Transparent,
                    RoundedCornerShape(18.dp)
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onToggle(!isEnabled) }
                .padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.RssFeed,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "Default Curated Feeds",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Direct RSS/Atom news categories",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = MaterialTheme.colorScheme.primary
                )
            )
        }

        AnimatedVisibility(visible = isEnabled) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.height(28.dp))
                Text(
                    text = "Categories",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availableCategories.forEach { (label, key) ->
                        val isSelected = selectedCats.contains(key)
                        CategoryChip(
                            label = label,
                            isSelected = isSelected,
                            onClick = {
                                val newSet = if (isSelected) {
                                    if (selectedCats.size > 1) selectedCats - key else selectedCats
                                } else {
                                    selectedCats + key
                                }
                                onCatsChanged(newSet)
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${selectedCats.size} of ${availableCategories.size} selected",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        SelectorField(
            label = "Offline Cache History",
            icon = Icons.Default.History,
            value = cacheOptions[selectedCacheDays] ?: "Keep All",
            options = cacheOptions,
            onSelected = onCacheDaysSelected
        )
    }
}

/** Step 3 (Conditional) – Translation & Region Selection */
@Composable
private fun TranslationRegionStep(
    showLang: Boolean,
    selectedLang: String,
    onLangSelected: (String) -> Unit,
    showRegion: Boolean,
    selectedRegion: String,
    onRegionSelected: (String) -> Unit
) {
    val languages = mapOf(
        "en" to "English",
        "es" to "Español",
        "fr" to "Français",
        "de" to "Deutsch",
        "hi" to "हिन्दी",
        "zh" to "中文"
    )
    val regions = mapOf(
        "US" to "United States",
        "GB" to "United Kingdom",
        "CA" to "Canada",
        "FR" to "France",
        "DE" to "Germany",
        "IN" to "India",
        "AU" to "Australia",
        "SG" to "Singapore",
        "HK" to "Hong Kong",
        "KR" to "South Korea",
        "JP" to "Japan",
        "BR" to "Brazil",
        "ZA" to "South Africa",
        "AE" to "UAE"
    )

    val headerText = when {
        showLang && showRegion -> "Translation &\nRegion"
        showRegion -> "Region\nPreferences"
        else -> "Translation\nPreferences"
    }

    val descText = when {
        showLang && showRegion -> "Set your target translation language and region."
        showRegion -> "Select the country for your curated default news feeds."
        else -> "Select your target language for offline AI translations."
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = headerText,
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 42.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = descText,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(36.dp))

        if (showRegion) {
            SelectorField(
                label = "Region / Country",
                icon = Icons.Default.Place,
                value = regions[selectedRegion] ?: "United States",
                options = regions,
                onSelected = onRegionSelected
            )
        }

        if (showLang && showRegion) {
            Spacer(modifier = Modifier.height(20.dp))
        }

        if (showLang) {
            SelectorField(
                label = "Target Language",
                icon = Icons.Default.Language,
                value = languages[selectedLang] ?: "English",
                options = languages,
                onSelected = onLangSelected
            )
        }
    }
}

@Composable
private fun CategoryChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
            .border(
                1.dp,
                if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                RoundedCornerShape(12.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
        )
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

/** Re-usable dropdown selector used in language/region step */
@Composable
fun SelectorField(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    options: Map<String, String>,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                        RoundedCornerShape(16.dp)
                    )
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = value,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                options.forEach { (code, name) ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = name,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        onClick = {
                            onSelected(code)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AiFeaturesStep(
    isAiEnabled: Boolean,
    onAiToggle: (Boolean) -> Unit,
    isTranslationEnabled: Boolean,
    onTranslationToggle: (Boolean) -> Unit,
    isSummaryEnabled: Boolean,
    onSummaryToggle: (Boolean) -> Unit,
    isSmartTagsEnabled: Boolean,
    onSmartTagsToggle: (Boolean) -> Unit,
    isGeminiSupported: Boolean?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "On-Device AI\nUpgrades",
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 42.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "StreamFolio runs local models directly on your device for absolute privacy.",
            fontSize = 15.sp,
            lineHeight = 22.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Global AI Switch Card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(
                    if (isAiEnabled)
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                    else
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
                .border(
                    1.dp,
                    if (isAiEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else Color.Transparent,
                    RoundedCornerShape(18.dp)
                )
                .clickable { onAiToggle(!isAiEnabled) }
                .padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isAiEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "Enable Local AI features",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Powered privately by Gemini Nano",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
            Switch(
                checked = isAiEnabled,
                onCheckedChange = onAiToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = MaterialTheme.colorScheme.primary
                )
            )
        }

        AnimatedVisibility(visible = isAiEnabled) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "INDIVIDUAL SERVICES",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                    letterSpacing = 1.sp
                )

                // Sub-Toggle 1: Translation
                SubToggleRow(
                    title = "Offline AI Translation",
                    subtitle = "Translate foreign feeds entirely offline",
                    checked = isTranslationEnabled,
                    onCheckedChange = onTranslationToggle
                )

                // Sub-Toggle 2: Summaries
                val isGeminiOk = isGeminiSupported != false
                SubToggleRow(
                    title = "Key Insights Generator",
                    subtitle = if (isGeminiOk) {
                        "Generate 3-bullet point summaries"
                    } else {
                        "Unavailable on this device; you can still read the normal article content without AI insights"
                    },
                    checked = if (isGeminiOk) isSummaryEnabled else false,
                    onCheckedChange = onSummaryToggle,
                    enabled = isGeminiOk,
                    errorText = if (!isGeminiOk) "Requires Gemini Nano. On unsupported devices, AI insights are disabled, but the regular article content remains available." else null
                )

                // Sub-Toggle 3: Smart Tags
                SubToggleRow(
                    title = "Smart Categories & Tags",
                    subtitle = if (isGeminiOk) {
                        "Automatically suggest tags dynamically with Gemini enhancement"
                    } else {
                        "Use basic keyword tagging on unsupported devices"
                    },
                    checked = isSmartTagsEnabled,
                    onCheckedChange = onSmartTagsToggle,
                    enabled = true,
                    isBeta = true,
                    warningText = "Tags are experimental and may occasionally be inaccurate.",
                    errorText = if (!isGeminiOk) "Gemini Nano is unavailable, so Smart Tags will use a basic fallback instead." else null
                )
            }
        }
    }
}

@Composable
private fun SubToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    errorText: String? = null,
    isBeta: Boolean = false,
    warningText: String? = null
) {
    val alpha = if (enabled) 1f else 0.5f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).alpha(alpha)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (isBeta) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                        modifier = Modifier.padding(start = 6.dp)
                    ) {
                        Text(
                            text = "BETA",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFD97706),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            if (warningText != null && enabled) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = warningText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFD97706)
                )
            }
            if (errorText != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = errorText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            modifier = Modifier.scale(0.85f),
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}
