# Android平台UTS开发增强指南

> 同步说明（来源：官方文档）
> - 对齐页面：uts for Android（https://doc.dcloud.net.cn/uni-app-x/plugin/uts-for-android.html）与 UTSAndroid API（https://doc.dcloud.net.cn/uni-app-x/uts/utsandroid.html）
> - 同步时间：2025-09-13
> - 重要更新：
>   - HBuilderX 4.81 起统一升级 Kotlin 2.2.0（请按官方“kotlin2 升级注意事项”适配）
>   - HBuilderX 4.36+：`app-android/config.json` 支持 `project.repositories`
>   - Android 最低 `minSdkVersion` = 21

## 包名默认规则（重要）

UTS 插件在 Android 侧会对应一个 lib module，若未显式指定包名，HBuilderX 会按如下规则生成默认包名：

- `utssdk` 根下插件：`uts.sdk.{插件目录名（转驼峰）}`
- `uni_modules/{pluginId}/utssdk` 下插件：`uts.sdk.modules.{插件目录名（转驼峰）}`

示例：
- `uni_modules/uni-getbatteryinfo/utssdk/app-android/` → `uts.sdk.modules.uniGetbatteryinfo`
- `uni_modules/uts-nativepage/utssdk/app-android/` → `uts.sdk.modules.utsNativepage`

混编（Kotlin）时，Kotlin 源码文件的 `package` 必须与上述默认包名严格一致，否则会导致找不到类/方法等问题。

## AndroidManifest.xml 示例与常见权限

当插件需要声明组件（Service/Activity）或权限时，可在 `utssdk/app-android/AndroidManifest.xml` 中配置，示例：

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <!-- 常见权限 -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <!-- 前台服务权限（如需在前台长驻 service）-->
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <!-- 录音权限（语音能力等场景）-->
    <uses-permission android:name="android.permission.RECORD_AUDIO" />

    <application>
        <!-- Service / Activity 等组件声明（按需） -->
        <service
            android:name="uts.sdk.modules.utsNativepage.ForeService"
            android:exported="false" />

        <activity
            android:name="uts.sdk.modules.utsNativepage.DemoActivity"
            android:exported="false" />
    </application>
  </manifest>
```

注意：
- 插件的 `AndroidManifest.xml` 与原生工程规则一致，会在云打包阶段与 App 主工程合并。
- 若引用 AndroidX 资源（如使用 AppCompat 内建资源），需在 `app-android/config.json` 添加依赖：

```json
{
  "dependencies": [
    "androidx.appcompat:appcompat:1.0.0"
  ]
}
```

## 常见权限清单（参考）

- 网络：`android.permission.INTERNET`, `android.permission.ACCESS_NETWORK_STATE`
- 存储：`android.permission.READ_EXTERNAL_STORAGE`, `android.permission.WRITE_EXTERNAL_STORAGE`（API 29+ 分区存储）
- 录音：`android.permission.RECORD_AUDIO`
- 前台服务：`android.permission.FOREGROUND_SERVICE`
- 定位：`android.permission.ACCESS_COARSE_LOCATION`, `android.permission.ACCESS_FINE_LOCATION`

## 编译 SDK 与目标 SDK（compileSdk/targetSdk）

- 按官方要求配置合理的 `compileSdkVersion` 与 `targetSdkVersion`（通常与 HBuilderX 自带的 Android SDK 对齐）。
- 过高的 targetSdk 可能触发更严格的权限/前台服务/后台限制，需要对应代码适配。

## 过时 API 警告处理

- 遇到 `@Deprecated` 或 `requires API` 的告警时，优先参考官方替代 API，并通过 `Build.VERSION.SDK_INT` 做分支以兼容低版本。
- 避免滥用 `@Suppress("DEPRECATION")`。当确需使用过时 API 时，需在注释中说明理由和替代方案。

## .so 库与原生资源限制

- 官方说明：暂不支持直接将 `.so` 文件放入插件目录进行打包。
- 三方 native 依赖建议通过远程依赖（Gradle/Maven）或以 AAR 形式集成（遵循官方“本地依赖/远程依赖”章节）。

## R 资源 unresolved 处理

- 若出现 `unresolved reference R`，优先检查：
  - 资源是否放置在 `utssdk/app-android/res/` 的正确子目录（如 `layout/`, `values/`）；
  - 包名是否一致（参考“包名默认规则”）；
  - 是否引用了 AndroidX 资源但未添加 `androidx.appcompat:appcompat` 等依赖；
  - 清理构建缓存并重试云打包。

## 隐私协议适配（必读）

- 若插件涉及个人信息采集或敏感权限（录音、定位、摄像头等），需遵守相关隐私合规要求。
- 参考官方隐私协议适配说明，确保在首次采集前获得用户授权，并在商店上架材料中如实说明用途。

## Android特有开发要点（精简）

按官方 API 结构给出常用用法，更多方法与边界请查 UTSAndroid 文档：
https://doc.dcloud.net.cn/uni-app-x/uts/utsandroid.html

### 上下文与 Activity

```ts
// #ifdef APP-ANDROID
import { UTSAndroid } from 'io.dcloud.uts'
import { Context } from 'android.content.Context'
import { Activity } from 'android.app.Activity'

const appCtx: Context = UTSAndroid.getAppContext()
const topActivity: Activity | null = UTSAndroid.getUniActivity()
// #endif
```

### 线程调度

```ts
// #ifdef APP-ANDROID
const main = UTSAndroid.getDispatcher('main')
const io = UTSAndroid.getDispatcher('io')

io.async(() => {
  // 执行耗时任务
  main.async(() => {
    // 切回主线程更新UI
  })
})
// #endif
```

### 动态权限

```ts
// #ifdef APP-ANDROID
const perms = [
  'android.permission.RECORD_AUDIO',
  'android.permission.CAMERA'
]

UTSAndroid.requestSystemPermission(
  UTSAndroid.getAppContext(),
  perms,
  (all, granted) => {
    if (all) {
      // 已全部授权
    }
  },
  (doNotAskAgain) => {
    // 引导前往系统设置
  }
)
// #endif
```

### 资源与路径

```ts
// #ifdef APP-ANDROID
const res = UTSAndroid.getResourcePath('logo.png')
const abs = UTSAndroid.convert2AbsFullPath('./data/config.json')
// #endif
```

## 参考

- uts for Android（原生环境配置、仓库/依赖、Manifest 示例）：
  https://doc.dcloud.net.cn/uni-app-x/plugin/uts-for-android.html
- UTSAndroid API（方法清单与参数说明）：
  https://doc.dcloud.net.cn/uni-app-x/uts/utsandroid.html

## UTS 与 Kotlin 差异重点清单（精选）

- 变量定义：优先使用 `let`/`const`（UTS 语义）代替 Kotlin 的 `val/var`，并遵循 UTS 的类型推断与只读约束。
- 方法定义：UTS 的函数签名与 Kotlin 写法不同，注意返回类型、默认参数与可空类型在 UTS 的表达方式。
- 非空断言：尽量避免频繁使用非空断言，优先用类型守卫与可空类型分支；
- 匿名内部类：UTS 有专门写法，参考官方“匿名内部类”章节；
- 一个类只能有一个构造函数：在 UTS 中避免使用多构造重载，改用可选参数或工厂方法；
- 指定 double：数值字面量需要按平台严格类型处理，必要时显式类型转换；
- Java 包引入：`java.lang` 等包的引入需按 UTS 导入规则，避免隐式引用失败；
- 警告优化：遇到“推断为 XXX，但预期为 Unit”类警告时检查函数返回值与调用上下文。

## 常见问题（精选）

### 新建 Activity/Service/Thread

- Activity/Service：在 `AndroidManifest.xml` 中按需声明，并在 UTS 中通过 Intent 等方式启动；
- Thread：建议使用 UTSAndroid 的 Dispatcher（`main`/`io`）而非直接 new Thread，避免线程切换问题。

### 如何生成 byte[]

```ts
// 常见写法（示例），按 UTS 当前版本的数组创建规则
const bytes = new Array<number>(len)
// 或使用平台 API 提供的 ByteBuffer/Arrays 工具（以官方示例为准）
```

### 如何实现接口 / 访问静态方法

- 接口：使用 UTS 的接口实现语法，注意泛型与可空差异；
- 静态方法：通过类名调用，若为 Java 静态工具类，需按 UTS 导入完整限定名。

### 泛型参数传递丢失

- 部分场景编译期擦除导致类型信息丢失，建议在关键路径提供显式类型、或改用不依赖泛型的签名。

### 获取原生 Class 对象

- 参考 UTSAndroid 提供的 `getJavaClass/getKotlinClass` 能力（以官方签名为准）。

### Qualified name 错误

- “Qualified name must be a '.'-separated identifier list”：检查包名与导入路径是否与默认包名规则一致；UTS 的包名需与 Kotlin/Manifest 保持一致。
