package com.example.data.model

enum class TimeUnit(val label: String, val multiplierMs: Long) {
    SECONDS("s", 1_000L),
    MINUTES("m", 60_000L),
    HOURS("H", 3_600_000L),
    DAYS("D", 86_400_000L)
}

data class Timeframe(
    val id: String,
    val displayName: String,
    val durationMs: Long,
    val nativeBinanceInterval: String?, // non-null if natively supported by Binance kline API
    val baseBinanceInterval: String,    // interval to fetch from Binance to aggregate
    val aggregationFactor: Int = 1,     // how many base candles form 1 target candle
    val isCustom: Boolean = false
) {
    val isSeconds: Boolean get() = durationMs < 60_000L

    companion object {
        val TF_1S = Timeframe("1s", "1s", 1_000L, null, "1m", 1)
        val TF_5S = Timeframe("5s", "5s", 5_000L, null, "1m", 1)
        val TF_15S = Timeframe("15s", "15s", 15_000L, null, "1m", 1)
        val TF_30S = Timeframe("30s", "30s", 30_000L, null, "1m", 1)

        val TF_1M = Timeframe("1m", "1m", 60_000L, "1m", "1m", 1)
        val TF_3M = Timeframe("3m", "3m", 180_000L, "3m", "3m", 1)
        val TF_5M = Timeframe("5m", "5m", 300_000L, "5m", "5m", 1)
        val TF_15M = Timeframe("15m", "15m", 900_000L, "15m", "15m", 1)
        val TF_30M = Timeframe("30m", "30m", 1_800_000L, "30m", "30m", 1)
        val TF_45M = Timeframe("45m", "45m", 2_700_000L, null, "15m", 3)

        val TF_1H = Timeframe("1H", "1H", 3_600_000L, "1h", "1h", 1)
        val TF_2H = Timeframe("2H", "2H", 7_200_000L, "2h", "2h", 1)
        val TF_3H = Timeframe("3H", "3H", 10_800_000L, null, "1h", 3)
        val TF_4H = Timeframe("4H", "4H", 14_400_000L, "4h", "4h", 1)
        val TF_6H = Timeframe("6H", "6H", 21_600_000L, "6h", "6h", 1)
        val TF_8H = Timeframe("8H", "8H", 28_800_000L, "8h", "8h", 1)
        val TF_12H = Timeframe("12H", "12H", 43_200_000L, "12h", "12h", 1)

        val TF_1D = Timeframe("1D", "1D", 86_400_000L, "1d", "1d", 1)
        val TF_1W = Timeframe("1W", "1W", 604_800_000L, "1w", "1w", 1)
        val TF_1MONTH = Timeframe("1M", "1M", 2_592_000_000L, "1M", "1M", 1)

        val STANDARD_TIMEFRAMES = listOf(
            TF_1S, TF_5S, TF_15S, TF_30S,
            TF_1M, TF_3M, TF_5M, TF_15M, TF_30M, TF_45M,
            TF_1H, TF_2H, TF_3H, TF_4H, TF_6H, TF_8H, TF_12H,
            TF_1D, TF_1W, TF_1MONTH
        )

        fun createCustom(value: Int, unit: TimeUnit): Timeframe {
            val durationMs = value * unit.multiplierMs
            val displayName = "$value${unit.label}"
            val id = "custom_$displayName"

            // Choose best base Binance interval for aggregation
            val (baseInterval, factor) = when (unit) {
                TimeUnit.SECONDS -> "1m" to 1
                TimeUnit.MINUTES -> {
                    when {
                        value % 15 == 0 -> "15m" to (value / 15)
                        value % 5 == 0 -> "5m" to (value / 5)
                        value % 3 == 0 -> "3m" to (value / 3)
                        else -> "1m" to value
                    }
                }
                TimeUnit.HOURS -> {
                    when {
                        value % 4 == 0 -> "4h" to (value / 4)
                        value % 2 == 0 -> "2h" to (value / 2)
                        else -> "1h" to value
                    }
                }
                TimeUnit.DAYS -> "1d" to value
            }

            return Timeframe(
                id = id,
                displayName = displayName,
                durationMs = durationMs,
                nativeBinanceInterval = null,
                baseBinanceInterval = baseInterval,
                aggregationFactor = factor,
                isCustom = true
            )
        }
    }
}
