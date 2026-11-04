package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.api.BinanceRepository
import com.example.data.model.AlertItem
import com.example.data.storage.TradingPreferences
import com.example.engine.AlertEngine
import com.example.sound.AlertSoundPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AlertMonitoringService : Service() {
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private lateinit var preferences: TradingPreferences
    private val repository = BinanceRepository()
    private var monitoringJob: Job? = null

    private lateinit var alertEngine: AlertEngine

    companion object {
        const val CHANNEL_ID = "apex_alerts_channel"
        const val FOREGROUND_NOTIF_ID = 1001

        fun startService(context: Context) {
            val intent = Intent(context, AlertMonitoringService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, AlertMonitoringService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        preferences = TradingPreferences(this)
        createNotificationChannel()

        alertEngine = AlertEngine { triggeredAlert, triggeredPrice ->
            handleTriggeredAlert(triggeredAlert, triggeredPrice)
        }

        startForegroundNotification()
        startMonitoringLoop()
    }

    private fun startForegroundNotification() {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ApexTrade Alert Engine")
            .setContentText("Monitoring Binance USD-M Futures alerts in background")
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                FOREGROUND_NOTIF_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(FOREGROUND_NOTIF_ID, notification)
        }
    }

    private fun startMonitoringLoop() {
        monitoringJob?.cancel()
        monitoringJob = scope.launch {
            while (isActive) {
                try {
                    val alerts = preferences.getAllAlerts().filter { it.isActive }
                    if (alerts.isNotEmpty()) {
                        // Query 24h tickers to check real Binance prices for all active alert symbols
                        val tickers = repository.get24hTickers()
                        val tickerMap = tickers.associateBy { it.symbol.uppercase() }

                        for (alert in alerts) {
                            val ticker = tickerMap[alert.symbol.uppercase()]
                            if (ticker != null && ticker.lastPrice > 0.0) {
                                alertEngine.checkPrice(alert.symbol, ticker.lastPrice, alerts, this@AlertMonitoringService)
                            }
                        }
                    }
                } catch (e: Exception) {
                    // ignore network blips in background
                }
                delay(3000) // Poll real market data every 3 seconds for background battery efficiency
            }
        }
    }

    private fun handleTriggeredAlert(alert: AlertItem, triggeredPrice: Double) {
        // Mark alert triggered in storage to prevent duplicate notifications
        val allAlerts = preferences.getAllAlerts().map {
            if (it.id == alert.id) it.copy(isActive = false, triggeredAt = System.currentTimeMillis()) else it
        }
        preferences.saveAlerts(allAlerts)

        // Dispatch high priority notification
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            alert.id.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🚨 ${alert.symbol} Price Alert Triggered")
            .setContentText("${alert.condition.title} @ ${String.format("%.2f", triggeredPrice)} (Target: ${alert.targetPrice})")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 300, 150, 300))
            .build()

        nm.notify(alert.id.hashCode(), notif)
        AlertSoundPlayer.play(this, alert.soundType)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Trading Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for Binance USD-M Futures price alerts"
                enableVibration(true)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        monitoringJob?.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
