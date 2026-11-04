package com.example.data.model

data class Candle(
    val openTime: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double,
    val closeTime: Long = 0L,
    val quoteVolume: Double = 0.0,
    val isClosed: Boolean = true
) {
    val isBullish: Boolean get() = close >= open
    val changePercent: Double get() = if (open != 0.0) ((close - open) / open) * 100.0 else 0.0
    val changeAmount: Double get() = close - open
}
