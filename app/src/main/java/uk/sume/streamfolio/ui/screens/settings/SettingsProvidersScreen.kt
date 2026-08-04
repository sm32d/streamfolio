package uk.sume.streamfolio.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import uk.sume.streamfolio.data.network.CuratedProvider
import uk.sume.streamfolio.data.network.DefaultFeedsConfig
import uk.sume.streamfolio.ui.components.HapticSwitch
import uk.sume.streamfolio.ui.components.RegionUtils
import uk.sume.streamfolio.ui.components.SubPageTopBar
import uk.sume.streamfolio.ui.theme.getThemeBackgroundBrush
import uk.sume.streamfolio.ui.viewmodel.NewsViewModel

@Composable
fun SettingsProvidersScreen(navController: NavController, viewModel: NewsViewModel) {
    val bgBrush = getThemeBackgroundBrush()
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
                else -> RegionUtils.getRegionInfo(r1).name.compareTo(RegionUtils.getRegionInfo(r2).name)
            }
        })
    }
    
    // Smart navigation filters
    var categoryFilter by remember { mutableStateOf(viewModel.filterCategoryOnSettings) }
    
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
                                imageVector = Icons.Default.Widgets,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Default Feed Publishers",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Customize active news sources per region & category",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Segmented-style toggle button row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { groupByCategory = false }
                        .background(if (!groupByCategory) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Group by Region",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (!groupByCategory) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { groupByCategory = true }
                        .background(if (groupByCategory) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Group by Category",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (groupByCategory) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            // 🎯 Quick Filter Horizontal Pill Strip
            var regionFilter by remember { mutableStateOf<String?>(null) }
            val availableCategoriesList = remember {
                listOf("Top Stories", "World", "Business", "Technology", "Science", "Sports", "Health", "Entertainment")
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!groupByCategory) {
                    // Region Quick Filter Pills
                    val isAllSelected = regionFilter == null
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isAllSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, if (isAllSelected) MaterialTheme.colorScheme.primary else Color.Transparent),
                        modifier = Modifier.clickable { regionFilter = null }
                    ) {
                        Text(
                            text = "ALL REGIONS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isAllSelected) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }

                    sortedRegions.forEach { rCode ->
                        val isSelected = regionFilter == rCode
                        val rName = RegionUtils.getRegionInfo(rCode).name
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent),
                            modifier = Modifier.clickable { regionFilter = if (isSelected) null else rCode }
                        ) {
                            Text(
                                text = rName.uppercase(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                } else {
                    // Category Quick Filter Pills with Vector Icons
                    val isAllSelected = categoryFilter == null
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isAllSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, if (isAllSelected) MaterialTheme.colorScheme.primary else Color.Transparent),
                        modifier = Modifier.clickable { categoryFilter = null }
                    ) {
                        Text(
                            text = "ALL CATEGORIES",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isAllSelected) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }

                    availableCategoriesList.forEach { catName ->
                        val isSelected = categoryFilter?.equals(catName, ignoreCase = true) == true
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent),
                            modifier = Modifier.clickable { categoryFilter = if (isSelected) null else catName }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = getCategoryIcon(catName),
                                    contentDescription = null,
                                    tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = catName.uppercase(),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                if (!groupByCategory) {
                    val activeSortedRegions = sortedRegions.filter { regionFilter == null || it == regionFilter }
                    activeSortedRegions.forEach { regionCode ->
                        val providersInRegion = (groupedProviders[regionCode] ?: emptyList()).filter {
                            categoryFilter == null || it.category.equals(categoryFilter, ignoreCase = true)
                        }
                        val isActiveRegion = regionCode == activeRegion
                        
                        if (providersInRegion.isNotEmpty()) {
                            val regionName = RegionUtils.getFormattedRegionName(regionCode)
                            
                            item(key = "region_header_$regionCode") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 20.dp, bottom = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = regionName,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isActiveRegion) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                        border = BorderStroke(1.dp, if (isActiveRegion) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                                    ) {
                                          Text(
                                              text = if (isActiveRegion) "Active Region" else "Inactive",
                                              fontSize = 11.sp,
                                              fontWeight = FontWeight.SemiBold,
                                              color = if (isActiveRegion) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                          )
                                    }
                                }
                            }
                            
                            item(key = "region_card_$regionCode") {
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
                                                .padding(horizontal = 16.dp, vertical = 12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = provider.publisherName.take(1).uppercase(),
                                                        fontSize = 17.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }

                                                Spacer(modifier = Modifier.width(14.dp))

                                                Column {
                                                    Text(
                                                        text = provider.publisherName,
                                                        fontSize = 15.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(
                                                            imageVector = getCategoryIcon(provider.category),
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = provider.category,
                                                            fontSize = 12.sp,
                                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                                        )
                                                    }
                                                }
                                            }

                                            HapticSwitch(
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
                                                    checkedTrackColor = MaterialTheme.colorScheme.primary
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
                } else {
                    val groupedByCategory = allProviders.groupBy { it.category }
                    val sortedCategories = groupedByCategory.keys.sorted().filter {
                        categoryFilter == null || it.equals(categoryFilter, ignoreCase = true)
                    }
                    
                    sortedCategories.forEach { categoryName ->
                        val providersInCat = (groupedByCategory[categoryName] ?: emptyList()).filter {
                            regionFilter == null || it.region == regionFilter
                        }

                        if (providersInCat.isNotEmpty()) {
                            item(key = "cat_header_$categoryName") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 20.dp, bottom = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = getCategoryIcon(categoryName),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = categoryName,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            
                            item(key = "cat_card_$categoryName") {
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
                                        
                                        val regionNameText = RegionUtils.getFormattedRegionName(provider.region)

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
                                                .padding(horizontal = 16.dp, vertical = 12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = provider.publisherName.take(1).uppercase(),
                                                        fontSize = 17.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }

                                                Spacer(modifier = Modifier.width(14.dp))

                                                Column {
                                                    Text(
                                                        text = provider.publisherName,
                                                        fontSize = 15.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = regionNameText,
                                                        fontSize = 12.sp,
                                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                                    )
                                                }
                                            }

                                            HapticSwitch(
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
                                                    checkedTrackColor = MaterialTheme.colorScheme.primary
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
