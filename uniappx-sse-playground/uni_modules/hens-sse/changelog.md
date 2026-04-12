# Changelog

## 2.0.0 - 2026-04-12

- 重构为文本流式 HTTP 客户端插件，支持 `sse`、`line`、`jsonl`、`raw`
- 新增 `connectStream(options) => StreamConnection`
- 新增 Harmony 平台实现，基于 `@ohos.net.http.requestInStream`
- 新增 `app-harmony/module.json5`，声明 `ohos.permission.INTERNET`
- 移除旧版 SSE API 与监听模型

## 1.0.0 - 2025-08-10

首个 SSE 版本。
