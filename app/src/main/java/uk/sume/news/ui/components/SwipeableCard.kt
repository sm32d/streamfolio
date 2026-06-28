package uk.sume.news.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SwipeableCard(
    modifier: Modifier = Modifier,
    onSwipeRight: () -> Unit, // e.g. Bookmark
    onSwipeLeft: () -> Unit,  // e.g. Dismiss/Skip
    content: @Composable () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val threshold = 400f // Swipe threshold in pixels

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        coroutineScope.launch {
                            if (offsetX.value > threshold) {
                                // Swipe Right action
                                offsetX.animateTo(1000f, animationSpec = tween(300))
                                onSwipeRight()
                                offsetX.snapTo(0f)
                            } else if (offsetX.value < -threshold) {
                                // Swipe Left action
                                offsetX.animateTo(-1000f, animationSpec = tween(300))
                                onSwipeLeft()
                                offsetX.snapTo(0f)
                            } else {
                                // Snap back to center
                                offsetX.animateTo(0f, animationSpec = tween(200))
                            }
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        coroutineScope.launch {
                            offsetX.snapTo(offsetX.value + dragAmount.x)
                        }
                    }
                )
            }
    ) {
        // Background swipe actions visual indicator
        val backgroundAlpha = (kotlin.math.abs(offsetX.value) / threshold).coerceIn(0f, 0.8f)
        val backgroundColor = when {
            offsetX.value > 0 -> Color(0xFF10B981).copy(alpha = backgroundAlpha) // Emerald Green for save
            else -> Color(0xFFF43F5E).copy(alpha = backgroundAlpha) // Coral Red for skip
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(24.dp))
                .background(backgroundColor)
                .padding(24.dp)
        ) {
            if (offsetX.value > 50f) {
                Icon(
                    imageVector = Icons.Default.Bookmark,
                    contentDescription = "Save Bookmark",
                    tint = Color.White,
                    modifier = Modifier
                        .scale((offsetX.value / threshold).coerceIn(0.5f, 1.5f))
                        .align(Alignment.CenterStart)
                )
            } else if (offsetX.value < -50f) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Dismiss",
                    tint = Color.White,
                    modifier = Modifier
                        .scale((kotlin.math.abs(offsetX.value) / threshold).coerceIn(0.5f, 1.5f))
                        .align(Alignment.CenterEnd)
                )
            }
        }

        // Foreground content card
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .fillMaxSize()
        ) {
            content()
        }
    }
}
