package com.example.ui.drawings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DrawingItem
import com.example.data.model.DrawingToolType
import com.example.ui.theme.BearRed
import com.example.ui.theme.BinanceGold
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.TVBlue
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToInt

@Composable
fun DrawingToolbar(
    isOpen: Boolean,
    activeTool: DrawingToolType,
    favoriteTools: List<DrawingToolType> = emptyList(),
    areDrawingsHidden: Boolean,
    onSelectTool: (DrawingToolType) -> Unit,
    onToggleFavorite: (DrawingToolType) -> Unit = {},
    onToggleHideDrawings: () -> Unit,
    onDeleteAllDrawings: () -> Unit
) {
    AnimatedVisibility(
        visible = isOpen,
        enter = fadeIn() + expandHorizontally(),
        exit = fadeOut() + shrinkHorizontally()
    ) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface.copy(alpha = 0.96f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
            modifier = Modifier
                .padding(start = 8.dp, top = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(4.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Pan / Select Cursor
                ToolRow(
                    title = "Pan / Select",
                    shortLabel = "Cursor",
                    icon = Icons.Default.PanTool,
                    isSelected = activeTool == DrawingToolType.CURSOR,
                    isFavorite = false,
                    showFavoriteButton = false,
                    onSelect = { onSelectTool(DrawingToolType.CURSOR) },
                    onToggleFavorite = {}
                )

                val tools = listOf(
                    Triple(DrawingToolType.TREND_LINE, "TL", Icons.Default.Timeline),
                    Triple(DrawingToolType.HORIZONTAL_LINE, "HL", null),
                    Triple(DrawingToolType.HORIZONTAL_RAY, "Ray", null),
                    Triple(DrawingToolType.VERTICAL_LINE, "VL", null),
                    Triple(DrawingToolType.RECTANGLE, "Box", null),
                    Triple(DrawingToolType.FIB_RETRACEMENT, "Fib", null),
                    Triple(DrawingToolType.FIB_EXTENSION, "FibExt", null),
                    Triple(DrawingToolType.PARALLEL_CHANNEL, "Chan", null),
                    Triple(DrawingToolType.PRICE_RANGE, "Range", null),
                    Triple(DrawingToolType.TEXT, "Txt", Icons.Default.TextFields),
                    Triple(DrawingToolType.BRUSH, "Brush", Icons.Default.Brush)
                )

                tools.forEach { (tool, label, icon) ->
                    val isFav = favoriteTools.contains(tool)
                    ToolRow(
                        title = tool.title,
                        shortLabel = label,
                        icon = icon,
                        isSelected = activeTool == tool,
                        isFavorite = isFav,
                        showFavoriteButton = true,
                        onSelect = { onSelectTool(tool) },
                        onToggleFavorite = { onToggleFavorite(tool) }
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(DarkBorder)
                        .padding(vertical = 2.dp)
                )

                // Hide / Show Drawings Toggle
                IconButton(
                    onClick = onToggleHideDrawings,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = if (areDrawingsHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Hide/Show Drawings",
                        tint = if (areDrawingsHidden) BinanceGold else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Delete All Drawings
                IconButton(
                    onClick = onDeleteAllDrawings,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Delete All",
                        tint = BearRed.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ToolRow(
    title: String,
    shortLabel: String,
    icon: ImageVector? = null,
    isSelected: Boolean,
    isFavorite: Boolean,
    showFavoriteButton: Boolean,
    onSelect: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) TVBlue.copy(alpha = 0.25f) else Color.Transparent)
            .padding(horizontal = 2.dp, vertical = 1.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (isSelected) TVBlue else Color.Transparent)
                .clickable { onSelect() },
            contentAlignment = Alignment.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (isSelected) Color.White else TextPrimary,
                    modifier = Modifier.size(17.dp)
                )
            } else {
                Text(
                    text = shortLabel,
                    color = if (isSelected) Color.White else TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (showFavoriteButton) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .clickable { onToggleFavorite() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) BinanceGold else TextSecondary.copy(alpha = 0.3f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

/**
 * Floating TradingView-Style Draggable Favorite Drawing Tools Bar
 */
@Composable
fun FavoriteDrawingToolsBar(
    isVisible: Boolean,
    favoriteTools: List<DrawingToolType>,
    activeTool: DrawingToolType,
    onSelectTool: (DrawingToolType) -> Unit,
    onOpenFullToolbar: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isVisible) return

    var offsetX by remember { mutableFloatStateOf(16f) }
    var offsetY by remember { mutableFloatStateOf(60f) }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface.copy(alpha = 0.94f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, TVBlue.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offsetX += dragAmount.x
                    offsetY += dragAmount.y
                }
            }
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Drag Handle
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = "Drag Toolbar",
                tint = TextSecondary.copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp)
            )

            // Cursor Tool Button
            val isCursorActive = activeTool == DrawingToolType.CURSOR
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isCursorActive) TVBlue else DarkSurfaceVariant)
                    .clickable { onSelectTool(DrawingToolType.CURSOR) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PanTool,
                    contentDescription = "Pan/Zoom",
                    tint = if (isCursorActive) Color.White else TextPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(20.dp)
                    .background(DarkBorder)
            )

            // Starred favorite tools
            favoriteTools.forEach { tool ->
                val isSelected = activeTool == tool
                val icon = when (tool) {
                    DrawingToolType.TREND_LINE -> Icons.Default.Timeline
                    DrawingToolType.TEXT -> Icons.Default.TextFields
                    DrawingToolType.BRUSH -> Icons.Default.Brush
                    else -> null
                }
                val label = when (tool) {
                    DrawingToolType.HORIZONTAL_LINE -> "HL"
                    DrawingToolType.HORIZONTAL_RAY -> "Ray"
                    DrawingToolType.VERTICAL_LINE -> "VL"
                    DrawingToolType.RECTANGLE -> "Box"
                    DrawingToolType.FIB_RETRACEMENT -> "Fib"
                    DrawingToolType.FIB_EXTENSION -> "FibExt"
                    DrawingToolType.PARALLEL_CHANNEL -> "Chan"
                    DrawingToolType.PRICE_RANGE -> "Range"
                    else -> tool.title.take(3)
                }

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) TVBlue else DarkSurfaceVariant)
                        .border(
                            1.dp,
                            if (isSelected) Color.White else Color.Transparent,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { onSelectTool(tool) },
                    contentAlignment = Alignment.Center
                ) {
                    if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = tool.title,
                            tint = if (isSelected) Color.White else TextPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Text(
                            text = label,
                            color = if (isSelected) Color.White else TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Star button to open full drawing tools menu
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .clickable { onOpenFullToolbar() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "All Drawings",
                    tint = BinanceGold,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Close button to hide favorites bar
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .clickable { onClose() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Favorites Bar",
                    tint = TextSecondary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
fun SelectedDrawingPropertyBar(
    selectedDrawing: DrawingItem,
    onUpdateDrawing: (DrawingItem) -> Unit,
    onDeleteDrawing: (String) -> Unit,
    onDismissSelection: () -> Unit
) {
    val availableColors = listOf(
        0xFF2962FF, // Blue
        0xFF089981, // Green
        0xFFF23645, // Red
        0xFFF0B90B, // Gold
        0xFF00E5FF, // Cyan
        0xFFFFFFFF  // White
    )

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface.copy(alpha = 0.95f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, TVBlue),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = selectedDrawing.toolType.title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TVBlue
            )

            // Colors
            availableColors.forEach { colorHex ->
                val isColorSelected = selectedDrawing.colorHex == colorHex
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color(colorHex))
                        .border(
                            1.5.dp,
                            if (isColorSelected) Color.White else Color.Transparent,
                            CircleShape
                        )
                        .clickable {
                            onUpdateDrawing(selectedDrawing.copy(colorHex = colorHex))
                        }
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Thickness (1px, 2px, 3px, 4px)
            listOf(1.0f, 2.0f, 3.0f, 4.0f).forEach { thickness ->
                val isThickSelected = selectedDrawing.strokeWidth == thickness
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isThickSelected) TVBlue else DarkSurfaceVariant)
                        .clickable {
                            onUpdateDrawing(selectedDrawing.copy(strokeWidth = thickness))
                        }
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${thickness.toInt()}px",
                        fontSize = 11.sp,
                        color = if (isThickSelected) Color.White else TextPrimary
                    )
                }
            }

            // Dashed toggle
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (selectedDrawing.isDashed) TVBlue else DarkSurfaceVariant)
                    .clickable {
                        onUpdateDrawing(selectedDrawing.copy(isDashed = !selectedDrawing.isDashed))
                    }
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "Dashed",
                    fontSize = 11.sp,
                    color = if (selectedDrawing.isDashed) Color.White else TextPrimary
                )
            }

            // Lock / Unlock
            IconButton(
                onClick = {
                    onUpdateDrawing(selectedDrawing.copy(isLocked = !selectedDrawing.isLocked))
                },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = if (selectedDrawing.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                    contentDescription = "Lock",
                    tint = if (selectedDrawing.isLocked) BinanceGold else TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Delete
            IconButton(
                onClick = { onDeleteDrawing(selectedDrawing.id) },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = BearRed,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
