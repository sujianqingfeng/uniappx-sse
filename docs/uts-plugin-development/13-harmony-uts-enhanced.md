# HarmonyOS 平台UTS开发增强指南（精要）

> 同步说明（来源：官方文档）
> - 对齐页面：uts for HarmonyOS（https://doc.dcloud.net.cn/uni-app-x/plugin/uts-for-harmony.html）、UTSHarmony API（https://doc.dcloud.net.cn/uni-app-x/uts/utsharmony.html）
> - 同步时间：2025-09-13
> - 版本要点：HBuilderX 4.22+ 起支持 ArkTS；建议 DevEco Studio/SDK 使用最新版本

## 环境

- HBuilderX：4.22+ 起支持 ArkTS（推荐更高版本）
- DevEco Studio：建议使用最新版本
- HarmonyOS API：API 9+（推荐 10+）

## 目录结构（示例）

```
uni_modules/your-plugin/
└─utssdk/app-harmony/
  ├─index.uts
  ├─config.json
  ├─module.json5
  └─Echo.ets
```

## module.json5（示例）

```json5
{
  "module": {
    "name": "harmonyPlugin",
    "type": "har",
    "deviceTypes": ["phone", "tablet"],
    "deliveryWithInstall": true,
    "pages": "$profile:main_pages",
    "requestPermissions": [
      { "name": "ohos.permission.INTERNET" }
    ]
  }
}
```

### module.json5 字段对照（参考）

| 字段 | 含义 | 备注 |
| --- | --- | --- |
| `module.name` | 模块名称 | 与工程一致，唯一 |
| `module.type` | 模块类型 | 建议 `har`（库模块） |
| `module.deviceTypes[]` | 设备类型 | `phone`/`tablet` 等 |
| `module.deliveryWithInstall` | 安装交付 | 常见为 `true` |
| `module.pages` | 页面配置 | 可引用 profile 如 `$profile:main_pages` |
| `module.requestPermissions[]` | 权限声明 | 如 `ohos.permission.INTERNET` |
| 其他能力字段 | 组件/ability 配置 | 以官方字段为准 |

说明：以上为常见字段示意，完整字段列表与含义以官方文档/SDK 版本为准。

推荐组织要点：
- `type` 使用 `har` 进行模块化封装；
- `deviceTypes` 按需配置（phone/tablet/…）；
- `requestPermissions` 集中声明权限；
- 资源与页面路由遵循官方工程结构规范；

> 依赖管理：使用 ohpm 与 HAR。依赖与编译产物组织请以官方文档为准（不同 SDK 版本字段可能存在差异）。

## 常用示例

```ts
// #ifdef APP-HARMONY
import { UTSHarmony } from 'DCloudUTSFoundation'

// 资源与路径
const res = UTSHarmony.getResourcePath('logo.png')
const abs = UTSHarmony.convert2AbsFullPath('./data/config.json')

// 主题与窗口
const theme = UTSHarmony.getOsTheme()
UTSHarmony.onAppThemeChange(() => {/* ... */})

// 像素转换
const px = UTSHarmony.devicePX2px(100)
// #endif
```

## 资源使用与引用（示意）

- 将资源放于平台约定目录（如 `Resources/`）；
- 使用 `UTSHarmony.getResourcePath('logo.png')` 获取路径，或按 ArkUI 资源机制引用；
- 注意资源名与路径大小写、打包后的可访问性。

## 使用 ets 文件（组织示意）

```
utssdk/app-harmony/
  ├─index.uts
  └─Echo.ets  // ArkTS/ets 实现
```

在 `index.uts` 中：

```ts
// #ifdef APP-HARMONY
import { Echo } from './Echo.ets'
export function echo(text: string): string { return (new Echo()).say(text) }
// #endif
```

## 特殊文件拷贝（说明）

- 若需将额外文件随包分发，请遵循官方工程结构与拷贝机制；
- 注意 module.json5 与构建脚本的产物组织，避免运行期找不到资源。

## ArkTS 注意

- 遵循 ArkTS 语法限制（动态属性/装饰器等特性不可用时，使用显式类型与普通方法替代）。
- 依赖管理使用 ohpm/HAR；字段组织与示例以官方文档为准。

> 提示：复杂 UI 建议以 ArkUI 声明式能力实现，再在 UTS 层进行桥接与调用，避免在 UTS 中直接处理大段 UI 逻辑。

## ohpm 依赖（说明）

- 推荐使用 ohpm 管理第三方依赖，并以 HAR 模块形式组织；
- 注意 SDK 与 API Level 匹配，遵循官方给出的字段规范；
- 具体字段与样例请以官方文档为准（不同版本可能存在差异）。

### ohpm 依赖流程（示例）

1) 在 `app-harmony/config.json` 或相关配置中声明依赖（以官方字段为准）。
2) 在开发环境执行 ohpm 安装（DevEco Studio 集成或命令行）。
3) 确认 `module.json5` 与构建产物（HAR/资源）一致并可被打包合并。
4) 真机运行与云打包验证依赖可用性；如遇版本冲突，按官方建议进行版本对齐或替代。
