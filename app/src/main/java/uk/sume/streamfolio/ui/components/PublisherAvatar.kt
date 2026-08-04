package uk.sume.streamfolio.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage

@Composable
fun PublisherAvatar(
    sourceName: String,
    logoUrl: String? = null,
    url: String? = null,
    size: Dp = 40.dp,
    modifier: Modifier = Modifier
) {
    val initial = sourceName.trim().take(1).uppercase().ifEmpty { "N" }
    val effectiveLogoUrl = when {
        !logoUrl.isNullOrBlank() -> logoUrl
        !url.isNullOrBlank() || sourceName.isNotBlank() -> {
            val domain = extractPublisherDomain(sourceName, url ?: "")
            if (domain.isNotBlank() && domain != "google.com") {
                "https://www.google.com/s2/favicons?sz=64&domain=$domain"
            } else null
        }
        else -> null
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
            .border(
                1.dp,
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (!effectiveLogoUrl.isNullOrBlank()) {
            SubcomposeAsyncImage(
                model = effectiveLogoUrl,
                contentDescription = sourceName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                error = {
                    Text(
                        text = initial,
                        fontSize = (size.value * 0.42f).sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            )
        } else {
            Text(
                text = initial,
                fontSize = (size.value * 0.42f).sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

private fun extractPublisherDomain(sourceName: String, url: String): String {
    if (url.isNotBlank()) {
        try {
            val uri = Uri.parse(url)
            var host = uri.host
            if (!host.isNullOrEmpty() && !host.contains("google.com")) {
                if (host.startsWith("www.")) host = host.substring(4)
                return host
            }
        } catch (_: Exception) {}
    }
    val lower = sourceName.lowercase()
    return when {
        lower.contains("bbc") -> "bbc.co.uk"
        lower.contains("npr") -> "npr.org"
        lower.contains("straits times") || lower == "st" -> "straitstimes.com"
        lower.contains("cna") || lower.contains("channel newsasia") -> "channelnewsasia.com"
        lower.contains("science daily") || lower.contains("sciencedaily") -> "sciencedaily.com"
        lower.contains("yahoo") -> "yahoo.com"
        lower.contains("reuters") -> "reuters.com"
        lower.contains("associated press") || lower == "ap" -> "apnews.com"
        lower.contains("new york times") || lower.contains("nytimes") -> "nytimes.com"
        lower.contains("bloomberg") -> "bloomberg.com"
        lower.contains("nbc") -> "nbcnews.com"
        lower.contains("guardian") -> "theguardian.com"
        lower.contains("cnn") -> "cnn.com"
        lower.contains("techcrunch") -> "techcrunch.com"
        lower.contains("the verge") || lower.contains("verge") -> "theverge.com"
        lower.contains("ars technica") -> "arstechnica.com"
        lower.contains("wired") -> "wired.com"
        lower.contains("engadget") -> "engadget.com"
        lower.contains("wall street journal") || lower.contains("wsj") -> "wsj.com"
        lower.contains("financial times") || lower.contains("ft") -> "ft.com"
        lower.contains("washington post") -> "washingtonpost.com"
        else -> ""
    }
}
