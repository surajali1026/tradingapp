package com.example.ui.drawings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PanTool
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DrawingItem
import com.example.data.model.DrawingToolType
import com.example.ui.theme.BearRed
import com.example.ui.theme.BinanceGold
import com.example.ui.theme.BullGreen
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.TVBlue
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun DrawingToolbar(
    isOpen: Boolean,
    activeTool: DrawingToolType,
    areDrawingsHidden: Boolean,
    onSelectTool: (DrawingToolType) -> Unit,
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
            colors = CardDefaults.cardColors(containerColor = DarkSurface.copy(alpha = 0.95f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
            modifier = Modifier.padding(start = 8.dp, top = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Cursor / Pan
                ToolButton(
                    title = "Pan / Select",
                    shortLabel = "Cursor",
                    icon = Icons.Default.PanTool,
                    isSelected = activeTool == DrawingToolType.CURSOR,
                    onClick = { onSelectTool(DrawingToolType.CURSOR) }
                )

                // Trend Line
                ToolButton(
                    title = "Trend Line",
                    shortLabel = "TL",
                    icon = Icons.Default.Timeline,
                    isSelected = activeTool == DrawingToolType.TREND_LINE,
                    onClick = { onSelectTool(DrawingToolType.TREND_LINE) }
                )

                // Horizontal Line
                ToolButton(
                    title = "Horizontal Line",
                    shortLabel = "HL",
                    isSelected = activeTool == DrawingToolType.HORIZONTAL_LINE,
                    onClick = { onSelectTool(DrawingToolType.HORIZONTAL_LINE) }
                )

                // Horizontal Ray
                ToolButton(
                    title = "Horizontal Ray",
                    shortLabel = "Ray",
                    isSelected = activeTool == DrawingToolType.HORIZONTAL_RAY,
                    onClick = { onSelectTool(DrawingToolType.HORIZONTAL_RAY) }
                )

                // Vertical Line
                ToolButton(
                    title = "Vertical Line",
                    shortLabel = "VL",
                    isSelected = activeTool == DrawingToolType.VERTICAL_LINE,
                    onClick = { onSelectTool(DrawingToolType.VERTICAL_LINE) }
                )

                // Rectangle
                ToolButton(
                    title = "Rectangle",
                    shortLabel = "Box",
                    isSelected = activeTool == DrawingToolType.RECTANGLE,
                    onClick = { onSelectTool(DrawingToolType.RECTANGLE) }
                )

                // Fibonacci Retracement
                ToolButton(
                    title = "Fib Retracement",
                    shortLabel = "Fib",
                    isSelected = activeTool == DrawingToolType.FIB_RETRACEMENT,
                    onClick = { onSelectTool(DrawingToolType.FIB_RETRACEMENT) }
                )

                // Fibonacci Extension
                ToolButton(
                    title = "Fib Extension",
                    shortLabel = "FibExt",
                    isSelected = activeTool == DrawingToolType.FIB_EXTENSION,
                    onClick = { onSelectTool(DrawingToolType.FIB_EXTENSION) }
                )

                // Parallel Channel
                ToolButton(
                    title = "Parallel Channel",
                    shortLabel = "Chan",
                    isSelected = activeTool == DrawingToolType.PARALLEL_CHANNEL,
                    onClick = { onSelectTool(DrawingToolType.PARALLEL_CHANNEL) }
                )

                // Price Range
                ToolButton(
                    title = "Price Range",
                    shortLabel = "Range",
                    isSelected = activeTool == DrawingToolType.PRICE_RANGE,
                    onClick = { onSelectTool(DrawingToolType.PRICE_RANGE) }
                )

                // Text
                ToolButton(
                    title = "Text",
                    shortLabel = "Txt",
                    icon = Icons.Default.TextFields,
                    isSelected = activeTool == DrawingToolType.TEXT,
                    onClick = { onSelectTool(DrawingToolType.TEXT) }
                )

                // Brush
                ToolButton(
                    title = "Brush",
                    shortLabel = "Brush",
                    icon = Icons.Default.Brush,
                    isSelected = activeTool == DrawingToolType.BRUSH,
                    onClick = { onSelectTool(DrawingToolType.BRUSH) }
                )

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
private fun ToolButton(
    title: String,
    shortLabel: String,
    icon: ImageVector? = null,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) TVBlue else Color.Transparent)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected) Color.White else TextPrimary,
                modifier = Modifier.size(18.dp)
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
