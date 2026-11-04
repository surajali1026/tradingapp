package com.example.data.storage

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.AlertCondition
import com.example.data.model.AlertItem
import com.example.data.model.AlertSoundType
import com.example.data.model.ChartPoint
import com.example.data.model.ChartScaleSettings
import com.example.data.model.DrawingItem
import com.example.data.model.DrawingToolType
import com.example.data.model.ScalePosition
import com.example.data.model.Timeframe
import org.json.JSONArray
import org.json.JSONObject

class TradingPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("apex_trading_prefs", Context.MODE_PRIVATE)

    // Watchlist symbols (reorderable)
    fun getWatchlist(): List<String> {
        val raw = prefs.getString("watchlist_symbols", null) ?: return listOf(
            "BTCUSDT", "ETHUSDT", "SOLUSDT", "BNBUSDT", "DOGEUSDT", "XRPUSDT"
        )
        val arr = JSONArray(raw)
        val list = mutableListOf<String>()
        for (i in 0 until arr.length()) {
            list.add(arr.getString(i))
        }
        return list
    }

    fun saveWatchlist(symbols: List<String>) {
        val arr = JSONArray()
        symbols.forEach { arr.put(it) }
        prefs.edit().putString("watchlist_symbols", arr.toString()).apply()
    }

    // Custom timeframes
    fun getCustomTimeframes(): List<Timeframe> {
        val raw = prefs.getString("custom_timeframes", null) ?: return emptyList()
        val arr = JSONArray(raw)
        val list = mutableListOf<Timeframe>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(
                Timeframe(
                    id = obj.getString("id"),
                    displayName = obj.getString("displayName"),
                    durationMs = obj.getLong("durationMs"),
                    nativeBinanceInterval = if (obj.has("nativeBinanceInterval") && !obj.isNull("nativeBinanceInterval")) obj.getString("nativeBinanceInterval") else null,
                    baseBinanceInterval = obj.getString("baseBinanceInterval"),
                    aggregationFactor = obj.getInt("aggregationFactor"),
                    isCustom = true
                )
            )
        }
        return list
    }

    fun saveCustomTimeframe(tf: Timeframe) {
        val current = getCustomTimeframes().filter { it.id != tf.id }.toMutableList()
        current.add(tf)
        val arr = JSONArray()
        current.forEach { item ->
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("displayName", item.displayName)
            obj.put("durationMs", item.durationMs)
            obj.put("nativeBinanceInterval", item.nativeBinanceInterval)
            obj.put("baseBinanceInterval", item.baseBinanceInterval)
            obj.put("aggregationFactor", item.aggregationFactor)
            arr.put(obj)
        }
        prefs.edit().putString("custom_timeframes", arr.toString()).apply()
    }

    fun deleteCustomTimeframe(id: String) {
        val filtered = getCustomTimeframes().filter { it.id != id }
        val arr = JSONArray()
        filtered.forEach { item ->
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("displayName", item.displayName)
            obj.put("durationMs", item.durationMs)
            obj.put("nativeBinanceInterval", item.nativeBinanceInterval)
            obj.put("baseBinanceInterval", item.baseBinanceInterval)
            obj.put("aggregationFactor", item.aggregationFactor)
            arr.put(obj)
        }
        prefs.edit().putString("custom_timeframes", arr.toString()).apply()
    }

    // Drawings (per symbol and timeframe)
    fun getDrawings(symbol: String, timeframeId: String): List<DrawingItem> {
        val key = "drawings_${symbol.uppercase()}_$timeframeId"
        val raw = prefs.getString(key, null) ?: return emptyList()
        val arr = JSONArray(raw)
        val list = mutableListOf<DrawingItem>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val ptsArr = obj.getJSONArray("points")
            val points = mutableListOf<ChartPoint>()
            for (p in 0 until ptsArr.length()) {
                val ptObj = ptsArr.getJSONObject(p)
                points.add(ChartPoint(ptObj.getLong("t"), ptObj.getDouble("p")))
            }
            list.add(
                DrawingItem(
                    id = obj.getString("id"),
                    symbol = obj.getString("symbol"),
                    timeframeId = obj.getString("timeframeId"),
                    toolType = DrawingToolType.valueOf(obj.getString("toolType")),
                    points = points,
                    colorHex = obj.getLong("colorHex"),
                    strokeWidth = obj.getDouble("strokeWidth").toFloat(),
                    isDashed = obj.optBoolean("isDashed", false),
                    isLocked = obj.optBoolean("isLocked", false),
                    isHidden = obj.optBoolean("isHidden", false),
                    textContent = obj.optString("textContent", "")
                )
            )
        }
        return list
    }

    fun saveDrawings(symbol: String, timeframeId: String, drawings: List<DrawingItem>) {
        val key = "drawings_${symbol.uppercase()}_$timeframeId"
        val arr = JSONArray()
        drawings.forEach { d ->
            val obj = JSONObject()
            obj.put("id", d.id)
            obj.put("symbol", d.symbol)
            obj.put("timeframeId", d.timeframeId)
            obj.put("toolType", d.toolType.name)
            obj.put("colorHex", d.colorHex)
            obj.put("strokeWidth", d.strokeWidth.toDouble())
            obj.put("isDashed", d.isDashed)
            obj.put("isLocked", d.isLocked)
            obj.put("isHidden", d.isHidden)
            obj.put("textContent", d.textContent)

            val ptsArr = JSONArray()
            d.points.forEach { pt ->
                val ptObj = JSONObject()
                ptObj.put("t", pt.timestamp)
                ptObj.put("p", pt.price)
                ptsArr.put(ptObj)
            }
            obj.put("points", ptsArr)
            arr.put(obj)
        }
        prefs.edit().putString(key, arr.toString()).apply()
    }

    // Alerts (Unlimited, persistent across symbols)
    fun getAllAlerts(): List<AlertItem> {
        val raw = prefs.getString("user_alerts", null) ?: return emptyList()
        val arr = JSONArray(raw)
        val list = mutableListOf<AlertItem>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(
                AlertItem(
                    id = obj.getString("id"),
                    symbol = obj.getString("symbol"),
                    targetPrice = obj.getDouble("targetPrice"),
                    condition = AlertCondition.valueOf(obj.getString("condition")),
                    soundType = AlertSoundType.valueOf(obj.optString("soundType", AlertSoundType.RADAR_PING.name)),
                    note = obj.optString("note", ""),
                    isActive = obj.optBoolean("isActive", true),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    triggeredAt = if (obj.has("triggeredAt") && !obj.isNull("triggeredAt")) obj.getLong("triggeredAt") else null
                )
            )
        }
        return list
    }

    fun saveAlerts(alerts: List<AlertItem>) {
        val arr = JSONArray()
        alerts.forEach { a ->
            val obj = JSONObject()
            obj.put("id", a.id)
            obj.put("symbol", a.symbol)
            obj.put("targetPrice", a.targetPrice)
            obj.put("condition", a.condition.name)
            obj.put("soundType", a.soundType.name)
            obj.put("note", a.note)
            obj.put("isActive", a.isActive)
            obj.put("createdAt", a.createdAt)
            obj.put("triggeredAt", a.triggeredAt)
            arr.put(obj)
        }
        prefs.edit().putString("user_alerts", arr.toString()).apply()
    }

    // Chart scale settings
    fun getChartSettings(): ChartScaleSettings {
        val raw = prefs.getString("chart_scale_settings", null) ?: return ChartScaleSettings()
        return try {
            val obj = JSONObject(raw)
            ChartScaleSettings(
                autoScale = obj.optBoolean("autoScale", true),
                isLogarithmic = obj.optBoolean("isLogarithmic", false),
                isPercentage = obj.optBoolean("isPercentage", false),
                isInverted = obj.optBoolean("isInverted", false),
                isScaleLocked = obj.optBoolean("isScaleLocked", false),
                scalePosition = ScalePosition.valueOf(obj.optString("scalePosition", ScalePosition.RIGHT.name)),
                showGrid = obj.optBoolean("showGrid", true),
                showVolume = obj.optBoolean("showVolume", true),
                showWatermark = obj.optBoolean("showWatermark", true),
                showLastPrice = obj.optBoolean("showLastPrice", true),
                showMarkPrice = obj.optBoolean("showMarkPrice", true),
                showIndexPrice = obj.optBoolean("showIndexPrice", true)
            )
        } catch (e: Exception) {
            ChartScaleSettings()
        }
    }

    fun saveChartSettings(settings: ChartScaleSettings) {
        val obj = JSONObject()
        obj.put("autoScale", settings.autoScale)
        obj.put("isLogarithmic", settings.isLogarithmic)
        obj.put("isPercentage", settings.isPercentage)
        obj.put("isInverted", settings.isInverted)
        obj.put("isScaleLocked", settings.isScaleLocked)
        obj.put("scalePosition", settings.scalePosition.name)
        obj.put("showGrid", settings.showGrid)
        obj.put("showVolume", settings.showVolume)
        obj.put("showWatermark", settings.showWatermark)
        obj.put("showLastPrice", settings.showLastPrice)
        obj.put("showMarkPrice", settings.showMarkPrice)
        obj.put("showIndexPrice", settings.showIndexPrice)
        prefs.edit().putString("chart_scale_settings", obj.toString()).apply()
    }

    // Favorite Drawing Tools
    fun getFavoriteDrawingTools(): List<DrawingToolType> {
        val raw = prefs.getString("favorite_drawing_tools", null) ?: return listOf(
            DrawingToolType.TREND_LINE,
            DrawingToolType.HORIZONTAL_LINE,
            DrawingToolType.RECTANGLE,
            DrawingToolType.FIB_RETRACEMENT,
            DrawingToolType.PRICE_RANGE
        )
        val arr = JSONArray(raw)
        val list = mutableListOf<DrawingToolType>()
        for (i in 0 until arr.length()) {
            try {
                list.add(DrawingToolType.valueOf(arr.getString(i)))
            } catch (e: Exception) {
                // ignore
            }
        }
        return list
    }

    fun saveFavoriteDrawingTools(tools: List<DrawingToolType>) {
        val arr = JSONArray()
        tools.forEach { arr.put(it.name) }
        prefs.edit().putString("favorite_drawing_tools", arr.toString()).apply()
    }

    fun isFavoritesBarVisible(): Boolean {
        return prefs.getBoolean("is_favorites_bar_visible", true)
    }

    fun setFavoritesBarVisible(visible: Boolean) {
        prefs.edit().putBoolean("is_favorites_bar_visible", visible).apply()
    }
}
