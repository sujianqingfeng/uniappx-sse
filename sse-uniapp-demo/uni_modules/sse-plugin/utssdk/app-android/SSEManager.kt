package uts.sdk.modules.ssePlugin

import android.util.Log
import android.os.Handler
import android.os.Looper
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

class SSEManager private constructor() {
    private val TAG = "SSEManager"
    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile private var logEnabled: Boolean = false

    fun setLoggingEnabled(enabled: Boolean) {
        logEnabled = enabled
    }

    private fun logDebug(message: String) {
        if (logEnabled) Log.d(TAG, message)
    }

    private fun logInfo(message: String) {
        if (logEnabled) Log.i(TAG, message)
    }

    private fun logWarn(message: String) {
        if (logEnabled) Log.w(TAG, message)
    }

    private fun logVerbose(message: String) {
        if (logEnabled) Log.v(TAG, message)
    }

    private fun logError(message: String, tr: Throwable? = null) {
        if (logEnabled) {
            if (tr != null) {
                Log.e(TAG, message, tr)
            } else {
                Log.e(TAG, message)
            }
        }
    }

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
     * 与 Web 侧一致的受限请求头过滤
     */
    private fun isForbiddenHeaderName(name: String?): Boolean {
        val n = (name ?: "").lowercase()
        if (n.startsWith("proxy-") || n.startsWith("sec-")) return true
        return when (n) {
            "accept-charset",
            "accept-encoding",
            "access-control-request-headers",
            "access-control-request-method",
            "connection",
            "content-length",
            "cookie",
            "cookie2",
            "date",
            "dnt",
            "expect",
            "host",
            "keep-alive",
            "origin",
            "referer",
            "te",
            "trailer",
            "transfer-encoding",
            "upgrade",
            "via" -> true
            else -> false
        }
    }

    private fun parseHeadersJson(headersJson: String?): Map<String, Any?>? {
        if (headersJson.isNullOrBlank()) return null
        return try {
            val obj = JSONObject(headersJson)
            val out = mutableMapOf<String, Any?>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val rawKey = keys.next()
                if (rawKey.isNullOrBlank()) continue
                if (isForbiddenHeaderName(rawKey)) continue
                val value = obj.opt(rawKey)
                val vStr = when (value) {
                    is JSONArray -> {
                        val parts = mutableListOf<String>()
                        for (i in 0 until value.length()) {
                            val item = value.opt(i)
                            parts.add(item?.toString() ?: "")
                        }
                        parts.joinToString(separator = ", ")
                    }
                    null -> null
                    else -> value.toString()
                }
                if (vStr != null) out[rawKey] = vStr.replace("\r", " ").replace("\n", " ")
            }
            if (out.isEmpty()) null else out
        } catch (t: Throwable) {
            logWarn("parseHeadersJson 失败，将不附加自定义头: ${t.message}")
            null
        }
    }

    /**
     * 适配 UTS 侧以 JSON 传递 headers 的调用，内部解析后复用现有实现
     */
    fun startConnectionWithHeadersJson(url: String, headersJson: String?, requestId: String, callback: SSECallback) {
        val parsed = parseHeadersJson(headersJson)
        startConnection(url, parsed, requestId, callback)
    }

    /**
     * 开始 SSE 连接（使用 HttpURLConnection 实现）
     */
    fun startConnection(url: String, headers: Map<String, Any?>?, requestId: String, callback: SSECallback) {
        logInfo("startConnection(): requestId=$requestId, url=$url, headersKeys=${headers?.keys}")
        if (connections.containsKey(requestId)) {
            logWarn("已存在 requestId 为 $requestId 的连接")
            return
        }

        val handle = ConnectionHandle()
        connections[requestId] = handle

        val worker = Thread {
            try {
                logDebug("[$requestId] 初始化 HttpURLConnection ...")
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
                logDebug("[$requestId] 连接配置: connectTimeout=${conn.connectTimeout}, readTimeout=${conn.readTimeout}, followRedirects=${conn.instanceFollowRedirects}")
                logInfo("[$requestId] 开始连接... -> ${conn.url}")
                try {
                    // 打印网络安全提示：是否为 http 明文
                    val isCleartext = conn.url.protocol.equals("http", ignoreCase = true)
                    logDebug("[$requestId] isCleartext=$isCleartext host=${conn.url.host}")
                } catch (_: Throwable) {}

                conn.connect()

                val code = conn.responseCode
                logInfo("[$requestId] 已连接，HTTP 响应码: $code")
                if (code !in 200..299) {
                    throw Exception("HTTP $code")
                }

                postToMain { callback.onOpen(requestId) }

                BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { reader ->
                    logDebug("[$requestId] 开始读取 SSE 流数据 ...")
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
                                logDebug("[$requestId] onMessage 派发，长度=${msg.length}，预览=${preview}")
                                postToMain { callback.onMessage(msg, requestId) }
                                dataBuffer = StringBuilder()
                            }
                            continue
                        }

                        if (l.startsWith(":")) {
                            // 注释/心跳，忽略
                            logVerbose("[$requestId] 心跳/注释: ${l}")
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
                                logDebug("[$requestId] event: $value")
                            }
                            "id" -> {
                                logDebug("[$requestId] id: $value")
                            }
                            "retry" -> {
                                // 读取并忽略；当前实现不做自动重连
                                logDebug("[$requestId] retry: $value (ignored)")
                            }
                            else -> {
                                // 忽略未知字段
                                logDebug("[$requestId] 未知字段: $field=$value (ignored)")
                            }
                        }
                    }
                    logInfo("[$requestId] 读取循环结束（取消=${handle.isCancelled.get()}）")
                }
            } catch (e: Exception) {
                val msg = e.message ?: e.toString()
                logError("[$requestId] SSE 连接或读取失败: $msg", e)
                if (msg.contains("CLEARTEXT", ignoreCase = true)) {
                    logError("[$requestId] 检测到明文 HTTP 被拦截，请确认 usesCleartextTraffic 或 networkSecurityConfig 放行对应域名/IP")
                }
                try { postToMain { callback.onError(e.message ?: e.toString(), requestId) } } catch (_: Exception) { }
            } finally {
                logInfo("[$requestId] 准备关闭连接与清理资源 ...")
                try {
                    handle.connection?.disconnect()
                } catch (_: Exception) { }
                connections.remove(requestId)
                try { postToMain { callback.onClose(requestId) } } catch (_: Exception) { }
                logInfo("[$requestId] 连接已关闭，清理完成")
            }
        }

        worker.isDaemon = true
        worker.name = "SSE-$requestId"
        handle.thread = worker
        logDebug("[$requestId] 启动工作线程: ${worker.name}")
        worker.start()
    }

    /**
     * 取消指定的 SSE 连接
     */
    fun cancelConnection(requestId: String) {
        logInfo("cancelConnection(): requestId=$requestId")
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
        logWarn("cancelAllConnections() 当前连接数=${connections.size}")
        connections.values.forEach { handle ->
            handle.isCancelled.set(true)
            try { handle.connection?.disconnect() } catch (_: Exception) { }
            try { handle.thread?.interrupt() } catch (_: Exception) { }
        }
        connections.clear()
    }
}


