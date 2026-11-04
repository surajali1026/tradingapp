package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CandlePriceSource
import com.example.data.model.DrawingToolType
import com.example.data.model.Timeframe
import com.example.ui.MainViewModel
import com.example.ui.alerts.AlertsDialog
import com.example.ui.chart.CandlestickChart
import com.example.ui.chart.PriceScaleSettingsDialog
import com.example.ui.drawings.DrawingToolbar
import com.example.ui.drawings.SelectedDrawingPropertyBar
import com.example.ui.markets.MarketListSheet
import com.example.ui.replay.BarReplayControls
import com.example.ui.theme.AlertLineColor
import com.example.ui.theme.BearRed
import com.example.ui.theme.BinanceGold
import com.example.ui.theme.BullGreen
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.IndexPricePurple
import com.example.ui.theme.MarkPriceCyan
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TVBlue
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.timeframe.CircularTimeframeSelector
import com.example.ui.timeframe.CustomTimeframeDialog
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                // Request Notification Permission on Android 13+
                val context = LocalContext.current
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { /* handled */ }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                TradingPlatformScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun TradingPlatformScreen(viewModel: MainViewModel) {
    // UI states from ViewModel
    val currentSymbol by viewModel.currentSymbol.collectAsStateWithLifecycle()
    val currentTimeframe by viewModel.currentTimeframe.collectAsStateWithLifecycle()
    val candlePriceSource by viewModel.candlePriceSource.collectAsStateWithLifecycle()
    val candles by viewModel.candles.collectAsStateWithLifecycle()
    val priceInfo by viewModel.priceInfo.collectAsStateWithLifecycle()
    val tickers by viewModel.tickers.collectAsStateWithLifecycle()
    val currentCategory by viewModel.currentCategory.collectAsStateWithLifecycle()
    val watchlistSymbols by viewModel.watchlistSymbols.collectAsStateWithLifecycle()
    val customTimeframes by viewModel.customTimeframes.collectAsStateWithLifecycle()
    val chartSettings by viewModel.chartSettings.collectAsStateWithLifecycle()
    val alerts by viewModel.alerts.collectAsStateWithLifecycle()
    val drawings by viewModel.drawings.collectAsStateWithLifecycle()
    val activeDrawingTool by viewModel.activeDrawingTool.collectAsStateWithLifecycle()
    val areDrawingsHidden by viewModel.areDrawingsHidden.collectAsStateWithLifecycle()
    val selectedDrawing by viewModel.selectedDrawing.collectAsStateWithLifecycle()
    val barReplayState by viewModel.barReplayState.collectAsStateWithLifecycle()
    val isSelectingReplayCandle by viewModel.isSelectingReplayCandle.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    // Dialog & overlay visibilities
    var showMarketSheet by remember { mutableStateOf(false) }
    var showScaleSettingsDialog by remember { mutableStateOf(false) }
    var showAlertsDialog by remember { mutableStateOf(false) }
    var showCustomTimeframeDialog by remember { mutableStateOf(false) }
    var isDrawingToolbarOpen by remember { mutableStateOf(false) }

    // Circular Timeframe Selector state
    var showCircularSelector by remember { mutableStateOf(false) }
    var circularSelectorOffset by remember { mutableStateOf(Offset(300f, 300f)) }

    // Android back handler
    BackHandler(
        enabled = showMarketSheet || showScaleSettingsDialog || showAlertsDialog ||
                showCustomTimeframeDialog || showCircularSelector || isDrawingToolbarOpen ||
                barReplayState.isReplayMode || isSelectingReplayCandle
    ) {
        when {
            showCircularSelector -> showCircularSelector = false
            showMarketSheet -> showMarketSheet = false
            showScaleSettingsDialog -> showScaleSettingsDialog = false
            showAlertsDialog -> showAlertsDialog = false
            showCustomTimeframeDialog -> showCustomTimeframeDialog = false
            isDrawingToolbarOpen -> isDrawingToolbarOpen = false
            isSelectingReplayCandle -> viewModel.toggleReplayCandleSelection()
            barReplayState.isReplayMode -> viewModel.stopReplay()
        }
    }

    val currentTicker = remember(tickers, currentSymbol) {
        tickers.firstOrNull { it.symbol.equals(currentSymbol, ignoreCase = true) }
    }

    // Common quick timeframes for the horizontal strip
    val quickTimeframes = remember {
        listOf(
            Timeframe.TF_1S,
            Timeframe.TF_1M,
            Timeframe.TF_3M,
            Timeframe.TF_5M,
            Timeframe.TF_15M,
            Timeframe.TF_30M,
            Timeframe.TF_1H,
            Timeframe.TF_4H,
            Timeframe.TF_1D,
            Timeframe.TF_1W
        )
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
        containerColor = DarkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
        ) {
            // 1. TOP HEADER APP BAR (Symbol, 24h Change, Quick Actions)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .background(DarkSurface)
                    .border(0.5.dp, DarkBorder)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Market Symbol Selector (Only USDT Perpetual Contracts)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { showMarketSheet = true }
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = currentSymbol,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Select USDT Contract",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )

                    // 24h Change Pill
                    currentTicker?.let { t ->
                        val isPos = t.priceChangePercent >= 0.0
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background((if (isPos) BullGreen else BearRed).copy(alpha = 0.2f))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = (if (isPos) "+" else "") + String.format(Locale.US, "%.2f%%", t.priceChangePercent),
                                color = if (isPos) BullGreen else BearRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Quick Header Action Icons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Drawing Toolbar toggle
                    IconButton(
                        onClick = { isDrawingToolbarOpen = !isDrawingToolbarOpen },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timeline,
                            contentDescription = "Drawing Tools",
                            tint = if (isDrawingToolbarOpen) TVBlue else TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Bar Replay toggle
                    IconButton(
                        onClick = {
                            if (barReplayState.isReplayMode) {
                                viewModel.stopReplay()
                            } else {
                                viewModel.toggleReplayCandleSelection()
                            }
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Bar Replay",
                            tint = if (barReplayState.isReplayMode || isSelectingReplayCandle) BinanceGold else TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Alerts button with active badge
                    val activeAlertsCount = alerts.count { it.symbol.equals(currentSymbol, ignoreCase = true) && it.isActive }
                    IconButton(
                        onClick = { showAlertsDialog = true },
                        modifier = Modifier.size(34.dp)
                    ) {
                        BadgedBox(
                            badge = {
                                if (activeAlertsCount > 0) {
                                    Badge(containerColor = AlertLineColor) {
                                        Text(text = activeAlertsCount.toString(), color = Color.Black)
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Alerts",
                                tint = if (activeAlertsCount > 0) AlertLineColor else TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Scale Settings button
                    IconButton(
                        onClick = { showScaleSettingsDialog = true },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Price Scale",
                            tint = TextSecondary,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }

            // 2. TIMEFRAME CHOICE SETTING BAR (Easy 1-tap selector + Radial Wheel + Custom TF)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .background(DarkBackground)
                    .border(0.5.dp, DarkBorder)
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Quick horizontal scrollable timeframe chips
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    quickTimeframes.forEach { tf ->
                        val isSelected = tf.id == currentTimeframe.id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) TVBlue else DarkSurfaceVariant)
                                .border(
                                    1.dp,
                                    if (isSelected) TVBlue else DarkBorder,
                                    RoundedCornerShape(6.dp)
                                )
                                .clickable { viewModel.selectTimeframe(tf) }
                                .padding(horizontal = 8.dp, vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tf.displayName,
                                color = if (isSelected) Color.White else TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }

                    // User Saved Custom Timeframes
                    customTimeframes.forEach { tf ->
                        val isSelected = tf.id == currentTimeframe.id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) BinanceGold else DarkSurfaceVariant)
                                .border(
                                    1.dp,
                                    if (isSelected) BinanceGold else DarkBorder,
                                    RoundedCornerShape(6.dp)
                                )
                                .clickable { viewModel.selectTimeframe(tf) }
                                .padding(horizontal = 8.dp, vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tf.displayName,
                                color = if (isSelected) Color.Black else BinanceGold,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // "+ Custom" Timeframe Dialog Opener
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(DarkSurfaceVariant)
                            .border(1.dp, DarkBorder, RoundedCornerShape(6.dp))
                            .clickable { showCustomTimeframeDialog = true }
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Custom TF",
                                tint = BinanceGold,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Custom",
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                // MT5-Style Radial Wheel Selector Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(TVBlue.copy(alpha = 0.25f))
                        .border(1.dp, TVBlue, RoundedCornerShape(6.dp))
                        .clickable {
                            circularSelectorOffset = Offset(300f, 320f)
                            showCircularSelector = true
                        }
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DonutLarge,
                            contentDescription = "Radial Wheel",
                            tint = TVBlue,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Wheel",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 3. CANDLE PRICE SOURCE SWITCHER & REAL-TIME PRICES STRIP
            // Allows user to click LAST, MARK, or INDEX price to switch chart candles!
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .background(DarkSurface)
                    .border(0.5.dp, DarkBorder)
                    .pointerInput(activeDrawingTool) {
                        // Horizontal swipe on this strip navigates to next/prev coin
                        if (activeDrawingTool == DrawingToolType.CURSOR) {
                            var totalDrag = 0f
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    if (totalDrag < -50f) {
                                        viewModel.switchToNextCoin()
                                    } else if (totalDrag > 50f) {
                                        viewModel.switchToPreviousCoin()
                                    }
                                    totalDrag = 0f
                                },
                                onHorizontalDrag = { _, dragAmount ->
                                    totalDrag += dragAmount
                                }
                            )
                        }
                    }
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Prev coin arrow
                IconButton(
                    onClick = { viewModel.switchToPreviousCoin() },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "Previous Coin",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // 3 Candle Price Selectors: [Last] [Mark] [Index]
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Candles:",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    // 1. Last Price (Click to switch chart to Last Price candles)
                    val isLastActive = candlePriceSource == CandlePriceSource.LAST
                    val lastP = if (priceInfo.lastPrice > 0.0) priceInfo.lastPrice else currentTicker?.lastPrice ?: 0.0
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isLastActive) TVBlue else DarkSurfaceVariant)
                            .border(
                                1.dp,
                                if (isLastActive) Color.White else DarkBorder,
                                RoundedCornerShape(6.dp)
                            )
                            .clickable { viewModel.setCandlePriceSource(CandlePriceSource.LAST) }
                            .padding(horizontal = 7.dp, vertical = 3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Last: ",
                                color = if (isLastActive) Color.White else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Normal
                            )
                            Text(
                                text = formatPrice(lastP),
                                color = if (isLastActive) Color.White else BullGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // 2. Mark Price (Click to switch chart to Mark Price candles)
                    val isMarkActive = candlePriceSource == CandlePriceSource.MARK
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isMarkActive) MarkPriceCyan.copy(alpha = 0.3f) else DarkSurfaceVariant)
                            .border(
                                1.dp,
                                if (isMarkActive) MarkPriceCyan else DarkBorder,
                                RoundedCornerShape(6.dp)
                            )
                            .clickable { viewModel.setCandlePriceSource(CandlePriceSource.MARK) }
                            .padding(horizontal = 7.dp, vertical = 3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(MarkPriceCyan)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Mark: ",
                                color = if (isMarkActive) MarkPriceCyan else TextSecondary,
                                fontSize = 11.sp
                            )
                            Text(
                                text = formatPrice(priceInfo.markPrice),
                                color = if (isMarkActive) Color.White else TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // 3. Index Price (Click to switch chart to Index Price candles)
                    val isIndexActive = candlePriceSource == CandlePriceSource.INDEX
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isIndexActive) IndexPricePurple.copy(alpha = 0.35f) else DarkSurfaceVariant)
                            .border(
                                1.dp,
                                if (isIndexActive) IndexPricePurple else DarkBorder,
                                RoundedCornerShape(6.dp)
                            )
                            .clickable { viewModel.setCandlePriceSource(CandlePriceSource.INDEX) }
                            .padding(horizontal = 7.dp, vertical = 3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(IndexPricePurple)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Index: ",
                                color = if (isIndexActive) IndexPricePurple else TextSecondary,
                                fontSize = 11.sp
                            )
                            Text(
                                text = formatPrice(priceInfo.indexPrice),
                                color = if (isIndexActive) Color.White else TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Next coin arrow
                IconButton(
                    onClick = { viewModel.switchToNextCoin() },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Next Coin",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // 4. BAR REPLAY BANNER / SELECTION PROMPT
            if (isSelectingReplayCandle) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(TVBlue)
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Tap any historical candle to start Bar Replay from that point",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            BarReplayControls(
                state = barReplayState,
                onTogglePlayPause = { viewModel.togglePlayPauseReplay() },
                onStepForward = { viewModel.stepForwardReplay() },
                onSelectSpeed = { viewModel.setReplaySpeed(it) },
                onStopReplay = { viewModel.stopReplay() }
            )

            // 5. SELECTED DRAWING PROPERTY BAR
            selectedDrawing?.let { drawing ->
                SelectedDrawingPropertyBar(
                    selectedDrawing = drawing,
                    onUpdateDrawing = { viewModel.updateDrawing(it) },
                    onDeleteDrawing = { viewModel.deleteDrawing(it) },
                    onDismissSelection = { viewModel.selectDrawing(null) }
                )
            }

            // 6. MAIN CANDLESTICK CHART CANVAS & FLOATING OVERLAYS
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                CandlestickChart(
                    symbol = currentSymbol,
                    candles = candles,
                    timeframe = currentTimeframe,
                    priceInfo = priceInfo,
                    candlePriceSource = candlePriceSource,
                    settings = chartSettings,
                    alerts = alerts,
                    drawings = drawings,
                    activeDrawingTool = activeDrawingTool,
                    areDrawingsHidden = areDrawingsHidden,
                    selectedDrawing = selectedDrawing,
                    onSelectDrawing = { viewModel.selectDrawing(it) },
                    onAddDrawing = { viewModel.addDrawing(it) },
                    onUpdateDrawing = { viewModel.updateDrawing(it) },
                    onLoadMoreHistory = { viewModel.loadMoreHistory() },
                    onSelectCandleForReplay = if (isSelectingReplayCandle) {
                        { candleIndex -> viewModel.startReplayFromIndex(candleIndex) }
                    } else null,
                    onOpenCircularTimeframe = { touchOffset ->
                        circularSelectorOffset = touchOffset
                        showCircularSelector = true
                    },
                    modifier = Modifier.fillMaxSize().testTag("candlestick_chart")
                )

                // Floating Drawing Toolbar
                DrawingToolbar(
                    isOpen = isDrawingToolbarOpen,
                    activeTool = activeDrawingTool,
                    areDrawingsHidden = areDrawingsHidden,
                    onSelectTool = { tool -> viewModel.setActiveDrawingTool(tool) },
                    onToggleHideDrawings = { viewModel.toggleHideDrawings() },
                    onDeleteAllDrawings = { viewModel.deleteAllDrawings() }
                )

                // MetaTrader 5 Circular Timeframe Wheel Overlay
                CircularTimeframeSelector(
                    isVisible = showCircularSelector,
                    centerOffset = circularSelectorOffset,
                    currentTimeframe = currentTimeframe,
                    onSelectTimeframe = { tf -> viewModel.selectTimeframe(tf) },
                    onOpenCustomDialog = { showCustomTimeframeDialog = true },
                    onDismiss = { showCircularSelector = false }
                )
            }
        }

        // 7. MODAL BOTTOM SHEETS & DIALOGS
        // Market List Sheet (USDT Contracts Only)
        MarketListSheet(
            isOpen = showMarketSheet,
            currentCategory = currentCategory,
            tickers = tickers,
            watchlistSymbols = watchlistSymbols,
            isLoading = isLoading,
            onSelectCategory = { viewModel.setCategory(it) },
            onSelectSymbol = { viewModel.selectSymbol(it) },
            onToggleWatchlist = { viewModel.toggleWatchlist(it) },
            onMoveWatchlistUp = { viewModel.moveWatchlistUp(it) },
            onMoveWatchlistDown = { viewModel.moveWatchlistDown(it) },
            onDismiss = { showMarketSheet = false }
        )

        // Custom Timeframe Dialog
        if (showCustomTimeframeDialog) {
            CustomTimeframeDialog(
                customTimeframes = customTimeframes,
                onAddCustomTimeframe = { viewModel.addCustomTimeframe(it) },
                onDeleteCustomTimeframe = { viewModel.deleteCustomTimeframe(it) },
                onSelectTimeframe = { viewModel.selectTimeframe(it) },
                onDismiss = { showCustomTimeframeDialog = false }
            )
        }

        // Price Scale Settings Dialog
        if (showScaleSettingsDialog) {
            PriceScaleSettingsDialog(
                settings = chartSettings,
                onUpdateSettings = { viewModel.updateChartSettings(it) },
                onResetScale = { viewModel.resetChartScale() },
                onDismiss = { showScaleSettingsDialog = false }
            )
        }

        // Alerts Dialog
        if (showAlertsDialog) {
            val lastPrice = if (priceInfo.lastPrice > 0.0) priceInfo.lastPrice else currentTicker?.lastPrice ?: 0.0
            AlertsDialog(
                symbol = currentSymbol,
                currentPrice = lastPrice,
                alerts = alerts,
                onAddAlert = { viewModel.addAlert(it) },
                onDeleteAlert = { viewModel.deleteAlert(it) },
                onToggleAlert = { id, active -> viewModel.toggleAlert(id, active) },
                onDismiss = { showAlertsDialog = false }
            )
        }
    }
}

private fun formatPrice(price: Double): String {
    return when {
        price >= 1000.0 -> String.format(Locale.US, "%,.2f", price)
        price >= 1.0 -> String.format(Locale.US, "%.4f", price)
        price > 0.0001 -> String.format(Locale.US, "%.6f", price)
        else -> String.format(Locale.US, "%.8f", price)
    }
}
