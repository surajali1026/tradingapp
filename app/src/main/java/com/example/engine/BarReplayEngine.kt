package com.example.engine

import com.example.data.model.Candle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class BarReplayState(
    val isReplayMode: Boolean = false,
    val isPlaying: Boolean = false,
    val speedMultiplier: Float = 1.0f,
    val currentIndex: Int = 0,
    val totalCandles: Int = 0
)

class BarReplayEngine(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    private var allCandles: List<Candle> = emptyList()
    private var tickerJob: Job? = null

    private val _state = MutableStateFlow(BarReplayState())
    val state = _state.asStateFlow()

    private val _visibleCandles = MutableStateFlow<List<Candle>>(emptyList())
    val visibleCandles = _visibleCandles.asStateFlow()

    fun startReplay(candles: List<Candle>, startCandleIndex: Int) {
        if (candles.isEmpty()) return
        allCandles = candles
        val safeIndex = startCandleIndex.coerceIn(0, candles.lastIndex)
        _state.value = BarReplayState(
            isReplayMode = true,
            isPlaying = false,
            speedMultiplier = 1.0f,
            currentIndex = safeIndex,
            totalCandles = candles.size
        )
        emitCurrentSlice(safeIndex)
    }

    fun play() {
        if (!_state.value.isReplayMode) return
        _state.value = _state.value.copy(isPlaying = true)
        startTicker()
    }

    fun pause() {
        tickerJob?.cancel()
        _state.value = _state.value.copy(isPlaying = false)
    }

    fun togglePlayPause() {
        if (_state.value.isPlaying) {
            pause()
        } else {
            play()
        }
    }

    fun stepForward() {
        if (!_state.value.isReplayMode) return
        pause()
        val next = (_state.value.currentIndex + 1).coerceAtMost(allCandles.lastIndex)
        _state.value = _state.value.copy(currentIndex = next)
        emitCurrentSlice(next)
    }

    fun setSpeed(speed: Float) {
        _state.value = _state.value.copy(speedMultiplier = speed)
        if (_state.value.isPlaying) {
            startTicker()
        }
    }

    fun stopReplay() {
        tickerJob?.cancel()
        _state.value = BarReplayState()
        _visibleCandles.value = allCandles
        allCandles = emptyList()
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (isActive && _state.value.isPlaying) {
                val delayMs = (1000L / _state.value.speedMultiplier).toLong().coerceAtLeast(100L)
                delay(delayMs)
                val curr = _state.value.currentIndex
                if (curr < allCandles.lastIndex) {
                    val next = curr + 1
                    _state.value = _state.value.copy(currentIndex = next)
                    emitCurrentSlice(next)
                } else {
                    pause()
                    break
                }
            }
        }
    }

    private fun emitCurrentSlice(index: Int) {
        if (allCandles.isNotEmpty() && index in allCandles.indices) {
            _visibleCandles.value = allCandles.subList(0, index + 1)
        }
    }
}
