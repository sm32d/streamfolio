package uk.sume.streamfolio.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// StreamFolio Cobalt Blue Branding Palette (redefined to match the app icon gradient)
val EmeraldPrimary = Color(0xFF1E88E5)    // Vibrant middle-blue from the icon
val EmeraldSecondary = Color(0xFF1565C0)  // Deep royal blue from outer icon gradient
val MintAccent = Color(0xFF00B0FF)        // Light cyan-blue accent for highlights

val DarkBackground = Color(0xFF060D1E)
val DarkSurface = Color(0xFF0E1A33)
val DarkSurfaceVariant = Color(0xFF1A2B4C)

val LightBackground = Color(0xFFF0F6FC)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFE1EBF5)

val AccentCoral = Color(0xFFF43F5E)
val AccentAmber = Color(0xFFF59E0B)

// Soft dynamic gradients for background and cards (blue-tinted cool gradients)
val LightGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFFE0F2FE), Color(0xFFF0F9FF), Color(0xFFF8FAFC))
)

val DarkGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFF060D1E), Color(0xFF0A142D), Color(0xFF020409))
)

val ShimmerGradient = Brush.linearGradient(
    colors = listOf(
        Color.LightGray.copy(alpha = 0.6f),
        Color.LightGray.copy(alpha = 0.2f),
        Color.LightGray.copy(alpha = 0.6f)
    )
)
