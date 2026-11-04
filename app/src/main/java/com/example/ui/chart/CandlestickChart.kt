package com.example.ui.chart

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AlertItem
import com.example.data.model.Candle
import com.example.data.model.CandlePriceSource
import com.example.data.model.ChartPoint
import com.example.data.model.ChartScaleSettings
import com.example.data.model.DrawingItem
import com.example.data.model.DrawingToolType
import com.example.data.model.PriceInfo
import com.example.data.model.ScalePosition
import com.example.data.model.Timeframe
import com.example.ui.theme.AlertLineColor
import com.example.ui.theme.BearRed
import com.example.ui.theme.BinanceGold
import com.example.ui.theme.BullGreen
import com.example.ui.theme.CrosshairColor
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.GridLineColor
import com.example.ui.theme.IndexPricePurple
import com.example.ui.theme.MarkPriceCyan
import com.example.ui.theme.TVBlue
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun CandlestickChart(
    symbol: String,
    candles: List<Candle>,
    timeframe: Timeframe,
    priceInfo: PriceInfo,
    candlePriceSource: CandlePriceSource = CandlePriceSource.LAST,
    settings: ChartScaleSettings,
    alerts: List<AlertItem>,
    drawings: List<DrawingItem>,
    activeDrawingTool: DrawingToolType,
    areDrawingsHidden: Boolean,
    selectedDrawing: DrawingItem?,
    onSelectDrawing: (DrawingItem?) -> Unit,
    onAddDrawing: (DrawingItem) -> Unit,
    onUpdateDrawing: (DrawingItem) -> Unit,
    onLoadMoreHistory: () -> Unit,
    onSelectCandleForReplay: ((Int) -> Unit)? = null,
    onOpenCircularTimeframe: (Offset) -> Unit,
    modifier: Modifier = Modifier
) {
    // Chart view state: horizontal pan (scrollOffset in candles), zoom (candleWidth in pixels)
    var candleWidth by remember { mutableFloatStateOf(16f) }
    var scrollOffset by remember { mutableFloatStateOf(0f) } // 0 = rightmost (latest)
    var verticalPanOffset by remember { mutableFloatStateOf(0f) }

    // Crosshair state
    var isCrosshairEnabled by remember { mutableStateOf(false) }
    var crosshairPos by remember { mutableStateOf<Offset?>(null) }
    var hoveredCandle by remember { mutableStateOf<Candle?>(null) }

    // Active drawing in-progress points
    val inProgressPoints = remember { mutableStateListOf<ChartPoint>() }

    // Auto-scroll to right when latest candle arrives if user is near the right edge
    LaunchedEffect(candles.size, candles.lastOrNull()?.closeTime) {
        if (scrollOffset < 3f) {
            scrollOffset = 0f
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            // Tap Gestures
            .pointerInput(activeDrawingTool, drawings, candles, candleWidth, scrollOffset) {
                detectTapGestures(
                    onLongPress = { offset ->
                        // Long press toggles crosshair at touch location or circular timeframe if near center
                        if (isCrosshairEnabled) {
                            onOpenCircularTimeframe(offset)
                        } else {
                            crosshairPos = offset
                            isCrosshairEnabled = true
                        }
                    },
                    onTap = { tapOffset ->
                        if (isCrosshairEnabled) {
                            // Single tap dismisses crosshair
                            isCrosshairEnabled = false
                            crosshairPos = null
                            hoveredCandle = null
                            return@detectTapGestures
                        }

                        if (onSelectCandleForReplay != null && candles.isNotEmpty()) {
                            // Hit test candle for replay start
                            val w = size.width
                            val priceScaleWidth = 140f
                            val chartW = w - priceScaleWidth
                            val candleSpacing = candleWidth * 0.25f
                            val totalSlot = candleWidth + candleSpacing
                            val visibleCount = (chartW / totalSlot).toInt() + 2
                            val rightIndex = (candles.lastIndex - scrollOffset.toInt()).coerceIn(0, candles.lastIndex)
                            val leftIndex = (rightIndex - visibleCount).coerceAtLeast(0)

                            for (i in rightIndex downTo leftIndex) {
                                val distFromRight = rightIndex - i
                                val x = chartW - (distFromRight * totalSlot) - candleWidth / 2
                                if (abs(tapOffset.x - x) <= totalSlot / 2) {
                                    onSelectCandleForReplay(i)
                                    return@detectTapGestures
                                }
                            }
                        }

                        if (activeDrawingTool == DrawingToolType.CURSOR) {
                            // Check if tapped near an existing drawing
                            val tapped = findDrawingNearPoint(
                                tapOffset = tapOffset,
                                drawings = drawings,
                                candles = candles,
                                candleWidth = candleWidth,
                                scrollOffset = scrollOffset,
                                chartWidth = size.width - 140f,
                                chartHeight = size.height - 40f,
                                settings = settings,
                                verticalOffset = verticalPanOffset
                            )
                            onSelectDrawing(tapped)
                        } else {
                            // Click-to-add for drawing tools
                            val pt = screenToChartPoint(
                                tapOffset, candles, candleWidth, scrollOffset,
                                size.width - 140f, size.height - 40f, settings, verticalPanOffset
                            )
                            if (pt != null) {
                                when (activeDrawingTool) {
                                    DrawingToolType.HORIZONTAL_LINE -> {
                                        val item = DrawingItem(
                                            symbol = symbol,
                                            timeframeId = timeframe.id,
                                            toolType = DrawingToolType.HORIZONTAL_LINE,
                                            points = listOf(pt)
                                        )
                                        onAddDrawing(item)
                                    }
                                    DrawingToolType.VERTICAL_LINE -> {
                                        val item = DrawingItem(
                                            symbol = symbol,
                                            timeframeId = timeframe.id,
                                            toolType = DrawingToolType.VERTICAL_LINE,
                                            points = listOf(pt)
                                        )
                                        onAddDrawing(item)
                                    }
                                    DrawingToolType.TEXT -> {
                                        val item = DrawingItem(
                                            symbol = symbol,
                                            timeframeId = timeframe.id,
                                            toolType = DrawingToolType.TEXT,
                                            points = listOf(pt),
                                            textContent = "Note @ ${String.format(Locale.US, "%.2f", pt.price)}"
                                        )
                                        onAddDrawing(item)
                                    }
                                    else -> {
                                        inProgressPoints.add(pt)
                                        val requiredPoints = when (activeDrawingTool) {
                                            DrawingToolType.TREND_LINE, DrawingToolType.HORIZONTAL_RAY,
                                            DrawingToolType.RECTANGLE, DrawingToolType.FIB_RETRACEMENT,
                                            DrawingToolType.PRICE_RANGE -> 2
                                            DrawingToolType.FIB_EXTENSION, DrawingToolType.PARALLEL_CHANNEL -> 3
                                            else -> 2
                                        }
                                        if (inProgressPoints.size >= requiredPoints) {
                                            val item = DrawingItem(
                                                symbol = symbol,
                                                timeframeId = timeframe.id,
                                                toolType = activeDrawingTool,
                                                points = inProgressPoints.toList()
                                            )
                                            onAddDrawing(item)
                                            inProgressPoints.clear()
                                        }
                                    }
                                }
                            }
                        }
                    }
                )
            }
            // Smooth Pan & Pinch Gesture handling
            .pointerInput(activeDrawingTool, candles.size, isCrosshairEnabled) {
                if (isCrosshairEnabled) {
                    // When crosshair is active, drag tracks the crosshair position
                    detectDragGestures(
                        onDrag = { change, _ ->
                            crosshairPos = change.position
                        },
                        onDragEnd = { /* keep active until tap */ }
                    )
                } else if (activeDrawingTool == DrawingToolType.CURSOR) {
                    // Butter-smooth scrolling and zoom
                    detectTransformGestures { _, pan, zoom, _ ->
                        // Smooth pinch to zoom
                        if (zoom != 1f) {
                            candleWidth = (candleWidth * zoom).coerceIn(4f, 60f)
                        }
                        // Fluid horizontal scrolling
                        if (pan.x != 0f) {
                            val candleDelta = -pan.x / candleWidth
                            val maxScroll = (candles.size - 5).coerceAtLeast(0).toFloat()
                            scrollOffset = (scrollOffset + candleDelta).coerceIn(0f, maxScroll)
                            if (scrollOffset >= maxScroll - 10f) {
                                onLoadMoreHistory()
                            }
                        }
                        // Vertical pan (only if scale is not locked)
                        if (!settings.isScaleLocked && pan.y != 0f) {
                            verticalPanOffset += pan.y
                        }
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            val timeScaleHeight = 44f
            val priceScaleWidth = 145f

            val isRightScale = settings.scalePosition == ScalePosition.RIGHT
            val chartLeft = if (isRightScale) 0f else priceScaleWidth
            val chartRight = if (isRightScale) width - priceScaleWidth else width
            val chartWidth = chartRight - chartLeft
            val chartHeight = height - timeScaleHeight

            if (candles.isEmpty()) {
                drawContext.canvas.nativeCanvas.drawText(
                    "Connecting to Binance USD-M Futures...",
                    chartWidth / 2f,
                    chartHeight / 2f,
                    Paint().apply {
                        color = TextSecondary.toArgb()
                        textSize = 36f
                        textAlign = Paint.Align.CENTER
                    }
                )
                return@Canvas
            }

            val candleSpacing = candleWidth * 0.25f
            val totalSlotWidth = candleWidth + candleSpacing
            val visibleCount = (chartWidth / totalSlotWidth).toInt() + 2

            val rightIndex = (candles.lastIndex - scrollOffset.toInt()).coerceIn(0, candles.lastIndex)
            val leftIndex = (rightIndex - visibleCount).coerceAtLeast(0)
            val visibleCandles = candles.subList(leftIndex, rightIndex + 1)

            if (visibleCandles.isEmpty()) return@Canvas

            // Compute visible min & max price
            var minP = Double.MAX_VALUE
            var maxP = Double.MIN_VALUE
            var maxVol = 0.0

            if (settings.autoScale || settings.manualMinPrice == null || settings.manualMaxPrice == null) {
                for (c in visibleCandles) {
                    if (c.low < minP) minP = c.low
                    if (c.high > maxP) maxP = c.high
                    if (c.volume > maxVol) maxVol = c.volume
                }
                // Add vertical padding
                val range = (maxP - minP).coerceAtLeast(0.0001)
                minP -= range * 0.08
                maxP += range * 0.08
            } else {
                minP = settings.manualMinPrice
                maxP = settings.manualMaxPrice
                for (c in visibleCandles) {
                    if (c.volume > maxVol) maxVol = c.volume
                }
            }

            val priceRange = (maxP - minP).coerceAtLeast(0.000001)
            val basePrice = visibleCandles.first().close

            fun priceToY(p: Double): Float {
                var norm = if (settings.isLogarithmic) {
                    val logP = ln(p.coerceAtLeast(0.00001))
                    val logMin = ln(minP.coerceAtLeast(0.00001))
                    val logMax = ln(maxP.coerceAtLeast(0.00001))
                    ((logP - logMin) / (logMax - logMin)).toFloat()
                } else {
                    ((p - minP) / priceRange).toFloat()
                }

                if (settings.isInverted) {
                    norm = 1f - norm
                }

                val y = chartHeight - (norm * chartHeight) + verticalPanOffset
                return y
            }

            fun yToPrice(y: Float): Double {
                val adjY = y - verticalPanOffset
                var norm = 1f - (adjY / chartHeight)
                if (settings.isInverted) norm = 1f - norm
                norm = norm.coerceIn(0f, 1f)

                return if (settings.isLogarithmic) {
                    val logMin = ln(minP.coerceAtLeast(0.00001))
                    val logMax = ln(maxP.coerceAtLeast(0.00001))
                    Math.exp((norm * (logMax - logMin) + logMin).toDouble())
                } else {
                    (minP + norm * priceRange)
                }
            }

            fun timeToX(t: Long): Float {
                val idx = candles.indexOfFirst { it.openTime >= t }
                return if (idx >= 0) {
                    val dist = rightIndex - idx
                    chartRight - (dist * totalSlotWidth) - (candleWidth / 2f)
                } else {
                    chartRight
                }
            }

            // 1. Watermark background showing Symbol, Timeframe, and Candle Price Source
            if (settings.showWatermark) {
                val watermarkPaint = Paint().apply {
                    color = Color.White.copy(alpha = 0.04f).toArgb()
                    textSize = chartWidth * 0.12f
                    typeface = Typeface.DEFAULT_BOLD
                    textAlign = Paint.Align.CENTER
                }
                drawContext.canvas.nativeCanvas.drawText(
                    symbol,
                    chartLeft + chartWidth / 2f,
                    chartHeight * 0.42f,
                    watermarkPaint
                )
                val tfPaint = Paint().apply {
                    color = Color.White.copy(alpha = 0.04f).toArgb()
                    textSize = chartWidth * 0.06f
                    typeface = Typeface.DEFAULT_BOLD
                    textAlign = Paint.Align.CENTER
                }
                val sourceLabel = when (candlePriceSource) {
                    CandlePriceSource.LAST -> "${timeframe.displayName} • LAST"
                    CandlePriceSource.MARK -> "${timeframe.displayName} • MARK PRICE"
                    CandlePriceSource.INDEX -> "${timeframe.displayName} • INDEX PRICE"
                }
                drawContext.canvas.nativeCanvas.drawText(
                    sourceLabel,
                    chartLeft + chartWidth / 2f,
                    chartHeight * 0.54f,
                    tfPaint
                )
            }

            // 2. Grid lines
            if (settings.showGrid) {
                val steps = 6
                val stepPrice = priceRange / steps
                for (i in 0..steps) {
                    val p = minP + i * stepPrice
                    val y = priceToY(p)
                    drawLine(
                        color = GridLineColor,
                        start = Offset(chartLeft, y),
                        end = Offset(chartRight, y),
                        strokeWidth = 1f
                    )
                }

                // Vertical time grid lines
                for (i in rightIndex downTo leftIndex step 10) {
                    val dist = rightIndex - i
                    val x = chartRight - (dist * totalSlotWidth)
                    drawLine(
                        color = GridLineColor,
                        start = Offset(x, 0f),
                        end = Offset(x, chartHeight),
                        strokeWidth = 1f
                    )
                }
            }

            // 3. Volume Bars (Bottom ~18%)
            if (settings.showVolume && maxVol > 0.0) {
                val volHeight = chartHeight * 0.18f
                for (i in rightIndex downTo leftIndex) {
                    val candle = candles[i]
                    val distFromRight = rightIndex - i
                    val x = chartRight - (distFromRight * totalSlotWidth) - candleWidth
                    val barH = ((candle.volume / maxVol) * volHeight).toFloat()
                    val y = chartHeight - barH

                    val volColor = if (candle.isBullish)
                        BullGreen.copy(alpha = 0.3f)
                    else
                        BearRed.copy(alpha = 0.3f)

                    drawRect(
                        color = volColor,
                        topLeft = Offset(x, y),
                        size = Size(candleWidth, barH)
                    )
                }
            }

            // 4. Candlesticks
            for (i in rightIndex downTo leftIndex) {
                val candle = candles[i]
                val distFromRight = rightIndex - i
                val x = chartRight - (distFromRight * totalSlotWidth) - candleWidth
                val centerX = x + candleWidth / 2f

                val openY = priceToY(candle.open)
                val closeY = priceToY(candle.close)
                val highY = priceToY(candle.high)
                val lowY = priceToY(candle.low)

                val candleColor = if (candle.isBullish) BullGreen else BearRed

                // Wick
                drawLine(
                    color = candleColor,
                    start = Offset(centerX, highY),
                    end = Offset(centerX, lowY),
                    strokeWidth = 1.5f
                )

                // Body
                val bodyTop = min(openY, closeY)
                val bodyBottom = max(openY, closeY)
                val bodyH = (bodyBottom - bodyTop).coerceAtLeast(1.5f)

                drawRect(
                    color = candleColor,
                    topLeft = Offset(x, bodyTop),
                    size = Size(candleWidth, bodyH)
                )
            }

            // 5. Draw User Drawings
            if (!areDrawingsHidden) {
                for (drawing in drawings) {
                    drawDrawingItem(
                        drawing = drawing,
                        isSelected = drawing.id == selectedDrawing?.id,
                        candles = candles,
                        rightIndex = rightIndex,
                        totalSlotWidth = totalSlotWidth,
                        candleWidth = candleWidth,
                        chartRight = chartRight,
                        chartLeft = chartLeft,
                        chartWidth = chartWidth,
                        chartHeight = chartHeight,
                        priceToY = ::priceToY,
                        timeToX = ::timeToX
                    )
                }

                // In-progress drawing preview
                if (inProgressPoints.isNotEmpty()) {
                    drawInProgressDrawing(
                        tool = activeDrawingTool,
                        points = inProgressPoints,
                        candles = candles,
                        rightIndex = rightIndex,
                        totalSlotWidth = totalSlotWidth,
                        chartRight = chartRight,
                        priceToY = ::priceToY,
                        timeToX = ::timeToX
                    )
                }
            }

            // 6. Alert Lines (clean horizontal alert line, no text clutter)
            val activeAlerts = alerts.filter { it.symbol.equals(symbol, ignoreCase = true) && it.isActive }
            for (alert in activeAlerts) {
                val alertY = priceToY(alert.targetPrice)
                if (alertY in 0f..chartHeight) {
                    drawLine(
                        color = AlertLineColor,
                        start = Offset(chartLeft, alertY),
                        end = Offset(chartRight, alertY),
                        strokeWidth = 1.8f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                    )
                    // Bell / price indicator on scale
                    val scaleX = if (isRightScale) chartRight else 0f
                    drawRect(
                        color = AlertLineColor,
                        topLeft = Offset(scaleX, alertY - 10f),
                        size = Size(priceScaleWidth, 20f)
                    )
                    drawContext.canvas.nativeCanvas.drawText(
                        formatPriceLabel(alert.targetPrice, settings, basePrice),
                        scaleX + priceScaleWidth / 2f,
                        alertY + 4f,
                        Paint().apply {
                            color = Color.Black.toArgb()
                            textSize = 22f
                            typeface = Typeface.DEFAULT_BOLD
                            textAlign = Paint.Align.CENTER
                        }
                    )
                }
            }

            // 7. Live Price Lines (Last, Mark, Index)
            val latestCandle = candles.lastOrNull()
            val lastP = if (priceInfo.lastPrice > 0.0) priceInfo.lastPrice else latestCandle?.close ?: 0.0

            // Mark Price line
            if (settings.showMarkPrice && priceInfo.markPrice > 0.0) {
                val markY = priceToY(priceInfo.markPrice)
                if (markY in 0f..chartHeight) {
                    drawLine(
                        color = MarkPriceCyan.copy(alpha = 0.7f),
                        start = Offset(chartLeft, markY),
                        end = Offset(chartRight, markY),
                        strokeWidth = 1f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                    )
                }
            }

            // Index Price line
            if (settings.showIndexPrice && priceInfo.indexPrice > 0.0) {
                val indexY = priceToY(priceInfo.indexPrice)
                if (indexY in 0f..chartHeight) {
                    drawLine(
                        color = IndexPricePurple.copy(alpha = 0.7f),
                        start = Offset(chartLeft, indexY),
                        end = Offset(chartRight, indexY),
                        strokeWidth = 1f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
                    )
                }
            }

            // Active Candle Price Line (Last, Mark, or Index depending on active candlePriceSource)
            val activeP = when (candlePriceSource) {
                CandlePriceSource.LAST -> lastP
                CandlePriceSource.MARK -> if (priceInfo.markPrice > 0) priceInfo.markPrice else latestCandle?.close ?: 0.0
                CandlePriceSource.INDEX -> if (priceInfo.indexPrice > 0) priceInfo.indexPrice else latestCandle?.close ?: 0.0
            }

            if (activeP > 0.0) {
                val activeY = priceToY(activeP)
                val lineColor = when (candlePriceSource) {
                    CandlePriceSource.MARK -> MarkPriceCyan
                    CandlePriceSource.INDEX -> IndexPricePurple
                    CandlePriceSource.LAST -> if (latestCandle?.isBullish == true) BullGreen else BearRed
                }
                if (activeY in 0f..chartHeight) {
                    drawLine(
                        color = lineColor,
                        start = Offset(chartLeft, activeY),
                        end = Offset(chartRight, activeY),
                        strokeWidth = 1.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
                    )

                    // Price Scale Pill
                    val scaleX = if (isRightScale) chartRight else 0f
                    drawRect(
                        color = lineColor,
                        topLeft = Offset(scaleX, activeY - 12f),
                        size = Size(priceScaleWidth, 24f)
                    )
                    drawContext.canvas.nativeCanvas.drawText(
                        formatPriceLabel(activeP, settings, basePrice),
                        scaleX + priceScaleWidth / 2f,
                        activeY + 5f,
                        Paint().apply {
                            color = if (candlePriceSource == CandlePriceSource.MARK) Color.Black.toArgb() else Color.White.toArgb()
                            textSize = 22f
                            typeface = Typeface.DEFAULT_BOLD
                            textAlign = Paint.Align.CENTER
                        }
                    )
                }
            }

            // 8. Price Scale Axis (Labels & Background)
            val scaleBgX = if (isRightScale) chartRight else 0f
            drawRect(
                color = DarkBackground,
                topLeft = Offset(scaleBgX, 0f),
                size = Size(priceScaleWidth, chartHeight)
            )
            drawLine(
                color = DarkBorder,
                start = Offset(if (isRightScale) chartRight else priceScaleWidth, 0f),
                end = Offset(if (isRightScale) chartRight else priceScaleWidth, chartHeight),
                strokeWidth = 1.2f
            )

            // Price Labels on Scale
            val numPriceLabels = 7
            val pStep = priceRange / numPriceLabels
            val scaleTextPaint = Paint().apply {
                color = TextSecondary.toArgb()
                textSize = 20f
                textAlign = Paint.Align.CENTER
            }
            for (i in 0..numPriceLabels) {
                val p = minP + i * pStep
                val y = priceToY(p)
                if (y in 10f..chartHeight - 10f) {
                    val label = formatPriceLabel(p, settings, basePrice)
                    drawContext.canvas.nativeCanvas.drawText(
                        label,
                        scaleBgX + priceScaleWidth / 2f,
                        y + 4f,
                        scaleTextPaint
                    )
                }
            }

            // 9. Time Scale Axis (Bottom)
            drawRect(
                color = DarkBackground,
                topLeft = Offset(0f, chartHeight),
                size = Size(width, timeScaleHeight)
            )
            drawLine(
                color = DarkBorder,
                start = Offset(0f, chartHeight),
                end = Offset(width, chartHeight),
                strokeWidth = 1.2f
            )

            val timeFormat = if (timeframe.isSeconds) {
                SimpleDateFormat("HH:mm:ss", Locale.US)
            } else if (timeframe.durationMs < 86_400_000L) {
                SimpleDateFormat("HH:mm", Locale.US)
            } else {
                SimpleDateFormat("MM/dd", Locale.US)
            }

            val timeTextPaint = Paint().apply {
                color = TextSecondary.toArgb()
                textSize = 19f
                textAlign = Paint.Align.CENTER
            }

            val timeStepSlots = (visibleCount / 5).coerceAtLeast(4)
            for (i in rightIndex downTo leftIndex step timeStepSlots) {
                val candle = candles[i]
                val dist = rightIndex - i
                val x = chartRight - (dist * totalSlotWidth) - candleWidth / 2f
                if (x in chartLeft..chartRight) {
                    val dateStr = timeFormat.format(Date(candle.openTime))
                    drawContext.canvas.nativeCanvas.drawText(
                        dateStr,
                        x,
                        chartHeight + 26f,
                        timeTextPaint
                    )
                }
            }

            // 10. Crosshair Overlay (Only when enabled)
            if (isCrosshairEnabled) {
                crosshairPos?.let { pos ->
                    if (pos.x in chartLeft..chartRight && pos.y in 0f..chartHeight) {
                        // Dotted crosshair lines
                        drawLine(
                            color = CrosshairColor,
                            start = Offset(chartLeft, pos.y),
                            end = Offset(chartRight, pos.y),
                            strokeWidth = 1f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
                        )
                        drawLine(
                            color = CrosshairColor,
                            start = Offset(pos.x, 0f),
                            end = Offset(pos.x, chartHeight),
                            strokeWidth = 1f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
                        )

                        // Price Tag on Scale
                        val hoveredPrice = yToPrice(pos.y)
                        val priceTagX = if (isRightScale) chartRight else 0f
                        drawRect(
                            color = DarkBorder,
                            topLeft = Offset(priceTagX, pos.y - 10f),
                            size = Size(priceScaleWidth, 20f)
                        )
                        drawContext.canvas.nativeCanvas.drawText(
                            formatPriceLabel(hoveredPrice, settings, basePrice),
                            priceTagX + priceScaleWidth / 2f,
                            pos.y + 4f,
                            Paint().apply {
                                color = Color.White.toArgb()
                                textSize = 20f
                                typeface = Typeface.DEFAULT_BOLD
                                textAlign = Paint.Align.CENTER
                            }
                        )

                        // Nearest candle under crosshair X
                        var nearestCandle: Candle? = null
                        var minDist = Float.MAX_VALUE
                        for (i in rightIndex downTo leftIndex) {
                            val c = candles[i]
                            val dist = rightIndex - i
                            val cx = chartRight - (dist * totalSlotWidth) - candleWidth / 2f
                            val d = abs(pos.x - cx)
                            if (d < minDist) {
                                minDist = d
                                nearestCandle = c
                            }
                        }

                        nearestCandle?.let { c ->
                            hoveredCandle = c
                            val timeStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(c.openTime))
                            val badgeW = 200f
                            val badgeX = (pos.x - badgeW / 2f).coerceIn(0f, width - badgeW)
                            drawRect(
                                color = DarkBorder,
                                topLeft = Offset(badgeX, chartHeight),
                                size = Size(badgeW, timeScaleHeight)
                            )
                            drawContext.canvas.nativeCanvas.drawText(
                                timeStr,
                                badgeX + badgeW / 2f,
                                chartHeight + 26f,
                                Paint().apply {
                                    color = Color.White.toArgb()
                                    textSize = 20f
                                    textAlign = Paint.Align.CENTER
                                }
                            )
                        }
                    }
                }
            }

            // 11. OHLCV Banner at Top Left
            val displayCandle = if (isCrosshairEnabled) (hoveredCandle ?: latestCandle) else latestCandle
            if (displayCandle != null) {
                val ohlcPaint = Paint().apply {
                    textSize = 22f
                    isAntiAlias = true
                }
                val ohlcColor = if (displayCandle.isBullish) BullGreen.toArgb() else BearRed.toArgb()

                var curX = chartLeft + 16f
                val curY = 32f

                // Symbol + Timeframe + Price Source indicator
                ohlcPaint.color = TextPrimary.toArgb()
                ohlcPaint.typeface = Typeface.DEFAULT_BOLD
                val candleModeText = when (candlePriceSource) {
                    CandlePriceSource.LAST -> ""
                    CandlePriceSource.MARK -> " [MARK]"
                    CandlePriceSource.INDEX -> " [INDEX]"
                }
                val headerText = "$symbol • ${timeframe.displayName}$candleModeText  "
                drawContext.canvas.nativeCanvas.drawText(headerText, curX, curY, ohlcPaint)
                curX += ohlcPaint.measureText(headerText)

                ohlcPaint.typeface = Typeface.DEFAULT
                // O
                ohlcPaint.color = TextSecondary.toArgb()
                drawContext.canvas.nativeCanvas.drawText("O ", curX, curY, ohlcPaint)
                curX += ohlcPaint.measureText("O ")
                ohlcPaint.color = ohlcColor
                val oStr = "${formatPrice(displayCandle.open)} "
                drawContext.canvas.nativeCanvas.drawText(oStr, curX, curY, ohlcPaint)
                curX += ohlcPaint.measureText(oStr)

                // H
                ohlcPaint.color = TextSecondary.toArgb()
                drawContext.canvas.nativeCanvas.drawText("H ", curX, curY, ohlcPaint)
                curX += ohlcPaint.measureText("H ")
                ohlcPaint.color = ohlcColor
                val hStr = "${formatPrice(displayCandle.high)} "
                drawContext.canvas.nativeCanvas.drawText(hStr, curX, curY, ohlcPaint)
                curX += ohlcPaint.measureText(hStr)

                // L
                ohlcPaint.color = TextSecondary.toArgb()
                drawContext.canvas.nativeCanvas.drawText("L ", curX, curY, ohlcPaint)
                curX += ohlcPaint.measureText("L ")
                ohlcPaint.color = ohlcColor
                val lStr = "${formatPrice(displayCandle.low)} "
                drawContext.canvas.nativeCanvas.drawText(lStr, curX, curY, ohlcPaint)
                curX += ohlcPaint.measureText(lStr)

                // C
                ohlcPaint.color = TextSecondary.toArgb()
                drawContext.canvas.nativeCanvas.drawText("C ", curX, curY, ohlcPaint)
                curX += ohlcPaint.measureText("C ")
                ohlcPaint.color = ohlcColor
                val cStr = "${formatPrice(displayCandle.close)} "
                drawContext.canvas.nativeCanvas.drawText(cStr, curX, curY, ohlcPaint)
                curX += ohlcPaint.measureText(cStr)

                // % Change
                val changeStr = (if (displayCandle.changePercent >= 0) "+" else "") +
                        String.format(Locale.US, "%.2f%%", displayCandle.changePercent)
                drawContext.canvas.nativeCanvas.drawText(changeStr, curX, curY, ohlcPaint)
            }
        }

        // Floating Quick Snap-to-Latest Button (Appears when user has scrolled back in history)
        AnimatedVisibility(
            visible = scrollOffset > 5f,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 155.dp, bottom = 52.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(TVBlue)
                    .border(1.dp, Color.White, RoundedCornerShape(16.dp))
                    .clickable {
                        scrollOffset = 0f
                        verticalPanOffset = 0f
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FastForward,
                        contentDescription = "Jump to Latest",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Latest",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Floating On-Chart Zoom / Crosshair Controls (Top Right corner)
        Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface.copy(alpha = 0.88f)),
            border = androidx.compose.foundation.BorderStroke(0.8.dp, DarkBorder),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 152.dp, top = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Zoom in
                IconButton(
                    onClick = { candleWidth = (candleWidth * 1.25f).coerceAtMost(60f) },
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Zoom In",
                        tint = TextPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Zoom out
                IconButton(
                    onClick = { candleWidth = (candleWidth * 0.8f).coerceAtLeast(4f) },
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Zoom Out",
                        tint = TextPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Crosshair toggle button
                IconButton(
                    onClick = {
                        isCrosshairEnabled = !isCrosshairEnabled
                        if (!isCrosshairEnabled) {
                            crosshairPos = null
                            hoveredCandle = null
                        }
                    },
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CenterFocusStrong,
                        contentDescription = "Crosshair",
                        tint = if (isCrosshairEnabled) TVBlue else TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Reset view
                IconButton(
                    onClick = {
                        scrollOffset = 0f
                        verticalPanOffset = 0f
                        candleWidth = 16f
                    },
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset Chart View",
                        tint = TextSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}

// Drawing Renderer Helpers
private fun DrawScope.drawDrawingItem(
    drawing: DrawingItem,
    isSelected: Boolean,
    candles: List<Candle>,
    rightIndex: Int,
    totalSlotWidth: Float,
    candleWidth: Float,
    chartRight: Float,
    chartLeft: Float,
    chartWidth: Float,
    chartHeight: Float,
    priceToY: (Double) -> Float,
    timeToX: (Long) -> Float
) {
    val drawColor = Color(drawing.colorHex)
    val stroke = Stroke(
        width = drawing.strokeWidth * 2f,
        pathEffect = if (drawing.isDashed) PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f) else null
    )

    when (drawing.toolType) {
        DrawingToolType.HORIZONTAL_LINE -> {
            val p = drawing.points.firstOrNull()?.price ?: return
            val y = priceToY(p)
            drawLine(
                color = drawColor,
                start = Offset(chartLeft, y),
                end = Offset(chartRight, y),
                strokeWidth = drawing.strokeWidth * 2f,
                pathEffect = stroke.pathEffect
            )
            if (isSelected) {
                drawCircle(color = TVBlue, radius = 6f, center = Offset(chartLeft + 30f, y))
                drawCircle(color = TVBlue, radius = 6f, center = Offset(chartRight - 30f, y))
            }
        }
        DrawingToolType.VERTICAL_LINE -> {
            val t = drawing.points.firstOrNull()?.timestamp ?: return
            val x = timeToX(t)
            drawLine(
                color = drawColor,
                start = Offset(x, 0f),
                end = Offset(x, chartHeight),
                strokeWidth = drawing.strokeWidth * 2f,
                pathEffect = stroke.pathEffect
            )
            if (isSelected) {
                drawCircle(color = TVBlue, radius = 6f, center = Offset(x, chartHeight * 0.3f))
                drawCircle(color = TVBlue, radius = 6f, center = Offset(x, chartHeight * 0.7f))
            }
        }
        DrawingToolType.TREND_LINE -> {
            if (drawing.points.size < 2) return
            val p1 = drawing.points[0]
            val p2 = drawing.points[1]
            val start = Offset(timeToX(p1.timestamp), priceToY(p1.price))
            val end = Offset(timeToX(p2.timestamp), priceToY(p2.price))
            drawLine(color = drawColor, start = start, end = end, strokeWidth = drawing.strokeWidth * 2f, pathEffect = stroke.pathEffect)
            if (isSelected) {
                drawCircle(color = TVBlue, radius = 6f, center = start)
                drawCircle(color = TVBlue, radius = 6f, center = end)
            }
        }
        DrawingToolType.HORIZONTAL_RAY -> {
            if (drawing.points.isEmpty()) return
            val p = drawing.points[0]
            val start = Offset(timeToX(p.timestamp), priceToY(p.price))
            drawLine(color = drawColor, start = start, end = Offset(chartRight, start.y), strokeWidth = drawing.strokeWidth * 2f, pathEffect = stroke.pathEffect)
            if (isSelected) {
                drawCircle(color = TVBlue, radius = 6f, center = start)
            }
        }
        DrawingToolType.RECTANGLE -> {
            if (drawing.points.size < 2) return
            val p1 = drawing.points[0]
            val p2 = drawing.points[1]
            val x1 = timeToX(p1.timestamp)
            val y1 = priceToY(p1.price)
            val x2 = timeToX(p2.timestamp)
            val y2 = priceToY(p2.price)

            val left = min(x1, x2)
            val top = min(y1, y2)
            val rectW = abs(x2 - x1)
            val rectH = abs(y2 - y1)

            // Fill
            drawRect(
                color = drawColor.copy(alpha = 0.15f),
                topLeft = Offset(left, top),
                size = Size(rectW, rectH)
            )
            // Border
            drawRect(
                color = drawColor,
                topLeft = Offset(left, top),
                size = Size(rectW, rectH),
                style = stroke
            )
            if (isSelected) {
                drawCircle(color = TVBlue, radius = 6f, center = Offset(x1, y1))
                drawCircle(color = TVBlue, radius = 6f, center = Offset(x2, y2))
            }
        }
        DrawingToolType.FIB_RETRACEMENT -> {
            if (drawing.points.size < 2) return
            val p1 = drawing.points[0]
            val p2 = drawing.points[1]
            val startX = min(timeToX(p1.timestamp), timeToX(p2.timestamp))
            val diff = p2.price - p1.price

            val levels = listOf(0.0, 0.236, 0.382, 0.5, 0.618, 0.786, 1.0, 1.618)
            val levelColors = listOf(
                Color(0xFF787B86), Color(0xFFF23645), Color(0xFFFFA726),
                Color(0xFF089981), Color(0xFF26A69A), Color(0xFF2962FF),
                Color(0xFF787B86), Color(0xFF9C27B0)
            )

            levels.forEachIndexed { idx, fib ->
                val levelPrice = p1.price + (diff * fib)
                val ly = priceToY(levelPrice)
                val col = levelColors.getOrElse(idx) { drawColor }

                drawLine(
                    color = col,
                    start = Offset(startX, ly),
                    end = Offset(chartRight, ly),
                    strokeWidth = 1.2f
                )
                drawContext.canvas.nativeCanvas.drawText(
                    "$fib (${String.format(Locale.US, "%.2f", levelPrice)})",
                    startX + 8f,
                    ly - 4f,
                    Paint().apply {
                        color = col.toArgb()
                        textSize = 18f
                    }
                )
            }
        }
        DrawingToolType.PRICE_RANGE -> {
            if (drawing.points.size < 2) return
            val p1 = drawing.points[0]
            val p2 = drawing.points[1]
            val x1 = timeToX(p1.timestamp)
            val y1 = priceToY(p1.price)
            val x2 = timeToX(p2.timestamp)
            val y2 = priceToY(p2.price)

            val left = min(x1, x2)
            val top = min(y1, y2)
            val rectW = abs(x2 - x1)
            val rectH = abs(y2 - y1)

            val priceDiff = p2.price - p1.price
            val pctDiff = if (p1.price > 0) (priceDiff / p1.price) * 100.0 else 0.0

            val boxColor = if (priceDiff >= 0) BullGreen else BearRed
            drawRect(
                color = boxColor.copy(alpha = 0.2f),
                topLeft = Offset(left, top),
                size = Size(rectW, rectH)
            )
            drawRect(
                color = boxColor,
                topLeft = Offset(left, top),
                size = Size(rectW, rectH),
                style = Stroke(1.5f)
            )

            val label = String.format(Locale.US, "%.2f (%+.2f%%)", priceDiff, pctDiff)
            drawContext.canvas.nativeCanvas.drawText(
                label,
                left + rectW / 2f,
                top + rectH / 2f,
                Paint().apply {
                    color = Color.White.toArgb()
                    textSize = 20f
                    typeface = Typeface.DEFAULT_BOLD
                    textAlign = Paint.Align.CENTER
                }
            )
        }
        DrawingToolType.TEXT -> {
            if (drawing.points.isEmpty()) return
            val p = drawing.points[0]
            val x = timeToX(p.timestamp)
            val y = priceToY(p.price)
            drawContext.canvas.nativeCanvas.drawText(
                drawing.textContent.ifBlank { "Note" },
                x,
                y,
                Paint().apply {
                    color = drawColor.toArgb()
                    textSize = 24f
                    typeface = Typeface.DEFAULT_BOLD
                }
            )
        }
        DrawingToolType.BRUSH -> {
            if (drawing.points.size < 2) return
            val path = Path()
            val first = drawing.points[0]
            path.moveTo(timeToX(first.timestamp), priceToY(first.price))
            for (i in 1 until drawing.points.size) {
                val pt = drawing.points[i]
                path.lineTo(timeToX(pt.timestamp), priceToY(pt.price))
            }
            drawPath(path = path, color = drawColor, style = stroke)
        }
        else -> {
            if (drawing.points.size >= 2) {
                val p1 = drawing.points[0]
                val p2 = drawing.points[1]
                drawLine(
                    color = drawColor,
                    start = Offset(timeToX(p1.timestamp), priceToY(p1.price)),
                    end = Offset(timeToX(p2.timestamp), priceToY(p2.price)),
                    strokeWidth = drawing.strokeWidth * 2f
                )
            }
        }
    }
}

private fun DrawScope.drawInProgressDrawing(
    tool: DrawingToolType,
    points: List<ChartPoint>,
    candles: List<Candle>,
    rightIndex: Int,
    totalSlotWidth: Float,
    chartRight: Float,
    priceToY: (Double) -> Float,
    timeToX: (Long) -> Float
) {
    if (points.isEmpty()) return
    val p1 = points[0]
    val o1 = Offset(timeToX(p1.timestamp), priceToY(p1.price))
    drawCircle(color = TVBlue, radius = 6f, center = o1)
}

private fun screenToChartPoint(
    tap: Offset,
    candles: List<Candle>,
    candleWidth: Float,
    scrollOffset: Float,
    chartWidth: Float,
    chartHeight: Float,
    settings: ChartScaleSettings,
    verticalOffset: Float
): ChartPoint? {
    if (candles.isEmpty()) return null
    val totalSlot = candleWidth + (candleWidth * 0.25f)
    val rightIndex = (candles.lastIndex - scrollOffset.toInt()).coerceIn(0, candles.lastIndex)

    val distFromRight = ((chartWidth - tap.x) / totalSlot).roundToInt()
    val candleIdx = (rightIndex - distFromRight).coerceIn(0, candles.lastIndex)
    val candle = candles[candleIdx]

    val minP = candles.minOfOrNull { it.low } ?: 0.0
    val maxP = candles.maxOfOrNull { it.high } ?: 1.0
    val range = (maxP - minP).coerceAtLeast(0.0001)

    val norm = (1f - ((tap.y - verticalOffset) / chartHeight)).coerceIn(0f, 1f)
    val price = minP + norm * range

    return ChartPoint(timestamp = candle.openTime, price = price)
}

private fun findDrawingNearPoint(
    tapOffset: Offset,
    drawings: List<DrawingItem>,
    candles: List<Candle>,
    candleWidth: Float,
    scrollOffset: Float,
    chartWidth: Float,
    chartHeight: Float,
    settings: ChartScaleSettings,
    verticalOffset: Float
): DrawingItem? {
    for (d in drawings.reversed()) {
        for (pt in d.points) {
            val idx = candles.indexOfFirst { it.openTime >= pt.timestamp }
            if (idx >= 0) {
                val totalSlot = candleWidth + (candleWidth * 0.25f)
                val rightIndex = (candles.lastIndex - scrollOffset.toInt()).coerceIn(0, candles.lastIndex)
                val dist = rightIndex - idx
                val x = chartWidth - (dist * totalSlot)
                val minP = candles.minOfOrNull { it.low } ?: 0.0
                val maxP = candles.maxOfOrNull { it.high } ?: 1.0
                val range = (maxP - minP).coerceAtLeast(0.0001)
                val y = chartHeight - (((pt.price - minP) / range).toFloat() * chartHeight)
                if (abs(tapOffset.x - x) < 30f && abs(tapOffset.y - y) < 30f) {
                    return d
                }
            }
        }
    }
    return null
}

private fun formatPrice(price: Double): String {
    return when {
        price >= 1000.0 -> String.format(Locale.US, "%,.2f", price)
        price >= 1.0 -> String.format(Locale.US, "%.4f", price)
        price > 0.0001 -> String.format(Locale.US, "%.6f", price)
        else -> String.format(Locale.US, "%.8f", price)
    }
}

private fun formatPriceLabel(price: Double, settings: ChartScaleSettings, basePrice: Double): String {
    return if (settings.isPercentage && basePrice > 0.0) {
        val pct = ((price - basePrice) / basePrice) * 100.0
        (if (pct >= 0) "+" else "") + String.format(Locale.US, "%.2f%%", pct)
    } else {
        formatPrice(price)
    }
}
