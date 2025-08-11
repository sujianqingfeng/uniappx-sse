### uniapp-navtive-plugin-sse

跨平台 Server-Sent Events (SSE) 原生能力插件，支持 uni-app X 与 uni-app（Web/Android/iOS）。提供统一 UTS 接口、多连接管理、事件监听、自定义请求头（自动过滤受限头），并内置 Android 模拟器环回地址适配。

支持的示例与子工程：
- `sse-uniapp-demo`: uni-app X 示例（uvue/uts）
- `sse-uniapp-v3-demo`: uni-app（Vue3）示例
- `sse-android`: Android 原生库（AAR）与示例
- `sse-ios-framework`: iOS 原生 Framework
- `sse-ios-demo`: iOS Swift 示例工程（集成 Framework）
- `sse-server`: 本地 Node.js SSE 测试服务器


### 特性

- 多平台：Web、App-Android、App-iOS 一套 API
- 多连接：支持同时打开多个 SSE 连接，按 `requestId` 隔离
- 事件监听：支持全局与按连接监听（open/message/error/close）
- 自定义请求头：与浏览器一致地过滤受限头名
- Android 模拟器：自动将 `localhost/127.0.0.1/[::1]` 映射为 `10.0.2.2`
- 简单易用：统一的 UTS 接口定义，`uvue/ts/js` 直接调用


### 目录结构

```
uniapp-navtive-plugin-sse/
  sse-uniapp-demo/            # uni-app X 示例（含 uni_modules/sse-plugin 插件）
  sse-uniapp-v3-demo/         # uni-app（Vue3）示例（同一套插件）
  sse-android/                # Android 原生库与构建脚本（AAR）
  sse-ios-framework/          # iOS 原生 Framework 与构建脚本
  sse-ios-demo/               # iOS 示例工程（集成上述 Framework）
  sse-server/                 # 本地 SSE 服务（Express）
  screenshots/                # 运行截图/GIF
```


### 快速体验

1) 启动本地 SSE 服务器

```bash
cd sse-server
pnpm i
pnpm dev
# 服务器地址： http://localhost:3000
# SSE 端点：   http://localhost:3000/sse
```

2) 运行示例应用

- uni-app X 示例：使用 HBuilderX 打开 `sse-uniapp-demo`，选择运行到 App-Android、App-iOS 或 Web。
- uni-app（Vue3）示例：使用 HBuilderX 打开 `sse-uniapp-v3-demo`，同上。

提示（Android 模拟器）：请使用 `http://10.0.2.2:3000/sse` 访问宿主机服务；插件与示例已内置自动映射。


### 使用方式（在页面/模块中）

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

// 启动连接（Android 模拟器建议 http://10.0.2.2:3000/sse）
sseConnectApi({
  url: 'http://localhost:3000/sse',
  requestId,
  headers: { 'User-Agent': 'UniApp-SSE-Plugin' },
  fail: (err) => console.error('connect fail', err)
})

// 关闭连接
sseCloseApi(requestId)

// 清理（移除监听）
sseRemoveEventListenerApi(null) // 传 null/undefined 清空全局监听
```


### API 列表（节选）

类型定义见 `uni_modules/sse-plugin/utssdk/interface.uts`

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


### Android 原生库（AAR）构建

文档：`sse-android/BUILD.md`

快速命令：

```bash
cd sse-android
./build-aar.sh -e debug            # 构建 debug 版本
./build-aar.sh -e release          # 构建 release 版本
./build-aar.sh -e release-minified # 构建混淆版
```

输出位置：`sse-android/sse-lib/build/outputs/aar/`

脚本会自动将 AAR 复制到示例插件目录：
`sse-uniapp-demo/uni_modules/sse-plugin/utssdk/app-android/libs/`

网络安全提示：示例已提供 `network_security_config.xml` 放行本地开发域名/IP：
`sse-uniapp-demo/nativeResources/android/res/xml/network_security_config.xml`


### iOS Framework 构建

文档：`sse-ios-framework/BUILD.md`

快速命令：

```bash
cd sse-ios-framework
./build-framework.sh            # 默认 Release，模拟器构建
./build-framework.sh -d         # 仅设备
./build-framework.sh -u         # 通用（模拟器+设备）
./build-framework.sh --clean    # 清理后构建
```

脚本会将生成的 Framework 自动复制到：
- 插件目录：`sse-uniapp-demo/uni_modules/sse-plugin/utssdk/app-ios/Frameworks/`
- iOS Playground：`sse-ios-demo/SSEDemo/`

ATS 提示：如需使用明文 HTTP 测试，请在 App 的 `Info.plist` 中配置 `NSAppTransportSecurity` 例外。


### Web 端实现

`uni_modules/sse-plugin/utssdk/web/index.js` 基于 Fetch + ReadableStream 实现，与 UTS API 对齐并支持自定义安全头部。


### 截图

![Android Demo](./screenshots/android-demo.gif)

![iOS Demo](./screenshots/ios-demo.gif)

![Web Demo](./screenshots/web-demo.gif)


### 常见问题

- Android 连接 http://localhost 失败？
  - 在模拟器上请改用 `http://10.0.2.2:3000/sse`，或直接使用示例默认值；插件也会自动映射。

- 自定义请求头未生效？
  - 浏览器与原生实现会过滤受限头名（如 `cookie`、`content-length` 等），请改用允许的自定义头。

- iOS 明文 HTTP 被拦截？
  - 为开发期间测试，可在 `Info.plist` 配置 ATS 例外，或改用 HTTPS。

- 如何在自己的项目中使用该插件？
  - 将 `sse-uniapp-demo/uni_modules/sse-plugin` 复制到你的项目 `uni_modules/` 下，即可通过上述 API 使用。


### 贡献

欢迎提交 Issue 与 PR。开发时可分别在 `sse-android`、`sse-ios-framework` 构建产物，并在 `sse-uniapp-demo` 或 `sse-uniapp-v3-demo` 中联调。


