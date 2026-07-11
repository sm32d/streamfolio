package uk.sume.streamfolio.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import uk.sume.streamfolio.ui.theme.DarkGradient
import uk.sume.streamfolio.ui.theme.EmeraldPrimary
import uk.sume.streamfolio.ui.theme.EmeraldSecondary
import uk.sume.streamfolio.ui.theme.LightGradient
import uk.sume.streamfolio.ui.viewmodel.NewsViewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import uk.sume.streamfolio.util.OpmlHelper
import uk.sume.streamfolio.util.OpmlFeed
import java.io.File
import androidx.core.content.FileProvider
import android.content.Intent
import android.widget.Toast
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.platform.LocalContext
import uk.sume.streamfolio.data.network.DefaultFeedsConfig
import uk.sume.streamfolio.data.network.CuratedProvider
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import uk.sume.streamfolio.data.model.CustomFeed

// ─── Main Settings Screen ───────────────────────────────────────────────────

@Composable
fun SettingsScreen(navController: NavController, viewModel: NewsViewModel) {
    val isDark = isSystemInDarkTheme()
    val bgBrush = if (isDark) DarkGradient else LightGradient

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
            // Header
            Text(
                text = "Settings",
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
            )
            Text(
                text = "Customise your StreamFolio experience.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // Section: Personalisation
            SectionLabel("Personalisation")
            Spacer(modifier = Modifier.height(10.dp))
            SettingsCard {
                SettingsRow(
                    icon = Icons.Default.Tune,
                    iconBg = EmeraldPrimary,
                    title = "Preferences",
                    subtitle = "Language & region for your news feed",
                    onClick = { navController.navigate("settings_preferences") }
                )
                CardDivider()
                SettingsRow(
                    icon = Icons.Default.Swipe,
                    iconBg = Color(0xFFEC4899),
                    title = "Swipe Gestures",
                    subtitle = "Customize left & right swipe actions",
                    onClick = { navController.navigate("settings_gestures") }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Section: Data Management
            SectionLabel("Data Management")
            Spacer(modifier = Modifier.height(10.dp))
            SettingsCard {
                SettingsRow(
                    icon = Icons.Default.List,
                    iconBg = Color(0xFF6366F1),
                    title = "Manage Content & Sources",
                    subtitle = "Customise topics and curation channels",
                    onClick = { navController.navigate("settings_categories") }
                )
                CardDivider()
                SettingsRow(
                    icon = Icons.Default.RssFeed,
                    iconBg = Color(0xFFF59E0B),
                    title = "Custom RSS Feeds",
                    subtitle = "Add and manage private RSS publishers",
                    onClick = { navController.navigate("settings_feeds") }
                )
                CardDivider()
                SettingsRow(
                    icon = Icons.Default.Backup,
                    iconBg = Color(0xFF10B981),
                    title = "Backup & Restore",
                    subtitle = "Import/export feeds and settings backup",
                    onClick = { navController.navigate("settings_backup") }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Section: AI Services
            SectionLabel("AI Services")
            Spacer(modifier = Modifier.height(10.dp))
            SettingsCard {
                SettingsRow(
                    icon = Icons.Default.AutoAwesome,
                    iconBg = Color(0xFF8B5CF6),
                    title = "AI Services & Preferences",
                    subtitle = "Offline translation, key insights & smart tags",
                    onClick = { navController.navigate("settings_ai") }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Section: About
            SectionLabel("About")
            Spacer(modifier = Modifier.height(10.dp))
            SettingsCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.linearGradient(listOf(EmeraldPrimary, EmeraldSecondary))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "StreamFolio",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Version 1.2.0-beta.6 · Free forever",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
            SectionLabel("More from Dominik Studios")
            Spacer(modifier = Modifier.height(10.dp))
            
            val context = LocalContext.current
            val imageLoader = remember {
                coil.ImageLoader.Builder(context)
                    .components {
                        if (android.os.Build.VERSION.SDK_INT >= 28) {
                            add(coil.decode.ImageDecoderDecoder.Factory())
                        } else {
                            add(coil.decode.GifDecoder.Factory())
                        }
                    }
                    .okHttpClient {
                        okhttp3.OkHttpClient.Builder()
                            .addInterceptor { chain ->
                                val request = chain.request().newBuilder()
                                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                                    .build()
                                chain.proceed(request)
                            }
                            .build()
                    }
                    .build()
            }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.thejoblog.com"))
                        context.startActivity(intent)
                    },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = "https://www.thejoblog.com/tjl.gif",
                            contentDescription = "The Job Log Logo",
                            imageLoader = imageLoader,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "The Job Log",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Your ultimate professional job hunting companion. Track application progress, interview schedules, and follow-ups elegantly.",
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

// ─── Sub-page: Preferences ──────────────────────────────────────────────────

@Composable
fun SettingsPreferencesScreen(navController: NavController, viewModel: NewsViewModel) {
    val currentLang = viewModel.prefs.language
    val currentRegion = viewModel.prefs.region
    var selectedLang by remember { mutableStateOf(currentLang) }
    var selectedRegion by remember { mutableStateOf(currentRegion) }

    val languages = mapOf(
        "en" to "🇬🇧 English",
        "es" to "🇪🇸 Español",
        "fr" to "🇫🇷 Français",
        "de" to "🇩🇪 Deutsch",
        "hi" to "🇮🇳 हिन्दी",
        "zh" to "🇨🇳 中文"
    )
    val regions = mapOf(
        "US" to "🇺🇸 United States",
        "GB" to "🇬🇧 United Kingdom",
        "CA" to "🇨🇦 Canada",
        "FR" to "🇫🇷 France",
        "DE" to "🇩🇪 Germany",
        "IN" to "🇮🇳 India",
        "AU" to "🇦🇺 Australia",
        "SG" to "🇸🇬 Singapore",
        "HK" to "🇭🇰 Hong Kong"
    )
    val cacheOptions = mapOf(
        "1" to "1 Day",
        "3" to "3 Days",
        "7" to "7 Days",
        "14" to "14 Days",
        "30" to "30 Days",
        "36500" to "Keep All"
    )
    var cacheDays by remember { mutableStateOf(viewModel.prefs.cacheHistoryDays.toString()) }

    val isDark = isSystemInDarkTheme()
    val bgBrush = if (isDark) DarkGradient else LightGradient

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
            SubPageTopBar(title = "Preferences", onBack = { navController.popBackStack() })

            SettingsSubHeader(
                title = "Preferences",
                description = "Choose the source regions and database cache retention durations that best match your news reading preferences.",
                icon = Icons.Default.Tune
            )

            SettingsSelectorField(
                label = "Region / Country",
                icon = Icons.Default.Place,
                value = regions[selectedRegion] ?: "United States",
                options = regions,
                onSelected = {
                    selectedRegion = it
                    viewModel.updatePreferences(viewModel.prefs.language, it)
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            SettingsSelectorField(
                label = "Offline Cache History",
                icon = Icons.Default.History,
                value = cacheOptions[cacheDays] ?: "7 Days",
                options = cacheOptions,
                onSelected = {
                    cacheDays = it
                    viewModel.prefs.cacheHistoryDays = it.toInt()
                }
            )

            Spacer(modifier = Modifier.height(28.dp))
            InfoNote("Changing language or region will refresh your news feed immediately.")
        }
    }
}

// ─── Sub-page: Manage Content & Sources ────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsManageContentScreen(navController: NavController, viewModel: NewsViewModel) {
    var isDefaultFeedsEnabled by remember { mutableStateOf(viewModel.prefs.isDefaultFeedsEnabled) }
    val availableCategories = listOf(
        "🗞️ Top Stories" to "Top Stories",
        "🌍 World" to "World",
        "💼 Business" to "Business",
        "💻 Technology" to "Technology",
        "🔬 Science" to "Science",
        "⚽ Sports" to "Sports",
        "❤️ Health" to "Health",
        "🎬 Entertainment" to "Entertainment"
    )
    var selectedCats by remember { mutableStateOf(viewModel.prefs.selectedCategories) }

    val isDark = isSystemInDarkTheme()
    val bgBrush = if (isDark) DarkGradient else LightGradient

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
            SubPageTopBar(title = "Manage Content & Sources", onBack = { navController.popBackStack() })

            SettingsSubHeader(
                title = "Content Categories",
                description = "Choose which news categories are displayed on your dashboard. You can toggle default feeds, change category order, or manage custom news sources.",
                icon = Icons.Default.Category
            )

            // Default Feeds toggle card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        if (isDefaultFeedsEnabled) EmeraldPrimary.copy(alpha = 0.08f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                    .border(
                        1.dp,
                        if (isDefaultFeedsEnabled) EmeraldPrimary.copy(alpha = 0.3f) else Color.Transparent,
                        RoundedCornerShape(18.dp)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            val newVal = !isDefaultFeedsEnabled
                            isDefaultFeedsEnabled = newVal
                            viewModel.prefs.isDefaultFeedsEnabled = newVal
                            viewModel.refreshCurrentFeed()
                        }
                    )
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
                                if (isDefaultFeedsEnabled) EmeraldPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
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
                    checked = isDefaultFeedsEnabled,
                    onCheckedChange = { checked ->
                        isDefaultFeedsEnabled = checked
                        viewModel.prefs.isDefaultFeedsEnabled = checked
                        viewModel.refreshCurrentFeed()
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = EmeraldPrimary
                    )
                )
            }

            AnimatedVisibility(visible = isDefaultFeedsEnabled) {
                Column {
                    Spacer(modifier = Modifier.height(28.dp))
                    Text(
                        text = "CATEGORIES",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        availableCategories.forEach { (label, key) ->
                            val isSelected = selectedCats.contains(key)
                            CategoryToggleChip(
                                label = label,
                                isSelected = isSelected,
                                onClick = {
                                    val updated = if (isSelected) {
                                        if (selectedCats.size > 1) selectedCats - key else selectedCats
                                    } else {
                                        selectedCats + key
                                    }
                                    selectedCats = updated
                                    viewModel.prefs.selectedCategories = updated
                                    viewModel.refreshCurrentFeed()
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "${selectedCats.size} of ${availableCategories.size} selected",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = "CURATION OPTIONS",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                modifier = Modifier.padding(bottom = 12.dp)
            )
            SettingsCard {
                SettingsRow(
                    icon = Icons.Default.Sort,
                    iconBg = Color(0xFF3B82F6),
                    title = "Reorder Category Tabs",
                    subtitle = "Adjust the position and priority of your category tabs",
                    onClick = { navController.navigate("settings_reorder") }
                )
                CardDivider()
                SettingsRow(
                    icon = Icons.Default.Widgets,
                    iconBg = Color(0xFF10B981),
                    title = "Default Feed Providers",
                    subtitle = "Enable or disable curated news publishers",
                    onClick = { navController.navigate("settings_providers") }
                )
            }
        }
    }
}

@Composable
fun SettingsReorderScreen(navController: NavController, viewModel: NewsViewModel) {
    val isDark = isSystemInDarkTheme()
    val bgBrush = if (isDark) DarkGradient else LightGradient

    val customFeeds by viewModel.customFeeds.collectAsState()
    val isDefaultFeedsEnabled = viewModel.prefs.isDefaultFeedsEnabled
    val selectedCategoriesPref = viewModel.prefs.selectedCategories
    
    // Merge default and custom RSS categories dynamically
    val allActiveCategories = remember(customFeeds, selectedCategoriesPref, isDefaultFeedsEnabled) {
        val defaultCategories = listOf("Top Stories", "World", "Business", "Technology", "Science", "Sports", "Health", "Entertainment")
        val googleCategories = if (isDefaultFeedsEnabled) {
            val filteredDefaults = defaultCategories.filter { selectedCategoriesPref.contains(it) }
            if (filteredDefaults.isEmpty()) listOf("Top Stories") else filteredDefaults
        } else {
            emptyList()
        }
        val rawCategories = (googleCategories + customFeeds.map { it.category }).distinct()
        val categoryOrder = viewModel.prefs.categoryOrder
        rawCategories.sortedWith(compareBy { cat ->
            val index = categoryOrder.indexOf(cat)
            if (index == -1) Int.MAX_VALUE else index
        })
    }

    var categoryList by remember(allActiveCategories) { mutableStateOf(allActiveCategories) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBrush)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp)
        ) {
            SubPageTopBar(title = "Reorder Category Tabs", onBack = { navController.popBackStack() })

            SettingsSubHeader(
                title = "Category Order",
                description = "Hold and drag the handles next to each category to change the order in which they appear on your dashboard tab layout.",
                icon = Icons.Default.Sort
            )

            var draggedIndex by remember { mutableStateOf(-1) }
            var dragOffset by remember { mutableStateOf(0f) }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 120.dp)
            ) {
                for (index in categoryList.indices) {
                    val category = categoryList[index]
                    val isDraggingThis = index == draggedIndex
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .graphicsLayer {
                                translationY = if (isDraggingThis) dragOffset else 0f
                                shadowElevation = if (isDraggingThis) 8f else 0f
                                scaleX = if (isDraggingThis) 1.02f else 1f
                                scaleY = if (isDraggingThis) 1.02f else 1f
                            }
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isDraggingThis) MaterialTheme.colorScheme.surfaceVariant
                                else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                            )
                            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DragHandle,
                                contentDescription = "Drag Handle",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                modifier = Modifier
                                    .size(24.dp)
                                    .pointerInput(index) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = {
                                                draggedIndex = index
                                                dragOffset = 0f
                                            },
                                            onDragEnd = {
                                                draggedIndex = -1
                                                dragOffset = 0f
                                                viewModel.prefs.categoryOrder = categoryList
                                                viewModel.refreshCurrentFeed()
                                            },
                                            onDragCancel = {
                                                draggedIndex = -1
                                                dragOffset = 0f
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                dragOffset += dragAmount.y
                                                
                                                val itemHeightPx = 180f
                                                if (dragOffset > itemHeightPx && draggedIndex < categoryList.size - 1) {
                                                    val newList = categoryList.toMutableList()
                                                    val nextIdx = draggedIndex + 1
                                                    val temp = newList[draggedIndex]
                                                    newList[draggedIndex] = newList[nextIdx]
                                                    newList[nextIdx] = temp
                                                    categoryList = newList
                                                    draggedIndex = nextIdx
                                                    dragOffset -= itemHeightPx
                                                } else if (dragOffset < -itemHeightPx && draggedIndex > 0) {
                                                    val newList = categoryList.toMutableList()
                                                    val prevIdx = draggedIndex - 1
                                                    val temp = newList[draggedIndex]
                                                    newList[draggedIndex] = newList[prevIdx]
                                                    newList[prevIdx] = temp
                                                    categoryList = newList
                                                    draggedIndex = prevIdx
                                                    dragOffset += itemHeightPx
                                                }
                                            }
                                        )
                                    }
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = category,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    if (index > 0) {
                                        val newList = categoryList.toMutableList()
                                        val temp = newList[index]
                                        newList[index] = newList[index - 1]
                                        newList[index - 1] = temp
                                        categoryList = newList
                                        viewModel.prefs.categoryOrder = newList
                                        viewModel.refreshCurrentFeed()
                                    }
                                },
                                enabled = index > 0 && draggedIndex == -1,
                                colors = IconButtonDefaults.iconButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onSurface,
                                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                                )
                            ) {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move Up")
                            }
                            IconButton(
                                onClick = {
                                    if (index < categoryList.size - 1) {
                                        val newList = categoryList.toMutableList()
                                        val temp = newList[index]
                                        newList[index] = newList[index + 1]
                                        newList[index + 1] = temp
                                        categoryList = newList
                                        viewModel.prefs.categoryOrder = newList
                                        viewModel.refreshCurrentFeed()
                                    }
                                },
                                enabled = index < categoryList.size - 1 && draggedIndex == -1,
                                colors = IconButtonDefaults.iconButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onSurface,
                                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                                )
                            ) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move Down")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsProvidersScreen(navController: NavController, viewModel: NewsViewModel) {
    val isDark = isSystemInDarkTheme()
    val bgBrush = if (isDark) DarkGradient else LightGradient
    
    val allProviders = remember { DefaultFeedsConfig.getAllCuratedProviders() }
    val groupedProviders = remember(allProviders) {
        allProviders.groupBy { it.region }
    }
    
    val activeRegion = viewModel.prefs.region.uppercase()
    val sortedRegions = remember(groupedProviders, activeRegion) {
        groupedProviders.keys.sortedWith(Comparator { r1, r2 ->
            when {
                r1 == activeRegion && r2 == activeRegion -> 0
                r1 == activeRegion -> -1
                r2 == activeRegion -> 1
                else -> r1.compareTo(r2)
            }
        })
    }
    
    // Smart navigation filters
    var categoryFilter by remember { mutableStateOf(viewModel.filterCategoryOnSettings) }
    
    // Smart initialization: default to category grouping if active region lacks default feeds, or if forced via empty feed navigation
    val currentCategory = viewModel.selectedCategory.value
    val hasFeedsInActiveRegion = remember(activeRegion, currentCategory) {
        DefaultFeedsConfig.getFeedsFor(
            region = activeRegion,
            category = currentCategory,
            disabledFeedUrls = emptySet(),
            enabledCrossRegionFeeds = emptySet()
        ).isNotEmpty()
    }
    var groupByCategory by remember { mutableStateOf(categoryFilter != null || !hasFeedsInActiveRegion) }
    
    LaunchedEffect(Unit) {
        viewModel.filterCategoryOnSettings = null
    }
    
    var disabledUrls by remember { mutableStateOf(viewModel.prefs.disabledFeedUrls) }
    var enabledCrossRegion by remember { mutableStateOf(viewModel.prefs.enabledCrossRegionFeeds) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBrush)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp)
        ) {
            SubPageTopBar(title = "Default Feed Providers", onBack = { navController.popBackStack() })

            SettingsSubHeader(
                title = "News Providers",
                description = "Manage which default, curated global news publishers and regional sources are active or hidden across your dashboard categories.",
                icon = Icons.Default.Public
            )

            // Category Filter Active Banner
            if (categoryFilter != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = EmeraldPrimary.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Showing category: $categoryFilter",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = EmeraldPrimary
                        )
                        Text(
                            text = "Show All",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary,
                            modifier = Modifier.clickable { categoryFilter = null }
                        )
                    }
                }
            }

            // Segmented-style toggle button row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { groupByCategory = false }
                        .background(if (!groupByCategory) EmeraldPrimary else Color.Transparent)
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Group by Region",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (!groupByCategory) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { groupByCategory = true }
                        .background(if (groupByCategory) EmeraldPrimary else Color.Transparent)
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Group by Category",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (groupByCategory) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 120.dp)
            ) {
                if (!groupByCategory) {
                    for (regionCode in sortedRegions) {
                        val providersInRegion = (groupedProviders[regionCode] ?: emptyList()).filter {
                            categoryFilter == null || it.category.equals(categoryFilter, ignoreCase = true)
                        }
                        val isActiveRegion = regionCode == activeRegion
                        
                        if (providersInRegion.isNotEmpty()) {
                            val regionName = when (regionCode) {
                                "US" -> "🇺🇸 United States"
                                "GB" -> "🇬🇧 United Kingdom"
                                "SG" -> "🇸🇬 Singapore"
                                "HK" -> "🇭🇰 Hong Kong"
                                "IN" -> "🇮🇳 India"
                                "CA" -> "🇨🇦 Canada"
                                "AU" -> "🇦🇺 Australia"
                                "FR" -> "🇫🇷 France"
                                "DE" -> "🇩🇪 Germany"
                                else -> regionCode
                            }
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 24.dp, bottom = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = regionName,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary
                                )
                                
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isActiveRegion) EmeraldPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                    border = BorderStroke(1.dp, if (isActiveRegion) EmeraldPrimary.copy(alpha = 0.3f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                                ) {
                                      Text(
                                          text = if (isActiveRegion) "Active Region" else "Inactive",
                                          fontSize = 11.sp,
                                          fontWeight = FontWeight.SemiBold,
                                          color = if (isActiveRegion) EmeraldPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                      )
                                }
                            }
                            
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                            ) {
                                val sortedProviders = providersInRegion.sortedBy { it.category }
                                for (i in sortedProviders.indices) {
                                    val provider = sortedProviders[i]
                                    val compositeKey = "${provider.region}|${provider.category}|${provider.url}"
                                    val isEnabled = if (isActiveRegion) {
                                        !disabledUrls.contains(compositeKey)
                                    } else {
                                        enabledCrossRegion.contains(compositeKey)
                                    }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                if (isActiveRegion) {
                                                    val isCurrentlyEnabled = !disabledUrls.contains(compositeKey)
                                                    val newDisabled = if (isCurrentlyEnabled) {
                                                        disabledUrls + compositeKey
                                                    } else {
                                                        disabledUrls - compositeKey
                                                    }
                                                    disabledUrls = newDisabled
                                                    viewModel.prefs.disabledFeedUrls = newDisabled
                                                    if (!isCurrentlyEnabled) {
                                                        viewModel.fetchSingleFeed(provider.url, provider.category)
                                                    }
                                                } else {
                                                    val isCurrentlyEnabled = enabledCrossRegion.contains(compositeKey)
                                                    val newEnabled = if (isCurrentlyEnabled) {
                                                        enabledCrossRegion - compositeKey
                                                    } else {
                                                        enabledCrossRegion + compositeKey
                                                    }
                                                    enabledCrossRegion = newEnabled
                                                    viewModel.prefs.enabledCrossRegionFeeds = newEnabled
                                                    if (!isCurrentlyEnabled) {
                                                        viewModel.fetchSingleFeed(provider.url, provider.category)
                                                    }
                                                }
                                                viewModel.triggerPrefsChanged()
                                            }
                                            .graphicsLayer {
                                                alpha = if (isEnabled) 1f else 0.6f
                                            }
                                            .padding(horizontal = 16.dp, vertical = 14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = provider.publisherName,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = provider.category,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                            )
                                        }
                                        Switch(
                                            checked = isEnabled,
                                            onCheckedChange = { checked ->
                                                if (isActiveRegion) {
                                                    val newDisabled = if (checked) {
                                                        disabledUrls - compositeKey
                                                    } else {
                                                        disabledUrls + compositeKey
                                                    }
                                                    disabledUrls = newDisabled
                                                    viewModel.prefs.disabledFeedUrls = newDisabled
                                                    if (checked) {
                                                        viewModel.fetchSingleFeed(provider.url, provider.category)
                                                    }
                                                } else {
                                                    val newEnabled = if (checked) {
                                                        enabledCrossRegion + compositeKey
                                                    } else {
                                                        enabledCrossRegion - compositeKey
                                                    }
                                                    enabledCrossRegion = newEnabled
                                                    viewModel.prefs.enabledCrossRegionFeeds = newEnabled
                                                    if (checked) {
                                                        viewModel.fetchSingleFeed(provider.url, provider.category)
                                                    }
                                                }
                                                viewModel.triggerPrefsChanged()
                                            },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color.White,
                                                checkedTrackColor = EmeraldPrimary
                                            )
                                        )
                                    }
                                    if (i < sortedProviders.size - 1) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(horizontal = 16.dp),
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    val groupedByCategory = allProviders.groupBy { it.category }
                    val sortedCategories = groupedByCategory.keys.sorted().filter {
                        categoryFilter == null || it.equals(categoryFilter, ignoreCase = true)
                    }
                    
                    for (categoryName in sortedCategories) {
                        val providersInCat = groupedByCategory[categoryName] ?: emptyList()
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 24.dp, bottom = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = categoryName,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                        }
                        
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                        ) {
                            val sortedProviders = providersInCat.sortedBy { it.publisherName }
                            for (i in sortedProviders.indices) {
                                val provider = sortedProviders[i]
                                val isProviderActiveRegion = provider.region == activeRegion
                                val compositeKey = "${provider.region}|${provider.category}|${provider.url}"
                                val isEnabled = if (isProviderActiveRegion) {
                                    !disabledUrls.contains(compositeKey)
                                } else {
                                    enabledCrossRegion.contains(compositeKey)
                                }
                                
                                val regionNameText = when (provider.region) {
                                    "US" -> "🇺🇸 United States"
                                    "GB" -> "🇬🇧 United Kingdom"
                                    "SG" -> "🇸🇬 Singapore"
                                    "HK" -> "🇭🇰 Hong Kong"
                                    "IN" -> "🇮🇳 India"
                                    "CA" -> "🇨🇦 Canada"
                                    "AU" -> "🇦🇺 Australia"
                                    "FR" -> "🇫🇷 France"
                                    "DE" -> "🇩🇪 Germany"
                                    else -> provider.region
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (isProviderActiveRegion) {
                                                val isCurrentlyEnabled = !disabledUrls.contains(compositeKey)
                                                val newDisabled = if (isCurrentlyEnabled) {
                                                    disabledUrls + compositeKey
                                                } else {
                                                    disabledUrls - compositeKey
                                                }
                                                disabledUrls = newDisabled
                                                viewModel.prefs.disabledFeedUrls = newDisabled
                                                if (!isCurrentlyEnabled) {
                                                    viewModel.fetchSingleFeed(provider.url, provider.category)
                                                }
                                            } else {
                                                val isCurrentlyEnabled = enabledCrossRegion.contains(compositeKey)
                                                val newEnabled = if (isCurrentlyEnabled) {
                                                    enabledCrossRegion - compositeKey
                                                } else {
                                                    enabledCrossRegion + compositeKey
                                                }
                                                enabledCrossRegion = newEnabled
                                                viewModel.prefs.enabledCrossRegionFeeds = newEnabled
                                                if (!isCurrentlyEnabled) {
                                                    viewModel.fetchSingleFeed(provider.url, provider.category)
                                                }
                                            }
                                            viewModel.triggerPrefsChanged()
                                        }
                                        .graphicsLayer {
                                            alpha = if (isEnabled) 1f else 0.6f
                                        }
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = provider.publisherName,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = regionNameText,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }
                                    Switch(
                                        checked = isEnabled,
                                        onCheckedChange = { checked ->
                                            if (isProviderActiveRegion) {
                                                val newDisabled = if (checked) {
                                                    disabledUrls - compositeKey
                                                } else {
                                                    disabledUrls + compositeKey
                                                }
                                                disabledUrls = newDisabled
                                                viewModel.prefs.disabledFeedUrls = newDisabled
                                                if (checked) {
                                                    viewModel.fetchSingleFeed(provider.url, provider.category)
                                                }
                                            } else {
                                                val newEnabled = if (checked) {
                                                    enabledCrossRegion + compositeKey
                                                } else {
                                                    enabledCrossRegion - compositeKey
                                                }
                                                enabledCrossRegion = newEnabled
                                                viewModel.prefs.enabledCrossRegionFeeds = newEnabled
                                                if (checked) {
                                                    viewModel.fetchSingleFeed(provider.url, provider.category)
                                                }
                                            }
                                            viewModel.triggerPrefsChanged()
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = EmeraldPrimary
                                        )
                                    )
                                }
                                if (i < sortedProviders.size - 1) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
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

// ─── Sub-page: Custom RSS Feeds ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsFeedsScreen(navController: NavController, viewModel: NewsViewModel) {
    val customFeeds by viewModel.customFeeds.collectAsState()

    var feedTitle by remember { mutableStateOf("") }
    var feedUrl by remember { mutableStateOf("") }
    var feedCategory by remember { mutableStateOf("") }

    var feedToDelete by remember { mutableStateOf<CustomFeed?>(null) }
    var selectedListCategory by remember { mutableStateOf("All") }

    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val bgBrush = if (isDark) DarkGradient else LightGradient

    // Delete Confirmation Dialog
    if (feedToDelete != null) {
        ModalBottomSheet(
            onDismissRequest = { feedToDelete = null },
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
                        text = "Delete Feed",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Are you sure you want to unsubscribe from \"${feedToDelete?.title}\"?",
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
                        onClick = { feedToDelete = null },
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
                            feedToDelete?.let { viewModel.removeCustomRssFeed(it) }
                            feedToDelete = null
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Delete", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
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
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 120.dp)
        ) {
            SubPageTopBar(title = "Custom RSS Feeds", onBack = { navController.popBackStack() })

            SettingsSubHeader(
                title = "Custom RSS Feeds",
                description = "Add private RSS publishers, custom URLs, and subscribe to independent publications and specialized topics.",
                icon = Icons.Default.RssFeed
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Add Custom Feed form card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                        RoundedCornerShape(20.dp)
                    )
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFF59E0B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Add New Feed",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = feedTitle,
                    onValueChange = { feedTitle = it },
                    label = { Text("Feed Name  e.g. Wired Tech") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldPrimary)
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = feedUrl,
                    onValueChange = { feedUrl = it },
                    label = { Text("RSS Feed URL") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldPrimary)
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = feedCategory,
                    onValueChange = { feedCategory = it },
                    label = { Text("Tab Label  e.g. Tech, Finance") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldPrimary)
                )

                Spacer(modifier = Modifier.height(20.dp))

                val canAdd = feedTitle.isNotBlank() && feedUrl.isNotBlank() && feedCategory.isNotBlank()
                Button(
                    onClick = {
                        if (canAdd) {
                            var formattedUrl = feedUrl.trim()
                            if (!formattedUrl.startsWith("http://") && !formattedUrl.startsWith("https://")) {
                                formattedUrl = "https://$formattedUrl"
                            }
                            viewModel.addCustomRssFeed(
                                title = feedTitle.trim(),
                                url = formattedUrl,
                                category = feedCategory.trim()
                            )
                            feedTitle = ""; feedUrl = ""; feedCategory = ""
                        }
                    },
                    enabled = canAdd,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldPrimary,
                        contentColor = Color.White
                    )
                ) {
                    Text("Add Feed", fontWeight = FontWeight.Bold)
                }
            }

            // Custom feeds list
            if (customFeeds.isNotEmpty()) {
                Spacer(modifier = Modifier.height(28.dp))
                SectionLabel("Active Feeds  ·  ${customFeeds.size}")
                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable category filter chips
                val categories = remember(customFeeds) {
                    listOf("All") + customFeeds.map { it.category }.distinct().sorted()
                }
                
                if (!categories.contains(selectedListCategory)) {
                    selectedListCategory = "All"
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { category ->
                        val isSelected = category == selectedListCategory
                        Surface(
                            modifier = Modifier.clickable { selectedListCategory = category },
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
                            border = BorderStroke(1.dp, if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                        ) {
                            Text(
                                text = category,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                val filteredFeeds = remember(customFeeds, selectedListCategory) {
                    if (selectedListCategory == "All") customFeeds
                    else customFeeds.filter { it.category == selectedListCategory }
                }

                SettingsCard {
                    filteredFeeds.forEachIndexed { index, feed ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(EmeraldPrimary.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.RssFeed, null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(feed.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                    Text(
                                        feed.url,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = EmeraldPrimary.copy(alpha = 0.1f),
                                        border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.15f))
                                    ) {
                                        Text(
                                            text = feed.category,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldPrimary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            IconButton(onClick = { feedToDelete = feed }) {
                                Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                        if (index < filteredFeeds.lastIndex) CardDivider()
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(28.dp))
                InfoNote("No custom feeds yet. Add one above to create custom category tabs on the home screen.")
            }
        }
    }
}


// ─── Shared Components ───────────────────────────────────────────────────────

@Composable
private fun SubPageTopBar(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = title,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f), RoundedCornerShape(20.dp))
    ) {
        content()
    }
}

@Composable
private fun CardDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 18.dp),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    iconBg: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun InfoNote(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun CategoryToggleChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(1.dp, if (isSelected) EmeraldPrimary else Color.Transparent, RoundedCornerShape(12.dp))
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
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
        )
        if (isSelected) {
            Icon(Icons.Default.CheckCircle, null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun SettingsSelectorField(
    label: String,
    value: String,
    icon: ImageVector,
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
                        tint = EmeraldPrimary,
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
fun SettingsSubHeader(title: String, description: String, icon: ImageVector) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun SettingsAiScreen(navController: NavController, viewModel: NewsViewModel) {
    val isAiEnabled by viewModel.isAiEnabled.collectAsState()
    val isTranslationEnabled by viewModel.isTranslationEnabled.collectAsState()
    val isSummaryEnabled by viewModel.isSummaryEnabled.collectAsState()
    val isSmartTagsEnabled by viewModel.isSmartTagsEnabled.collectAsState()
    val isGeminiSupported by viewModel.isGeminiSupported.collectAsState()

    val targetLang by viewModel.translationTargetLanguage.collectAsState()
    var showLangDropdown by remember { mutableStateOf(false) }

    val languagesMap = mapOf(
        "en" to "🇬🇧 English",
        "es" to "🇪🇸 Español",
        "fr" to "🇫🇷 Français",
        "de" to "🇩🇪 Deutsch",
        "hi" to "🇮🇳 हिन्दी",
        "zh" to "🇨🇳 中文"
    )

    val isDark = isSystemInDarkTheme()
    val bgBrush = if (isDark) DarkGradient else LightGradient

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
            SubPageTopBar(title = "AI Services", onBack = { navController.popBackStack() })

            SettingsSubHeader(
                title = "AI Services",
                description = "Unlock offline machine learning translation, smart categorization hashtags, and localized private summarization powered by Gemini Nano.",
                icon = Icons.Default.AutoAwesome
            )

            SettingsCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(EmeraldPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Enable On-Device AI",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Run private models locally on this device",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                            )
                        }
                    }
                    Switch(
                        checked = isAiEnabled,
                        onCheckedChange = { viewModel.setAiEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = EmeraldPrimary
                        )
                    )
                }

                if (isAiEnabled) {
                    CardDivider()

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF3B82F6).copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Translate,
                                        contentDescription = null,
                                        tint = Color(0xFF3B82F6),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Offline AI Translation",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Translate foreign news feeds automatically",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                            }
                            Switch(
                                checked = isTranslationEnabled,
                                onCheckedChange = { viewModel.setTranslationEnabled(it) },
                                modifier = Modifier.scale(0.85f),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = EmeraldPrimary
                                )
                            )
                        }

                        if (isTranslationEnabled) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                    .clickable { showLangDropdown = true }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Target Language",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = languagesMap[targetLang] ?: "English",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldPrimary
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF3B82F6).copy(alpha = 0.08f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = Color(0xFF3B82F6),
                                        modifier = Modifier
                                            .size(16.dp)
                                            .padding(top = 1.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "First-time translation downloads a language pack of about 30MB and needs an internet connection. After download, translations run offline on-device.",
                                        fontSize = 11.sp,
                                        lineHeight = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showLangDropdown,
                                onDismissRequest = { showLangDropdown = false },
                                modifier = Modifier.fillMaxWidth(0.8f).background(MaterialTheme.colorScheme.surface)
                            ) {
                                languagesMap.forEach { (code, label) ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            viewModel.setTranslationTargetLanguage(code)
                                            viewModel.triggerBackgroundAiPreDownload()
                                            showLangDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    CardDivider()

                    val isGeminiAvail = isGeminiSupported != false
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isGeminiAvail) Color(0xFF10B981).copy(alpha = 0.15f)
                                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = if (isGeminiAvail) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "AI Key Insights Generator",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isGeminiAvail) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                )
                                Text(
                                    text = if (isGeminiAvail) {
                                        "Provide 3 key takeaways of parsed articles"
                                    } else {
                                        "Requires Gemini Nano; unsupported devices still show the regular article content without AI insights"
                                    },
                                    fontSize = 11.sp,
                                    color = if (isGeminiAvail) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                           else MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                                )
                            }
                        }
                        Switch(
                            checked = isSummaryEnabled && isGeminiAvail,
                            onCheckedChange = { if (isGeminiAvail) viewModel.setSummaryEnabled(it) },
                            enabled = isGeminiAvail,
                            modifier = Modifier.scale(0.85f),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = EmeraldPrimary
                            )
                        )
                    }



                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isGeminiAvail) Color(0xFF8B5CF6).copy(alpha = 0.15f)
                                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Category,
                                    contentDescription = null,
                                    tint = if (isGeminiAvail) Color(0xFF8B5CF6) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Smart Categories & Tags",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (isGeminiAvail) {
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
                                    text = if (isGeminiAvail) {
                                        "Automatically suggest tags dynamically with Gemini enhancement"
                                    } else {
                                        "Uses basic keyword tagging when Gemini Nano is unavailable"
                                    },
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isGeminiAvail) {
                                        "Tags are experimental and may occasionally be inaccurate."
                                    } else {
                                        "Fallback tagging is more basic and may be less precise."
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFD97706)
                                )
                            }
                        }
                        Switch(
                            checked = isSmartTagsEnabled,
                            onCheckedChange = { viewModel.setSmartTagsEnabled(it) },
                            enabled = true,
                            modifier = Modifier.scale(0.85f),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = EmeraldPrimary
                            )
                        )
                    }
                }
            }
        }
    }
}

// ─── Sub-page: Swipe Gestures ───────────────────────────────────────────
@Composable
fun SettingsGesturesScreen(navController: NavController, viewModel: NewsViewModel) {
    val swipeLeftAction by viewModel.swipeLeftAction.collectAsState()
    val swipeRightAction by viewModel.swipeRightAction.collectAsState()

    val options = mapOf(
        "bookmark" to "📌 Toggle Bookmark",
        "share" to "📤 Share Article",
        "read" to "📖 Toggle Read Status",
        "none" to "❌ None / Disable"
    )

    val isDark = isSystemInDarkTheme()
    val bgBrush = if (isDark) DarkGradient else LightGradient

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
            SubPageTopBar(title = "Swipe Gestures", onBack = { navController.popBackStack() })

            SettingsSubHeader(
                title = "Gesture Actions",
                description = "Configure the actions executed when you swipe articles to the left or to the right on your feed listings.",
                icon = Icons.Default.Swipe
            )

            SettingsSelectorField(
                label = "Swipe Left Action",
                icon = Icons.Default.KeyboardArrowLeft,
                value = options[swipeLeftAction] ?: "Toggle Bookmark",
                options = options,
                onSelected = {
                    viewModel.setSwipeLeftAction(it)
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            SettingsSelectorField(
                label = "Swipe Right Action",
                icon = Icons.Default.KeyboardArrowRight,
                value = options[swipeRightAction] ?: "Share",
                options = options,
                onSelected = {
                    viewModel.setSwipeRightAction(it)
                }
            )

            Spacer(modifier = Modifier.height(28.dp))
            InfoNote("Swiping actions are responsive and trigger instantly with subtle haptic feedback.")
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SettingsBackupScreen(navController: NavController, viewModel: NewsViewModel) {
    val customFeeds by viewModel.customFeeds.collectAsState()
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val bgBrush = if (isDark) DarkGradient else LightGradient

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
                        Toast.makeText(context, "Invalid backup file.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to read backup: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun exportSubscriptions(feeds: List<CustomFeed>) {
        if (feeds.isEmpty()) {
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
            context.startActivity(Intent.createChooser(shareIntent, "Export OPML Subscriptions"))
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to export OPML: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    fun exportFullBackup(feeds: List<CustomFeed>) {
        try {
            val backupJson = uk.sume.streamfolio.util.BackupHelper.generateBackupJson(context, feeds)
            val tempFile = File(context.cacheDir, "streamfolio_backup.json")
            tempFile.writeText(backupJson)
            
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, tempFile)
            
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Export StreamFolio Backup"))
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to export backup: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    // OPML Import Confirmation Bottom Sheet
    if (parsedFeedsToImport != null) {
        val feeds = remember(parsedFeedsToImport, customFeeds) {
            val existingUrls = customFeeds.map { it.url.trim().lowercase() }.toSet()
            parsedFeedsToImport!!.filter { opmlFeed ->
                var formattedUrl = opmlFeed.xmlUrl.trim()
                if (!formattedUrl.startsWith("http://") && !formattedUrl.startsWith("https://")) {
                    formattedUrl = "https://$formattedUrl"
                }
                !existingUrls.contains(formattedUrl.lowercase())
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
                                                colors = CheckboxDefaults.colors(checkedColor = EmeraldPrimary)
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
                                            color = EmeraldPrimary
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
                                                            .background(EmeraldPrimary)
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
                                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldPrimary)
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
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White),
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
        ModalBottomSheet(
            onDismissRequest = { fullBackupJsonToRestore = null },
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
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFEF5350),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Restore Settings",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "This will overwrite all of your current custom RSS feeds, swipe action choices, categories, and translation preferences. Are you sure you want to proceed?",
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
                        onClick = { fullBackupJsonToRestore = null },
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
                            viewModel.restoreSettingsBackup(
                                backupJson = backupJson,
                                onSuccess = {
                                    Toast.makeText(context, "Backup restored successfully!", Toast.LENGTH_SHORT).show()
                                },
                                onError = { e ->
                                    Toast.makeText(context, "Restore failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                }
                            )
                            fullBackupJsonToRestore = null
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Restore", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
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

            SettingsSubHeader(
                title = "Backup & Restore",
                description = "Export and import your custom RSS subscriptions, or create a full application settings backup to restore StreamFolio on another device.",
                icon = Icons.Default.Backup
            )

            Spacer(modifier = Modifier.height(28.dp))
            SectionLabel("Full Settings Backup")
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Import Settings Backup
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { backupPickerLauncher.launch("*/*") },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(EmeraldPrimary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SettingsBackupRestore,
                                contentDescription = "Import Backup",
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Import Settings",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Restore app preferences",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Export Settings Backup
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { exportFullBackup(customFeeds) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(EmeraldPrimary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = "Export Backup",
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Export Settings",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Backup preferences & feeds",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(36.dp))
            SectionLabel("OPML Feed Outline")
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Import OPML
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { opmlPickerLauncher.launch("*/*") },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(EmeraldPrimary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = "Import",
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Import OPML",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Upload subscription list",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Export OPML
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { exportSubscriptions(customFeeds) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(EmeraldPrimary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = "Export",
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Export OPML",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Save subscription list",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            InfoNote("Full Settings Backups generate a standard text JSON file that securely restores feeds, category orders, UI gestures, and translation choices.")
        }
    }
}

