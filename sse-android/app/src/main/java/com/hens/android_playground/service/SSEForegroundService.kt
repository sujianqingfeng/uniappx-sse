package com.hens.android_playground.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.hens.sse.demo.MainActivity
import com.hens.sse.demo.R
import com.hens.sse.library.SSEManager
import java.util.concurrent.CopyOnWriteArraySet

class SSEForegroundService : Service(), SSEManager.SSECallback {

    private val tag = "SSEForegroundService"

    companion object {
        private const val CHANNEL_ID = "sse_channel"
        private const val CHANNEL_NAME = "SSE Foreground"
        private const val NOTIFICATION_ID = 101
        private const val REQUEST_ID = "sse-foreground"
        const val ACTION_START = "com.hens.sse.demo.action.START_SSE"
        const val ACTION_STOP = "com.hens.sse.demo.action.STOP_SSE"
        const val EXTRA_URL = "extra_sse_url"

        fun startService(context: Context) {
            val intent = Intent(context, SSEForegroundService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun startConnection(context: Context, url: String) {
            val intent = Intent(context, SSEForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_URL, url)
            }
            ContextCompat.startForegroundService(context, intent)
            Log.d("SSEForegroundService", "startConnection(): url=$url")
        }

        fun stopConnection(context: Context) {
            val intent = Intent(context, SSEForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            ContextCompat.startForegroundService(context, intent)
            Log.d("SSEForegroundService", "stopConnection() called")
        }
    }

    inner class LocalBinder : Binder() {
        fun getService(): SSEForegroundService = this@SSEForegroundService
    }

    interface StatusListener {
        fun onStatusChanged(message: String, connected: Boolean)
    }

    private val binder = LocalBinder()
    private val listeners = CopyOnWriteArraySet<StatusListener>()
    private val sseManager = SSEManager.getInstance()

    @Volatile
    private var isConnected = false
    @Volatile
    private var lastMessage: String = "点击按钮开始 SSE 连接"
    

    override fun onCreate() {
        super.onCreate()
        Log.d(tag, "Service created")
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification(lastMessage, isConnected))
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(tag, "onStartCommand action=${intent?.action} url=${intent?.getStringExtra(EXTRA_URL)}")
        when (intent?.action) {
            ACTION_START -> {
                val url = intent.getStringExtra(EXTRA_URL).orEmpty()
                startSse(url)
            }
            ACTION_STOP -> stopSse()
        }
        return START_STICKY
    }

    fun startSse(url: String) {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) {
            Log.w(tag, "startSse(): empty url")
            notifyListeners("❗ 请输入有效的 SSE 地址", false)
            return
        }
        if (isConnected) {
            Log.w(tag, "startSse(): already connected")
            notifyListeners("⚠️ 已连接，如需切换请先断开", true)
            return
        }
        Log.i(tag, "startSse(): connecting -> $trimmed")
        notifyListeners("正在连接到 SSE 服务器...", false)
        sseManager.startConnection(
            trimmed,
            mapOf(
                "Authorization" to "Bearer your-token-here",
                "User-Agent" to "SSE-Demo-App/1.0"
            ),
            REQUEST_ID,
            this
        )
    }

    fun stopSse() {
        sseManager.cancelConnection(REQUEST_ID)
        notifyListeners("已断开 SSE 连接", false)
    }

    fun registerListener(listener: StatusListener) {
        listeners.add(listener)
        listener.onStatusChanged(lastMessage, isConnected)
    }

    fun unregisterListener(listener: StatusListener) {
        listeners.remove(listener)
    }

    override fun onDestroy() {
        stopSse()
        Log.d(tag, "Service destroyed")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        super.onDestroy()
    }

    // region SSE Callbacks
    override fun onOpen(requestId: String) {
        isConnected = true
        notifyListeners("✅ SSE 连接已建立 (ID: $requestId)", true)
    }

    override fun onMessage(message: String, requestId: String) {
        notifyListeners(message, true)
    }

    override fun onError(error: String, requestId: String) {
        isConnected = false
        notifyListeners("❌ 连接错误 (ID: $requestId): $error", false)
    }

    override fun onClose(requestId: String) {
        isConnected = false
        notifyListeners("⏹️ 连接已关闭 (ID: $requestId)", false)
    }
    // endregion

    private fun notifyListeners(message: String, connected: Boolean) {
        lastMessage = message
        isConnected = connected
        listeners.forEach { it.onStatusChanged(message, connected) }
        updateNotification(message, connected)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            if (nm?.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "保持 SSE 长连接的前台服务"
                }
                nm?.createNotificationChannel(channel)
            }
        }
    }

    

    private fun buildNotification(content: String, connected: Boolean): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stopIntent = Intent(this, SSEForegroundService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("SSE 连接服务")
            .setContentText(content)
            .setContentIntent(pendingIntent)
            .setOngoing(connected)
            .addAction(
                R.drawable.ic_notification,
                "断开",
                stopPendingIntent
            )
            .build()
    }

    private fun updateNotification(content: String, connected: Boolean) {
        val notification = buildNotification(content, connected)
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, notification)
    }
}
