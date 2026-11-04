package com.example.data.model

import java.util.UUID

enum class AlertCondition(val title: String) {
    REACHES("Price Reaches Level"),
    CROSSES_ABOVE("Price Crosses Above"),
    CROSSES_BELOW("Price Crosses Below"),
    CANDLE_CLOSE_ABOVE("Candle Closes Above"),
    CANDLE_CLOSE_BELOW("Candle Closes Below"),
    HORIZONTAL_LINE_TOUCH("Horizontal Line Touch"),
    HORIZONTAL_LINE_BREAKOUT("Horizontal Line Breakout"),
    TRENDLINE_TOUCH("Trendline Touch")
}

enum class AlertSoundType(val displayName: String) {
    RADAR_PING("Radar Ping"),
    SIREN("High Siren"),
    BELL("Bell Chime"),
    CASH_REGISTER("Cash Register"),
    ALARM_PULSE("Alarm Pulse")
}

data class AlertItem(
    val id: String = UUID.randomUUID().toString(),
    val symbol: String,
    val targetPrice: Double,
    val condition: AlertCondition = AlertCondition.CROSSES_ABOVE,
    val soundType: AlertSoundType = AlertSoundType.RADAR_PING,
    val note: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val triggeredAt: Long? = null
)
