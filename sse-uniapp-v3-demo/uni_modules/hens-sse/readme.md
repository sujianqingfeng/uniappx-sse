# hens-sse

用于 uni-app x / uni-app 的文本流式 HTTP 客户端插件。

当前支持平台：

- Web
- App Android
- App iOS
- App Harmony

适用场景：

- 标准 SSE
- 按行文本流
- JSONL / NDJSON
- 原始文本 chunk

## 导入

```ts
import { connectStream } from '@/uni_modules/hens-sse'
```

## 快速开始

```ts
import { connectStream } from '@/uni_modules/hens-sse'

const connection = connectStream({
  url: 'http://localhost:3000/sse',
  method: 'GET',
  protocol: 'sse',
  debug: true,
  headers: {
    'X-Demo-Protocol': 'sse'
  }
})

connection.onOpen((evt) => {
  console.log('open', evt.statusCode, evt.headers)
})

connection.onChunk((evt) => {
  console.log('chunk', evt.text)
})

connection.onMessage((evt) => {
  console.log('message', evt.event, evt.id, evt.data, evt.rawText)
})

connection.onError((err) => {
  console.error('error', err.errCode, err.errMsg, err.data)
})

connection.onComplete(() => {
  console.log('complete')
})

// 手动停止
connection.abort()
```

## API

### `connectStream(options)`

参数：

- `url: string`
  流式接口地址，必填。
- `method?: string | null`
  请求方法，默认 `GET`。
- `headers?: UTSJSONObject | null`
  自定义请求头。
- `body?: UTSJSONObject | string | null`
  请求体。建议只在 `POST` / `PUT` / `PATCH` / `DELETE` 时传入。
- `timeout?: number | null`
  超时时间，单位毫秒。App 平台默认 `60000`。
- `protocol?: 'sse' | 'line' | 'jsonl' | 'raw' | null`
  解析协议，默认 `sse`。
- `autoParseJson?: boolean | null`
  是否自动把 message 文本解析为 JSON。未传时按协议默认值处理：`sse` / `line` 默认 `false`，`jsonl` 默认 `true`。
- `debug?: boolean | null`
  是否输出插件内部调试日志，默认 `false`。开启后会打印 `connect/open/chunk/message/error/complete/abort`，其中 `chunk` 会输出完整文本内容。

返回值：

```ts
type StreamConnection = {
  abort(): void
  onOpen(callback): void
  offOpen(): void
  onChunk(callback): void
  offChunk(): void
  onMessage(callback): void
  offMessage(): void
  onError(callback): void
  offError(): void
  onComplete(callback): void
  offComplete(): void
}
```

## 事件说明

### `onOpen`

收到响应头后触发一次。

如果服务端返回 `4xx` / `5xx`，也会先触发 `onOpen`，随后再触发 `onError` 和 `onComplete`。

Harmony 平台说明：

- Harmony 端基于 `@ohos.net.http.requestInStream`
- 鸿蒙响应头事件不一定能在流刚建立时拿到最终状态码
- 因此 `evt.statusCode` 在 Harmony 上可能为 `NaN`
- 最终 HTTP 错误仍会通过 `onError` 和 `onComplete` 上报

```ts
connection.onOpen((evt) => {
  console.log(evt.statusCode)
  console.log(evt.headers)
})
```

### `onChunk`

每收到一段原始文本就触发一次。无论是哪种协议，都会先走 `onChunk`。

```ts
connection.onChunk((evt) => {
  console.log(evt.text)
})
```

### `onMessage`

仅在 `sse` / `line` / `jsonl` 下触发，`raw` 不会触发。

- `sse`: `evt.data` 保持原始字符串
- `line`: `evt.data` 保持原始字符串
- `jsonl`: `evt.data` 优先解析为 JSON；解析失败时保留原始字符串
- 显式传 `autoParseJson: true/false` 时，会覆盖上面的协议默认行为

```ts
connection.onMessage((evt) => {
  console.log(evt.event)
  console.log(evt.id)
  console.log(evt.data)
  console.log(evt.rawText)
})
```

### `onError`

网络失败、HTTP 非 2xx、解析失败时触发。

```ts
connection.onError((err) => {
  console.error(err.errCode, err.errMsg, err.data)
})
```

### `onComplete`

连接正常结束、发生错误、或手动调用 `abort()` 后都会触发一次。

```ts
connection.onComplete(() => {
  console.log('stream finished')
})
```

## 协议差异

### `sse`

按标准 Server-Sent Events 解析。

- `onChunk`: 收到原始文本片段时触发
- `onMessage`: 收到完整 SSE message 时触发
- `evt.data`: 保持原始字符串，不自动解析 JSON

```ts
const connection = connectStream({
  url: 'http://localhost:3000/sse',
  protocol: 'sse'
})
```

### `line`

按换行切分，每一行对应一个 message。

- `onChunk`: 收到原始文本片段时触发
- `onMessage`: 每一行触发一次
- `evt.data`: 行文本

```ts
const connection = connectStream({
  url: 'http://localhost:3000/line-stream',
  protocol: 'line'
})
```

### `jsonl`

按换行切分，每一行按 JSONL / NDJSON 解析。

- `onChunk`: 收到原始文本片段时触发
- `onMessage`: 每一行触发一次
- `evt.data`: 优先解析为 JSON；解析失败时保留原始字符串

```ts
const connection = connectStream({
  url: 'http://localhost:3000/jsonl-stream',
  protocol: 'jsonl'
})
```

### 覆盖默认解析行为

```ts
const connection = connectStream({
  url: 'http://localhost:3000/sse',
  protocol: 'sse',
  autoParseJson: true
})
```

```ts
const connection = connectStream({
  url: 'http://localhost:3000/jsonl-stream',
  protocol: 'jsonl',
  autoParseJson: false
})
```

### `raw`

不做 message 切分，只保留 chunk。

- `onChunk`: 收到原始文本片段时触发
- `onMessage`: 不触发

```ts
const connection = connectStream({
  url: 'http://localhost:3000/raw-stream',
  protocol: 'raw'
})
```

## 常见用法

### POST + JSON 请求体

```ts
const connection = connectStream({
  url: 'http://localhost:3000/sse',
  method: 'POST',
  protocol: 'sse',
  headers: {
    'Content-Type': 'application/json'
  },
  body: {
    topic: 'demo',
    userId: 'u_001'
  }
})
```

### 发送纯文本请求体

```ts
const connection = connectStream({
  url: 'http://localhost:3000/raw-stream',
  method: 'POST',
  protocol: 'raw',
  headers: {
    'Content-Type': 'text/plain'
  },
  body: 'hello stream'
})
```

### 开启调试日志

```ts
const connection = connectStream({
  url: 'http://localhost:3000/sse',
  protocol: 'sse',
  debug: true
})
```

开启后，插件会在控制台输出连接生命周期日志。`chunk` 日志会打印完整文本内容，适合排查流式拆包问题；如果响应里包含敏感信息，不建议在生产环境打开。

### 页面卸载时关闭连接

```ts
let connection: ReturnType<typeof connectStream> | null = null

onUnload(() => {
  connection?.abort()
  connection = null
})
```

## 平台注意事项

- Android 模拟器访问本机服务时，建议把 `localhost` 改成 `10.0.2.2`。
- 第一版统一以 UTF-8 文本流为前提。
- 第一版不暴露二进制 chunk。
- `body` 传对象时，会序列化为 JSON 字符串。
- 手动 `abort()` 不应作为错误处理；如果你只是主动停止连接，请在 `onComplete` 里做收尾。

## 错误码

- `9030002`: URL 无效
- `9030003`: 网络请求失败
- `9030004`: HTTP 状态码错误
- `9030005`: 流解码失败
- `9030007`: 当前环境不支持流读取
