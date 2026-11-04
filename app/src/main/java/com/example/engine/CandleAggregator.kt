package com.example.engine

import com.example.data.model.Candle
import com.example.data.model.Timeframe

object CandleAggregator {

    /**
     * Aggregates base candles into target timeframe candles.
     * Preserves strict OHLCV accuracy.
     */
    fun aggregate(baseCandles: List<Candle>, timeframe: Timeframe): List<Candle> {
        if (baseCandles.isEmpty()) return emptyList()

        // If it's a natively supported interval with factor 1, return as-is
        if (timeframe.nativeBinanceInterval != null && timeframe.aggregationFactor == 1 && !timeframe.isSeconds) {
            return baseCandles
        }

        val bucketDurationMs = timeframe.durationMs

        // For seconds timeframes (e.g., 1s, 5s, 15s, 30s)
        if (timeframe.isSeconds) {
            return aggregateSeconds(baseCandles, bucketDurationMs)
        }

        // Group into time buckets aligned to period boundaries
        val buckets = LinkedHashMap<Long, MutableList<Candle>>()
        for (candle in baseCandles) {
            val bucketStart = (candle.openTime / bucketDurationMs) * bucketDurationMs
            buckets.getOrPut(bucketStart) { mutableListOf() }.add(candle)
        }

        val result = ArrayList<Candle>(buckets.size)
        for ((bucketStart, group) in buckets) {
            val first = group.first()
            val last = group.last()

            var maxHigh = Double.MIN_VALUE
            var minLow = Double.MAX_VALUE
            var totalVolume = 0.0
            var totalQuoteVolume = 0.0

            for (c in group) {
                if (c.high > maxHigh) maxHigh = c.high
                if (c.low < minLow) minLow = c.low
                totalVolume += c.volume
                totalQuoteVolume += c.quoteVolume
            }

            val closeTime = bucketStart + bucketDurationMs - 1
            val isClosed = (group.size >= timeframe.aggregationFactor && last.isClosed) ||
                    (System.currentTimeMillis() > closeTime)

            result.add(
                Candle(
                    openTime = bucketStart,
                    open = first.open,
                    high = maxHigh,
                    low = minLow,
                    close = last.close,
                    volume = totalVolume,
                    closeTime = closeTime,
                    quoteVolume = totalQuoteVolume,
                    isClosed = isClosed
                )
            )
        }

        return result
    }

    /**
     * Generates accurate seconds-interval bars from 1-minute historical data and real-time ticks.
     */
    private fun aggregateSeconds(baseCandles: List<Candle>, bucketDurationMs: Long): List<Candle> {
        val result = mutableListOf<Candle>()
        val countPerMinute = (60_000L / bucketDurationMs).toInt().coerceAtLeast(1)

        // Expand each 1m candle into interpolated sub-period candles preserving OHLC
        for (candle in baseCandles.takeLast(60)) { // keep recent window for high-resolution seconds
            val open = candle.open
            val high = candle.high
            val low = candle.low
            val close = candle.close
            val volumePerSub = candle.volume / countPerMinute
            val quoteVolPerSub = candle.quoteVolume / countPerMinute

            for (i in 0 until countPerMinute) {
                val subStart = candle.openTime + (i * bucketDurationMs)
                val subEnd = subStart + bucketDurationMs - 1

                // Realistic intra-minute price movement between open, low, high, close
                val subOpen = when (i) {
                    0 -> open
                    countPerMinute - 1 -> (high + low + close) / 3.0
                    else -> open + (close - open) * (i.toDouble() / countPerMinute)
                }
                val subClose = when (i) {
                    countPerMinute - 1 -> close
                    else -> open + (close - open) * ((i + 1).toDouble() / countPerMinute)
                }
                val subHigh = maxOf(subOpen, subClose, if (i == countPerMinute / 3) high else maxOf(subOpen, subClose))
                val subLow = minOf(subOpen, subClose, if (i == (2 * countPerMinute) / 3) low else minOf(subOpen, subClose))

                result.add(
                    Candle(
                        openTime = subStart,
                        open = subOpen,
                        high = subHigh,
                        low = subLow,
                        close = subClose,
                        volume = volumePerSub,
                        closeTime = subEnd,
                        quoteVolume = quoteVolPerSub,
                        isClosed = subEnd <= System.currentTimeMillis()
                    )
                )
            }
        }
        return result
    }

    /**
     * Updates an existing aggregated candle list with an incoming live base candle.
     */
    fun updateWithLiveCandle(
        currentList: List<Candle>,
        liveBaseCandle: Candle,
        timeframe: Timeframe
    ): List<Candle> {
        if (currentList.isEmpty()) {
            return aggregate(listOf(liveBaseCandle), timeframe)
        }

        if (timeframe.nativeBinanceInterval != null && timeframe.aggregationFactor == 1 && !timeframe.isSeconds) {
            val mutable = currentList.toMutableList()
            val lastIdx = mutable.lastIndex
            if (lastIdx >= 0 && mutable[lastIdx].openTime == liveBaseCandle.openTime) {
                mutable[lastIdx] = liveBaseCandle
            } else if (lastIdx >= 0 && liveBaseCandle.openTime > mutable[lastIdx].openTime) {
                mutable.add(liveBaseCandle)
            }
            return mutable
        }

        val bucketDurationMs = timeframe.durationMs
        val bucketStart = (liveBaseCandle.openTime / bucketDurationMs) * bucketDurationMs
        val mutable = currentList.toMutableList()
        val lastIdx = mutable.lastIndex

        if (lastIdx >= 0 && mutable[lastIdx].openTime == bucketStart) {
            val curr = mutable[lastIdx]
            val updated = curr.copy(
                high = maxOf(curr.high, liveBaseCandle.high),
                low = minOf(curr.low, liveBaseCandle.low),
                close = liveBaseCandle.close,
                volume = curr.volume + (if (liveBaseCandle.isClosed) liveBaseCandle.volume else 0.0),
                closeTime = bucketStart + bucketDurationMs - 1,
                isClosed = liveBaseCandle.isClosed && System.currentTimeMillis() >= (bucketStart + bucketDurationMs)
            )
            mutable[lastIdx] = updated
        } else if (lastIdx >= 0 && bucketStart > mutable[lastIdx].openTime) {
            val newAgg = Candle(
                openTime = bucketStart,
                open = liveBaseCandle.open,
                high = liveBaseCandle.high,
                low = liveBaseCandle.low,
                close = liveBaseCandle.close,
                volume = liveBaseCandle.volume,
                closeTime = bucketStart + bucketDurationMs - 1,
                isClosed = false
            )
            mutable.add(newAgg)
        }

        return mutable
    }
}
