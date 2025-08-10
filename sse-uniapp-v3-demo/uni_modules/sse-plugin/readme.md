# sse-plugin

跨平台 Server-Sent Events (SSE) 原生能力插件，支持 UniApp X 与 UniApp（Web/Android/iOS）。

提供多连接管理、全局/按连接事件监听、自定义请求头（自动过滤不安全头）、便捷的 Android 模拟器环回地址处理（localhost → 10.0.2.2）。

## 特性

- 多平台：Web、App-Android、App-iOS 一套 API
- 多连接：支持同时打开多个 SSE 连接，按 `requestId` 隔离
- 事件监听：支持全局监听与按连接监听（open/message/error/close）
- 自定义请求头：与浏览器一致地过滤受限头部键名
- Android 模拟器：自动将 `localhost/127.0.0.1/[::1]` 映射为 `10.0.2.2`
- 简单易用：统一的 UTS 接口定义，易于在 `uvue/ts/js` 中调用


## 快速上手

在页面或模块中引入 API：

```ts
import { 
  sseConnectApi, 
  sseCloseApi, 
  sseAddEventListenerApi, 
  sseRemoveEventListenerApi 
} from '@/uni_modules/sse-plugin'

const requestId = `sse_${Date.now()}`

// 可选：注册全局监听或按连接监听
sseAddEventListenerApi({
  requestId, // 省略则为全局监听
  onOpen: (e) => console.log('open', e.requestId),
  onMessage: (e) => console.log('message', e.message),
  onError: (e) => console.error('error', e.error),
  onClose: (e) => console.log('close', e.requestId)
})

// 启动连接
// Android 模拟器建议使用 http://10.0.2.2:3000/sse
sseConnectApi({
  url: 'http://localhost:3000/sse',
  requestId,
  headers: { 'User-Agent': 'UniApp-X-SSE-Plugin' },
  fail: (err) => console.error('connect fail', err)
})

// 关闭连接
sseCloseApi(requestId)

// 清理（移除监听）
sseRemoveEventListenerApi(null) // 传 null/undefined 清空全局监听
```

## API 说明

类型定义（摘自 `utssdk/interface.uts`）：

```ts
export type SSEConnectOptions = {
  url: string
  headers?: UTSJSONObject
  requestId?: string
  success?: (res: SSEConnectResult) => void
  fail?: (res: SSEApiFail) => void
  complete?: (res: any) => void
}

export type SSEEventListenerOptions = {
  requestId?: string
  onMessage?: (event: { requestId: string; message: string }) => void
  onError?: (event: { requestId: string; error: string }) => void
  onClose?: (event: { requestId: string }) => void
  onOpen?: (event: { requestId: string; message: string }) => void
}

export declare const sseConnectApi: (options: SSEConnectOptions) => void
export declare const sseCloseApi: (requestId: string) => void
export declare const sseAddEventListenerApi: (options: SSEEventListenerOptions) => void
export declare const sseRemoveEventListenerApi: (requestId?: string) => void
export declare const closeAllSSEConnections: () => void
```

注意：插件会与浏览器一致过滤部分不安全/受限请求头（如 `accept-encoding`、`cookie`、`content-length`、`origin`、`referer`、以 `sec-`/`proxy-` 开头等）。

