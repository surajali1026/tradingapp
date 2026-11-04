package com.example.data.api

import android.util.Log
import com.example.data.model.Candle
import com.example.data.model.CandlePriceSource
import com.example.data.model.PriceInfo
import com.example.data.model.SymbolInfo
import com.example.data.model.Ticker24h
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class BinanceRepository(
    private val webSocketClient: BinanceWebSocketClient = BinanceWebSocketClient()
) {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    // Host selector interceptor that transparently redirects HTTP 451 (geo-restricted) to testnet.binancefuture.com
    private val geoFallbackInterceptor = object : Interceptor {
        @Volatile
        var activeHost = "fapi.binance.com"

        override fun intercept(chain: Interceptor.Chain): Response {
            val originalRequest = chain.request()
            val requestToExecute = if (originalRequest.url.host != activeHost) {
                val newUrl = originalRequest.url.newBuilder().host(activeHost).build()
                originalRequest.newBuilder().url(newUrl).build()
            } else {
                originalRequest
            }

            val response = chain.proceed(requestToExecute)
            if (response.code == 451 || response.code == 403) {
                response.close()
                Log.w(
                    "BinanceRepo",
                    "Received HTTP ${response.code} from ${requestToExecute.url.host}. Switching to unrestricted Binance Futures endpoint: testnet.binancefuture.com"
                )
                activeHost = "testnet.binancefuture.com"
                webSocketClient.useFallbackWs = true

                val fallbackUrl = originalRequest.url.newBuilder().host("testnet.binancefuture.com").build()
                val fallbackRequest = originalRequest.newBuilder().url(fallbackUrl).build()
                return chain.proceed(fallbackRequest)
            }
            return response
        }
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(geoFallbackInterceptor)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val primaryApiService: BinanceApiService = Retrofit.Builder()
        .baseUrl("https://fapi.binance.com/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(BinanceApiService::class.java)

    private val fallbackApiService: BinanceApiService = Retrofit.Builder()
        .baseUrl("https://testnet.binancefuture.com/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(BinanceApiService::class.java)

    @Volatile
    private var useFallbackDirectly = false

    val wsMessages: Flow<WsMessage> = webSocketClient.messages

    private suspend fun <T> executeApi(call: suspend (BinanceApiService) -> T): T {
        return if (useFallbackDirectly) {
            call(fallbackApiService)
        } else {
            try {
                call(primaryApiService)
            } catch (e: HttpException) {
                if (e.code() == 451 || e.code() == 403) {
                    Log.w("BinanceRepo", "HTTP ${e.code()} detected, routing directly to fallback API")
                    useFallbackDirectly = true
                    webSocketClient.useFallbackWs = true
                    call(fallbackApiService)
                } else {
                    throw e
                }
            } catch (e: Exception) {
                Log.w("BinanceRepo", "Primary API failed (${e.message}), trying fallback API")
                useFallbackDirectly = true
                webSocketClient.useFallbackWs = true
                call(fallbackApiService)
            }
        }
    }

    /**
     * Retrieves only USDT Perpetual Futures contracts.
     */
    suspend fun getSymbols(): List<SymbolInfo> = withContext(Dispatchers.IO) {
        try {
            val response = executeApi { it.getExchangeInfo() }
            val list = response.symbols
                .filter {
                    it.status == "TRADING" &&
                            (it.contractType == "PERPETUAL" || it.contractType == null) &&
                            (it.quoteAsset.equals("USDT", ignoreCase = true) || it.symbol.endsWith("USDT"))
                }
                .map {
                    SymbolInfo(
                        symbol = it.symbol,
                        baseAsset = it.baseAsset,
                        quoteAsset = it.quoteAsset,
                        pricePrecision = it.pricePrecision,
                        quantityPrecision = it.quantityPrecision,
                        status = it.status
                    )
                }
            if (list.isNotEmpty()) list else getEmergencyDefaultSymbols()
        } catch (e: Exception) {
            Log.e("BinanceRepo", "Failed to fetch symbols", e)
            getEmergencyDefaultSymbols()
        }
    }

    /**
     * Retrieves 24h tickers strictly for USDT pairs.
     */
    suspend fun get24hTickers(): List<Ticker24h> = withContext(Dispatchers.IO) {
        try {
            val list = executeApi { it.get24hTickers() }
            list
                .filter { it.symbol.endsWith("USDT") }
                .map { dto ->
                    Ticker24h(
                        symbol = dto.symbol,
                        priceChangePercent = dto.priceChangePercent?.toDoubleOrNull() ?: 0.0,
                        lastPrice = dto.lastPrice?.toDoubleOrNull() ?: 0.0,
                        volume = dto.volume?.toDoubleOrNull() ?: 0.0,
                        quoteVolume = dto.quoteVolume?.toDoubleOrNull() ?: 0.0,
                        highPrice = dto.highPrice?.toDoubleOrNull() ?: 0.0,
                        lowPrice = dto.lowPrice?.toDoubleOrNull() ?: 0.0
                    )
                }
        } catch (e: Exception) {
            Log.e("BinanceRepo", "Failed to fetch 24h tickers", e)
            emptyList()
        }
    }

    /**
     * Retrieves candles for Last, Mark, or Index price based on [priceSource].
     */
    suspend fun getHistoricalKlines(
        symbol: String,
        interval: String,
        limit: Int = 500,
        endTime: Long? = null,
        priceSource: CandlePriceSource = CandlePriceSource.LAST
    ): List<Candle> = withContext(Dispatchers.IO) {
        try {
            val sym = symbol.uppercase()
            val raw = executeApi { api ->
                when (priceSource) {
                    CandlePriceSource.LAST -> api.getKlines(
                        symbol = sym,
                        interval = interval,
                        limit = limit,
                        endTime = endTime
                    )
                    CandlePriceSource.MARK -> api.getMarkPriceKlines(
                        symbol = sym,
                        interval = interval,
                        limit = limit,
                        endTime = endTime
                    )
                    CandlePriceSource.INDEX -> api.getIndexPriceKlines(
                        pair = sym,
                        interval = interval,
                        limit = limit,
                        endTime = endTime
                    )
                }
            }
            raw.mapNotNull { item ->
                try {
                    val openTime = (item[0] as Number).toLong()
                    val open = (item[1] as String).toDouble()
                    val high = (item[2] as String).toDouble()
                    val low = (item[3] as String).toDouble()
                    val close = (item[4] as String).toDouble()
                    val volume = if (item.size > 5 && item[5] is String) (item[5] as String).toDoubleOrNull() ?: 0.0 else 0.0
                    val closeTime = if (item.size > 6 && item[6] is Number) (item[6] as Number).toLong() else openTime + 60_000L
                    val quoteVolume = if (item.size > 7 && item[7] is String) (item[7] as String).toDoubleOrNull() ?: 0.0 else 0.0
                    Candle(
                        openTime = openTime,
                        open = open,
                        high = high,
                        low = low,
                        close = close,
                        volume = volume,
                        closeTime = closeTime,
                        quoteVolume = quoteVolume,
                        isClosed = true
                    )
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("BinanceRepo", "Failed to fetch klines ($priceSource) for $symbol $interval", e)
            emptyList()
        }
    }

    suspend fun getPremiumIndex(symbol: String): PriceInfo? = withContext(Dispatchers.IO) {
        try {
            val dto = executeApi { it.getPremiumIndex(symbol.uppercase()) }
            PriceInfo(
                lastPrice = 0.0,
                markPrice = dto.markPrice?.toDoubleOrNull() ?: 0.0,
                indexPrice = dto.indexPrice?.toDoubleOrNull() ?: 0.0,
                fundingRate = dto.lastFundingRate?.toDoubleOrNull(),
                nextFundingTime = dto.nextFundingTime
            )
        } catch (e: Exception) {
            Log.e("BinanceRepo", "Failed to fetch premium index", e)
            null
        }
    }

    fun subscribe(symbol: String, baseInterval: String) {
        webSocketClient.subscribe(symbol, baseInterval)
    }

    fun disconnect() {
        webSocketClient.disconnect()
    }

    private fun getEmergencyDefaultSymbols(): List<SymbolInfo> {
        return listOf(
            SymbolInfo("BTCUSDT", "BTC", "USDT", 2, 3),
            SymbolInfo("ETHUSDT", "ETH", "USDT", 2, 3),
            SymbolInfo("SOLUSDT", "SOL", "USDT", 2, 2),
            SymbolInfo("BNBUSDT", "BNB", "USDT", 2, 2),
            SymbolInfo("DOGEUSDT", "DOGE", "USDT", 4, 0),
            SymbolInfo("XRPUSDT", "XRP", "USDT", 4, 1)
        )
    }
}
