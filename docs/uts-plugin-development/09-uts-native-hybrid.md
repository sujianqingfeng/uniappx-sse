# UTS原生混编开发指南（精简）

> 同步说明（来源：官方文档）
> - 参考：
>   - uts for Android：https://doc.dcloud.net.cn/uni-app-x/plugin/uts-for-android.html
>   - uts for iOS：https://doc.dcloud.net.cn/uni-app-x/plugin/uts-for-ios.html
>   - uts for HarmonyOS：https://doc.dcloud.net.cn/uni-app-x/plugin/uts-for-harmony.html
> - 同步时间：2025-09-13
> - 版本要点：Android 最低 `minSdkVersion` = 21；HarmonyOS ArkTS 自 HBuilderX 4.22+；Kotlin 2.2.0 自 HBuilderX 4.81 起

## 概述

UTS 原生混编允许在 `utssdk/app-android|app-ios|app-harmony` 中直接放置原生代码（Kotlin/Swift/ArkTS），并在 UTS 中直接调用，无需额外桥接。

## 目录结构（示例）

```
uni_modules/your-plugin/
├─utssdk/
│ ├─app-android/
│ │ ├─index.uts
│ │ └─Echo.kt           // Kotlin 代码
│ ├─app-ios/
│ │ ├─index.uts
│ │ └─Echo.swift        // Swift 代码
│ └─app-harmony/
│   ├─index.uts
│   └─Echo.ets          // ArkTS 代码
├─interface.uts
├─unierror.uts
└─package.json
```

## Android（Kotlin）

包名规则（重要）：`uni_modules/{pluginId}/utssdk/app-android/` 下默认包名为
`uts.sdk.modules.{插件目录名（转驼峰）}`。Kotlin 文件 `package` 必须与之完全一致。

```kotlin
// utssdk/app-android/Echo.kt
package uts.sdk.modules.utsNativepage // 按实际插件目录名替换

class Echo {
    fun say(text: String): String = "Echo: $text"
    companion object { fun create(): Echo = Echo() }
}
```

```ts
// utssdk/app-android/index.uts
// #ifdef APP-ANDROID
import { Echo } from './Echo.kt'

export function echo(text: string): string {
  return Echo.create().say(text)
}
// #endif
```

Android 其他：Manifest/资源/远程依赖等配置，请见官方“uts for Android”。

## iOS（Swift）

Swift 的外部参数标签会影响 UTS 调用。建议方法签名明确外部标签，或使用下划线省略。

```swift
// utssdk/app-ios/Echo.swift
import Foundation

@objc public class Echo: NSObject {
    @objc public func say(_ text: String) -> String { // 省略外部标签
        return "Echo: \(text)"
    }
}
```

```ts
// utssdk/app-ios/index.uts
// #ifdef APP-IOS
import { Echo } from './Echo.swift'

export function echo(text: string): string {
  return (new Echo()).say(text)
}
// #endif
```

iOS 其他：在 `app-ios/config.json` 添加缺失的系统 Framework；必要的 Info.plist 权限说明需合规。

## HarmonyOS（ArkTS）

```ts
// utssdk/app-harmony/Echo.ets
export class Echo {
  say(text: string): string { return `Echo: ${text}` }
}
```

```ts
// utssdk/app-harmony/index.uts
// #ifdef APP-HARMONY
import { Echo } from './Echo.ets'

export function echo(text: string): string {
  return (new Echo()).say(text)
}
// #endif
```

## 调试与打包

- 真机运行/自定义基座/断点调试：参考 UTS 插件介绍页“真机运行/Debug”。
- 云打包时，平台子目录下的清单与依赖会合并到主工程。

## 参考

- UTS 插件介绍与原生混编：
  https://doc.dcloud.net.cn/uni-app-x/plugin/uts-plugin.html#uts-native-hybrid
- 平台专章：Android / iOS / HarmonyOS

## 常见问题（混编要点）

- Android 包名一致性：Kotlin 文件 package 必须与默认包名完全一致（详见 Android 包名默认规则），否则会出现找不到类/方法等错误。
- iOS 参数标签：Swift 外部参数名会体现在 UTS 调用签名，推荐使用 `_` 省略或保持一致，减少调用歧义。
