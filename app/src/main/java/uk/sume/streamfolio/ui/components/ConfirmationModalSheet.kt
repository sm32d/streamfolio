package uk.sume.streamfolio.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/**
 * Shared confirmation modal bottom sheet designed with consistent StreamFolio aesthetics:
 * - Rounded top corners (28.dp)
 * - Header badge with icon and title
 * - Subtle body text with line height
 * - Smooth animated dismissal (`sheetState.hide()`)
 * - Clean vertical CTA buttons (Pill-shaped 24.dp corners, 48.dp height)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmationModalSheet(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    icon: ImageVector = Icons.Default.Delete,
    iconTint: Color = MaterialTheme.colorScheme.error,
    iconBgColor: Color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
    primaryButtonText: String = "Delete",
    primaryButtonColor: Color = MaterialTheme.colorScheme.error,
    primaryButtonTextColor: Color = Color.White,
    onPrimaryClick: () -> Unit,
    secondaryButtonText: String? = "Cancel",
    onSecondaryClick: (() -> Unit)? = null,
    secondaryButtonColor: Color = MaterialTheme.colorScheme.error,
    isSecondaryOutlined: Boolean = false,
    tertiaryButtonText: String? = null,
    onTertiaryClick: (() -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    fun animateDismiss(action: () -> Unit = {}) {
        coroutineScope.launch {
            sheetState.hide()
        }.invokeOnCompletion {
            action()
            onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(iconBgColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Body Text
            Text(
                text = message,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // CTA Buttons Stack
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Primary Action Button
                Button(
                    onClick = { animateDismiss { onPrimaryClick() } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryButtonColor)
                ) {
                    Text(primaryButtonText, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = primaryButtonTextColor)
                }

                // Secondary Action Button (if provided)
                if (secondaryButtonText != null) {
                    if (isSecondaryOutlined) {
                        OutlinedButton(
                            onClick = {
                                animateDismiss {
                                    onSecondaryClick?.invoke()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = secondaryButtonColor),
                            border = BorderStroke(1.dp, secondaryButtonColor.copy(alpha = 0.4f))
                        ) {
                            Text(secondaryButtonText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                animateDismiss {
                                    onSecondaryClick?.invoke()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(24.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
                        ) {
                            Text(secondaryButtonText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Tertiary Text Button (if provided)
                if (tertiaryButtonText != null) {
                    TextButton(
                        onClick = {
                            animateDismiss {
                                onTertiaryClick?.invoke()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Text(
                            text = tertiaryButtonText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}
