package uk.sume.news.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import uk.sume.news.ui.theme.DarkGradient
import uk.sume.news.ui.theme.LightGradient
import uk.sume.news.ui.viewmodel.NewsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController, viewModel: NewsViewModel) {
    val currentLang = viewModel.prefs.language
    val currentRegion = viewModel.prefs.region
    val customFeeds by viewModel.customFeeds.collectAsState()

    var selectedLang by remember { mutableStateOf(currentLang) }
    var selectedRegion by remember { mutableStateOf(currentRegion) }

    val languages = mapOf("en" to "English", "es" to "Español", "fr" to "Français", "de" to "Deutsch", "hi" to "हिन्दी", "zh" to "中文")
    val regions = mapOf(
        "US" to "United States",
        "GB" to "United Kingdom",
        "CA" to "Canada",
        "FR" to "France",
        "DE" to "Germany",
        "IN" to "India",
        "AU" to "Australia",
        "SG" to "Singapore"
    )

    // Custom Feed creation inputs
    var feedTitle by remember { mutableStateOf("") }
    var feedUrl by remember { mutableStateOf("") }
    var feedCategory by remember { mutableStateOf("") }

    val isDark = isSystemInDarkTheme()
    val bgBrush = if (isDark) DarkGradient else LightGradient

    // Active sub-page navigation state ("preferences", "categories", "feeds", or null for main menu)
    var activeSubPage by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = activeSubPage != null) {
        activeSubPage = null
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBrush)
    ) {
        AnimatedContent(
            targetState = activeSubPage,
            transitionSpec = {
                if (targetState != null) {
                    // Slide in sub-page from right to left
                    (slideInHorizontally { width -> width } + fadeIn()).togetherWith(slideOutHorizontally { width -> -width } + fadeOut())
                } else {
                    // Slide out sub-page from left to right (going back to main menu)
                    (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(slideOutHorizontally { width -> width } + fadeOut())
                }
            },
            label = "SettingsNavigation"
        ) { page ->
            when (page) {
                null -> {
                    // Main Settings Menu (System Settings Style)
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 120.dp)
                    ) {
                        // Header
                        Text(
                            text = "Settings",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .windowInsetsPadding(WindowInsets.statusBars)
                                .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 24.dp)
                        )

                        // Settings List Container
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                        ) {
                            // Menu 1: Preferences
                            SettingsMenuItem(
                                title = "Preferences",
                                subtitle = "Feed region, system language, etc.",
                                icon = Icons.Default.Tune,
                                onClick = { activeSubPage = "preferences" }
                            )
                            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                            // Menu 2: Topics & Categories
                            SettingsMenuItem(
                                title = "Topics & Categories",
                                subtitle = "Manage active Google News topics",
                                icon = Icons.Default.List,
                                onClick = { activeSubPage = "categories" }
                            )
                            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                            // Menu 3: Custom RSS Feeds
                            SettingsMenuItem(
                                title = "Custom RSS Feeds",
                                subtitle = "Manage private RSS feed publishers",
                                icon = Icons.Default.RssFeed,
                                onClick = { activeSubPage = "feeds" }
                            )
                        }
                    }
                }
                "preferences" -> {
                    // Sub-Page: Preferences
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 120.dp)
                    ) {
                        SubPageHeader(
                            title = "Preferences",
                            onBack = { activeSubPage = null }
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                                .padding(20.dp)
                        ) {
                            SelectorField(
                                label = "Language",
                                value = languages[selectedLang] ?: "English",
                                icon = Icons.Default.Language,
                                options = languages,
                                onSelected = {
                                    selectedLang = it
                                    viewModel.updatePreferences(it, selectedRegion)
                                }
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            SelectorField(
                                label = "Region",
                                value = regions[selectedRegion] ?: "United States",
                                icon = Icons.Default.Place,
                                options = regions,
                                onSelected = {
                                    selectedRegion = it
                                    viewModel.updatePreferences(selectedLang, it)
                                }
                            )
                        }
                    }
                }
                "categories" -> {
                    // Sub-Page: Categories
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 120.dp)
                    ) {
                        SubPageHeader(
                            title = "Topics & Categories",
                            onBack = { activeSubPage = null }
                        )

                        var isGoogleNewsEnabled by remember { mutableStateOf(viewModel.prefs.isGoogleNewsEnabled) }
                        val availableCategories = listOf("Top Stories", "Business", "Technology", "Science", "Sports", "Health", "Entertainment")
                        var selectedCats by remember { mutableStateOf(viewModel.prefs.selectedCategories) }

                        // Switch to completely toggle Google News
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                                .padding(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Enable Google News Feeds",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Toggle standard news categories globally",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                                Switch(
                                    checked = isGoogleNewsEnabled,
                                    onCheckedChange = { checked ->
                                        isGoogleNewsEnabled = checked
                                        viewModel.prefs.isGoogleNewsEnabled = checked
                                        viewModel.refreshCurrentFeed()
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }

                            AnimatedVisibility(visible = isGoogleNewsEnabled) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Spacer(modifier = Modifier.height(20.dp))
                                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                                    Spacer(modifier = Modifier.height(16.dp))

                                    Text(
                                        text = "Topics of Interest",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )

                                    availableCategories.forEach { cat ->
                                        val isCatSelected = selectedCats.contains(cat)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable {
                                                    val updated = if (isCatSelected) {
                                                        if (selectedCats.size > 1) selectedCats - cat else selectedCats
                                                    } else {
                                                        selectedCats + cat
                                                    }
                                                    selectedCats = updated
                                                    viewModel.prefs.selectedCategories = updated
                                                    viewModel.refreshCurrentFeed()
                                                }
                                                .padding(vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = cat,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Checkbox(
                                                checked = isCatSelected,
                                                onCheckedChange = { checked ->
                                                    val updated = if (!checked) {
                                                        if (selectedCats.size > 1) selectedCats - cat else selectedCats
                                                    } else {
                                                        selectedCats + cat
                                                    }
                                                    selectedCats = updated
                                                    viewModel.prefs.selectedCategories = updated
                                                    viewModel.refreshCurrentFeed()
                                                },
                                                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                "feeds" -> {
                    // Sub-Page: Custom RSS Feeds
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 120.dp)
                    ) {
                        SubPageHeader(
                            title = "Custom RSS Feeds",
                            onBack = { activeSubPage = null }
                        )

                        // Adder Card
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                                .padding(20.dp)
                        ) {
                            Text(
                                text = "Add New RSS Feed",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            OutlinedTextField(
                                value = feedTitle,
                                onValueChange = { feedTitle = it },
                                label = { Text("Feed Name (e.g. Wired Tech)") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = feedUrl,
                                onValueChange = { feedUrl = it },
                                label = { Text("RSS Feed URL") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = feedCategory,
                                onValueChange = { feedCategory = it },
                                label = { Text("Category (e.g. Tech, Finance)") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary
                                )
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = {
                                    if (feedTitle.isNotBlank() && feedUrl.isNotBlank() && feedCategory.isNotBlank()) {
                                        var formattedUrl = feedUrl.trim()
                                        if (!formattedUrl.startsWith("http://") && !formattedUrl.startsWith("https://")) {
                                            formattedUrl = "https://$formattedUrl"
                                        }
                                        viewModel.addCustomRssFeed(
                                            title = feedTitle.trim(),
                                            url = formattedUrl,
                                            category = feedCategory.trim()
                                        )
                                        feedTitle = ""
                                        feedUrl = ""
                                        feedCategory = ""
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text(text = "Add Custom Feed", fontWeight = FontWeight.Bold)
                            }
                        }

                        // Feeds List Card
                        if (customFeeds.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(24.dp))

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                                    .padding(20.dp)
                            ) {
                                Text(
                                    text = "Active Custom Feeds",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )

                                customFeeds.forEach { feed ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = feed.title,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = feed.url,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                                maxLines = 1
                                            )
                                        }
                                        IconButton(onClick = { viewModel.removeCustomRssFeed(feed) }) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = MaterialTheme.colorScheme.error
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
    }
}

@Composable
fun SettingsMenuItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun SubPageHeader(
    title: String,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(start = 12.dp, end = 24.dp, top = 16.dp, bottom = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
