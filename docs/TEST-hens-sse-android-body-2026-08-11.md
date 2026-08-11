# hens-sse uni-app Android 嵌套 Body 验证记录

验证日期：2026-08-11

## 验证目标

验证传统 uni-app Vue3 项目通过 Android SSE 插件发送 POST JSON 请求时，普通 JavaScript 对象中的嵌套 `messages` 数组是否会发生字段丢失。

## 验证环境

- HBuilderX：5.23.2026080626
- 测试项目：`uniapp-sse-playground`
- 项目类型：传统 uni-app Vue3
- 测试设备：Android 模拟器 `emulator-5556`
- 服务端接口：`POST http://10.0.2.2:3000/body-check`
- 请求 Content-Type：`application/json; charset=utf-8`

实际验证时使用了问题反馈中提供的 token，但出于安全考虑，本文档不会保存该值。

## 请求体结构

```json
{
  "lang": "cn",
  "modelType": "chat",
  "messages": [
    {
      "role": "user",
      "content": "你好",
      "fileList": [],
      "writeInfo": {},
      "openRea": 0,
      "todoList": null,
      "todoLists": false
    }
  ],
  "token": "<redacted>",
  "channelId": "1",
  "openRea": 0,
  "openId": "",
  "plat": 100
}
```

## 测试一：直接传 JavaScript 对象

页面先通过 `JSON.parse` 得到普通 JavaScript 对象，再直接传给插件：

```js
connectStream({
  url,
  method: 'POST',
  headers: {
    'Content-Type': 'application/json; charset=utf-8'
  },
  body
})
```

### 测试结果

问题可以稳定复现。

- Android 请求成功到达服务端，User-Agent 为 `okhttp/3.12.11`。
- 顶层字段 `lang`、`modelType`、`token`、`channelId`、`openRea`、`openId` 和 `plat` 均存在。
- `messages` 数组存在，长度为 1。
- `messages` 内的消息对象变成空对象 `{}`。
- `role`、`content`、`fileList`、`writeInfo`、`todoList` 等消息对象内部字段全部丢失。
- 服务端 `/body-check` 返回 `ok: false`。

服务端实际收到的关键部分为：

```json
{
  "messages": [
    {}
  ]
}
```

## 测试二：页面先序列化为 JSON 字符串

页面在调用插件前执行 `JSON.stringify`，将字符串作为 `body` 传给插件，并继续使用 JSON Content-Type：

```js
const bodyText = JSON.stringify(body)

connectStream({
  url,
  method: 'POST',
  headers: {
    'Content-Type': 'application/json; charset=utf-8'
  },
  body: bodyText
})
```

### 测试结果

验证通过。

- 服务端收到完整的 `messages` 数组和消息对象。
- `role: "user"`、`content: "你好"` 被完整保留。
- 空数组、空对象、`null`、布尔值、数字和空字符串均被保留。
- `/body-check` 返回 `ok: true`、`messagesPresent: true`、`messageCount: 1`。
- Android 客户端收到 HTTP 200，成功解析 SSE 消息并正常结束连接。

## 结论

客户反馈的问题在传统 uni-app Android 环境下可以复现。

问题不是服务端或 OkHttp 主动删除 `messages`，而是普通 JavaScript 对象作为 `UTSJSONObject` 参数进入 JS→UTS 桥接后，嵌套数组中的对象属性发生丢失。插件进入 Kotlin 前再次序列化时，`messages` 已经变成 `[{}]`。

当前已验证可用的规避方式是：在 uni-app JavaScript 页面调用插件前执行 `JSON.stringify(body)`，然后把 JSON 字符串传给插件，同时将 Content-Type 设置为 `application/json; charset=utf-8`。

## Demo 环境问题

原 Demo 使用 `APP-ANDROID` 条件判断 Android 模拟器地址，但传统 uni-app App 编译没有命中该条件，导致实际请求发往 `localhost:3000`。

`uniapp-sse-playground` 已改为在 `APP-PLUS` 运行环境中通过 `uni.getSystemInfoSync().platform` 判断 Android，并将本地服务地址映射到 `10.0.2.2`。该调整只修复 Demo 的本地服务连接，不影响 Body 对照测试结论。
