# Android 前台服务保活实现说明（SSE 插件）

本文档给出在 Android 端为 SSE 连接提供“前台服务（Foreground Service）+ WakeLock + 多连接计数”的保活方案与落地步骤。实现风格参考了 uniapp-x-recorder 插件的 `RecordingService` 设计，并与本仓库现有结构对齐，以尽可能少的改动解决“应用切到后台后几秒内 SSE 断开”的问题。

## 背景与目标
- 问题：当前 Android 上将 App 切到后台后，SSE 连接在数秒内被系统回收，导致事件中断。
- 目标：
  - 提供可选的“前台服务保活”能力；默认关闭，按连接粒度启用。
  - 前台服务在存在任一启用的连接时保持运行，无连接时自动停止。
  - 允许自定义通知渠道/文案/重要度，并可选启用 PARTIAL_WAKE_LOCK。
  - 不破坏 iOS / Web 端；不影响未开启前台服务的调用。

## 总体设计
- 新增前台服务 `SseForegroundService`（Kotlin）：
  - 创建通知渠道（Android 8+），`startForeground()` 常驻通知，`foregroundServiceType` 使用 `dataSync`。
  - 可选获取 `PARTIAL_WAKE_LOCK`（默认启用，10 分钟超时）。
  - 提供 `configure(...)`、`start(context, wakeLockEnabled)`、`stop(context)` 静态方法。
- 在 `SSEManager` 内做“是否使用前台服务”的连接级计数：
  - `fgRefCount` 计数 > 0 时前台服务保持运行；降为 0 时停止。
  - 每个连接的 `ConnectionHandle` 记录 `useForeground`，确保 finally 阶段正确递减。
- UTS 层（Android）在 `sseConnectApi` 解析新配置：
  - `foregroundEnabled`、`foreground*` 文案、`foregroundImportance`、`wakeLockEnabled`、`notifications`（Android 13+ 通知权限策略）。
  - 按需调用 `SseForegroundService.configure(...)`，并通过新重载的 `startConnectionWithHeadersJson(..., useForeground)` 将开关透传到 Kotlin。

## 变更清单
1) UTS 接口扩展（路径：`sse-uniapp-demo/uni_modules/sse-plugin/utssdk/interface.uts`）
   - 在 `SSEConnectOptions` 中新增字段：
     - `foregroundEnabled?: boolean`（默认 false）
     - `foregroundChannelId?: string`
     - `foregroundChannelName?: string`
     - `foregroundTitle?: string`
     - `foregroundText?: string`
     - `foregroundImportance?: 'min'|'low'|'default'|'high'|'max'`
     - `notifications?: true | false | 'auto'`（Android 13+ 申请通知权限策略，默认 'auto'）
     - `wakeLockEnabled?: boolean`（默认 true）

2) 新增前台服务（路径：`sse-uniapp-demo/uni_modules/sse-plugin/utssdk/app-android/SseForegroundService.kt`）
   - 关键点：
     - `NotificationChannel`（8.0+），默认渠道 ID `sse_channel`，名称 `SSE`。
     - 默认文案：`title='SSE 正在保持连接'`，`text='后台保持连接以接收消息'`。
     - `foregroundServiceType="dataSync"`，小图标可先用 `android.R.drawable.stat_notify_sync`。
     - `WakeLock`：`PARTIAL_WAKE_LOCK`（10 分钟超时），按 `wakeLockEnabled` 控制是否启用。

3) AndroidManifest 权限与 Service 声明（路径：`sse-uniapp-demo/uni_modules/sse-plugin/utssdk/app-android/AndroidManifest.xml`）
   - 新增/确认权限：
     - `<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />`
     - `<uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />`（targetSdk 34+ 推荐）
     - `<uses-permission android:name="android.permission.WAKE_LOCK" />`
     - `<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />`（Android 13+）
   - 注册服务：
     - `<service android:name=".SseForegroundService" android:exported="false" android:foregroundServiceType="dataSync" />`

4) Kotlin 管理层改造（路径：`sse-uniapp-demo/uni_modules/sse-plugin/utssdk/app-android/SSEManager.kt`）
   - 结构变更：
     - `private class ConnectionHandle { ..., var useForeground: Boolean = false }`
     - `private val fgRefCount = AtomicInteger(0)`（单例域）
   - 新增重载：
     - `startConnectionWithHeadersJson(url, headersJson, requestId, callback, useForeground, wakeLockEnabled)`
       - 解析 headers 后复用现有 `startConnection(...)` 逻辑；
       - 启动前：若 `useForeground` 且 `fgRefCount.incrementAndGet() == 1`，调用 `SseForegroundService.start(ctx, wakeLockEnabled)`；
       - finally：若 `useForeground` 且 `fgRefCount.decrementAndGet() == 0`，调用 `SseForegroundService.stop(ctx)`。

5) UTS Android 桥接（路径：`sse-uniapp-demo/uni_modules/sse-plugin/utssdk/app-android/index.uts`）
   - 在 `sseConnectApi`：
     - 读取新增字段；将 `foregroundImportance` 映射为 `NotificationManager` 等级（可借鉴 recorder 的 `mapImportance`）。
     - `notifications` 策略：当为 `'auto'` 且系统为 Android 13+ 时，通过 `UTSAndroid` 检查并申请 `POST_NOTIFICATIONS`；失败时按需降级或回调错误。
     - 当任一 `foreground*` 文案/渠道配置存在时，先 `SseForegroundService.configure(...)`。
     - 改为调用 `SSEManager.startConnectionWithHeadersJson(finalUrl, headersJson, requestId, sseCallback, foregroundEnabled, wakeLockEnabled)`。

## API 变化（调用侧）
示例：
```ts
sseConnectApi({
  url: 'http://10.0.2.2:3000/sse',
  headers: { 'User-Agent': 'UniApp-SSE-Plugin' },
  requestId: `sse_${Date.now()}`,
  // 开启前台服务
  foregroundEnabled: true,
  // Android 13+ 通知权限策略
  notifications: 'auto',
  // 自定义通知（可选）
  foregroundChannelId: 'sse_channel',
  foregroundChannelName: 'SSE',
  foregroundTitle: 'SSE 正在连接',
  foregroundText: '后台保持连接以接收消息',
  foregroundImportance: 'low',
  // WakeLock（可选，默认 true）
  wakeLockEnabled: true,
  success: (r) => console.log('open', r),
  fail: (e) => console.error('err', e)
})
```

## 兼容性与默认行为
- 默认 `foregroundEnabled=false`：行为与当前版本一致（无前台服务）。
- 多连接：
  - 若同时有 A/B 两条连接且均启用了前台服务，只有当 A、B 都关闭时服务才停止。
  - 若 A 启用、B 未启用，停止 A 后若无其他启用连接，服务停止。
- iOS / Web：忽略新增字段，不受影响。

## 权限与合规
- Android 13+ 通知权限：
  - `notifications='auto'`：在连接前检查并按需申请；用户拒绝时可提醒并降级（不启用前台服务）。
  - `notifications=false`：不申请；若系统要求通知权限导致 `startForeground` 失败，将回调错误。
- WakeLock：仅在连接期间持有，默认 10 分钟超时，确保在 finally 或服务销毁时释放。
- 明文 HTTP：保持现有 `network_security_config`/`usesCleartextTraffic` 检查提示（SSEManager 已有日志）。

## 实现要点与伪代码
1) `SseForegroundService.kt`（结构要点）
```kotlin
class SseForegroundService : Service() {
  companion object {
    fun configure(channelId: String?, channelName: String?, importance: Int?, title: String?, text: String?) { /* ... */ }
    fun start(ctx: Context, wakeLockEnabled: Boolean) { /* create channel; startForeground; acquire PARTIAL_WAKE_LOCK if enabled */ }
    fun stop(ctx: Context) { /* stopService; release WakeLock */ }
  }
  override fun onCreate() { /* create channel */ }
  override fun onStartCommand(i: Intent?, f: Int, id: Int) = START_STICKY
  override fun onDestroy() { /* release */ }
  override fun onBind(i: Intent?) = null
}
```

2) `SSEManager.kt`（计数 + 生命周期）
```kotlin
private val fgRefCount = AtomicInteger(0)

fun startConnectionWithHeadersJson(url: String, headersJson: String?, requestId: String,
  cb: SSECallback, useForeground: Boolean, wakeLockEnabled: Boolean) {
  val ctx = UTSAndroid.getUniActivity()!!
  val handle = ConnectionHandle().apply { this.useForeground = useForeground }
  if (useForeground && fgRefCount.incrementAndGet() == 1) {
    SseForegroundService.start(ctx, wakeLockEnabled)
  }
  try { /* 原有连接与读取逻辑 */ } finally {
    if (handle.useForeground && fgRefCount.decrementAndGet() == 0) {
      SseForegroundService.stop(ctx)
    }
  }
}
```

3) `index.uts`（UTS 侧参数透传）
```ts
const fgEnabled = options.foregroundEnabled === true
const wakeLock = (options.wakeLockEnabled == null) ? true : !!options.wakeLockEnabled
// importance → Int
// notifications: 'auto' -> Android 13+ 检查/申请 POST_NOTIFICATIONS
if (cfgProvided) SseForegroundService.configure(...)
SSEManager.startConnectionWithHeadersJson(finalUrl, headersJson, requestId, sseCallback, fgEnabled, wakeLock)
```

## 开发步骤
1) 新增 `SseForegroundService.kt` 与 Manifest 声明。
2) 扩展 `SSEConnectOptions` 类型定义（UTS）。
3) 改造 `SSEManager.kt`：添加计数与新重载方法。
4) 更新 `index.uts`：解析新字段、权限策略、调用新重载。
5) 使用 HBuilderX 运行 `sse-uniapp-demo` 验证；模拟器 URL 使用 `http://10.0.2.2:3000/sse`。

## 本地验证与验收
- 启动服务端：`cd sse-server && pnpm install && pnpm dev`（SSE: `http://localhost:3000/sse`）。
- Android 模拟器：`url` 替换为 `http://10.0.2.2:3000/sse`。
- 前台服务开启用例：
  - 连接成功后切后台 ≥ 10 分钟，事件持续无中断；通知常驻。
  - 多连接 A/B 叠加：关闭 A 服务仍在；关闭 B 计数归零，服务关闭。
- 失败场景：
  - 通知权限被拒：`notifications='auto'` 时弹窗后仍拒绝 → 插件回调错误或降级行为符合预期。
- 在 `docs/TEST-sse-foreground-service-2025-11-12.md` 记录手测步骤与截图（建议）。

## 回滚方案
- 配置开关在调用层（`foregroundEnabled`）默认关闭；若出现兼容问题，可先停止传入该字段即回到原行为。

## 后续增强（可选）
- 将 SSE 连接迁入 Service 内，通过 Binder/广播回传事件，进一步提升保活强度。
- 指数退避重连 + 抖动；监听网络变化精准恢复。
- 电池优化豁免引导；必要时新增 `WifiLock`（Wi‑Fi 直连场景）。
- 动态更新通知正文展示最近事件统计/时间戳。
