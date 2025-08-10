package uts.sdk.modules.ssePlugin

import android.util.Log
import android.os.Handler
import android.os.Looper
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

class SSEManager private constructor() {
    private val TAG = "SSEManager"
    private val mainHandler = Handler(Looper.getMainLooper())

    private fun postToMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            mainHandler.post { block() }
        }
    }

    /**
     * 保存每个活动连接的可取消句柄
     */
    private class ConnectionHandle {
        val isCancelled = AtomicBoolean(false)
        @Volatile var connection: HttpURLConnection? = null
        @Volatile var thread: Thread? = null
    }

    private val connections = ConcurrentHashMap<String, ConnectionHandle>()

    companion object {
        @Volatile
        private var INSTANCE: SSEManager? = null

        fun getInstance(): SSEManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SSEManager().also { INSTANCE = it }
            }
        }
    }

    // 定义回调接口
    interface SSECallback {
        fun onOpen(requestId: String)
        fun onMessage(message: String, requestId: String)
        fun onError(error: String, requestId: String)
        fun onClose(requestId: String)
    }

    /**
     * 开始 SSE 连接（使用 HttpURLConnection 实现）
     */
    fun startConnection(url: String, headers: Map<String, Any?>?, requestId: String, callback: SSECallback) {
        Log.i(TAG, "startConnection(): requestId=$requestId, url=$url, headersKeys=${headers?.keys}")
        if (connections.containsKey(requestId)) {
            Log.w(TAG, "已存在 requestId 为 $requestId 的连接")
            return
        }

        val handle = ConnectionHandle()
        connections[requestId] = handle

        val worker = Thread {
            try {
                Log.d(TAG, "[$requestId] 初始化 HttpURLConnection ...")
                val urlObj = URL(url)
                val conn = (urlObj.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 30_000
                    readTimeout = 0 // SSE 长连接，读取不超时
                    setRequestProperty("Accept", "text/event-stream")
                    setRequestProperty("Cache-Control", "no-cache")
                    setRequestProperty("Connection", "keep-alive")
                    // 自定义请求头
                    headers?.forEach { (k, v) -> setRequestProperty(k, v?.toString() ?: "") }
                    instanceFollowRedirects = true
                }
                handle.connection = conn
                Log.d(TAG, "[$requestId] 连接配置: connectTimeout=${conn.connectTimeout}, readTimeout=${conn.readTimeout}, followRedirects=${conn.instanceFollowRedirects}")
                Log.i(TAG, "[$requestId] 开始连接... -> ${conn.url}")
                try {
                    // 打印网络安全提示：是否为 http 明文
                    val isCleartext = conn.url.protocol.equals("http", ignoreCase = true)
                    Log.d(TAG, "[$requestId] isCleartext=$isCleartext host=${conn.url.host}")
                } catch (_: Throwable) {}

                conn.connect()

                val code = conn.responseCode
                Log.i(TAG, "[$requestId] 已连接，HTTP 响应码: $code")
                if (code !in 200..299) {
                    throw Exception("HTTP $code")
                }

                postToMain { callback.onOpen(requestId) }

                BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { reader ->
                    Log.d(TAG, "[$requestId] 开始读取 SSE 流数据 ...")
                    var dataBuffer = StringBuilder()

                    while (!handle.isCancelled.get()) {
                        val l = reader.readLine() ?: break
                        if (l.isEmpty()) {
                            // 事件结束，派发消息
                            if (dataBuffer.isNotEmpty()) {
                                val msg = if (dataBuffer.endsWith("\n")) {
                                    dataBuffer.substring(0, dataBuffer.length - 1)
                                } else {
                                    dataBuffer.toString()
                                }
                                val preview = if (msg.length > 200) msg.substring(0, 200) + "…" else msg
                                Log.d(TAG, "[$requestId] onMessage 派发，长度=${msg.length}，预览=${preview}")
                                postToMain { callback.onMessage(msg, requestId) }
                                dataBuffer = StringBuilder()
                            }
                            continue
                        }

                        if (l.startsWith(":")) {
                            // 注释/心跳，忽略
                            Log.v(TAG, "[$requestId] 心跳/注释: ${l}")
                            continue
                        }

                        val idx = l.indexOf(':')
                        val field = if (idx == -1) l else l.substring(0, idx)
                        var value = if (idx == -1) "" else l.substring(idx + 1)
                        if (value.startsWith(" ")) value = value.substring(1)

                        when (field) {
                            "data" -> {
                                dataBuffer.append(value).append('\n')
                            }
                            "event" -> {
                                Log.d(TAG, "[$requestId] event: $value")
                            }
                            "id" -> {
                                Log.d(TAG, "[$requestId] id: $value")
                            }
                            "retry" -> {
                                // 读取并忽略；当前实现不做自动重连
                                Log.d(TAG, "[$requestId] retry: $value (ignored)")
                            }
                            else -> {
                                // 忽略未知字段
                                Log.d(TAG, "[$requestId] 未知字段: $field=$value (ignored)")
                            }
                        }
                    }
                    Log.i(TAG, "[$requestId] 读取循环结束（取消=${handle.isCancelled.get()}）")
                }
            } catch (e: Exception) {
                val msg = e.message ?: e.toString()
                Log.e(TAG, "[$requestId] SSE 连接或读取失败: $msg", e)
                if (msg.contains("CLEARTEXT", ignoreCase = true)) {
                    Log.e(TAG, "[$requestId] 检测到明文 HTTP 被拦截，请确认 usesCleartextTraffic 或 networkSecurityConfig 放行对应域名/IP")
                }
                try { postToMain { callback.onError(e.message ?: e.toString(), requestId) } } catch (_: Exception) { }
            } finally {
                Log.i(TAG, "[$requestId] 准备关闭连接与清理资源 ...")
                try {
                    handle.connection?.disconnect()
                } catch (_: Exception) { }
                connections.remove(requestId)
                try { postToMain { callback.onClose(requestId) } } catch (_: Exception) { }
                Log.i(TAG, "[$requestId] 连接已关闭，清理完成")
            }
        }

        worker.isDaemon = true
        worker.name = "SSE-$requestId"
        handle.thread = worker
        Log.d(TAG, "[$requestId] 启动工作线程: ${worker.name}")
        worker.start()
    }

    /**
     * 取消指定的 SSE 连接
     */
    fun cancelConnection(requestId: String) {
        Log.i(TAG, "cancelConnection(): requestId=$requestId")
        connections[requestId]?.let { handle ->
            handle.isCancelled.set(true)
            try {
                handle.connection?.disconnect()
            } catch (_: Exception) { }
            try {
                handle.thread?.interrupt()
            } catch (_: Exception) { }
            connections.remove(requestId)
        }
    }

    /**
     * 取消所有 SSE 连接
     */
    fun cancelAllConnections() {
        Log.w(TAG, "cancelAllConnections() 当前连接数=${connections.size}")
        connections.values.forEach { handle ->
            handle.isCancelled.set(true)
            try { handle.connection?.disconnect() } catch (_: Exception) { }
            try { handle.thread?.interrupt() } catch (_: Exception) { }
        }
        connections.clear()
    }
}


