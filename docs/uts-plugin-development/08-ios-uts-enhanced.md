# iOS平台UTS开发增强指南

> 同步说明（来源：官方文档）
> - 对齐页面：uts for iOS（https://doc.dcloud.net.cn/uni-app-x/plugin/uts-for-ios.html）与 UTSiOS API（https://doc.dcloud.net.cn/uni-app-x/uts/utsios.html）
> - 同步时间：2025-09-13
> - 重要提示：
>   - 涉及 UI 的操作需在主线程执行（如 `DispatchQueue.main.async`）
>   - 缺失系统 Framework 需在 `app-ios/config.json` 中显式添加依赖

## 基础

### 导入方式

```ts
// #ifdef APP-IOS
import { UTSiOS } from 'DCloudUTSFoundation'
// #endif
```

### 常用能力（精简示例）

```ts
// #ifdef APP-IOS
// 设备与应用信息
const info = {
  deviceId: UTSiOS.getDeviceId(),
  model: UTSiOS.getModel(),
  appId: UTSiOS.getAppId(),
  appName: UTSiOS.getAppName(),
  appVersion: UTSiOS.getAppVersion()
}

// 资源与路径
const res = UTSiOS.getResourcePath('logo.png')
const abs = UTSiOS.convert2AbsFullPath(res)

// 主题
const systemTheme = UTSiOS.getOsTheme() // 'light' | 'dark'
UTSiOS.onOsThemeChange((t: string) => {
  console.log('系统主题变化', t)
})

// UI 主线程
// 需要在主线程执行的 UI 更新逻辑放入主线程队列
// DispatchQueue.main.async { /* update ui */ }
// #endif
```

## iOS 依赖配置

### 在 app-ios/config.json 添加系统 Framework

```json
{
  "frameworks": [
    "AVFoundation",
    "CoreLocation"
  ]
}
```

提示：iOS 平台不支持 `libraries` 字段。若需引入第三方库，请使用 CocoaPods 并在 `utssdk/app-ios/config.json` 配置 `dependencies-pods`（详见《在 UTS 插件中使用 CocoaPods（iOS）》文档）。

### Info.plist 权限示例（节选）

```xml
<key>NSCameraUsageDescription</key>
<string>需要使用相机用于拍摄</string>
<key>NSMicrophoneUsageDescription</key>
<string>需要使用麦克风进行录音</string>
```

规范提醒：权限用途描述需清晰、与实际功能一致，避免影响商店审核。

### entitlement（能力）示例

当插件需要启用某些系统能力（如 Push、App Groups、Keychain Sharing）时，需要在主工程或插件侧声明 entitlement。示意：

```xml
<!-- 仅为示意，实际由 Xcode 配置生成 -->
<key>com.apple.security.application-groups</key>
<array>
  <string>group.com.example.app</string>
  </array>
```

说明：entitlement 通常由应用工程统一配置，插件如需此能力，请在文档中明确要求并与宿主工程对齐。

### 不包含 Modules 的 framework（注意事项）

对于不包含 Modules 的三方 framework，需在 `config.json` 按官方说明进行特殊处理，或优先使用包含 Modules 的版本。
如遇符号找不到或头文件不可见等问题，请核对 framework 产物与编译设置，并参考官方“3.4.3 不包含 Modules 的 framework 使用说明”。

## Swift 与 UTS 差异重点清单（精选）

- 常量/变量：Swift 的 `let/var` 与 UTS 的 `const/let` 语义相近，但以 UTS 的类型系统与推断为准；
- 可选类型：Swift Optional 在 UTS 中用可空类型表达（`string | null`），注意判空与解包；
- 构造方法与参数标签：Swift 外部参数名会体现在 UTS 调用签名，必要时使用 `_` 省略外部标签；
- 枚举值：按 Swift/UTS 的枚举各自语法，必要时做值映射；
- 类继承与协议：遵循 Swift 的继承/协议语法，UTS 侧用接口/类型约束衔接；
- 系统版本判断：使用 `#available(iOS X, *)` 做兼容分支；
- 闭包：注意捕获列表，避免循环引用（见下节）；
- target-action：注意选择器签名与 UTS 的回调签名一致；
- 字典类型：Swift Dictionary 与 UTSJSONObject 的互操作时，进行必要的断言与转换；
- 异步方法：主线程与后台线程的队列切换清晰、回调时机与线程安全；
- try/catch：异常处理按 Swift 语法，UTS 侧暴露时用统一的错误接口与错误码体系；
- Swift 特有修饰符/指针操作：尽量封装在 Swift 侧，UTS 保持简洁签名。

## 避免闭包循环引用（示例）

```swift
class Foo {
    var handler: (() -> Void)?
    func work() {
        // 使用弱引用，避免 self 与闭包互相持有
        handler = { [weak self] in
            guard let self = self else { return }
            self.doSomething()
        }
    }
    func doSomething() { /* ... */ }
}
```

UTS 层调用时，确保回调最终在主线程更新 UI，避免线程争用。

## Swift Framework 版本兼容性（说明）

- Swift 语义/ABI 在不同 Xcode/工具链版本可能存在兼容性差异；
- 若三方 framework 构建工具链与打包环境不一致，会出现链接失败或符号不可见；
- 建议：统一 Xcode/Swift 版本；优先使用包含 Modules 且与打包环境一致的二进制；必要时使用源码或 SPM/CocoaPods 重新构建以匹配环境。

## 参考

- uts for iOS（原生环境配置、Framework 引入、真机运行与调试）：
  https://doc.dcloud.net.cn/uni-app-x/plugin/uts-for-ios.html
- UTSiOS API（方法清单与参数说明）：
  https://doc.dcloud.net.cn/uni-app-x/uts/utsios.html
