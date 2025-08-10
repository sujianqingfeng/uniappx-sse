# Changelog

## 1.0.0 - 2025-08-10

首个可用版本，提供跨平台 SSE 能力与示例：

- Android：Kotlin 原生实现，支持多连接、事件分发、受限头过滤、日志开关
- iOS：基于 URLSession 的流式实现，支持多连接、事件分发
- Web：基于 `fetch` + `ReadableStream` 的解析实现
- 统一 UTS 接口：`sseConnectApi`、`sseCloseApi`、`sseAddEventListenerApi`、`sseRemoveEventListenerApi`、`closeAllSSEConnections`
- 多连接监听模型：全局监听与按 `requestId` 监听
- 自定义请求头支持（自动过滤浏览器不允许的头）
- Android 模拟器环回地址处理（localhost/127.0.0.1/[::1] → 10.0.2.2）


