# Android原生实现指南（精要）

> 同步说明（来源：官方文档）
> - 对齐页面：uts for Android（https://doc.dcloud.net.cn/uni-app-x/plugin/uts-for-android.html）
> - 同步时间：2025-09-13
> - 版本要点：minSdk 21；`project.repositories`（HX 4.36+）；Kotlin 2.2.0（HX 4.81 起）

## 概述

UTS 组件在 Android 平台通过原生 View 与系统能力实现。本章提供最小必要的结构、清单与示例，配合官方文档即可完成实现。

## 目录结构

```
utssdk/app-android/
├── index.uts                 // 主实现
├── config.json               // 平台配置（依赖/仓库/ABI 等）
├── AndroidManifest.xml       // 可选：权限与组件声明
├── res/                      // 资源
└── libs/                     // 本地 .aar/.jar
```

## config.json（示例）

```json
{
  "minSdkVersion": 21,
  "compileSdkVersion": 34,
  "targetSdkVersion": 34,
  "dependencies": [
    "androidx.appcompat:appcompat:1.6.1"
  ],
  "abis": ["armeabi-v7a", "arm64-v8a"],
  "project": {
    "repositories": [
      "maven { url 'https://jitpack.io' }",
      "maven { url 'https://maven.google.com/' }"
    ]
  }
}
```

## 包名与 Manifest

包名默认规则（与 07 章一致）：

- `utssdk` 根下：`uts.sdk.{插件目录名（驼峰）}`
- `uni_modules/{pluginId}/utssdk`：`uts.sdk.modules.{插件目录名（驼峰）}`

Kotlin 源码 `package` 必须与默认包名一致。

Manifest 片段（按需）：

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
  <uses-permission android:name="android.permission.INTERNET" />
  <application>
    <service android:name="uts.sdk.modules.utsNativepage.ForeService" android:exported="false" />
  </application>
</manifest>
```

提示：使用 AndroidX 资源时在 `config.json` 添加 `androidx.appcompat:appcompat` 依赖。

## 动态权限（示例）

```ts
import { UTSAndroid } from 'io.dcloud.uts'

const perms = ['android.permission.CAMERA']
UTSAndroid.requestSystemPermission(
  UTSAndroid.getAppContext(),
  perms,
  (all) => { /* 已授权 */ },
  () => { /* 引导前往设置 */ }
)
```

## 原生视图最小示例

```ts
import UniNativeViewElement from 'io.dcloud.uniapp.framework.UniNativeViewElement'
import View from 'android.view.View'
import Context from 'android.content.Context'

export class SimpleNativeComponent {
  private nativeView: View | null = null
  private context: Context | null = null

  bindView(element: UniNativeViewElement): void {
    this.context = element.getUniActivity()
    // 创建并设置 View（此处仅示意）
    this.nativeView = new View(this.context!)
    element.setNativeView(this.nativeView!)
  }
}
```

更多：自定义 View、事件分发、动画与资源访问等细节，请结合官方 uts for Android 文档与 07 章。
