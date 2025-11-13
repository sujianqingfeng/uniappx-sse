# UTSAndroid - Android 平台内置对象（精要）

> 同步说明（来源：官方文档）
> - API 参考：UTSAndroid（https://doc.dcloud.net.cn/uni-app-x/uts/utsandroid.html）
> - 平台专章：uts for Android（https://doc.dcloud.net.cn/uni-app-x/plugin/uts-for-android.html）
> - 同步时间：2025-09-13
> - 版本要点：Android minSdk 21；Kotlin 2.2.0（HX 4.81 起）；`project.repositories`（HX 4.36+）
> - 说明：本页为精要示例，完整参数/返回值以官方文档为准。

## 导入

```ts
import { UTSAndroid } from 'io.dcloud.uts'
```

## 常用示例

### 目录（对齐官方主题）
- 导入与环境
- 上下文与 Activity
- 资源与路径
- 应用/系统信息与主题
- 线程调度（main/io）
- 动态权限
- 生命周期监听与返回键拦截
- 原生类访问与反射（能力）
- Activity 回调（能力）

### 应用上下文与 Activity

```ts
import { Context } from 'android.content.Context'
import { Activity } from 'android.app.Activity'

const appCtx: Context = UTSAndroid.getAppContext()
const topActivity: Activity | null = UTSAndroid.getUniActivity()
```

注记：
- 平台：Android
- 线程：任意（UI 操作请切回 main）
- 最低 HBuilderX：以官方为准

### 线程调度（main / io / default）

```ts
const main = UTSAndroid.getDispatcher('main')
const io = UTSAndroid.getDispatcher('io')

io.async(() => {
  // 后台耗时
  main.async(() => {
    // 回主线程更新UI
  })
})
```

注记：
- 线程语义：`main` 主线程，`io` 后台线程；如涉及 UI 操作请切回 `main`。
- 可用性：随 UTSAndroid 提供，建议使用最新 HBuilderX 以获得完整 API。

### 动态权限

```ts
const perms = ['android.permission.CAMERA']
UTSAndroid.requestSystemPermission(
  UTSAndroid.getAppContext(),
  perms,
  (all, granted) => {
    console.log('granted', all, granted)
  },
  (doNotAskAgain) => {
    // 引导前往系统设置
  }
)
```

注记：Android 6.0+ 需动态申请危险权限；若用户勾选“不要再询问”，需引导前往系统设置授权。
最低 HX：以官方为准；targetSdk 提升可能带来权限策略变化。

### 资源与路径

```ts
const res = UTSAndroid.getResourcePath('logo.png')
const abs = UTSAndroid.convert2AbsFullPath('./data/config.json')
```

注记：
- 平台：Android
- 线程：任意
- 最低 HBuilderX：以官方为准

### 应用/系统信息（节选）

```ts
const appId = UTSAndroid.getAppId()
const appName = UTSAndroid.getAppName()
const appVer = UTSAndroid.getAppVersion()
const theme = UTSAndroid.getOsTheme() // 'light' | 'dark'
UTSAndroid.onOsThemeChange((t: string) => {
  // 主题变化回调（示意）
  console.log('theme changed', t)
})
```

注记：
- 平台：Android
- 线程：任意（主题回调通常在主线程使用更安全）
- 最低 HBuilderX：以官方为准

## 生命周期监听（节选）

```ts
UTSAndroid.onAppActivityPause(() => {/* ... */})
UTSAndroid.onAppActivityResume(() => {/* ... */})
UTSAndroid.onAppActivityDestroy(() => {/* ... */})
UTSAndroid.onAppActivityBack(() => false) // 返回 false 走默认返回
```

注记：
- 平台：Android
- 线程：回调通常在主线程；请避免在回调中执行耗时操作。

## 原生类访问与反射（能力说明）

- 能力：UTS 提供访问原生类描述的能力（如获取 Java/Kotlin Class）。
- 用途：用于反射调用或与需要 Class 参数的原生 API 交互。
- 参考：完整签名与示例请查官方 UTSAndroid 文档对应条目。

示意：

```ts
// 获取目标类（以实际 API 与类名为准）
// const clazz = UTSAndroid.getJavaClass('android.view.View')
// 使用 clazz 进行反射或传参
```

## Activity 回调（能力说明）

- 能力：监听 Activity 相关回调（如 onActivityResult 类似语义，或通过专门回调接口）。
- 用途：启动外部 Activity 并在返回时获取结果、权限回调拼接等。
- 参考：请查官方文档中 UTSActivityCallback/相关回调能力的说明与示例。

示意：

```ts
// 注册回调（以官方签名为准）
// UTSAndroid.onActivityCallback((requestCode: Int, resultCode: Int, data: any) => {
//   // 处理返回结果
// })
```

## 提示

- 运行环境：Android 要求 `minSdkVersion >= 21`；Kotlin 自 HBuilderX 4.81 起统一 2.2.0。
- 若需更多方法（如 `getJavaClass/getKotlinClass/onActivityCallback` 等），请查官方 API 文档的完整清单与示例。
 - 本页示例偏向常用与入门，严谨场景（线程、权限、版本判断）以官方签名与约束为准。

## 官方目录映射（参考）

- 平台专用对象与 API：
  - UTSAndroid（核心对象）
  - UTSAndroidHookProxy（hook 代理）
  - UTSActivityCallback（Activity 回调接口）
- 常用条目：
  - getAppContext / getUniActivity / requestSystemPermission
  - getDispatcher('main'|'io') / onAppActivityResume/Pause/Destroy/Back
  - 资源与路径、应用/系统信息、主题与主题变更

完整清单与参数说明以官方文档为准。
