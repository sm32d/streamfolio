package uk.sume.streamfolio.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// StreamFolio Cobalt Blue Branding Palette (redefined to match the app icon gradient)
val EmeraldPrimary = Color(0xFF1E88E5)    // Vibrant middle-blue from the icon
val EmeraldSecondary = Color(0xFF1565C0)  // Deep royal blue from outer icon gradient
val MintAccent = Color(0xFF00B0FF)        // Light cyan-blue accent for highlights

val ErrorRed = Color(0xFFD32F2F)          // Conventional red for destructive actions

val DarkBackground = Color(0xFF060D1E)
val DarkSurface = Color(0xFF0E1A33)
val DarkSurfaceVariant = Color(0xFF1A2B4C)

val LightBackground = Color(0xFFF0F6FC)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFE1EBF5)

val AccentCoral = Color(0xFFF43F5E)
val AccentAmber = Color(0xFFF59E0B)

// Container/outline tones for the M3 ColorScheme slots the app actually uses
// (selected-row backgrounds, card borders, etc.). darkColorScheme()/lightColorScheme()
// silently default any slot you don't pass to Material3's baseline purple palette, which is
// why selected rows (e.g. the Save-to-Collection sheet) showed up purple instead of blue —
// these keep every container/outline in the same blue family as the rest of the brand palette.
val PrimaryContainerDark = Color(0xFF12386B)
val OnPrimaryContainerDark = Color(0xFFD3E4FA)
val SecondaryContainerDark = Color(0xFF0D2B57)
val OnSecondaryContainerDark = Color(0xFFCFE0F5)
val TertiaryContainerDark = Color(0xFF00364D)
val OnTertiaryContainerDark = Color(0xFFB3E5FC)
val OutlineDark = Color(0xFF3A537A)
val OutlineVariantDark = Color(0xFF26385C)

val PrimaryContainerLight = Color(0xFFD3E4FA)
val OnPrimaryContainerLight = Color(0xFF0B2E5E)
val SecondaryContainerLight = Color(0xFFDCE9FB)
val OnSecondaryContainerLight = Color(0xFF0D2B57)
val TertiaryContainerLight = Color(0xFFDFF4FF)
val OnTertiaryContainerLight = Color(0xFF00364D)
val OutlineLight = Color(0xFF8DA3C2)
val OutlineVariantLight = Color(0xFFC3D3EA)

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
