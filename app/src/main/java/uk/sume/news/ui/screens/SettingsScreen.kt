package uk.sume.news.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import uk.sume.news.data.model.CustomFeed
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBrush)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 120.dp)
        ) {
            
            // Header Title
            Text(
                text = "Settings",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 48.dp, bottom = 16.dp)
            )

            // Section 1: Custom Region & Language
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
                    text = "Preferences",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))

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

            Spacer(modifier = Modifier.height(24.dp))

            // Section 2: Custom RSS Feed Adder
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
                    text = "Custom RSS Feeds",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))

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
                            viewModel.addCustomRssFeed(
                                title = feedTitle.trim(),
                                url = feedUrl.trim(),
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

            // Section 3: List of Custom Feeds
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
                        text = "Active Feeds",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))

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
