package com.example

import com.example.data.model.AlertCondition
import com.example.data.model.AlertItem
import com.example.data.model.Candle
import com.example.data.model.TimeUnit
import com.example.data.model.Timeframe
import com.example.engine.AlertEngine
import com.example.engine.CandleAggregator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TradingEngineUnitTest {

    @Test
    fun testCandleAggregator_aggregatesCorrectly() {
        // Base 1-minute candles
        val c1 = Candle(openTime = 0L, open = 100.0, high = 105.0, low = 98.0, close = 102.0, volume = 10.0, quoteVolume = 1000.0)
        val c2 = Candle(openTime = 60_000L, open = 102.0, high = 110.0, low = 101.0, close = 108.0, volume = 15.0, quoteVolume = 1500.0)
        val c3 = Candle(openTime = 120_000L, open = 108.0, high = 109.0, low = 95.0, close = 97.0, volume = 20.0, quoteVolume = 2000.0)

        val targetTf = Timeframe.createCustom(3, TimeUnit.MINUTES) // 3m timeframe
        val aggregated = CandleAggregator.aggregate(listOf(c1, c2, c3), targetTf)

        assertEquals(1, aggregated.size)
        val bar = aggregated.first()
        assertEquals(0L, bar.openTime)
        assertEquals(100.0, bar.open, 0.001) // first open
        assertEquals(110.0, bar.high, 0.001) // max high
        assertEquals(95.0, bar.low, 0.001)   // min low
        assertEquals(97.0, bar.close, 0.001) // last close
        assertEquals(45.0, bar.volume, 0.001) // sum volume
        assertEquals(4500.0, bar.quoteVolume, 0.001) // sum quoteVolume
    }

    @Test
    fun testAlertEngine_triggersPriceCrossesAbove() {
        var triggered = false
        val engine = AlertEngine { alert, price ->
            triggered = true
            assertEquals(100.0, alert.targetPrice, 0.001)
        }

        val alert = AlertItem(
            symbol = "BTCUSDT",
            targetPrice = 100.0,
            condition = AlertCondition.CROSSES_ABOVE
        )

        // Previous price 98
        engine.checkPrice("BTCUSDT", 98.0, listOf(alert), null)
        assertFalse(triggered)

        // Current price 102 -> crosses above 100!
        engine.checkPrice("BTCUSDT", 102.0, listOf(alert), null)
        assertTrue(triggered)
    }

    @Test
    fun testAlertEngine_triggersCandleCloseBelow() {
        var triggered = false
        val engine = AlertEngine { alert, price ->
            triggered = true
        }

        val alert = AlertItem(
            symbol = "BTCUSDT",
            targetPrice = 50.0,
            condition = AlertCondition.CANDLE_CLOSE_BELOW
        )

        val candle = Candle(
            openTime = 0L,
            open = 55.0,
            high = 56.0,
            low = 48.0,
            close = 49.0,
            volume = 10.0,
            isClosed = true
        )

        engine.checkCandleClose("BTCUSDT", candle, listOf(alert), null)
        assertTrue(triggered)
    }

    @Test
    fun testCustomTimeframe_creation() {
        val tf = Timeframe.createCustom(45, TimeUnit.MINUTES)
        assertEquals("45m", tf.displayName)
        assertEquals(2_700_000L, tf.durationMs)
        assertEquals("15m", tf.baseBinanceInterval)
        assertEquals(3, tf.aggregationFactor)
    }
}
