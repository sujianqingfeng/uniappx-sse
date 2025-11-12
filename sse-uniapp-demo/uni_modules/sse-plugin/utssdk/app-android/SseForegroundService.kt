package uts.sdk.modules.ssePlugin

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import java.util.concurrent.atomic.AtomicBoolean
import io.dcloud.uts.console

class SseForegroundService : Service() {
    private val TAG = "SseForegroundService"
    private var wakeLock: PowerManager.WakeLock? = null
    private val isConfigured = AtomicBoolean(false)

    companion object {
        private const val DEFAULT_CHANNEL_ID = "sse_channel"
        private const val DEFAULT_CHANNEL_NAME = "SSE"
        private const val DEFAULT_TITLE = "SSE 正在保持连接"
        private const val DEFAULT_TEXT = "后台保持连接以接收消息"
        private const val NOTIFICATION_ID = 1001

        // 配置信息（全局静态保存）
        private var configuredChannelId: String = DEFAULT_CHANNEL_ID
        private var configuredChannelName: String = DEFAULT_CHANNEL_NAME
        private var configuredTitle: String = DEFAULT_TITLE
        private var configuredText: String = DEFAULT_TEXT
        private var configuredImportance: Int = NotificationManager.IMPORTANCE_LOW

        /**
         * 配置前台服务参数
         */
        fun configure(
            channelId: String? = null,
            channelName: String? = null,
            importance: Int? = null,
            title: String? = null,
            text: String? = null
        ) {
            configuredChannelId = channelId ?: DEFAULT_CHANNEL_ID
            configuredChannelName = channelName ?: DEFAULT_CHANNEL_NAME
            configuredTitle = title ?: DEFAULT_TITLE
            configuredText = text ?: DEFAULT_TEXT
            configuredImportance = importance ?: NotificationManager.IMPORTANCE_LOW
            console.log("[SseForegroundService] 配置更新: channelId=$configuredChannelId, title=$configuredTitle, importance=$configuredImportance")
            Log.d("SseForegroundService", "配置更新: channelId=$configuredChannelId, title=$configuredTitle")
        }

        /**
         * 启动前台服务
         */
        fun start(context: Context, wakeLockEnabled: Boolean = true) {
            console.log("[SseForegroundService.start] 开始启动服务 (wakeLock=$wakeLockEnabled, SDK=${Build.VERSION.SDK_INT})")
            console.log("[SseForegroundService.start] Context类型: ${context.javaClass.name}")
            
            try {
                val intent = Intent(context, SseForegroundService::class.java).apply {
                    putExtra("wakeLockEnabled", wakeLockEnabled)
                }
                console.log("[SseForegroundService.start] Intent 已创建，目标类: ${SseForegroundService::class.java.name}")
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    console.log("[SseForegroundService.start] 使用 startForegroundService (Android 8+)")
                    val result = context.startForegroundService(intent)
                    console.log("[SseForegroundService.start] startForegroundService 返回: $result")
                } else {
                    console.log("[SseForegroundService.start] 使用 startService (Android < 8)")
                    val result = context.startService(intent)
                    console.log("[SseForegroundService.start] startService 返回: $result")
                }
                
                console.log("[SseForegroundService.start] ✅ 前台服务启动请求已发送")
                Log.d("SseForegroundService", "前台服务启动请求已发送 (wakeLock=$wakeLockEnabled)")
            } catch (e: Exception) {
                console.log("[SseForegroundService.start] ❌ 启动失败: ${e.message}")
                console.log("[SseForegroundService.start] 异常堆栈: ${e.stackTraceToString()}")
                throw e
            }
        }

        /**
         * 停止前台服务
         */
        fun stop(context: Context) {
            console.log("[SseForegroundService.stop] 准备停止服务")
            try {
                val intent = Intent(context, SseForegroundService::class.java)
                context.stopService(intent)
                console.log("[SseForegroundService.stop] ✅ 前台服务停止请求已发送")
                Log.d("SseForegroundService", "前台服务停止请求已发送")
            } catch (e: Exception) {
                console.log("[SseForegroundService.stop] ❌ 停止失败: ${e.message}")
                Log.e("SseForegroundService", "停止前台服务失败", e)
            }
        }

        /**
         * 创建通知渠道（Android 8.0+）
         */
        private fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                console.log("[SseForegroundService] 创建通知渠道: $configuredChannelId (importance=$configuredImportance)")
                val channel = NotificationChannel(
                    configuredChannelId,
                    configuredChannelName,
                    configuredImportance
                ).apply {
                    description = "SSE 后台连接保持通知"
                    setShowBadge(false)
                    enableVibration(false)
                    setSound(null, null)
                }

                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
                console.log("[SseForegroundService] ✅ 通知渠道已创建")
                Log.d("SseForegroundService", "通知渠道已创建: $configuredChannelId")
            }
        }

        /**
         * 创建前台通知
         */
        private fun createNotification(context: Context): Notification {
            createNotificationChannel(context)

            return NotificationCompat.Builder(context, configuredChannelId)
                .setContentTitle(configuredTitle)
                .setContentText(configuredText)
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .setPriority(configuredImportance)
                .setOngoing(true)
                .setAutoCancel(false)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build()
        }

        /**
         * 获取 WakeLock
         */
        private fun acquireWakeLock(context: Context): PowerManager.WakeLock? {
            console.log("[SseForegroundService] 准备获取 WakeLock (PARTIAL_WAKE_LOCK, 10分钟)")
            return try {
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                val wakeLock = powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "SseForegroundService:SSE"
                ).apply {
                    setReferenceCounted(false)
                    acquire(10 * 60 * 1000L) // 10分钟超时
                }
                console.log("[SseForegroundService] ✅ WakeLock 已获取")
                Log.d("SseForegroundService", "WakeLock 已获取")
                wakeLock
            } catch (e: Exception) {
                console.log("[SseForegroundService] ❌ WakeLock 获取失败: ${e.message}")
                Log.e("SseForegroundService", "获取 WakeLock 失败", e)
                null
            }
        }

        /**
         * 释放 WakeLock
         */
        private fun releaseWakeLock(wakeLock: PowerManager.WakeLock?) {
            console.log("[SseForegroundService] 准备释放 WakeLock")
            try {
                wakeLock?.let {
                    if (it.isHeld) {
                        it.release()
                        console.log("[SseForegroundService] ✅ WakeLock 已释放")
                        Log.d("SseForegroundService", "WakeLock 已释放")
                    } else {
                        console.log("[SseForegroundService] WakeLock 未持有，跳过释放")
                    }
                }
            } catch (e: Exception) {
                console.log("[SseForegroundService] ❌ WakeLock 释放失败: ${e.message}")
                Log.e("SseForegroundService", "释放 WakeLock 失败", e)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        console.log("[SseForegroundService] 📱 Service onCreate 被调用")
        Log.d(TAG, "前台服务 onCreate")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        console.log("[SseForegroundService] 🚀 Service onStartCommand 被调用 (startId=$startId)")
        Log.d(TAG, "前台服务 onStartCommand")
        
        val wakeLockEnabled = intent?.getBooleanExtra("wakeLockEnabled", true) ?: true
        console.log("[SseForegroundService] WakeLock 配置: $wakeLockEnabled")
        
        // 启动前台通知
        console.log("[SseForegroundService] 创建通知...")
        val notification = createNotification(this)
        
        console.log("[SseForegroundService] 调用 startForeground (ID=$NOTIFICATION_ID)")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            console.log("[SseForegroundService] 使用 FOREGROUND_SERVICE_TYPE_DATA_SYNC")
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        console.log("[SseForegroundService] ✅ startForeground 调用完成")
        
        // 获取 WakeLock（如果启用）
        if (wakeLockEnabled) {
            console.log("[SseForegroundService] 启用 WakeLock，准备获取...")
            wakeLock = acquireWakeLock(this)
        } else {
            console.log("[SseForegroundService] WakeLock 未启用")
        }
        
        isConfigured.set(true)
        console.log("[SseForegroundService] ✅ Service 配置完成，返回 START_STICKY")
        
        return START_STICKY
    }

    override fun onDestroy() {
        console.log("[SseForegroundService] 🛑 Service onDestroy 被调用")
        Log.d(TAG, "前台服务 onDestroy")
        
        // 释放 WakeLock
        releaseWakeLock(wakeLock)
        wakeLock = null
        
        // 停止前台服务
        console.log("[SseForegroundService] 停止前台服务...")
        stopForeground(true)
        stopSelf()
        
        isConfigured.set(false)
        console.log("[SseForegroundService] ✅ Service 清理完成")
        
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): android.os.IBinder? {
        return null
    }
}
