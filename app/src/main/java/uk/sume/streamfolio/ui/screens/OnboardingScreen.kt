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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Place
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import uk.sume.streamfolio.ui.theme.DarkGradient
import uk.sume.streamfolio.ui.theme.EmeraldPrimary
import uk.sume.streamfolio.ui.theme.EmeraldSecondary
import uk.sume.streamfolio.ui.theme.LightGradient
import uk.sume.streamfolio.ui.viewmodel.NewsViewModel
import java.util.Locale

/**
 * Premium full-screen onboarding wizard with step-by-step flow.
 * Design is consistent with the rest of the app's glassmorphic aesthetic.
 */
@Composable
fun OnboardingScreen(navController: NavController, viewModel: NewsViewModel) {
    val systemLocale = Locale.getDefault()
    val defaultLang = systemLocale.language.takeIf { it in listOf("en", "es", "fr", "de", "hi", "zh") } ?: "en"
    val defaultRegion = systemLocale.country.takeIf { it in listOf("US", "GB", "CA", "FR", "DE", "IN", "AU", "SG") } ?: "US"

    var selectedLang by rememberSaveable { mutableStateOf(defaultLang) }
    var selectedRegion by rememberSaveable { mutableStateOf(defaultRegion) }
    var isDefaultFeedsEnabled by rememberSaveable { mutableStateOf(true) }
    var selectedCats by rememberSaveable {
        mutableStateOf(setOf("Top Stories", "World", "Business", "Technology", "Science", "Sports", "Health", "Entertainment"))
    }

    var currentStep by rememberSaveable { mutableStateOf(0) }
    val totalSteps = 3

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
    val bgBrush = if (isDark) DarkGradient else LightGradient

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
                        1 -> LanguageRegionStep(
                            selectedLang = selectedLang,
                            onLangSelected = { selectedLang = it },
                            selectedRegion = selectedRegion,
                            onRegionSelected = { selectedRegion = it }
                        )
                        2 -> DefaultFeedsStep(
                            isEnabled = isDefaultFeedsEnabled,
                            onToggle = { isDefaultFeedsEnabled = it },
                            selectedCats = selectedCats,
                            onCatsChanged = { selectedCats = it }
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
                        viewModel.prefs.isDefaultFeedsEnabled = isDefaultFeedsEnabled
                        viewModel.prefs.selectedCategories = selectedCats
                        viewModel.prefs.isCompletedOnboarding = true
                        viewModel.refreshCurrentFeed()
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
                            isActive -> EmeraldPrimary
                            isPassed -> EmeraldPrimary.copy(alpha = 0.5f)
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
                containerColor = EmeraldPrimary
            )
        ) {
            Text(
                text = if (isFinalStep) "Start Reading →" else "Continue →",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

/** Step 0 – Welcome */
@Composable
private fun WelcomeStep(systemLocale: Locale) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        // App icon badge
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.linearGradient(
                        listOf(EmeraldPrimary, EmeraldSecondary)
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

        FeatureRow(emoji = "🌍", title = "Global Coverage", subtitle = "News from top sources worldwide")
        Spacer(modifier = Modifier.height(16.dp))
        FeatureRow(emoji = "📂", title = "Custom Feeds", subtitle = "Add your own RSS feeds")
        Spacer(modifier = Modifier.height(16.dp))
        FeatureRow(emoji = "🎙️", title = "Text-to-Speech", subtitle = "Listen to articles hands-free")
        Spacer(modifier = Modifier.height(16.dp))
        FeatureRow(emoji = "🔖", title = "Bookmarks", subtitle = "Save stories to read later")

        Spacer(modifier = Modifier.height(28.dp))

        // Locale detection chip
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "✨", fontSize = 14.sp)
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
private fun FeatureRow(emoji: String, title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = emoji, fontSize = 20.sp)
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

/** Step 1 – Language & Region */
@Composable
private fun LanguageRegionStep(
    selectedLang: String,
    onLangSelected: (String) -> Unit,
    selectedRegion: String,
    onRegionSelected: (String) -> Unit
) {
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
        "SG" to "🇸🇬 Singapore"
    )

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
        Text(
            text = "Language &\nRegion",
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 42.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Choose the language and country for your news feed.",
            fontSize = 15.sp,
            lineHeight = 22.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(36.dp))

        SelectorField(
            label = "Feed Language",
            icon = Icons.Default.Language,
            value = languages[selectedLang] ?: "English",
            options = languages,
            onSelected = onLangSelected
        )

        Spacer(modifier = Modifier.height(20.dp))

        SelectorField(
            label = "Region / Country",
            icon = Icons.Default.Place,
            value = regions[selectedRegion] ?: "United States",
            options = regions,
            onSelected = onRegionSelected
        )
    }
}

/** Step 2 – Default Curated Feeds & categories */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DefaultFeedsStep(
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    selectedCats: Set<String>,
    onCatsChanged: (Set<String>) -> Unit
) {
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

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
        Text(
            text = "Personalise\nYour Feed",
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 42.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Enable default curated categories and pick your interests.",
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
                        EmeraldPrimary.copy(alpha = 0.08f)
                    else
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
                .border(
                    1.dp,
                    if (isEnabled) EmeraldPrimary.copy(alpha = 0.3f) else Color.Transparent,
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
                            if (isEnabled) EmeraldPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
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
                    checkedTrackColor = EmeraldPrimary
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
    }
}

@Composable
private fun CategoryChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
            .border(
                1.dp,
                if (isSelected) EmeraldPrimary else Color.Transparent,
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
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
        )
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.85f),
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
