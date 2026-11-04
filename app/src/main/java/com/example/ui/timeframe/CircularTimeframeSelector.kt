package com.example.ui.timeframe

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Timeframe
import com.example.ui.theme.BinanceGold
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.TVBlue
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun CircularTimeframeSelector(
    isVisible: Boolean,
    centerOffset: Offset,
    currentTimeframe: Timeframe,
    onSelectTimeframe: (Timeframe) -> Unit,
    onOpenCustomDialog: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!isVisible) return

    val density = LocalDensity.current

    // Group timeframes into Outer Ring (lower TF: 1s .. 45m) and Inner Ring (higher TF: 1H .. 1M)
    val outerTimeframes = remember {
        listOf(
            Timeframe.TF_1S, Timeframe.TF_5S, Timeframe.TF_15S, Timeframe.TF_30S,
            Timeframe.TF_1M, Timeframe.TF_3M, Timeframe.TF_5M, Timeframe.TF_15M,
            Timeframe.TF_30M, Timeframe.TF_45M
        )
    }

    val innerTimeframes = remember {
        listOf(
            Timeframe.TF_1H, Timeframe.TF_2H, Timeframe.TF_3H, Timeframe.TF_4H,
            Timeframe.TF_6H, Timeframe.TF_8H, Timeframe.TF_12H,
            Timeframe.TF_1D, Timeframe.TF_1W, Timeframe.TF_1MONTH
        )
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                // Catch touches and dismiss if tapped backdrop
                detectDragGestures(
                    onDragEnd = { onDismiss() },
                    onDragCancel = { onDismiss() },
                    onDrag = { _, _ -> }
                )
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onDismiss()
            }
            .background(Color.Black.copy(alpha = 0.55f))
    ) {
        val screenWidthPx = with(density) { maxWidth.toPx() }
        val screenHeightPx = with(density) { maxHeight.toPx() }

        // Keep wheel centered comfortably within screen edges
        val safeCenterX = centerOffset.x.coerceIn(160f, screenWidthPx - 160f)
        val safeCenterY = centerOffset.y.coerceIn(160f, screenHeightPx - 160f)

        val outerRadiusDp = 135.dp
        val innerRadiusDp = 80.dp
        val outerRadiusPx = with(density) { outerRadiusDp.toPx() }
        val innerRadiusPx = with(density) { innerRadiusDp.toPx() }

        var hoveredTimeframe by remember { mutableStateOf<Timeframe?>(null) }
        var isHoveringCustom by remember { mutableStateOf(false) }

        // Touch tracking to support sliding finger and releasing to pick
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull()
                            if (change != null) {
                                if (change.pressed) {
                                    val touchPos = change.position
                                    val dx = (touchPos.x - safeCenterX).toDouble()
                                    val dy = (touchPos.y - safeCenterY).toDouble()
                                    val dist = sqrt(dx * dx + dy * dy)
                                    val angleRad = atan2(dy, dx) // -PI to PI
                                    val normalizedAngle = if (angleRad < 0.0) angleRad + (2.0 * PI) else angleRad

                                    val outerRadD = outerRadiusPx.toDouble()
                                    val innerRadD = innerRadiusPx.toDouble()

                                    if (dist in (outerRadD - 30.0)..(outerRadD + 35.0)) {
                                        // Outer ring hit test
                                        val step = (2.0 * PI) / outerTimeframes.size.toDouble()
                                        val rawIdx = ((normalizedAngle + (step / 2.0)) / step).toInt()
                                        val idx = (rawIdx % outerTimeframes.size + outerTimeframes.size) % outerTimeframes.size
                                        hoveredTimeframe = outerTimeframes[idx]
                                        isHoveringCustom = false
                                    } else if (dist in (innerRadD - 26.0)..(innerRadD + 26.0)) {
                                        // Inner ring hit test
                                        val totalSlots = innerTimeframes.size + 1 // +1 for custom button
                                        val step = (2.0 * PI) / totalSlots.toDouble()
                                        val rawIdx = ((normalizedAngle + (step / 2.0)) / step).toInt()
                                        val idx = (rawIdx % totalSlots + totalSlots) % totalSlots
                                        if (idx < innerTimeframes.size) {
                                            hoveredTimeframe = innerTimeframes[idx]
                                            isHoveringCustom = false
                                        } else {
                                            hoveredTimeframe = null
                                            isHoveringCustom = true
                                        }
                                    } else {
                                        hoveredTimeframe = null
                                        isHoveringCustom = false
                                    }
                                } else {
                                    // Released finger!
                                    if (hoveredTimeframe != null) {
                                        onSelectTimeframe(hoveredTimeframe!!)
                                        onDismiss()
                                    } else if (isHoveringCustom) {
                                        onOpenCustomDialog()
                                        onDismiss()
                                    }
                                }
                            }
                        }
                    }
                }
        ) {
            // Radial layout container
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (safeCenterX - with(density) { 150.dp.toPx() }).roundToInt(),
                            (safeCenterY - with(density) { 150.dp.toPx() }).roundToInt()
                        )
                    }
                    .size(300.dp),
                contentAlignment = Alignment.Center
            ) {
                // Background radial glow
                Box(
                    modifier = Modifier
                        .size(290.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    DarkSurface.copy(alpha = 0.95f),
                                    DarkBackground.copy(alpha = 0.95f)
                                )
                            )
                        )
                        .border(1.5.dp, DarkBorder, CircleShape)
                )

                // Outer guide circle line
                Box(
                    modifier = Modifier
                        .size(outerRadiusDp * 2)
                        .border(0.8.dp, DarkBorder.copy(alpha = 0.5f), CircleShape)
                )

                // Inner guide circle line
                Box(
                    modifier = Modifier
                        .size(innerRadiusDp * 2)
                        .border(0.8.dp, DarkBorder.copy(alpha = 0.5f), CircleShape)
                )

                // Center Current Timeframe Indicator
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .shadow(8.dp, CircleShape)
                        .clip(CircleShape)
                        .background(TVBlue)
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = currentTimeframe.displayName,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                // Outer Ring Timeframe Nodes (1s to 45m)
                val outerStep = (2 * PI) / outerTimeframes.size
                outerTimeframes.forEachIndexed { index, tf ->
                    val angle = index * outerStep
                    val x = (cos(angle) * outerRadiusPx).toFloat()
                    val y = (sin(angle) * outerRadiusPx).toFloat()

                    TimeframeNode(
                        timeframe = tf,
                        isSelected = tf.id == currentTimeframe.id,
                        isHovered = tf.id == hoveredTimeframe?.id,
                        offsetX = x,
                        offsetY = y,
                        onClick = {
                            onSelectTimeframe(tf)
                            onDismiss()
                        }
                    )
                }

                // Inner Ring Timeframe Nodes (1H to 1M) + Custom (+)
                val innerTotal = innerTimeframes.size + 1
                val innerStep = (2 * PI) / innerTotal
                innerTimeframes.forEachIndexed { index, tf ->
                    val angle = index * innerStep
                    val x = (cos(angle) * innerRadiusPx).toFloat()
                    val y = (sin(angle) * innerRadiusPx).toFloat()

                    TimeframeNode(
                        timeframe = tf,
                        isSelected = tf.id == currentTimeframe.id,
                        isHovered = tf.id == hoveredTimeframe?.id,
                        offsetX = x,
                        offsetY = y,
                        isSmall = true,
                        onClick = {
                            onSelectTimeframe(tf)
                            onDismiss()
                        }
                    )
                }

                // Custom (+) Button Node on inner ring
                val customAngle = innerTimeframes.size * innerStep
                val customX = (cos(customAngle) * innerRadiusPx).toFloat()
                val customY = (sin(customAngle) * innerRadiusPx).toFloat()

                Box(
                    modifier = Modifier
                        .offset { IntOffset(customX.roundToInt(), customY.roundToInt()) }
                        .size(34.dp)
                        .shadow(4.dp, CircleShape)
                        .clip(CircleShape)
                        .background(if (isHoveringCustom) BinanceGold else DarkSurface)
                        .border(1.dp, if (isHoveringCustom) Color.White else BinanceGold, CircleShape)
                        .clickable {
                            onOpenCustomDialog()
                            onDismiss()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Custom Timeframe",
                        tint = if (isHoveringCustom) Color.Black else BinanceGold,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TimeframeNode(
    timeframe: Timeframe,
    isSelected: Boolean,
    isHovered: Boolean,
    offsetX: Float,
    offsetY: Float,
    isSmall: Boolean = false,
    onClick: () -> Unit
) {
    val size = if (isSmall) 34.dp else 38.dp
    val bgColor = when {
        isSelected -> TVBlue
        isHovered -> TVBlue.copy(alpha = 0.8f)
        else -> DarkSurface
    }
    val textColor = when {
        isSelected || isHovered -> Color.White
        else -> TextPrimary
    }
    val borderColor = when {
        isSelected -> Color.White
        isHovered -> TVBlue
        else -> DarkBorder
    }

    Box(
        modifier = Modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .size(size)
            .shadow(4.dp, CircleShape)
            .clip(CircleShape)
            .background(bgColor)
            .border(1.dp, borderColor, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = timeframe.displayName,
            color = textColor,
            fontSize = if (isSmall) 11.sp else 12.sp,
            fontWeight = if (isSelected || isHovered) FontWeight.Bold else FontWeight.Medium
        )
    }
}
