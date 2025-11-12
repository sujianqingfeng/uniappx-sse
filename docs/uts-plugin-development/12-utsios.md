# UTSiOS - iOS 平台内置对象（精要）

> 同步说明（来源：官方文档）
> - API 参考：UTSiOS（https://doc.dcloud.net.cn/uni-app-x/uts/utsios.html）
> - 平台专章：uts for iOS（https://doc.dcloud.net.cn/uni-app-x/plugin/uts-for-ios.html）
> - 同步时间：2025-09-13
> - 版本要点：建议使用最新 HBuilderX；需在 `app-ios/config.json` 添加缺失 Framework；UI 操作需主线程
> - 说明：本页为精要示例，完整参数/返回值以官方文档为准。

## 导入

```ts
import { UTSiOS } from 'DCloudUTSFoundation'
```

## 常用示例

### 目录（对齐官方主题）
- 导入与环境
- 设备/应用信息
- 资源与路径
- 当前窗口/控制器与颜色
- 系统/应用主题
- 内存管理与对象释放
- 参数标签与 UTS 调用
- 线程与系统版本（说明）

### 设备/应用信息（节选）

```ts
const info = {
  deviceId: UTSiOS.getDeviceId(),
  model: UTSiOS.getModel(),
  appId: UTSiOS.getAppId(),
  appName: UTSiOS.getAppName(),
  appVersion: UTSiOS.getAppVersion(),
  appVersionCode: UTSiOS.getAppVersionCode()
}
```

### 资源与路径

```ts
const res = UTSiOS.getResourcePath('logo.png')
const abs = UTSiOS.convert2AbsFullPath(res)
const dataPath = UTSiOS.getDataPath()
```

注记：
- 平台：iOS
- 线程：任意
- 最低 HBuilderX：以官方为准

### 当前窗口/控制器与颜色

```ts
// 需在主线程操作 UI（示意）
const vc = UTSiOS.getCurrentViewController()
const keyWindow = UTSiOS.getKeyWindow()
const blue = UTSiOS.colorWithString('systemBlue')
```

注记：UI 相关操作在主线程更安全，必要时使用 `DispatchQueue.main.async`。
最低 HX：以官方为准；部分 API 受系统版本限制，请结合 `#available` 编写兼容分支。

### 主题

```ts
const systemTheme = UTSiOS.getOsTheme() // 'light' | 'dark'
UTSiOS.onOsThemeChange((t: string) => {
  console.log('系统主题变化', t)
})

// 应用主题
const appTheme = UTSiOS.getAppTheme()
UTSiOS.setAppTheme('dark') // 'light' | 'dark'
```

注记：主题变更回调在主线程触发更安全；涉及 UI 更新请确保在主线程执行。

### 内存与对象释放

```ts
// 使用完原生对象后，按需释放
const view = UIView()
UTSiOS.destroyInstance(view)
```

注记：
- 线程：UI 相关操作需在主线程；可用 `DispatchQueue.main.async`。
- 版本：部分 API 受 iOS 系统版本限制，注意使用 `#available` 做兼容分支。

## 线程与系统版本（说明）

- 主线程：涉及 UIKit 的创建/更新必须在主线程；建议包装回调至主线程再操作 UI。
- 系统版本：请结合 `#available(iOS X, *)` 做分支以兼容旧系统功能差异（相关示例见 06/08 章节）。

## 参数标签与 UTS 调用（示例）

```swift
// Swift（带外部参数名 text）
@objc public class Echo: NSObject {
  @objc public func say(text: String) -> String { return "Echo: \(text)" }
}
```

```swift
// Swift（省略外部参数名）
@objc public class Echo: NSObject {
  @objc public func say(_ text: String) -> String { return "Echo: \(text)" }
}
```

UTS 调用建议与签名保持一致；若易混淆，使用 `_` 省略外部标签更稳妥。

## 官方目录映射（参考）

- 平台内置库：DCloudUTSFoundation（核心对象）
- 常用条目：
  - getCurrentViewController / getKeyWindow
  - colorWithString / getResourcePath / convert2AbsFullPath / getDataPath
  - 主题获取与变更回调、应用主题设置、destroyInstance

完整清单与参数说明以官方文档为准。

## 配置要点

- 需要系统 Framework 时，在 `app-ios/config.json` 的 `frameworks` 中添加，例如：`AVFoundation`、`CoreLocation`。
- 权限描述需在 Info.plist 中补齐，如 `NSCameraUsageDescription`、`NSMicrophoneUsageDescription`，确保审核合规。
- UI 相关操作请在主线程执行（可用 `DispatchQueue.main.async`）。

## 参考

- uts for iOS（原生环境配置、Framework 引入、真机运行与调试）：
  https://doc.dcloud.net.cn/uni-app-x/plugin/uts-for-ios.html
- UTSiOS API（方法清单与参数说明）：
  https://doc.dcloud.net.cn/uni-app-x/uts/utsios.html
