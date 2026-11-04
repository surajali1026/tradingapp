package com.example.data.model

enum class MarketCategory(val title: String) {
    ALL_FUTURES("All Futures"),
    TOP_100_VOLUME("Top 100 Volume"),
    HIGHEST_VOLUME("Highest Volume"),
    TOP_GAINERS("Top Gainers"),
    TOP_LOSERS("Top Losers"),
    WATCHLIST("My Watchlist")
}

enum class ScalePosition {
    RIGHT,
    LEFT
}

data class ChartScaleSettings(
    val autoScale: Boolean = true,
    val isLogarithmic: Boolean = false,
    val isPercentage: Boolean = false,
    val isInverted: Boolean = false,
    val isScaleLocked: Boolean = false,
    val manualMinPrice: Double? = null,
    val manualMaxPrice: Double? = null,
    val scalePosition: ScalePosition = ScalePosition.RIGHT,
    val showGrid: Boolean = true,
    val showVolume: Boolean = true,
    val showWatermark: Boolean = true,
    val showLastPrice: Boolean = true,
    val showMarkPrice: Boolean = true,
    val showIndexPrice: Boolean = true
)
