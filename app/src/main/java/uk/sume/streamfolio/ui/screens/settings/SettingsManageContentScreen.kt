package uk.sume.streamfolio.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import uk.sume.streamfolio.ui.components.CardDivider
import uk.sume.streamfolio.ui.components.HapticSwitch
import uk.sume.streamfolio.ui.components.SettingsCard
import uk.sume.streamfolio.ui.components.SettingsRow
import uk.sume.streamfolio.ui.components.SubPageTopBar
import uk.sume.streamfolio.ui.theme.getThemeBackgroundBrush
import uk.sume.streamfolio.ui.viewmodel.NewsViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsManageContentScreen(navController: NavController, viewModel: NewsViewModel) {
    var isDefaultFeedsEnabled by remember { mutableStateOf(viewModel.prefs.isDefaultFeedsEnabled) }
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
    var selectedCats by remember { mutableStateOf(viewModel.prefs.selectedCategories) }

    val bgBrush = getThemeBackgroundBrush()

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
                                imageVector = Icons.Default.Category,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Feed Composition & Sources",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Configure default news categories & custom RSS curation",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section 1: Default Curated Feeds
            Text(
                text = "CURATED FEED CATEGORIES",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Default Feeds toggle card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        if (isDefaultFeedsEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                    .border(
                        1.dp,
                        if (isDefaultFeedsEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else Color.Transparent,
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
                                if (isDefaultFeedsEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
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
                HapticSwitch(
                    checked = isDefaultFeedsEnabled,
                    onCheckedChange = { checked ->
                        isDefaultFeedsEnabled = checked
                        viewModel.prefs.isDefaultFeedsEnabled = checked
                        viewModel.refreshCurrentFeed()
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            AnimatedVisibility(visible = isDefaultFeedsEnabled) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
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
                                icon = getCategoryIcon(label),
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
                        text = "${selectedCats.size} of ${availableCategories.size} categories active",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = "ADVANCED CURATION",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                modifier = Modifier.padding(bottom = 12.dp)
            )
            SettingsCard {
                SettingsRow(
                    icon = Icons.Default.Sort,
                    iconBg = MaterialTheme.colorScheme.secondary,
                    title = "Reorder Category Tabs",
                    subtitle = "Adjust the position and priority of your category tabs",
                    onClick = { navController.navigate("settings_reorder") }
                )
                CardDivider()
                SettingsRow(
                    icon = Icons.Default.Widgets,
                    iconBg = MaterialTheme.colorScheme.tertiary,
                    title = "Default Feed Providers",
                    subtitle = "Enable or disable curated news publishers",
                    onClick = { navController.navigate("settings_providers") }
                )
            }
        }
    }
}

@Composable
private fun CategoryToggleChip(
    label: String,
    isSelected: Boolean,
    icon: ImageVector? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
        )
        if (isSelected) {
            Icon(Icons.Default.CheckCircle, null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(14.dp))
        }
    }
}

private fun getCategoryIcon(category: String): ImageVector {
    return when (category.lowercase()) {
        "top stories" -> Icons.Default.Star
        "world" -> Icons.Default.Public
        "business" -> Icons.Default.BusinessCenter
        "technology", "tech" -> Icons.Default.Memory
        "science" -> Icons.Default.Science
        "sports" -> Icons.Default.SportsBasketball
        "health" -> Icons.Default.MedicalServices
        "entertainment" -> Icons.Default.Movie
        else -> Icons.Default.RssFeed
    }
}
