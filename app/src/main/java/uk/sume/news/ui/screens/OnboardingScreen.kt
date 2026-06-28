package uk.sume.news.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import uk.sume.news.ui.theme.DarkGradient
import uk.sume.news.ui.theme.LightGradient
import uk.sume.news.ui.viewmodel.NewsViewModel
import java.util.Locale

@Composable
fun OnboardingScreen(navController: NavController, viewModel: NewsViewModel) {
    val systemLocale = Locale.getDefault()
    
    // Auto-detect default language & region codes
    val defaultLang = systemLocale.language.takeIf { it in listOf("en", "es", "fr", "de", "hi", "zh") } ?: "en"
    val defaultRegion = systemLocale.country.takeIf { it in listOf("US", "GB", "CA", "FR", "DE", "IN", "AU", "SG") } ?: "US"

    var selectedLang by remember { mutableStateOf(defaultLang) }
    var selectedRegion by remember { mutableStateOf(defaultRegion) }
    var isGoogleNewsEnabled by remember { mutableStateOf(true) }

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

    var isDark = isSystemInDarkTheme()
    val bgBrush = if (isDark) DarkGradient else LightGradient

    // Button pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "PulseTransition")
    val buttonScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ButtonScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBrush)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(32.dp))
                .verticalScroll(rememberScrollState())
                .padding(32.dp)
        ) {
            Text(
                text = "Welcome to PrimeFeed",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "Your personalized, modern, and free news reader helper.",
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Auto-detection indicator alert
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    .padding(12.dp)
            ) {
                Text(
                    text = "✨ Detected system locale: ${systemLocale.displayLanguage} (${systemLocale.displayCountry})",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Language Selector
            SelectorField(
                label = "Feed Language",
                value = languages[selectedLang] ?: "English",
                icon = Icons.Default.Language,
                options = languages,
                onSelected = { selectedLang = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Region Selector
            SelectorField(
                label = "Feed Region / Country",
                value = regions[selectedRegion] ?: "United States",
                icon = Icons.Default.Place,
                options = regions,
                onSelected = { selectedRegion = it }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Google News Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .clickable { isGoogleNewsEnabled = !isGoogleNewsEnabled }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.RssFeed,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Enable Google News",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Include global topic RSS categories",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
                Switch(
                    checked = isGoogleNewsEnabled,
                    onCheckedChange = { isGoogleNewsEnabled = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            AnimatedVisibility(visible = isGoogleNewsEnabled) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(20.dp))

                    // Categories Selector
                    Text(
                        text = "Select Categories of Interest",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    val availableCategories = listOf("Top Stories", "Business", "Technology", "Science", "Sports", "Health", "Entertainment")
                    var selectedCats by remember { mutableStateOf(availableCategories.toSet()) }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        availableCategories.chunked(3).forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowItems.forEach { cat ->
                                    val isCatSelected = selectedCats.contains(cat)
                                    val chipBg = if (isCatSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    val chipColor = if (isCatSelected) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(chipBg)
                                            .clickable {
                                                selectedCats = if (isCatSelected) {
                                                    if (selectedCats.size > 1) selectedCats - cat else selectedCats
                                                } else {
                                                    selectedCats + cat
                                                }
                                            }
                                            .padding(vertical = 8.dp, horizontal = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = cat,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = chipColor,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                                if (rowItems.size < 3) {
                                    Spacer(modifier = Modifier.weight((3 - rowItems.size).toFloat()))
                                }
                            }
                        }
                    }

                    // Save helper state to trigger outside scope on start click
                    LaunchedEffect(selectedCats) {
                        viewModel.prefs.selectedCategories = selectedCats
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Start Button with micro-interactions
            Button(
                onClick = {
                    viewModel.updatePreferences(selectedLang, selectedRegion)
                    viewModel.prefs.isGoogleNewsEnabled = isGoogleNewsEnabled
                    viewModel.prefs.isCompletedOnboarding = true
                    viewModel.refreshCurrentFeed()
                    navController.navigate("home_screen") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .scale(buttonScale),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = "Start Reading",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

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
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
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
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            options.forEach { (code, name) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        onSelected(code)
                        expanded = false
                    }
                )
            }
        }
    }
}
