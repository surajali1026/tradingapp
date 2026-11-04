package com.example.data.model

import java.util.UUID

enum class DrawingToolType(val title: String) {
    CURSOR("Select / Move"),
    TREND_LINE("Trend Line"),
    HORIZONTAL_LINE("Horizontal Line"),
    HORIZONTAL_RAY("Horizontal Ray"),
    VERTICAL_LINE("Vertical Line"),
    RECTANGLE("Rectangle"),
    FIB_RETRACEMENT("Fib Retracement"),
    FIB_EXTENSION("Fib Extension"),
    PARALLEL_CHANNEL("Parallel Channel"),
    PRICE_RANGE("Price Range"),
    TEXT("Text Note"),
    BRUSH("Brush")
}

data class ChartPoint(
    val timestamp: Long,
    val price: Double
)

data class DrawingItem(
    val id: String = UUID.randomUUID().toString(),
    val symbol: String,
    val timeframeId: String,
    val toolType: DrawingToolType,
    val points: List<ChartPoint>,
    val colorHex: Long = 0xFF2962FF, // default Blue
    val strokeWidth: Float = 2.0f,
    val isDashed: Boolean = false,
    val isLocked: Boolean = false,
    val isHidden: Boolean = false,
    val textContent: String = ""
)
