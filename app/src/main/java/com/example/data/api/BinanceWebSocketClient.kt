package com.example.data.api

import android.util.Log
import com.example.data.model.Candle
import com.example.data.model.PriceInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed class WsMessage {
    data class KlineUpdate(val symbol: String, val candle: Candle) : WsMessage()
    data class PriceUpdate(val symbol: String, val priceInfo: PriceInfo) : WsMessage()
    data class ConnectionState(val isConnected: Boolean) : WsMessage()
}

class BinanceWebSocketClient(
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .pingInterval(20, TimeUnit.SECONDS)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()
) {
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var webSocket: WebSocket? = null
    private var currentSymbol: String = "BTCUSDT"
    private var currentInterval: String = "1m"
    private var shouldBeConnected = false
    private var reconnectJob: Job? = null

    @Volatile
    var useFallbackWs: Boolean = false

    private val _messages = MutableSharedFlow<WsMessage>(extraBufferCapacity = 64)
    val messages = _messages.asSharedFlow()

    fun subscribe(symbol: String, baseInterval: String) {
        val sym = symbol.uppercase()
        val intv = baseInterval.lowercase()
        if (currentSymbol == sym && currentInterval == intv && webSocket != null) {
            return
        }
        currentSymbol = sym
        currentInterval = intv
        shouldBeConnected = true
        connect()
    }

    private fun connect() {
        closeInternal()
        val symLower = currentSymbol.lowercase()
        val intv = currentInterval
        val host = if (useFallbackWs) "stream.binancefuture.com" else "fstream.binance.com"
        val url = "wss://$host/stream?streams=${symLower}@kline_${intv}/${symLower}@markPrice@1s"
        Log.d("BinanceWS", "Connecting to: $url (fallback=$useFallbackWs)")

        val request = Request.Builder().url(url).build()
        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("BinanceWS", "Connected to $host")
                scope.launch {
                    _messages.emit(WsMessage.ConnectionState(true))
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val root = JSONObject(text)
                    val stream = root.optString("stream")
                    val data = root.optJSONObject("data") ?: return

                    if (stream.contains("@kline")) {
                        val k = data.optJSONObject("k") ?: return
                        val openTime = k.optLong("t")
                        val open = k.optString("o").toDoubleOrNull() ?: 0.0
                        val high = k.optString("h").toDoubleOrNull() ?: 0.0
                        val low = k.optString("l").toDoubleOrNull() ?: 0.0
                        val close = k.optString("c").toDoubleOrNull() ?: 0.0
                        val volume = k.optString("v").toDoubleOrNull() ?: 0.0
                        val closeTime = k.optLong("T")
                        val quoteVolume = k.optString("q").toDoubleOrNull() ?: 0.0
                        val isClosed = k.optBoolean("x", false)

                        val candle = Candle(
                            openTime = openTime,
                            open = open,
                            high = high,
                            low = low,
                            close = close,
                            volume = volume,
                            closeTime = closeTime,
                            quoteVolume = quoteVolume,
                            isClosed = isClosed
                        )
                        val sym = data.optString("s", currentSymbol)
                        scope.launch {
                            _messages.emit(WsMessage.KlineUpdate(sym, candle))
                        }
                    } else if (stream.contains("@markPrice")) {
                        val markPrice = data.optString("p").toDoubleOrNull() ?: 0.0
                        val indexPrice = data.optString("i").toDoubleOrNull() ?: 0.0
                        val fundingRate = data.optString("r").toDoubleOrNull()
                        val nextFundingTime = data.optLong("T")
                        val sym = data.optString("s", currentSymbol)

                        scope.launch {
                            _messages.emit(
                                WsMessage.PriceUpdate(
                                    sym,
                                    PriceInfo(
                                        lastPrice = 0.0,
                                        markPrice = markPrice,
                                        indexPrice = indexPrice,
                                        fundingRate = fundingRate,
                                        nextFundingTime = nextFundingTime
                                    )
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.e("BinanceWS", "Error parsing WS message", e)
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("BinanceWS", "Closed: $code / $reason")
                scope.launch {
                    _messages.emit(WsMessage.ConnectionState(false))
                }
                scheduleReconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.w("BinanceWS", "Failure on $host: ${t.message}")
                if (!useFallbackWs) {
                    Log.i("BinanceWS", "Switching WebSocket to unrestricted stream.binancefuture.com")
                    useFallbackWs = true
                }
                scope.launch {
                    _messages.emit(WsMessage.ConnectionState(false))
                }
                scheduleReconnect()
            }
        })
    }

    private fun scheduleReconnect() {
        if (!shouldBeConnected) return
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(3000)
            if (shouldBeConnected) {
                Log.d("BinanceWS", "Attempting reconnection...")
                connect()
            }
        }
    }

    private fun closeInternal() {
        try {
            webSocket?.cancel()
            webSocket = null
        } catch (e: Exception) {
            // ignore
        }
    }

    fun disconnect() {
        shouldBeConnected = false
        reconnectJob?.cancel()
        closeInternal()
    }
}
