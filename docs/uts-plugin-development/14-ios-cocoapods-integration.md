# 在 UTS 插件中使用 CocoaPods（iOS）

> 同步说明（来源：官方文档）
> - 官方页：https://doc.dcloud.net.cn/uni-app-x/plugin/uts-ios-cocoapods.html
> - 同步时间：2025-09-13
> - 版本边界：建议使用较新的 HBuilderX；若使用源/仓库字段，请参考官方文档对应 HX 版本（如 `dependencies-pod-sources` 需 HX 4.61+）

本文与 DCloud 官方文档保持一致：在 UTS 插件中通过 `utssdk/app-ios/config.json` 声明 CocoaPods 依赖，HBuilderX 自动生成 Podfile 与安装依赖，无需手工修改 Podfile。

参考： https://doc.dcloud.net.cn/uni-app-x/plugin/uts-ios-cocoapods.html

## 环境要求

- HBuilderX 3.8.5+（支持在 UTS 插件中声明 CocoaPods 依赖）
- Xcode（建议最新版）
- macOS 上已安装 Ruby 与 CocoaPods

快速检查：
```bash
ruby --version
pod --version
pod setup
```

## 在 `config.json` 中声明依赖

位置：`uni_modules/<plugin>/utssdk/app-ios/config.json`

最小示例：
```json
{
  "deploymentTarget": "9.0", // 可选，默认 9.0
  "dependencies-pods": [
    { "name": "AFNetworking", "version": "4.0.1" },
    { "name": "WechatOpenSDK", "version": "2.0.2" }
  ]
}
```

进阶示例（多源与 Git 仓库）：
```json
{
  "deploymentTarget": "12.0",
  "dependencies-pod-sources": [
    "https://github.com/CocoaPods/Specs.git"
  ],
  "dependencies-pods": [
    { "name": "Alamofire", "version": "5.7.3" },
    { "name": "PrivatePod", "version": "1.0.0", "source": "https://github.com/test/test-specs.git" },
    { "name": "SomePodFromGit", "repo": { "git": "https://github.com/user/some-pod.git", "tag": "1.2.3" } }
  ]
}
```

字段说明：
- `deploymentTarget`：插件最低 iOS 版本（默认 9.0）。
- `dependencies-pod-sources`（可选，HX 4.61+）：配置 CocoaPods 源。
- `dependencies-pods`：依赖库列表。
  - `name`：必填。
  - `version`：可选；若使用 `repo`（Git 仓库）可不填版本。
  - `source`（可选，HX 4.61+）：为单个库指定 specs 源。
  - `repo`（可选，HX 3.8.10+）：以 Git 方式指定 Pod 源（支持 `git`、`tag`、`branch`）。

注意：不要自行维护 Podfile 或添加自定义 `podfile-configuration` 字段，Podfile 由 HBuilderX 负责生成和管理。

## 使用步骤

1) 在 `config.json` 填写依赖。
2) HBuilderX 选择“运行到 iOS 设备/模拟器”，首次会自动执行 `pod install` 并构建。
3) 若网络较慢，可先在终端执行 `pod setup` 或配置国内镜像源后再运行。

## 常见问题

- 安装超时/网络错误：使用国内镜像（如清华 TUNA），或配置代理；必要时执行 `pod repo update`。
- API 变更：三方库版本升级可能导致 API 不兼容，需要同步调整 Swift/Objective‑C 桥接代码。
- 重复集成：切换到 CocoaPods 后，请移除手工嵌入的 `.framework` 与 `.bundle`，避免重复符号或资源冲突。

### .a 静态库相关说明（要点）

- 对仅提供 `.a` 的库，需同时具备对应的头文件与依赖；
- 若不包含 Modules，需在桥接头/搜索路径进行额外配置（遵循官方 3.4.2/3.4.3 说明）；
- 建议优先选择包含 Modules 的 framework 或通过 CocoaPods 获取规范产物，降低集成成本。

### 不包含 Modules 的 framework（进一步说明）

- 可能需要在 `HEADER_SEARCH_PATHS`/`LIBRARY_SEARCH_PATHS` 补充搜索路径；
- 确保 Swift/ObjC 可见性正确（`public`/`@objc` 等）；
- 若仍报符号缺失，核对 framework 架构集（arm64/simulator）与打包环境一致性。

## 进阶：私有源与 subspec

### 私有 Specs 仓库（HX 版本：`dependencies-pod-sources` 需 HX 4.61+）

```json
{
  "dependencies-pod-sources": [
    "https://github.com/CocoaPods/Specs.git",
    "https://github.com/your-org/your-private-specs.git"
  ],
  "dependencies-pods": [
    { "name": "YourPrivatePod", "version": "~> 1.4.0" }
  ]
}
```

注意：确保构建环境有私有仓库访问权限（token/SSH key）。

### 使用 subspec（子规格）

```json
{
  "dependencies-pods": [
    { "name": "Alamofire/NetworkReachabilityManager", "version": "~> 5.7" }
  ]
}
```

### Git 直接依赖（无需指定版本）

```json
{
  "dependencies-pods": [
    { "name": "SomePod", "repo": { "git": "https://github.com/user/SomePod.git", "branch": "main" } }
  ]
}
```

提示：Git 依赖在不可用时可能导致构建失败，建议固定 tag 并在发布前验证网络与缓存。

## 示例：Huawei ScanKit（通过 CocoaPods）

`utssdk/app-ios/config.json`：
```json
{
  "deploymentTarget": "12.0",
  "dependencies-pods": [
    { "name": "ScanKitFrameWork", "version": "~> 1.0.1.300" }
  ]
}
```

如库版本带来 API 变化（构造函数签名、属性重命名等），请同步更新原生桥接代码。

## 参考
- 官方文档： https://doc.dcloud.net.cn/uni-app-x/plugin/uts-ios-cocoapods.html
- CocoaPods： https://cocoapods.org/
