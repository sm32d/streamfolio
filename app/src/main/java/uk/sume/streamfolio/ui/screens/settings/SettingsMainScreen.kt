package uk.sume.streamfolio.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import uk.sume.streamfolio.ui.components.CardDivider
import uk.sume.streamfolio.ui.components.SectionLabel
import uk.sume.streamfolio.ui.components.SettingsCard
import uk.sume.streamfolio.ui.components.SettingsRow
import uk.sume.streamfolio.ui.components.StreamFolioRadioCard
import uk.sume.streamfolio.ui.theme.getThemeBackgroundBrush
import uk.sume.streamfolio.ui.viewmodel.NewsViewModel
import java.net.URLEncoder

@Composable
fun SettingsMainScreen(navController: NavController, viewModel: NewsViewModel) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val bgBrush = getThemeBackgroundBrush()

    var versionTapCount by remember { mutableIntStateOf(0) }
    var lastVersionTapTime by remember { mutableLongStateOf(0L) }
    var showDjEasterEgg by remember { mutableStateOf(false) }
    var djBroadcastText by remember { mutableStateOf("") }
    var currentBroadcastArticleUrl by remember { mutableStateOf<String?>(null) }
    val articles by viewModel.articles.collectAsState()
    val isTtsPlaying by viewModel.ttsHelper.isPlaying.collectAsState()

    val spinNextDjHeadline = remember(articles) {
        {
            val intros = listOf(
                "Now spinning on 99.9 FM: ",
                "Coming up next on StreamFolio FM: ",
                "This just in: ",
                "In the headlines right now: ",
                "We've got a big one for you: ",
                "Here's what's trending: ",
                "Your next story: ",
                "Breaking news coming your way: ",
                "Let's get into this one: ",
                "This one's a big deal: "
            )
            val attributions = listOf(
                "From %s.",
                "Courtesy of %s.",
                "Straight from %s.",
                "Reported by %s.",
                "via %s."
            )
            val transitions = listOf(
                "Alright, let's get into this...",
                "Moving on...",
                "And now for something different...",
                "In other news...",
                "Stay with us for this one...",
                "You won't want to miss this..."
            )
            val quips = listOf(
                "You won't want to miss this one.",
                "This is a big deal.",
                "Stay locked in.",
                "This one's got people talking.",
                "A lot of buzz around this one."
            )

            val validArticles = articles.filter { it.title.isNotBlank() }
            if (validArticles.isNotEmpty()) {
                val randomArt = validArticles.random()
                val useIntro = (0..9).random() < 7
                val useAttribution = (0..9).random() < 6
                val useTransition = (0..9).random() < 2
                val useQuip = !useTransition && (0..9).random() < 2

                val parts = mutableListOf<String>()
                if (useTransition) parts.add(transitions.random())
                if (useIntro) parts.add(intros.random())
                parts.add(randomArt.title)
                if (useAttribution) parts.add(attributions.random().format(randomArt.sourceName))
                else if (useQuip) parts.add(quips.random())

                val textToSay = parts.joinToString(" ")
                djBroadcastText = textToSay
                currentBroadcastArticleUrl = randomArt.link
                viewModel.ttsHelper.speakDjBroadcast(textToSay)
            } else {
                val fallbackText = "Spinning hot news tracks! StreamFolio is operating at peak frequency!"
                djBroadcastText = fallbackText
                currentBroadcastArticleUrl = null
                viewModel.ttsHelper.speakDjBroadcast(fallbackText)
            }
        }
    }

    DisposableEffect(showDjEasterEgg) {
        if (showDjEasterEgg) {
            viewModel.ttsHelper.onDjUtteranceCompleted = {
                spinNextDjHeadline()
            }
        } else {
            viewModel.ttsHelper.onDjUtteranceCompleted = null
        }
        onDispose {
            viewModel.ttsHelper.onDjUtteranceCompleted = null
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
                .then(
                    if (showDjEasterEgg && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                        Modifier.blur(16.dp)
                    } else {
                        Modifier
                    }
                )
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
                    iconBg = MaterialTheme.colorScheme.primary,
                    title = "Preferences",
                    subtitle = "Language & region for your news feed",
                    onClick = { navController.navigate("settings_preferences") }
                )
                CardDivider()
                SettingsRow(
                    icon = Icons.Default.Swipe,
                    iconBg = MaterialTheme.colorScheme.secondary,
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
                    iconBg = MaterialTheme.colorScheme.tertiary,
                    title = "Manage Content & Sources",
                    subtitle = "Customise topics and curation channels",
                    onClick = { navController.navigate("settings_categories") }
                )
                CardDivider()
                SettingsRow(
                    icon = Icons.Default.RssFeed,
                    iconBg = MaterialTheme.colorScheme.primary,
                    title = "Custom RSS Feeds",
                    subtitle = "Add and manage private RSS publishers",
                    onClick = { navController.navigate("settings_feeds") }
                )
                CardDivider()
                SettingsRow(
                    icon = Icons.Default.Backup,
                    iconBg = MaterialTheme.colorScheme.secondary,
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
                    iconBg = MaterialTheme.colorScheme.tertiary,
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
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            val now = System.currentTimeMillis()
                            if (now - lastVersionTapTime < 600) {
                                versionTapCount++
                            } else {
                                versionTapCount = 1
                            }
                            lastVersionTapTime = now
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                            if (versionTapCount == 3) {
                                viewModel.showInfoToast("2 more taps for StreamFolio FM...", Icons.Default.Radio)
                            } else if (versionTapCount == 4) {
                                viewModel.showInfoToast("1 more tap for StreamFolio FM!", Icons.Default.Radio)
                            } else if (versionTapCount >= 5) {
                                versionTapCount = 0
                                showDjEasterEgg = true
                                currentBroadcastArticleUrl = null
                                val introLine = "Yo! You're locked into StreamFolio FM 99.9! All the top headlines, zero noise. Stay tuned!"
                                djBroadcastText = introLine
                                viewModel.ttsHelper.speakDjBroadcast(introLine)
                            }
                        }
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))
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
                            text = "Version 1.3.0 · Free forever",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                        )
                    }
                }
                CardDivider()
                SettingsRow(
                    icon = Icons.Default.Email,
                    iconBg = MaterialTheme.colorScheme.primary,
                    title = "Contact Us",
                    subtitle = "sfolio@sume.uk",
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:sfolio@sume.uk")
                                putExtra(Intent.EXTRA_SUBJECT, "StreamFolio Feedback")
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            viewModel.showInfoToast("Could not open mail app", Icons.Default.Email)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(28.dp))
            SectionLabel("More from Dominik Studios")
            Spacer(modifier = Modifier.height(10.dp))

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

        // Dim Scrim Backdrop with Tap-to-Dismiss
        AnimatedVisibility(
            visible = showDjEasterEgg,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        viewModel.ttsHelper.onDjUtteranceCompleted = null
                        viewModel.ttsHelper.stop()
                        showDjEasterEgg = false
                        currentBroadcastArticleUrl = null
                    }
            )
        }

        AnimatedVisibility(
            visible = showDjEasterEgg,
            enter = fadeIn(animationSpec = tween(300)) + slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(400)
            ),
            exit = fadeOut(animationSpec = tween(300)) + slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(400)
            ),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 92.dp)
        ) {
            StreamFolioRadioCard(
                activeBroadcastText = djBroadcastText,
                isPlaying = isTtsPlaying,
                currentArticleUrl = currentBroadcastArticleUrl,
                onOpenArticle = {
                    currentBroadcastArticleUrl?.let { url ->
                        viewModel.ttsHelper.onDjUtteranceCompleted = null
                        viewModel.ttsHelper.stop()
                        showDjEasterEgg = false
                        val encodedUrl = URLEncoder.encode(url, "UTF-8")
                        navController.navigate("detail_screen/$encodedUrl")
                    }
                },
                onTuneOff = {
                    viewModel.ttsHelper.onDjUtteranceCompleted = null
                    viewModel.ttsHelper.stop()
                    showDjEasterEgg = false
                }
            )
        }
    }
}
