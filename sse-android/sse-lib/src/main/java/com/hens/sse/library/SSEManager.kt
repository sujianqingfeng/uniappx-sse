package com.hens.sse.library

import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

class SSEManager private constructor() {
    private val TAG = "SSEManager"

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
        if (connections.containsKey(requestId)) {
            Log.w(TAG, "已存在 requestId 为 $requestId 的连接")
            return
        }

        val handle = ConnectionHandle()
        connections[requestId] = handle

        val worker = Thread {
            try {
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

                conn.connect()

                val code = conn.responseCode
                if (code !in 200..299) {
                    throw Exception("HTTP $code")
                }

                callback.onOpen(requestId)

                BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { reader ->
                    var dataBuffer = StringBuilder()
                    var eventType: String? = null
                    var eventIdTmp: String? = null

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
                                callback.onMessage(msg, requestId)
                                dataBuffer = StringBuilder()
                                eventType = null
                                eventIdTmp = null
                            }
                            continue
                        }

                        if (l.startsWith(":")) {
                            // 注释/心跳，忽略
                            continue
                        }

                        val idx = l.indexOf(':')
                        val field = if (idx == -1) l else l.substring(0, idx)
                        var value = if (idx == -1) "" else l.substring(idx + 1)
                        if (value.startsWith(" ")) value = value.substring(1)

                        when (field) {
                            "data" -> dataBuffer.append(value).append('\n')
                            "event" -> eventType = value
                            "id" -> eventIdTmp = value
                            "retry" -> {
                                // 读取并忽略；当前实现不做自动重连
                            }
                            else -> {
                                // 忽略未知字段
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "SSE 连接或读取失败", e)
                try {
                    callback.onError(e.message ?: e.toString(), requestId)
                } catch (_: Exception) { }
            } finally {
                try {
                    handle.connection?.disconnect()
                } catch (_: Exception) { }
                connections.remove(requestId)
                try {
                    callback.onClose(requestId)
                } catch (_: Exception) { }
            }
        }

        worker.isDaemon = true
        worker.name = "SSE-$requestId"
        handle.thread = worker
        worker.start()
    }

    /**
     * 取消指定的 SSE 连接
     */
    fun cancelConnection(requestId: String) {
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
        connections.values.forEach { handle ->
            handle.isCancelled.set(true)
            try { handle.connection?.disconnect() } catch (_: Exception) { }
            try { handle.thread?.interrupt() } catch (_: Exception) { }
        }
        connections.clear()
    }
}