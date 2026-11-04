package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.BinanceRepository
import com.example.data.api.WsMessage
import com.example.data.model.AlertItem
import com.example.data.model.Candle
import com.example.data.model.CandlePriceSource
import com.example.data.model.ChartScaleSettings
import com.example.data.model.DrawingItem
import com.example.data.model.DrawingToolType
import com.example.data.model.MarketCategory
import com.example.data.model.PriceInfo
import com.example.data.model.SymbolInfo
import com.example.data.model.Ticker24h
import com.example.data.model.Timeframe
import com.example.data.storage.TradingPreferences
import com.example.engine.AlertEngine
import com.example.engine.BarReplayEngine
import com.example.engine.BarReplayState
import com.example.engine.CandleAggregator
import com.example.service.AlertMonitoringService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BinanceRepository()
    private val preferences = TradingPreferences(application)
    private val replayEngine = BarReplayEngine(viewModelScope)

    // Alert Engine for in-app alert evaluation
    private val alertEngine = AlertEngine { triggeredAlert, triggeredPrice ->
        val current = _alerts.value.map {
            if (it.id == triggeredAlert.id) it.copy(isActive = false, triggeredAt = System.currentTimeMillis()) else it
        }
        _alerts.value = current
        preferences.saveAlerts(current)
    }

    // State flows
    private val _symbols = MutableStateFlow<List<SymbolInfo>>(emptyList())
    val symbols = _symbols.asStateFlow()

    private val _tickers = MutableStateFlow<List<Ticker24h>>(emptyList())
    val tickers = _tickers.asStateFlow()

    private val _currentSymbol = MutableStateFlow("BTCUSDT")
    val currentSymbol = _currentSymbol.asStateFlow()

    private val _currentTimeframe = MutableStateFlow(Timeframe.TF_15M)
    val currentTimeframe = _currentTimeframe.asStateFlow()

    // Candle Price Source: LAST, MARK, or INDEX price candles
    private val _candlePriceSource = MutableStateFlow(CandlePriceSource.LAST)
    val candlePriceSource = _candlePriceSource.asStateFlow()

    private val _rawBaseCandles = MutableStateFlow<List<Candle>>(emptyList())

    // Aggregated / Replayed candles shown on chart
    private val _candles = MutableStateFlow<List<Candle>>(emptyList())
    val candles = _candles.asStateFlow()

    private val _priceInfo = MutableStateFlow(PriceInfo())
    val priceInfo = _priceInfo.asStateFlow()

    private val _currentCategory = MutableStateFlow(MarketCategory.TOP_100_VOLUME)
    val currentCategory = _currentCategory.asStateFlow()

    private val _watchlistSymbols = MutableStateFlow(preferences.getWatchlist().filter { it.endsWith("USDT") })
    val watchlistSymbols = _watchlistSymbols.asStateFlow()

    private val _customTimeframes = MutableStateFlow(preferences.getCustomTimeframes())
    val customTimeframes = _customTimeframes.asStateFlow()

    private val _chartSettings = MutableStateFlow(preferences.getChartSettings())
    val chartSettings = _chartSettings.asStateFlow()

    private val _alerts = MutableStateFlow(preferences.getAllAlerts())
    val alerts = _alerts.asStateFlow()

    private val _drawings = MutableStateFlow<List<DrawingItem>>(emptyList())
    val drawings = _drawings.asStateFlow()

    private val _activeDrawingTool = MutableStateFlow(DrawingToolType.CURSOR)
    val activeDrawingTool = _activeDrawingTool.asStateFlow()

    private val _areDrawingsHidden = MutableStateFlow(false)
    val areDrawingsHidden = _areDrawingsHidden.asStateFlow()

    private val _selectedDrawing = MutableStateFlow<DrawingItem?>(null)
    val selectedDrawing = _selectedDrawing.asStateFlow()

    val barReplayState: StateFlow<BarReplayState> = replayEngine.state
    private val _isSelectingReplayCandle = MutableStateFlow(false)
    val isSelectingReplayCandle = _isSelectingReplayCandle.asStateFlow()

    // Orientation & Fullscreen states
    private val _orientationMode = MutableStateFlow(OrientationMode.PORTRAIT)
    val orientationMode = _orientationMode.asStateFlow()

    private val _isFullscreen = MutableStateFlow(false)
    val isFullscreen = _isFullscreen.asStateFlow()

    // Favorite Drawing Tools
    private val _favoriteDrawingTools = MutableStateFlow(preferences.getFavoriteDrawingTools())
    val favoriteDrawingTools = _favoriteDrawingTools.asStateFlow()

    private val _isFavoritesBarVisible = MutableStateFlow(preferences.isFavoritesBarVisible())
    val isFavoritesBarVisible = _isFavoritesBarVisible.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private var historyLoadJob: Job? = null

    init {
        // Start background alert monitoring service
        try {
            AlertMonitoringService.startService(application)
        } catch (e: Exception) {
            // ignore
        }

        loadSymbolsAndTickers()
        loadDrawingsForCurrentChart()
        listenToWebSocket()
        loadChartData(_currentSymbol.value, _currentTimeframe.value, _candlePriceSource.value)

        // Observe bar replay candles
        viewModelScope.launch {
            replayEngine.visibleCandles.collect { replayedList ->
                if (replayEngine.state.value.isReplayMode) {
                    _candles.value = replayedList
                }
            }
        }
    }

    private fun loadSymbolsAndTickers() {
        viewModelScope.launch {
            _isLoading.value = true
            val syms = repository.getSymbols()
            _symbols.value = syms
            val ticks = repository.get24hTickers()
            _tickers.value = ticks
            _isLoading.value = false
        }
    }

    private fun listenToWebSocket() {
        viewModelScope.launch {
            repository.wsMessages.collect { msg ->
                when (msg) {
                    is WsMessage.KlineUpdate -> {
                        if (msg.symbol.equals(_currentSymbol.value, ignoreCase = true)) {
                            // Update last price
                            _priceInfo.value = _priceInfo.value.copy(lastPrice = msg.candle.close)

                            // Check alerts on live price
                            alertEngine.checkPrice(_currentSymbol.value, msg.candle.close, _alerts.value, getApplication())
                            if (msg.candle.isClosed) {
                                alertEngine.checkCandleClose(_currentSymbol.value, msg.candle, _alerts.value, getApplication())
                            }

                            // Only inject into chart candles if candle source is LAST
                            if (_candlePriceSource.value == CandlePriceSource.LAST && !barReplayState.value.isReplayMode) {
                                handleLiveCandleUpdate(msg.candle)
                            }
                        }
                    }
                    is WsMessage.PriceUpdate -> {
                        if (msg.symbol.equals(_currentSymbol.value, ignoreCase = true)) {
                            val curr = _priceInfo.value
                            val updatedPrice = curr.copy(
                                markPrice = msg.priceInfo.markPrice,
                                indexPrice = msg.priceInfo.indexPrice,
                                fundingRate = msg.priceInfo.fundingRate ?: curr.fundingRate,
                                nextFundingTime = msg.priceInfo.nextFundingTime ?: curr.nextFundingTime
                            )
                            _priceInfo.value = updatedPrice

                            // If candle source is MARK or INDEX, update chart candle with latest mark/index price!
                            if (!barReplayState.value.isReplayMode) {
                                val currentPrice = when (_candlePriceSource.value) {
                                    CandlePriceSource.MARK -> updatedPrice.markPrice
                                    CandlePriceSource.INDEX -> updatedPrice.indexPrice
                                    CandlePriceSource.LAST -> 0.0
                                }
                                if (currentPrice > 0.0) {
                                    handleLivePriceTickForCandles(currentPrice)
                                }
                            }
                        }
                    }
                    is WsMessage.ConnectionState -> {
                        // connection status update
                    }
                }
            }
        }
    }

    private fun handleLiveCandleUpdate(liveBaseCandle: Candle) {
        val raw = _rawBaseCandles.value.toMutableList()
        val lastIdx = raw.lastIndex
        if (lastIdx >= 0 && raw[lastIdx].openTime == liveBaseCandle.openTime) {
            raw[lastIdx] = liveBaseCandle
        } else if (lastIdx >= 0 && liveBaseCandle.openTime > raw[lastIdx].openTime) {
            raw.add(liveBaseCandle)
        }
        _rawBaseCandles.value = raw

        // Aggregate into target timeframe
        _candles.value = CandleAggregator.updateWithLiveCandle(
            _candles.value,
            liveBaseCandle,
            _currentTimeframe.value
        )
    }

    private fun handleLivePriceTickForCandles(tickPrice: Double) {
        val list = _candles.value.toMutableList()
        if (list.isEmpty()) return
        val lastIdx = list.lastIndex
        val curr = list[lastIdx]
        list[lastIdx] = curr.copy(
            high = maxOf(curr.high, tickPrice),
            low = minOf(curr.low, tickPrice),
            close = tickPrice
        )
        _candles.value = list
    }

    fun selectSymbol(symbol: String) {
        if (_currentSymbol.value.equals(symbol, ignoreCase = true)) return
        _currentSymbol.value = symbol.uppercase()
        loadDrawingsForCurrentChart()
        loadChartData(symbol, _currentTimeframe.value, _candlePriceSource.value)
    }

    fun selectTimeframe(tf: Timeframe) {
        if (_currentTimeframe.value.id == tf.id) return
        _currentTimeframe.value = tf
        loadDrawingsForCurrentChart()
        loadChartData(_currentSymbol.value, tf, _candlePriceSource.value)
    }

    /**
     * Switches between Last Price, Mark Price, and Index Price candles.
     */
    fun setCandlePriceSource(source: CandlePriceSource) {
        if (_candlePriceSource.value == source) return
        _candlePriceSource.value = source
        loadChartData(_currentSymbol.value, _currentTimeframe.value, source)
    }

    private fun loadChartData(
        symbol: String,
        tf: Timeframe,
        source: CandlePriceSource = _candlePriceSource.value
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            // Subscribe WebSocket to the base interval needed
            repository.subscribe(symbol, tf.baseBinanceInterval)

            // Fetch initial historical candles from Binance for the selected price source (LAST, MARK, or INDEX)
            val limit = if (tf.aggregationFactor > 1) 1000 else 500
            val rawCandles = repository.getHistoricalKlines(
                symbol = symbol,
                interval = tf.baseBinanceInterval,
                limit = limit,
                priceSource = source
            )
            _rawBaseCandles.value = rawCandles

            // Aggregate into target timeframe
            val aggregated = CandleAggregator.aggregate(rawCandles, tf)
            _candles.value = aggregated

            // Fetch premium index for Mark & Index prices
            val premium = repository.getPremiumIndex(symbol)
            if (premium != null) {
                _priceInfo.value = premium.copy(
                    lastPrice = if (source == CandlePriceSource.LAST) (rawCandles.lastOrNull()?.close ?: 0.0) else _priceInfo.value.lastPrice
                )
            }
            _isLoading.value = false
        }
    }

    fun loadMoreHistory() {
        if (_isLoading.value) return
        val raw = _rawBaseCandles.value
        if (raw.isEmpty()) return
        val oldestTime = raw.first().openTime

        historyLoadJob?.cancel()
        historyLoadJob = viewModelScope.launch {
            val older = repository.getHistoricalKlines(
                symbol = _currentSymbol.value,
                interval = _currentTimeframe.value.baseBinanceInterval,
                limit = 500,
                endTime = oldestTime - 1,
                priceSource = _candlePriceSource.value
            )
            if (older.isNotEmpty()) {
                val combinedRaw = older + _rawBaseCandles.value
                _rawBaseCandles.value = combinedRaw
                _candles.value = CandleAggregator.aggregate(combinedRaw, _currentTimeframe.value)
            }
        }
    }

    // Coin Swipe Navigation
    fun switchToNextCoin() {
        val list = getOrderedCoinList()
        if (list.isEmpty()) return
        val current = _currentSymbol.value
        val idx = list.indexOfFirst { it.equals(current, ignoreCase = true) }
        val nextIdx = if (idx in 0 until list.lastIndex) idx + 1 else 0
        selectSymbol(list[nextIdx])
    }

    fun switchToPreviousCoin() {
        val list = getOrderedCoinList()
        if (list.isEmpty()) return
        val current = _currentSymbol.value
        val idx = list.indexOfFirst { it.equals(current, ignoreCase = true) }
        val prevIdx = if (idx > 0) idx - 1 else list.lastIndex
        selectSymbol(list[prevIdx])
    }

    private fun getOrderedCoinList(): List<String> {
        val allUsdtTickers = _tickers.value.filter { it.symbol.endsWith("USDT") }
        return when (_currentCategory.value) {
            MarketCategory.ALL_FUTURES -> allUsdtTickers.map { it.symbol }
            MarketCategory.TOP_100_VOLUME -> allUsdtTickers.sortedByDescending { it.quoteVolume }.take(100).map { it.symbol }
            MarketCategory.HIGHEST_VOLUME -> allUsdtTickers.sortedByDescending { it.quoteVolume }.map { it.symbol }
            MarketCategory.TOP_GAINERS -> allUsdtTickers.sortedByDescending { it.priceChangePercent }.map { it.symbol }
            MarketCategory.TOP_LOSERS -> allUsdtTickers.sortedBy { it.priceChangePercent }.map { it.symbol }
            MarketCategory.WATCHLIST -> _watchlistSymbols.value.filter { it.endsWith("USDT") }
        }
    }

    fun setCategory(cat: MarketCategory) {
        _currentCategory.value = cat
    }

    // Watchlist
    fun toggleWatchlist(symbol: String) {
        val sym = symbol.uppercase()
        val current = _watchlistSymbols.value.toMutableList()
        if (current.contains(sym)) {
            current.remove(sym)
        } else {
            current.add(sym)
        }
        _watchlistSymbols.value = current
        preferences.saveWatchlist(current)
    }

    fun moveWatchlistUp(symbol: String) {
        val sym = symbol.uppercase()
        val list = _watchlistSymbols.value.toMutableList()
        val idx = list.indexOf(sym)
        if (idx > 0) {
            list.removeAt(idx)
            list.add(idx - 1, sym)
            _watchlistSymbols.value = list
            preferences.saveWatchlist(list)
        }
    }

    fun moveWatchlistDown(symbol: String) {
        val sym = symbol.uppercase()
        val list = _watchlistSymbols.value.toMutableList()
        val idx = list.indexOf(sym)
        if (idx in 0 until list.lastIndex) {
            list.removeAt(idx)
            list.add(idx + 1, sym)
            _watchlistSymbols.value = list
            preferences.saveWatchlist(list)
        }
    }

    // Custom Timeframes
    fun addCustomTimeframe(tf: Timeframe) {
        preferences.saveCustomTimeframe(tf)
        _customTimeframes.value = preferences.getCustomTimeframes()
    }

    fun deleteCustomTimeframe(id: String) {
        preferences.deleteCustomTimeframe(id)
        _customTimeframes.value = preferences.getCustomTimeframes()
    }

    // Scale Settings
    fun updateChartSettings(settings: ChartScaleSettings) {
        _chartSettings.value = settings
        preferences.saveChartSettings(settings)
    }

    fun resetChartScale() {
        updateChartSettings(ChartScaleSettings())
    }

    // Drawings
    private fun loadDrawingsForCurrentChart() {
        val list = preferences.getDrawings(_currentSymbol.value, _currentTimeframe.value.id)
        _drawings.value = list
        _selectedDrawing.value = null
    }

    fun setActiveDrawingTool(tool: DrawingToolType) {
        _activeDrawingTool.value = tool
        _selectedDrawing.value = null
    }

    fun addDrawing(item: DrawingItem) {
        val list = _drawings.value.toMutableList()
        list.add(item)
        _drawings.value = list
        preferences.saveDrawings(_currentSymbol.value, _currentTimeframe.value.id, list)
        _activeDrawingTool.value = DrawingToolType.CURSOR
        _selectedDrawing.value = item
    }

    fun updateDrawing(item: DrawingItem) {
        val list = _drawings.value.map { if (it.id == item.id) item else it }
        _drawings.value = list
        _selectedDrawing.value = item
        preferences.saveDrawings(_currentSymbol.value, _currentTimeframe.value.id, list)
    }

    fun selectDrawing(drawing: DrawingItem?) {
        _selectedDrawing.value = drawing
    }

    fun deleteDrawing(id: String) {
        val list = _drawings.value.filter { it.id != id }
        _drawings.value = list
        _selectedDrawing.value = null
        preferences.saveDrawings(_currentSymbol.value, _currentTimeframe.value.id, list)
    }

    fun deleteAllDrawings() {
        _drawings.value = emptyList()
        _selectedDrawing.value = null
        preferences.saveDrawings(_currentSymbol.value, _currentTimeframe.value.id, emptyList())
    }

    fun toggleHideDrawings() {
        _areDrawingsHidden.value = !_areDrawingsHidden.value
    }

    // Alerts
    fun addAlert(item: AlertItem) {
        val list = _alerts.value.toMutableList()
        list.add(item)
        _alerts.value = list
        preferences.saveAlerts(list)
    }

    fun deleteAlert(id: String) {
        val list = _alerts.value.filter { it.id != id }
        _alerts.value = list
        preferences.saveAlerts(list)
    }

    fun toggleAlert(id: String, isActive: Boolean) {
        val list = _alerts.value.map { if (it.id == id) it.copy(isActive = isActive) else it }
        _alerts.value = list
        preferences.saveAlerts(list)
    }

    // Bar Replay
    fun toggleReplayCandleSelection() {
        _isSelectingReplayCandle.value = !_isSelectingReplayCandle.value
    }

    fun startReplayFromIndex(index: Int) {
        _isSelectingReplayCandle.value = false
        val currentCandles = _candles.value
        replayEngine.startReplay(currentCandles, index)
    }

    fun togglePlayPauseReplay() {
        replayEngine.togglePlayPause()
    }

    fun stepForwardReplay() {
        replayEngine.stepForward()
    }

    fun setReplaySpeed(speed: Float) {
        replayEngine.setSpeed(speed)
    }

    fun stopReplay() {
        replayEngine.stopReplay()
        // restore latest live candles
        _candles.value = CandleAggregator.aggregate(_rawBaseCandles.value, _currentTimeframe.value)
    }

    // Orientation & Fullscreen Actions
    fun toggleOrientation() {
        _orientationMode.value = when (_orientationMode.value) {
            OrientationMode.PORTRAIT -> OrientationMode.LANDSCAPE
            OrientationMode.LANDSCAPE -> OrientationMode.SENSOR
            OrientationMode.SENSOR -> OrientationMode.PORTRAIT
        }
    }

    fun setOrientationMode(mode: OrientationMode) {
        _orientationMode.value = mode
    }

    fun toggleFullscreen() {
        _isFullscreen.value = !_isFullscreen.value
    }

    fun setFullscreen(fullscreen: Boolean) {
        _isFullscreen.value = fullscreen
    }

    // Favorite Drawing Tools Actions
    fun toggleFavoriteDrawingTool(tool: DrawingToolType) {
        val current = _favoriteDrawingTools.value.toMutableList()
        if (current.contains(tool)) {
            current.remove(tool)
        } else {
            current.add(tool)
        }
        _favoriteDrawingTools.value = current
        preferences.saveFavoriteDrawingTools(current)
    }

    fun toggleFavoritesBar() {
        val next = !_isFavoritesBarVisible.value
        _isFavoritesBarVisible.value = next
        preferences.setFavoritesBarVisible(next)
    }

    override fun onCleared() {
        repository.disconnect()
        super.onCleared()
    }
}

enum class OrientationMode {
    PORTRAIT,
    LANDSCAPE,
    SENSOR
}
