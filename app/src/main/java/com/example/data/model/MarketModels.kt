package com.example.data.model

enum class CandlePriceSource(val label: String, val shortLabel: String) {
    LAST("Last Price", "Last"),
    MARK("Mark Price", "Mark"),
    INDEX("Index Price", "Index")
}

data class Ticker24h(
    val symbol: String,
    val priceChangePercent: Double,
    val lastPrice: Double,
    val volume: Double,
    val quoteVolume: Double,
    val highPrice: Double = 0.0,
    val lowPrice: Double = 0.0
)

data class SymbolInfo(
    val symbol: String,
    val baseAsset: String,
    val quoteAsset: String,
    val pricePrecision: Int = 2,
    val quantityPrecision: Int = 3,
    val status: String = "TRADING"
) {
    val isUsdtPair: Boolean get() = quoteAsset.equals("USDT", ignoreCase = true) || symbol.endsWith("USDT")
}

data class PriceInfo(
    val lastPrice: Double = 0.0,
    val markPrice: Double = 0.0,
    val indexPrice: Double = 0.0,
    val fundingRate: Double? = null,
    val nextFundingTime: Long? = null
)
