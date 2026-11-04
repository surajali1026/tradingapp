package com.example.data.api

import retrofit2.http.GET
import retrofit2.http.Query

interface BinanceApiService {

    @GET("fapi/v1/exchangeInfo")
    suspend fun getExchangeInfo(): ExchangeInfoResponse

    @GET("fapi/v1/ticker/24hr")
    suspend fun get24hTickers(): List<Ticker24hResponse>

    @GET("fapi/v1/klines")
    suspend fun getKlines(
        @Query("symbol") symbol: String,
        @Query("interval") interval: String,
        @Query("limit") limit: Int = 500,
        @Query("endTime") endTime: Long? = null
    ): List<List<Any>>

    @GET("fapi/v1/markPriceKlines")
    suspend fun getMarkPriceKlines(
        @Query("symbol") symbol: String,
        @Query("interval") interval: String,
        @Query("limit") limit: Int = 500,
        @Query("endTime") endTime: Long? = null
    ): List<List<Any>>

    @GET("fapi/v1/indexPriceKlines")
    suspend fun getIndexPriceKlines(
        @Query("pair") pair: String,
        @Query("interval") interval: String,
        @Query("limit") limit: Int = 500,
        @Query("endTime") endTime: Long? = null
    ): List<List<Any>>

    @GET("fapi/v1/premiumIndex")
    suspend fun getPremiumIndex(
        @Query("symbol") symbol: String
    ): PremiumIndexResponse
}

data class ExchangeInfoResponse(
    val symbols: List<SymbolDto>
)

data class SymbolDto(
    val symbol: String,
    val status: String,
    val baseAsset: String,
    val quoteAsset: String,
    val pricePrecision: Int = 2,
    val quantityPrecision: Int = 3,
    val contractType: String? = null
)

data class Ticker24hResponse(
    val symbol: String,
    val priceChangePercent: String?,
    val lastPrice: String?,
    val volume: String?,
    val quoteVolume: String?,
    val highPrice: String?,
    val lowPrice: String?
)

data class PremiumIndexResponse(
    val symbol: String,
    val markPrice: String?,
    val indexPrice: String?,
    val lastFundingRate: String?,
    val nextFundingTime: Long?
)
