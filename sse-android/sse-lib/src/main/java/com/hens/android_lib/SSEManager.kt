package com.hens.sse.library

import android.util.Log
import java.text.SimpleDateFormat
import java.util.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

public class SSEManager private constructor() {
    // companion object for singleton
    companion object {
        private const val TAG = "SSEManager"
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
        fun onError(error: Exception, requestId: String)
        fun onClose(requestId: String)
    }
    
    // 使用 ConcurrentHashMap 来存储活动的连接
    private val connections = ConcurrentHashMap<String, Call>()
    
    // OkHttpClient instance
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS) // SSE 是长连接，读取超时设为无限
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    /**
     * 开始 SSE 连接
     * @param url SSE 服务器地址
     * @param headers 请求头
     * @param requestId 请求 ID，用于标识和管理连接
     */
    public fun startConnection(
        url: String,
        headers: Map<String, String> = emptyMap(),
        requestId: String,
        callback: SSECallback
    ) {
        // 检查是否已存在相同 requestId 的连接
        if (connections.containsKey(requestId)) {
            Log.w(TAG, "已存在 requestId 为 $requestId 的连接")
            return
        }
        
        val requestBuilder = Request.Builder()
            .url(url)
            .addHeader("Accept", "text/event-stream")
            .addHeader("Cache-Control", "no-cache")
            .addHeader("Connection", "keep-alive")
        
        // 添加自定义请求头
        for ((key, value) in headers) {
            requestBuilder.addHeader(key, value)
        }
        
        val request = requestBuilder.build()
        
        val call = client.newCall(request)
        connections[requestId] = call
        
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                connections.remove(requestId)
                callback.onError(e, requestId)
            }
            
            override fun onResponse(call: Call, response: Response) {
                // 检查 HTTP 响应状态码
                if (!response.isSuccessful) {
                    connections.remove(requestId)
                    val error = Exception("HTTP ${response.code}")
                    callback.onError(error, requestId)
                    return
                }
                
                // 通知连接已打开
                callback.onOpen(requestId)
                
                // 处理响应体
                response.body?.let { body ->
                    try {
                        body.charStream().use { reader ->
                            var line: String?
                            while (reader.readLine().also { line = it } != null) {
                                line?.let {
                                    if (it.startsWith("data:")) {
                                        val message = it.substring(5).trim()
                                        callback.onMessage(message, requestId)
                                    }
                                    // 可以在这里处理其他 SSE 事件类型，如 event:, id:, retry: 等
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "读取 SSE 数据时发生错误", e)
                        callback.onError(e, requestId)
                    } finally {
                        connections.remove(requestId)
                        callback.onClose(requestId)
                    }
                }
            }
        })
    }
    
    /**
     * 取消指定的 SSE 连接
     * @param requestId 请求 ID
     */
    public fun cancelConnection(requestId: String) {
        connections[requestId]?.let { call ->
            call.cancel()
            connections.remove(requestId)
        }
    }
    
    /**
     * 取消所有 SSE 连接
     */
    public fun cancelAllConnections() {
        val requestIds = connections.keys.toTypedArray()
        for (id in requestIds) {
            connections[id]?.cancel()
        }
        connections.clear()
    }
}