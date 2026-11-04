package com.example.engine

import android.content.Context
import com.example.data.model.AlertCondition
import com.example.data.model.AlertItem
import com.example.data.model.Candle
import com.example.sound.AlertSoundPlayer
import kotlin.math.abs

class AlertEngine(
    private val onAlertTriggered: (AlertItem, Double) -> Unit
) {
    // Map of symbol to previous price for crossover evaluation
    private val lastPrices = HashMap<String, Double>()

    fun checkPrice(symbol: String, currentPrice: Double, alerts: List<AlertItem>, context: Context?) {
        val sym = symbol.uppercase()
        val prevPrice = lastPrices[sym] ?: currentPrice
        lastPrices[sym] = currentPrice

        val activeAlerts = alerts.filter { it.symbol.equals(sym, ignoreCase = true) && it.isActive }
        for (alert in activeAlerts) {
            val target = alert.targetPrice
            val isTriggered = when (alert.condition) {
                AlertCondition.REACHES, AlertCondition.HORIZONTAL_LINE_TOUCH -> {
                    val threshold = target * 0.0005 // 0.05% proximity tolerance
                    abs(currentPrice - target) <= threshold ||
                            (prevPrice < target && currentPrice >= target) ||
                            (prevPrice > target && currentPrice <= target)
                }
                AlertCondition.CROSSES_ABOVE, AlertCondition.HORIZONTAL_LINE_BREAKOUT -> {
                    prevPrice <= target && currentPrice > target
                }
                AlertCondition.CROSSES_BELOW -> {
                    prevPrice >= target && currentPrice < target
                }
                AlertCondition.TRENDLINE_TOUCH -> {
                    val threshold = target * 0.0005
                    abs(currentPrice - target) <= threshold
                }
                else -> false // Candle close evaluated separately
            }

            if (isTriggered) {
                context?.let { AlertSoundPlayer.play(it, alert.soundType) }
                onAlertTriggered(alert, currentPrice)
            }
        }
    }

    fun checkCandleClose(symbol: String, candle: Candle, alerts: List<AlertItem>, context: Context?) {
        if (!candle.isClosed) return
        val sym = symbol.uppercase()
        val activeAlerts = alerts.filter { it.symbol.equals(sym, ignoreCase = true) && it.isActive }
        for (alert in activeAlerts) {
            val isTriggered = when (alert.condition) {
                AlertCondition.CANDLE_CLOSE_ABOVE -> candle.close > alert.targetPrice
                AlertCondition.CANDLE_CLOSE_BELOW -> candle.close < alert.targetPrice
                else -> false
            }

            if (isTriggered) {
                context?.let { AlertSoundPlayer.play(it, alert.soundType) }
                onAlertTriggered(alert, candle.close)
            }
        }
    }
}
