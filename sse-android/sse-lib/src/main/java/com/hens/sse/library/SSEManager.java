package com.hens.sse.library;

import android.util.Log;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SSEManager {
    private static final String TAG = "SSEManager";
    private static SSEManager INSTANCE = null;

    // 定义回调接口
    public interface SSECallback {
        void onOpen(String requestId);
        void onMessage(String message, String requestId);
        void onError(Exception error, String requestId);
        void onClose(String requestId);
    }

    // 使用 ConcurrentHashMap 来存储活动的连接
    private final ConcurrentHashMap<String, Call> connections = new ConcurrentHashMap<>();

    // OkHttpClient instance
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.SECONDS) // SSE 是长连接，读取超时设为无限
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    // 私有构造函数
    private SSEManager() {
    }

    // 获取单例实例
    public static SSEManager getInstance() {
        if (INSTANCE == null) {
            synchronized (SSEManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new SSEManager();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * 开始 SSE 连接
     * @param url SSE 服务器地址
     * @param headers 请求头
     * @param requestId 请求 ID，用于标识和管理连接
     */
    public void startConnection(String url, Map<String, String> headers, String requestId, SSECallback callback) {
        // 检查是否已存在相同 requestId 的连接
        if (connections.containsKey(requestId)) {
            Log.w(TAG, "已存在 requestId 为 " + requestId + " 的连接");
            return;
        }

        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .addHeader("Accept", "text/event-stream")
                .addHeader("Cache-Control", "no-cache")
                .addHeader("Connection", "keep-alive");

        // 添加自定义请求头
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                requestBuilder.addHeader(entry.getKey(), entry.getValue());
            }
        }

        Request request = requestBuilder.build();
        Call call = client.newCall(request);
        connections.put(requestId, call);

        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                connections.remove(requestId);
                callback.onError(e, requestId);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                // 检查 HTTP 响应状态码
                if (!response.isSuccessful()) {
                    connections.remove(requestId);
                    Exception error = new Exception("HTTP " + response.code());
                    callback.onError(error, requestId);
                    return;
                }

                // 通知连接已打开
                callback.onOpen(requestId);

                // 处理响应体
                if (response.body() != null) {
                    try {
                        String line;
                        while ((line = response.body().source().readUtf8Line()) != null) {
                            if (line.startsWith("data:")) {
                                String message = line.substring(5).trim();
                                callback.onMessage(message, requestId);
                            }
                            // 可以在这里处理其他 SSE 事件类型，如 event:, id:, retry: 等
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "读取 SSE 数据时发生错误", e);
                        callback.onError(e, requestId);
                    } finally {
                        connections.remove(requestId);
                        callback.onClose(requestId);
                    }
                }
            }
        });
    }

    /**
     * 取消指定的 SSE 连接
     * @param requestId 请求 ID
     */
    public void cancelConnection(String requestId) {
        Call call = connections.get(requestId);
        if (call != null) {
            call.cancel();
            connections.remove(requestId);
        }
    }

    /**
     * 取消所有 SSE 连接
     */
    public void cancelAllConnections() {
        for (Map.Entry<String, Call> entry : connections.entrySet()) {
            entry.getValue().cancel();
        }
        connections.clear();
    }
}