package uk.sume.streamfolio.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Premium HSL-inspired palette (Teal / Emerald / Deep Blue Gray)
val EmeraldPrimary = Color(0xFF0F9F7E)
val EmeraldSecondary = Color(0xFF059669)
val MintAccent = Color(0xFF34D399)

val DarkBackground = Color(0xFF0B121F)
val DarkSurface = Color(0xFF151F32)
val DarkSurfaceVariant = Color(0xFF1E2D4A)

val LightBackground = Color(0xFFF0F4F8)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFE2E8F0)

val AccentCoral = Color(0xFFF43F5E)
val AccentAmber = Color(0xFFF59E0B)

// Soft dynamic gradients for background and cards
val LightGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFFE0F2FE), Color(0xFFF0FDF4), Color(0xFFF8FAFC))
)

val DarkGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFF080E1A), Color(0xFF0F172A), Color(0xFF020617))
)

val ShimmerGradient = Brush.linearGradient(
    colors = listOf(
        Color.LightGray.copy(alpha = 0.6f),
        Color.LightGray.copy(alpha = 0.2f),
        Color.LightGray.copy(alpha = 0.6f)
    )
)
