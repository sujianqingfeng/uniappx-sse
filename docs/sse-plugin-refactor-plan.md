# sse-plugin 重构计划

## 背景

当前 `uniappx-sse-playground/uni_modules/sse-plugin` 的实现以“标准 SSE 客户端”为中心设计：

- 对外 API 固定为 `sseConnectApi`、`sseCloseApi`、`sseAddEventListenerApi`
- 事件模型固定为 `open/message/error/close`
- 消息载荷固定为 `message: string`
- 平台实现直接把网络读取和 SSE 解析耦合在一起

这个设计对标准 `text/event-stream` 场景可以工作，但对以下场景扩展性很差：

- 服务端不是严格的 SSE 格式，只是普通文本流
- 服务端按行返回 JSONL
- 服务端只输出 `data:` 文本，但没有严格按 SSE block 组织
- 服务端虽然是流式 HTTP，但并不提供 `event` / `id` / 空行分隔等标准字段

本次重构不要求兼容旧 API，目标是把插件从“标准 SSE 插件”重构为“文本流式 HTTP 客户端插件”。

建议同步重命名插件 id 与导入路径，避免继续使用 `sse-plugin` 这个已经不准确的语义名称。当前推荐在实施前明确新名称，例如：

- `stream-plugin`
- `text-stream-client`
- `event-stream-client`

如果短期内不改包名，也要在文档中明确说明“插件能力已不再局限于标准 SSE”，避免使用者继续按旧语义理解。

## 重构目标

### 目标能力

- 支持标准 SSE
- 支持非标准文本流
- 支持按行协议
- 支持 JSONL 协议
- 第一版统一以 UTF-8 文本流为前提
- 提供统一的连接对象，避免全局监听器注册表
- 统一 Android / iOS / Web 的流式消费模型

### 明确不做

- 不兼容现有 `sseConnectApi` / `sseAddEventListenerApi` 风格
- 第一版不暴露 Android 前台服务相关配置
- 第一版不暴露 `ArrayBuffer` 二进制 chunk 给业务层
- 第一版不支持非 UTF-8 文本流
- 第一版不支持业务层自定义 UTS parser 回调注入
- 第一版不处理 Harmony 实现，只提供占位或明确标注不支持

## 现状问题

### 1. 对外接口过度绑定 SSE

当前接口定义位于 `utssdk/interface.uts`，核心问题是公共接口直接以 SSE 命名并绑定 SSE 事件模型。

问题点：

- `SSEConnectOptions` 只适合 GET + SSE 长连接语义
- `SSEMessageEvent` 只有 `message: string`
- 没有原始 text chunk 事件
- 没有协议模式选择
- 全局监听器模型增加状态复杂度

### 2. Android 实现把传输层和协议层写死在一起

Android `SSEManager.kt` 直接使用 `HttpURLConnection` 读取文本，并在读取循环中只处理 `data:` 行，将空行视为一个事件结束。

问题点：

- 不能复用于 `line` / `jsonl` / `raw` 等协议
- `event:` / `id:` 只记录不产出到业务层
- 无法独立测试 parser
- 前台服务保活逻辑和流式能力耦合在同一实现中

### 3. iOS 实现对流式 chunk 边界处理不稳

iOS `SSEFramework.swift` 在 `didReceive data` 中直接把当前 `Data` 解码成字符串后按行扫描 `data:`。

问题点：

- 没有跨 chunk 缓冲区
- 一个逻辑事件被拆成多个 `Data` 包时可能解析错误
- 无法支持非标准协议

### 4. Web 实现虽然使用 Fetch Stream，但解析器仍然写死为 SSE

Web 已有 `fetch + ReadableStream` 基础，但仍然在 `readSSEStream` 中固化了 SSE 解析规则。

### 5. 当前 API 形态不利于后续扩展

参考 `uniappx-plugin-langgraph` 的经验，更合理的方式是：

- 底层负责通用流式 request 和 text chunk 回调
- 上层 transport 负责协议解析和连接对象封装
- 页面侧只操作返回的连接对象，不操作全局 listener 注册表

## 新 API 设计

## 设计原则

- API 名称不再绑定 `SSE`
- 连接由返回对象承载生命周期
- 底层统一只处理文本流
- 上层 parser 负责把文本流切成 message
- `raw` / `line` / `sse` / `jsonl` 都走同一套连接模型
- `connectStream` 不抛同步异常，参数错误、URL 错误、网络错误统一通过连接对象异步回调上报
- `connectStream` 必须先构造并返回连接对象，再启动底层请求，避免业务层错过早期 `onOpen` / `onError`

## 草案

```ts
export type StreamProtocol = 'sse' | 'line' | 'jsonl' | 'raw'

export type StreamConnectOptions = {
  url: string
  method?: string | null
  headers?: UTSJSONObject | null
  body?: UTSJSONObject | string | null
  timeout?: number | null
  protocol?: StreamProtocol | null
}

export type StreamOpenEvent = {
  statusCode: number
  headers: UTSJSONObject
}

export type StreamChunkEvent = {
  text: string
}

export type StreamMessageEvent = {
  event?: string
  id?: string
  data: any
  rawText: string
}

export interface StreamConnection {
  onOpen(callback: ((evt: StreamOpenEvent) => void) | null): void
  onChunk(callback: ((evt: StreamChunkEvent) => void) | null): void
  onMessage(callback: ((evt: StreamMessageEvent) => void) | null): void
  onError(callback: ((err: any) => void) | null): void
  onComplete(callback: (() => void) | null): void
  abort(): void
}

export function connectStream(options: StreamConnectOptions): StreamConnection
```

## 协议语义

### `raw`

- 不做消息切分
- 每次 text chunk 到达时触发 `onChunk`
- 不触发 `onMessage`

### `line`

- 以 `\n` / `\r\n` 为消息边界
- 每一行触发一次 `onMessage`
- `data` 为字符串，`rawText` 为原始文本行

### `jsonl`

- 以 `\n` / `\r\n` 为消息边界
- 每一行尝试 `JSON.parse`
- 解析成功则 `data` 为对象
- 解析失败触发 `onError` 并终止连接

### `sse`

- 标准 SSE block 解析
- 支持 `event:` / `id:` / 多行 `data:`
- `data` 优先尝试 `JSON.parse`，失败则回退字符串
- `rawText` 保留 block 中拼接后的原始 data 文本

## 目录结构调整

建议按 `uniappx-plugin-dev` 推荐结构改造 `utssdk/`：

```text
utssdk/
  interface.uts
  protocol.uts
  runtime.uts
  platform.uts
  unierror.uts
  logger.uts
  internal/
    decoder.uts
    parser-sse.uts
    parser-line.uts
    parser-jsonl.uts
    transport.uts
  app-android/
    index.uts
    config.json
  app-ios/
    index.uts
    config.json
  web/
    index.js
    config.json
```

### 各文件职责

`interface.uts`

- 唯一公共 API 权威定义
- 只暴露 `connectStream` 及其类型

`protocol.uts`

- 协议常量
- 默认值归一化
- 参数校验

`runtime.uts`

- 连接实例状态
- 回调注册和解绑
- 运行时共享逻辑

`platform.uts`

- 平台标识
- 平台能力分支

`unierror.uts`

- 错误码定义
- 错误对象工厂

`internal/decoder.uts`

- UTF-8 chunk 解码
- 处理跨 chunk 字符边界

`internal/parser-*.uts`

- 各协议解析器
- 只处理字符串 buffer 和 message 切分

`internal/transport.uts`

- 调用底层 request / stream 能力
- 接收 headers / chunk / complete / fail
- 把 text chunk 喂给 parser
- 驱动 `StreamConnection`

## 平台实现方案

## Android

### 目标

- 去掉 SSE 专用 `SSEManager` 语义
- 保留流式 HTTP 读取能力
- 提供 text chunk 回调

### 改造建议

- 优先参考 `hans-request` 的 `RequestTask` 设计
- 底层能力收敛为：
  - `onHeadersReceived`
  - `onTextChunkReceived`
  - `abort`
  - `complete/fail`
- 不在 Android 原生层做 SSE 解析
- 如果保留前台服务相关代码，先下沉为 Android 内部可选实现，不进入公共 API

### 风险

- 当前前台服务逻辑混在现有实现中，拆分时要避免残留状态泄漏
- 当前 `HttpURLConnection` 可继续使用，但若后续需要更强控制，可评估迁移到 OkHttp

## iOS

### 目标

- 修复当前按 data 包直接解析字符串的问题
- 建立稳定的 text chunk 流

### 改造建议

- 保留 `URLSessionDataDelegate`
- `didReceive response` 只负责 headers/open
- `didReceive data` 只负责把 `Data` 交给 UTF-8 decoder，产出 text chunk
- parser 全部上移到 UTS transport 层

### 风险

- iOS UTS 到 Swift 的桥接对复杂对象和多次回调有约束
- 第一版应优先使用简单文本事件模型，避免引入额外桥接复杂度
- 需要明确无论参数校验失败还是 URL 非法，错误都应以异步方式上报给 `StreamConnection`

## Web

### 目标

- 继续基于 `fetch + ReadableStream`
- 从 SSE 专用读取器改为通用文本流读取器

### 改造建议

- `reader.read()` 后统一解码 text chunk
- 由 transport 层统一喂给 parser
- Web 端实现作为最快的 API 验证平台

## 错误模型建议

建议在 `unierror.uts` 中统一错误模型，至少区分以下类别：

- 参数错误
- URL 非法
- 网络错误
- HTTP 状态错误
- 文本解码错误
- 协议解析错误
- 平台不支持

建议错误对象携带：

- `errSubject`
- `errCode`
- `errMsg`
- `data`

`data` 中建议保留：

- `platform`
- `url`
- `method`
- `statusCode`
- `protocol`
- `raw`

## 分阶段实施计划

## 第一阶段：接口与结构重建

目标：先把公共 API 和目录结构改正。

任务：

- 新建重构版 `interface.uts`
- 新建 `protocol.uts`、`runtime.uts`、`platform.uts`
- 重定义错误码和错误对象
- 明确旧 API 不再保留

产出：

- 新接口可在 HBuilderX 中正常提示
- 各平台 `index` 先以最小可编译骨架接上

## 第二阶段：底层通用文本流 transport

目标：各平台统一提供 `headers + textChunk + complete + fail + abort`。

任务：

- Android 重构现有流式读取逻辑
- iOS 重构为稳定 chunk 解码流程
- Web 改成通用 reader
- transport 层接收各平台流事件

产出：

- `protocol = raw` 可以工作
- 页面可以看到 `onOpen` / `onChunk` / `onComplete`

## 第三阶段：协议解析器

目标：完成上层消息语义。

任务：

- 实现 `parser-line.uts`
- 实现 `parser-jsonl.uts`
- 实现 `parser-sse.uts`
- 在 transport 中根据 `protocol` 选择 parser

产出：

- `line` / `jsonl` / `sse` 都能输出 `onMessage`
- `raw` 仍只提供 chunk 能力

## 第四阶段：demo 页面与文档

目标：用 demo 和文档固定正确用法。

任务：

- 重写 demo 页面调用方式
- 增加标准 SSE 示例
- 增加非标准文本流示例
- 增加 JSONL 示例
- 更新 `readme.md` 和 `changelog.md`

产出：

- demo 成为新的验收用例
- 文档不再描述旧 API

## 第五阶段：验证与回归检查

目标：确认重构后的跨端行为一致。

验证项：

- 标准 SSE 多行 `data:` 拼接
- SSE `event:` / `id:` 正确透出
- JSONL 多条消息连续到达
- 非标准文本流按行切分
- chunk 被拆包时消息仍正确拼接
- HTTP 非 2xx 时正确报错
- 主动 `abort()` 后不再继续回调

## 推荐实施顺序

1. 先完成 API 和 transport 骨架，不保留兼容层
2. 先用 Web 完成 parser 与连接模型验证，快速冻结 API
3. Android 与 iOS 的通用流式实现并行推进；如果当前主验收平台是 uni-app x App，则 Android 优先级不低于 iOS
4. 最后统一 parser 和 demo

这个顺序的原因：

- API 不先定，平台实现会反复返工
- Web 最适合作为 parser 与连接模型的低成本验证环境
- iOS 的 chunk 处理问题是当前实现里最大的正确性风险之一
- Android 是当前 demo 的重要运行平台，不应被放到过晚阶段才暴露底层问题
- parser 独立后，协议扩展成本才会显著下降

## 需要注意的边界

- `raw` 模式下不触发 `onMessage`，只触发 `onChunk`
- `jsonl` 模式遇到非法 JSON 时立即 `onError` 并中止连接
- `sse` 模式下 `data` 为空字符串时是否仍派发事件，建议遵循标准 SSE 语义
- 第一版不支持 `ArrayBuffer body`，避免把插件目标再次扩展成通用二进制流客户端
- 第一版统一按 UTF-8 文本流处理；若服务端编码不满足该前提，视为不支持场景
- 需要明确插件是否同步更名；如果不更名，必须在 README 和 demo 中反复强调新语义范围

## 结论

本次重构的核心不是“增强 SSE 插件”，而是把插件改造成一层稳定的文本流 transport + 多协议 parser。

只有先把：

- 网络读取
- 文本解码
- 协议解析
- 连接对象生命周期

这四层拆开，后续支持非标准 SSE、JSONL、逐行文本流才不会再次陷入平台分叉和 API 失控。
