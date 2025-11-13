# UTS插件开发指南

> 同步说明（来源：官方文档）
> - 已对照官方页面更新：UTS 插件介绍（https://doc.dcloud.net.cn/uni-app-x/plugin/uts-plugin.html）
> - 同步时间：2025-09-13
> - 版本要点：
>   - HBuilderX 3.6+ 支持在 uni-app 使用 UTS 插件，3.9+ 支持在 uni-app x 使用 UTS 插件
>   - Android 最低 API Level：21（Android 5.0）
>   - HarmonyOS ArkTS 支持自 HBuilderX 4.22+
>   - HBuilderX 4.81 起统一升级 Kotlin 2.2.0（详见 uts for Android 升级注意事项）

## 概述

UTS插件是uni-app生态中用于扩展原生功能的重要机制。通过UTS插件，开发者可以创建跨平台的原生功能模块，为uni-app应用提供丰富的原生能力支持。

### UTS插件的优势

1. **跨平台统一**: 一套代码编译到多个平台
2. **原生性能**: 编译为原生代码，性能接近原生应用
3. **类型安全**: 强类型系统提供编译时错误检查
4. **易于调试**: 支持断点调试和错误追踪
5. **生态兼容**: 与uni-app生态无缝集成

## 插件项目结构

### 标准插件结构

```
uni_modules/your-plugin-name/
├── package.json                     # 插件描述文件
└── utssdk/                          # UTS SDK实现目录
    ├── interface.uts                # 插件接口定义
    ├── unierror.uts                 # 错误码定义
    ├── index.uts                    # 跨平台通用实现（可选）
    ├── app-android/                 # Android平台实现
    │   ├── index.uts                # Android主实现文件
    │   ├── config.json              # Android平台配置
    │   ├── AndroidManifest.xml      # Android清单文件（可选）
    │   ├── assets/                  # Android资源文件（可选）
    │   ├── libs/                    # Android第三方库（可选）
    │   └── res/                     # Android资源文件（可选）
    ├── app-ios/                     # iOS平台实现
    │   ├── index.uts                # iOS主实现文件
    │   ├── config.json              # iOS平台配置
    │   ├── Info.plist               # iOS info配置（可选）
    │   ├── Frameworks/              # iOS框架文件（可选）
    │   └── Resources/               # iOS资源文件（可选）
    └── app-harmony/                 # HarmonyOS平台实现
        └── index.uts                # HarmonyOS主实现
```

### package.json配置

```json
{
  "id": "your-plugin-name",
  "displayName": "插件显示名称",
  "version": "1.0.0",
  "description": "插件功能描述",
  "keywords": [
    "uni-app",
    "uts",
    "plugin"
  ],
  "repository": {
    "type": "git",
    "url": "https://github.com/username/your-plugin-name.git"
  },
  "engines": {
    "HBuilderX": "^4.25.0"
  },
  "dcloudext": {
    "type": "uts-plugin",
    "sale": {
      "regular": {
        "price": "0.00"
      },
      "sourcecode": {
        "price": "0.00"
      }
    },
    "contact": {
      "qq": "QQ群号码"
    },
    "declaration": {
      "ads": "插件是否包含广告",
      "data": "插件采集的数据类型", 
      "permissions": "插件需要的权限列表"
    }
  },
  "uni_modules": {
    "dependencies": [],
    "encrypt": [],
    "platforms": {
      "cloud": {
        "tcb": "y",
        "aliyun": "y",
        "alipay": "n"
      },
      "client": {
        "vue": {
          "vue2": "n",
          "vue3": "y"
        },
        "app": {
          "app-android": "y",
          "app-ios": "y",
          "app-harmony": "u"
        },
        "mp-weixin": "y",
        "mp-alipay": "n",
        "mp-baidu": "n",
        "mp-toutiao": "n",
        "mp-lark": "n",
        "mp-qq": "n",
        "mp-kuaishou": "n",
        "mp-jd": "n",
        "mp-360": "n",
        "quickapp-webview": "n",
        "quickapp-webview-union": "n",
        "quickapp-webview-huawei": "n"
      }
    }
  }
}
```

## 接口设计

### interface.uts - 接口定义

```typescript
/**
 * 插件接口定义文件 - 基于官方规范
 * 定义插件对外暴露的API接口类型
 */

// 基础配置选项类型
export type YourPluginOptions = {
    /**
     * API密钥
     */
    apiKey?: string
    /**
     * 是否启用调试模式
     */
    debug?: boolean
    /**
     * 超时时间（毫秒）
     */
    timeout?: number
}

// API结果类型
export type YourPluginResult = {
    /**
     * 返回的数据
     */
    data: any
    /**
     * 状态信息
     */
    status: string
}

// 错误码类型（联合类型）
export type YourPluginErrorCode = 1001001 | 1001002 | 1001003

// 错误接口（继承IUniError）
export interface YourPluginFail extends IUniError {
    errCode: YourPluginErrorCode
}

// 异步方法选项类型
export type YourPluginAsyncOptions = {
    /**
     * 参数
     */
    param: string
    /**
     * 成功回调
     */
    success?: (res: YourPluginResult) => void
    /**
     * 失败回调
     */
    fail?: (res: YourPluginFail) => void
    /**
     * 完成回调
     */
    complete?: () => void
}

// API类型定义
export type YourPluginInit = (options: YourPluginOptions) => void
export type YourPluginSync = (param: string) => YourPluginResult
export type YourPluginAsync = (options: YourPluginAsyncOptions) => void

// 导出所有类型
export interface YourPluginInterface {
    init: YourPluginInit
    syncMethod: YourPluginSync
    asyncMethod: YourPluginAsync
}
```

提示：`engines.HBuilderX` 的版本范围请结合项目目标平台按需设置。若需兼容 uni-app 与 uni-app x，请参考上方“版本要点”。

### 创建 UTS 插件（HBuilderX）

建议参考官方页面的“创建 UTS 插件”流程（支持在 HBuilderX 中直接新建 UTS 插件工程/uni_modules 插件）：

- 新建插件 → 选择 UTS 插件类型（或在 Hello UTS 示例中复制现有插件示例做改造）
- 填写插件标识（与 `package.json.id` 对应）、显示名、版本等基础信息
- 系统生成 `uni_modules/<pluginId>/utssdk/` 目录与各平台子目录（`app-android|app-ios|app-harmony|web|mp-*`）
- 打开 `interface.uts`、`unierror.uts`、各平台 `index.uts` 完成接口与实现
- 运行到真机/自定义基座进行联调
- 详细步骤与界面以官方页面为准：https://doc.dcloud.net.cn/uni-app-x/plugin/uts-plugin.html

相关：调试与运行（真机、自定义基座、断点）见《调试与运行》：./16-debug-and-run.md

#### 创建向导字段说明（要点）

- 插件标识（id）：对应 `package.json.id`，建议使用 `uni-` 前缀与 kebab-case；
- 显示名称（displayName）：市场展示名称，应简洁明了；
- 版本（version）：遵循语义化版本（SemVer），破坏性变更需大版本；
- 描述/关键字（description/keywords）：便于检索与分类；
- 兼容引擎（engines.HBuilderX）：按最低兼容版本填写；
- dcloudext：
  - type：`uts-plugin`；
  - sale：付费/源码授权等设置；
  - contact：作者联系方式；
  - declaration：广告、数据采集、权限等合规声明；
- uni_modules.platforms：勾选支持的平台矩阵（含 app-android/app-ios/app-harmony/mp/web 等）。

建议：
- 完成创建后立即跑通“最小实现 + 真机运行”，再逐步补功能；
- 在 `utssdk/` 下按平台拆分实现，公共逻辑放 `index.uts` 以减少重复。

#### 创建向导字段表（参考）

| 向导字段 | package.json 映射 | 必填 | 说明 | 备注 |
| --- | --- | --- | --- | --- |
| 插件标识 | `id` | 是 | 插件唯一标识，建议 kebab-case | 市场上架后不建议随意变更 |
| 显示名称 | `displayName` | 是 | 市场展示名称 | 简洁、可检索 |
| 版本 | `version` | 是 | 语义化版本（SemVer） | 破坏性变更升主版本 |
| 描述 | `description` | 建议 | 简要说明功能场景 | 配合截图/GIF 更直观 |
| 关键词 | `keywords[]` | 建议 | 市场检索标签 | 用词规范、贴合功能 |
| 引擎版本 | `engines.HBuilderX` | 建议 | 最低 HX 版本 | 便于用户选型 |
| 平台矩阵 | `uni_modules.platforms` | 建议 | 勾选支持平台 | 与实现一致 |
| 扩展类型 | `dcloudext.type` | 是 | 固定为 `uts-plugin` | —— |
| 售卖策略 | `dcloudext.sale` | 选填 | 常规/源码授权价 | 与条款一致 |
| 联系方式 | `dcloudext.contact` | 选填 | QQ/邮箱等 | 便于支持 |
| 合规声明 | `dcloudext.declaration` | 建议 | 广告/数据/权限说明 | 审核关注点 |

占位：HBuilderX 创建向导截图 Step 1/2/3（待补图）

#### 创建向导图解（占位）

![创建向导 Step 1](./images/wizard-step-1.png)

![创建向导 Step 2](./images/wizard-step-2.png)

![创建向导 Step 3](./images/wizard-step-3.png)

### 依赖整合（跨生态）

在 `uni_modules/<pluginId>/` 下，UTS 插件可统一封装多生态依赖，供插件使用者一次集成：

- web 与小程序生态：`utssdk/web/`、`utssdk/mp-*`（可放置 JS 实现与 NPM 依赖）
- Android 生态：`utssdk/app-android/`（`config.json` → dependencies/plugins/repositories，Gradle 依赖）
- iOS 生态：`utssdk/app-ios/`（`config.json` → frameworks/libraries/dependencies，CocoaPods 依赖）
- HarmonyOS 生态：`utssdk/app-harmony/`（`config.json` → ohpm 依赖等）

这样，插件作者可以在一个 `uni_modules` 插件中同时管理 npm、Gradle、CocoaPods、ohpm 等多种依赖输出。

参考：uni_modules 规范 https://uniapp.dcloud.net.cn/plugin/uni_modules.html

### 插件使用（uni-app 与 uni-app x）

以下示例仅作调用参考，具体以实际插件导出方法为准：

- 在 uni-app（JS/TS）项目中（示例）

```ts
// 调用插件对外导出的方法（根据插件导出项调整）
// 推荐使用路径别名导入：
import { init, asyncMethod } from '@/uni_modules/your-plugin/utssdk/index.uts'

init({ debug: true })
asyncMethod({
  param: 'hello',
  success: (res) => console.log('success', res),
  fail: (err) => console.error('fail', err),
  complete: () => console.log('complete')
})
```

- 在 uni-app x（UTS）项目中（示例）

```ts
// 在 uvue/uts 代码中直接导入并调用
import { init, asyncMethod } from '@/uni_modules/your-plugin/utssdk/index.uts'

init({ debug: true })
asyncMethod({
  param: 'world',
  success: (res) => console.log('success', res),
  fail: (err) => console.error('fail', err)
})
```

说明：
- 实际导出函数名、参数与返回值，请以插件 `interface.uts`/`index.uts` 为准。
- 旧项目/特定编译器模式可能需要条件编译或路径调整，建议参考官方示例 Hello UTS。

### 插件间依赖（uni_modules.dependencies）

当你的插件依赖其他 uni_modules 插件时，可在 `package.json.uni_modules.dependencies` 中声明：

```json
{
  "uni_modules": {
    "dependencies": [
      "uni-some-dep@^1.2.0",
      "uni-another-dep@~2.0.5"
    ]
  }
}
```

建议：
- 锁定大版本（^/~）以平衡升级与兼容；
- 在文档中注明依赖插件的用途与最低版本要求；
- 避免循环依赖；尽量将通用能力抽为独立依赖插件。

（iOS）被其他插件引用时的规范：
- 命名清晰、避免与宿主/其他插件符号冲突；
- 需要的系统 Framework/entitlement 在 `app-ios/config.json` 与文档中明确；
- 如需在 Swift 层暴露类型，保证可见性与模块化结构清晰（详见 08 章）。

### 环境数据交互速览

- 数组传参：在 uni-app（JS）与 uni-app x（UTS）间传递数组时，判定数组使用 `Array.isArray`；
- JSON 属性访问：使用安全断言与可选链，UTSJSONObject 按需断言/`instanceof`；
- 回调类型：单次回调使用 `success/fail/complete`；多次推送使用 `UTSCallback` 模式（见下文与 15 章）；
- Android 强类型：平台 API 要求精确数字类型时（如 Int），请按平台类型传参（详见 07/10 章）。

更多示例与细节：见《10 数据类型》《15 常见问题与报错》。

### 打包与发布（概览）

- `dcloudext`：补充 `type/sale/contact/declaration` 等字段；如涉及隐私采集，声明权限/用途；
- 真机运行/自定义基座/断点调试：见《16 调试与运行》；
- 上架与审核：准备功能说明、权限用途文案、隐私与合规材料；
- 版本管理：遵循语义化版本；破坏性变更需大版本升级并在文档中显著提示。

### 平台专项规范（链接）

- UTS 与 uni-app 环境数据交互说明（官方）：
  https://doc.dcloud.net.cn/uni-app-x/plugin/uts-plugin.html#%E4%BA%A4%E4%BA%92
- iOS 平台下 uts 插件被别的插件引用时开发规范（官方）：
  https://doc.dcloud.net.cn/uni-app-x/plugin/uts-plugin.html#ios-uts-dep

### 持续回调与 UTSCallback（重要）

当需要“多次回调”时，请使用持续回调的写法，而不是只触发一次的 success/fail：

```ts
// interface.uts
export type WatchOptions = {
  onData: (data: any) => void // 可持续触发
  onError?: (err: IUniError) => void
  stop?: () => void
}

// app-android/index.uts（示例）
export function watch(options: WatchOptions): void {
  const main = UTSAndroid.getDispatcher('main')
  const io = UTSAndroid.getDispatcher('io')
  let stopped = false

  options.stop = () => { stopped = true }

  io.async(() => {
    let counter = 0
    while (!stopped && counter < 5) {
      Thread.sleep(500)
      const payload = { index: counter++, ts: Date.now() }
      main.async(() => { options.onData(payload) })
    }
  })
}
```

说明：
- 若回调需要在 UI 线程执行，请使用主线程调度（如 `UTSAndroid.getDispatcher('main')` 或 iOS 的 `DispatchQueue.main.async`）。
- 对于需“取消订阅”的场景，暴露 `stop` 或返回带有 `stop()` 的句柄对象更易用。

### 在 uni-app 上的导出限制（兼容提示）

根据官方文档，UTS 插件在 uni-app（非 x）环境的导出存在限制：
- 不支持导出重载方法；
- 函数作为参数可能存在限制（尤其匿名函数/闭包）；
- 泛型与某些 Kotlin/Swift 特性在 JS 调用通道无法完全表达；
- 建议提供“面向 JS 的薄适配层”，保证在 uni-app 与 uni-app x 两端均可稳定调用。

### unierror.uts - 错误码定义

```typescript
/**
 * 错误处理定义 - 基于官方uni规范
 */

import { YourPluginErrorCode, YourPluginFail } from "./interface.uts"

// 错误主题（插件名称）
export const UniErrorSubject = 'uni-plugin-yourplugin'

// 错误码与错误信息映射
export const YourPluginUniErrors: Map<YourPluginErrorCode, string> = new Map([
    [1001001, '参数错误'],
    [1001002, '网络连接失败'],
    [1001003, '服务不可用']
])

// 错误实现类
export class YourPluginFailImpl extends UniError implements YourPluginFail {
    override errCode: YourPluginErrorCode
    
    constructor(errCode: YourPluginErrorCode) {
        super()
        this.errSubject = UniErrorSubject
        this.errCode = errCode
        this.errMsg = YourPluginUniErrors.get(errCode) ?? ""
    }
}

// 错误工厂函数
export function createYourPluginError(errCode: YourPluginErrorCode): YourPluginFail {
    return new YourPluginFailImpl(errCode)
}
```

## 平台实现

### Android平台实现

```typescript
// utssdk/app-android/index.uts
import { YourPluginOptions, YourPluginResult, YourPluginAsyncOptions, YourPluginInit, YourPluginSync, YourPluginAsync } from '../interface.uts'
import { createYourPluginError } from '../unierror.uts'

// Android原生API导入
import Context from "android.content.Context"
import Activity from "android.app.Activity"
import Log from "android.util.Log"

/**
 * 插件Android实现类 - 基于UTSAndroid官方规范
 */
class YourPluginImpl {
    
    private context: Context
    private activity: Activity | null = null
    private isInitialized: boolean = false
    private options: YourPluginOptions | null = null
    
    constructor() {
        this.context = UTSAndroid.getAppContext() as Context
        this.activity = UTSAndroid.getUniActivity()
    }
    
    /**
     * 初始化插件
     */
    init(options: YourPluginOptions): void {
        try {
            this.options = options
            
            // 执行平台特定的初始化逻辑
            this.performAndroidInit(options)
            
            this.isInitialized = true
            
            if (options.debug) {
                Log.d("YourPlugin", "Plugin initialized successfully")
            }
            
        } catch (error) {
            Log.e("YourPlugin", "Failed to initialize plugin: " + error.message)
            throw createYourPluginError(1001001)
        }
    }
    
    /**
     * 同步方法实现
     */
    syncMethod(param: string): YourPluginResult {
        if (!this.isInitialized) {
            throw createYourPluginError(1001001)
        }
        
        try {
            // 执行同步业务逻辑
            const result = {
                data: {
                    processed: true,
                    input: param,
                    timestamp: Date.now(),
                    platform: "android",
                    deviceInfo: this.getDeviceInfo()
                },
                status: "success"
            }
            
            return result
            
        } catch (error) {
            Log.e("YourPlugin", "Sync method error: " + error.message)
            throw createYourPluginError(1001002)
        }
    }
    
    /**
     * 异步方法实现 - 使用UTSAndroid线程调度
     */
    asyncMethod(options: YourPluginAsyncOptions): void {
        if (!this.isInitialized) {
            const error = createYourPluginError(1001001)
            if (options.fail) {
                options.fail(error)
            }
            if (options.complete) {
                options.complete()
            }
            return
        }
        
        // 使用UTSAndroid线程调度器在IO线程执行异步操作
        UTSAndroid.getDispatcher("io").async(function(_) {
            try {
                // 模拟异步处理
                Thread.sleep(1000)
                
                const result: YourPluginResult = {
                    data: {
                        processed: true,
                        input: options.param,
                        timestamp: Date.now(),
                        platform: "android",
                        threadName: Thread.currentThread().getName()
                    },
                    status: "success"
                }
                
                // 切换到主线程调用回调
                UTSAndroid.getDispatcher("main").async(function(_) {
                    if (options.success) {
                        options.success(result)
                    }
                    if (options.complete) {
                        options.complete()
                    }
                }, null)
                
            } catch (error) {
                Log.e("YourPlugin", "Async method error: " + error.message)
                
                // 切换到主线程调用错误回调
                UTSAndroid.getDispatcher("main").async(function(_) {
                    if (options.fail) {
                        options.fail(createYourPluginError(1001002))
                    }
                    if (options.complete) {
                        options.complete()
                    }
                }, null)
            }
        }, null)
    }
    
    // === 私有方法 ===
    
    /**
     * Android特定初始化
     */
    private performAndroidInit(options: YourPluginOptions): void {
        // 检查必要权限
        this.checkRequiredPermissions()
        
        // 获取应用信息
        const appInfo = this.getAppInfo()
        Log.i("YourPlugin", "App Info: " + JSON.stringify(appInfo))
        
        // 检查网络状态
        const networkStatus = this.getNetworkStatus()
        Log.i("YourPlugin", "Network Status: " + networkStatus)
    }
    
    /**
     * 检查必要权限
     */
    private checkRequiredPermissions(): void {
        const permissions = [
            "android.permission.INTERNET",
            "android.permission.ACCESS_NETWORK_STATE"
        ]
        
        permissions.forEach(permission => {
            const granted = this.context.checkSelfPermission(permission) == 
                android.content.pm.PackageManager.PERMISSION_GRANTED
            
            if (!granted) {
                Log.w("YourPlugin", `Permission not granted: ${permission}`)
            }
        })
    }
    
    /**
     * 获取应用信息
     */
    private getAppInfo(): any {
        try {
            const packageManager = this.context.getPackageManager()
            const packageInfo = packageManager.getPackageInfo(this.context.getPackageName(), 0)
            
            return {
                packageName: this.context.getPackageName(),
                versionName: packageInfo.versionName,
                versionCode: packageInfo.versionCode,
                targetSdkVersion: packageInfo.applicationInfo?.targetSdkVersion || 0
            }
        } catch (error) {
            Log.e("YourPlugin", "Failed to get app info: " + error.message)
            return null
        }
    }
    
    /**
     * 获取设备信息
     */
    private getDeviceInfo(): any {
        return {
            brand: android.os.Build.BRAND,
            model: android.os.Build.MODEL,
            device: android.os.Build.DEVICE,
            manufacturer: android.os.Build.MANUFACTURER,
            version: {
                release: android.os.Build.VERSION.RELEASE,
                sdkInt: android.os.Build.VERSION.SDK_INT
            }
        }
    }
    
    /**
     * 获取网络状态
     */
    private getNetworkStatus(): string {
        try {
            const connectivityManager = this.context.getSystemService(Context.CONNECTIVITY_SERVICE) 
                as android.net.ConnectivityManager
            const networkInfo = connectivityManager.getActiveNetworkInfo()
            
            if (networkInfo == null || !networkInfo.isConnected()) {
                return 'disconnected'
            }
            
            when (networkInfo.getType()) {
                android.net.ConnectivityManager.TYPE_WIFI -> return 'wifi'
                android.net.ConnectivityManager.TYPE_MOBILE -> return 'mobile'
                else -> return 'other'
            }
        } catch (error) {
            Log.e("YourPlugin", "Failed to get network status: " + error.message)
            return 'unknown'
        }
    }
    
    /**
     * 请求系统权限
     */
    requestPermissions(
        permissions: Array<string>,
        callback: (allGranted: boolean, grantedList: Array<string>) => void
    ): void {
        if (!this.activity) {
            callback(false, [])
            return
        }
        
        UTSAndroid.requestSystemPermission(
            this.activity!,
            permissions,
            function(allGranted: boolean, grantedList: Array<string>) {
                callback(allGranted, grantedList)
            },
            function(doNotAskAgain: boolean, grantedList: Array<string>) {
                callback(false, grantedList)
            }
        )
    }
}

// 导出插件实例
const yourPluginInstance = new YourPluginImpl()

// 导出所有接口方法 - 符合UTS规范
export const init : YourPluginInit = (options: YourPluginOptions): void => {
    return yourPluginInstance.init(options)
}

export const syncMethod : YourPluginSync = (param: string): YourPluginResult => {
    return yourPluginInstance.syncMethod(param)
}

export const asyncMethod : YourPluginAsync = (options: YourPluginAsyncOptions): void => {
    return yourPluginInstance.asyncMethod(options)
}

// 默认导出
export default {
    init,
    syncMethod,
    asyncMethod
}
```

### iOS平台实现

```typescript
// utssdk/app-ios/index.uts
import { YourPluginType, PluginOptions, PluginResult, PluginRequestOptions, PluginPromise } from '../interface.uts'
import { PluginError, PluginErrorCode, PluginErrorFactory, PluginResultWrapper } from '../unierror.uts'

// iOS原生API导入
import Foundation from "Foundation"
import UIKit from "UIKit"
import Dispatch from "Dispatch"

// 插件iOS实现类
class YourPluginImpl implements YourPluginType {
    
    private isInitialized: boolean = false
    private options: PluginOptions | null = null
    private eventListeners: Map<string, Array<(data: any) => void>> = new Map()
    
    constructor() {
        // iOS特定初始化
    }
    
    /**
     * 初始化插件
     */
    init(options: PluginOptions): void {
        do {
            // 执行iOS特定的初始化逻辑
            try {
                self.performIOSInit(options)
                self.options = options
                self.isInitialized = true
                
                NSLog("YourPlugin: Plugin initialized successfully")
            } catch {
                NSLog("YourPlugin: Failed to initialize plugin - \(error)")
                throw PluginErrorFactory.create(PluginErrorCode.INITIALIZATION_FAILED, "\(error)")
            }
        }
    }
    
    /**
     * 同步方法实现
     */
    syncMethod(params: any): PluginResult {
        if (!self.isInitialized) {
            return PluginResultWrapper.failureWithCode(
                PluginErrorCode.INITIALIZATION_FAILED,
                "插件未初始化"
            )
        }
        
        do {
            try {
                // 执行同步业务逻辑
                let result = self.performSyncOperation(params)
                return PluginResultWrapper.success(result, "操作成功")
                
            } catch {
                NSLog("YourPlugin: Sync method error - \(error)")
                return PluginResultWrapper.failure(
                    PluginErrorFactory.create(PluginErrorCode.UNKNOWN_ERROR, "\(error)")
                )
            }
        }
    }
    
    /**
     * 异步方法实现（回调风格）
     */
    asyncMethod(options: PluginRequestOptions): void {
        if (!self.isInitialized) {
            let error = PluginErrorFactory.create(
                PluginErrorCode.INITIALIZATION_FAILED,
                "插件未初始化"
            )
            
            if (options.fail != null) {
                options.fail!(error)
            }
            if (options.complete != null) {
                options.complete!()
            }
            return
        }
        
        // 在后台队列执行异步操作
        DispatchQueue.global(qos: .userInitiated).async {
            do {
                try {
                    let result = self.performAsyncOperation(options.params)
                    
                    // 切换到主队列调用回调
                    DispatchQueue.main.async {
                        if (options.success != null) {
                            options.success!(PluginResultWrapper.success(result))
                        }
                        if (options.complete != null) {
                            options.complete!()
                        }
                    }
                    
                } catch {
                    NSLog("YourPlugin: Async method error - \(error)")
                    
                    DispatchQueue.main.async {
                        if (options.fail != null) {
                            options.fail!(PluginErrorFactory.create(PluginErrorCode.UNKNOWN_ERROR, "\(error)"))
                        }
                        if (options.complete != null) {
                            options.complete!()
                        }
                    }
                }
            }
        }
    }
    
    /**
     * 异步方法实现（Promise风格）
     */
    asyncMethodPromise(params: any): PluginPromise {
        return new Promise((resolve, reject) => {
            self.asyncMethod({
                params: params,
                success: (result) => resolve(result),
                fail: (error) => reject(error)
            })
        })
    }
    
    /**
     * 事件监听
     */
    on(event: string, callback: (data: any) => void): void {
        var listeners = self.eventListeners.get(event)
        if (listeners == null) {
            listeners = []
            self.eventListeners.set(event, listeners!)
        }
        listeners!.push(callback)
        
        NSLog("YourPlugin: Added listener for event - \(event)")
    }
    
    /**
     * 移除事件监听
     */
    off(event: string, callback?: (data: any) => void): void {
        let listeners = self.eventListeners.get(event)
        if (listeners != null) {
            if (callback != null) {
                if let index = listeners!.firstIndex(where: { $0 === callback }) {
                    listeners!.remove(at: index)
                }
            } else {
                listeners!.removeAll()
            }
        }
        
        NSLog("YourPlugin: Removed listener for event - \(event)")
    }
    
    /**
     * 获取插件信息
     */
    getPluginInfo(): { name: string, version: string, platform: string } {
        return {
            name: "YourPlugin",
            version: "1.0.0",
            platform: "iOS"
        }
    }
    
    // === 私有方法 ===
    
    private performIOSInit(options: PluginOptions): void {
        // iOS特定的初始化逻辑
        if (options.debug == true) {
            NSLog("YourPlugin: Debug mode enabled")
        }
        
        // 检查iOS版本兼容性
        self.checkIOSCompatibility()
        
        // 初始化iOS资源
        self.initializeIOSResources()
    }
    
    private performSyncOperation(params: any): any {
        // 同步业务逻辑实现
        return [
            "processed": true,
            "params": params,
            "timestamp": Date().timeIntervalSince1970
        ]
    }
    
    private performAsyncOperation(params: any): any {
        // 模拟异步操作
        Thread.sleep(forTimeInterval: 1.0)
        
        return [
            "processed": true,
            "params": params,
            "timestamp": Date().timeIntervalSince1970,
            "queue": DispatchQueue.current?.label ?? "unknown"
        ]
    }
    
    private checkIOSCompatibility(): void {
        let systemVersion = UIDevice.current.systemVersion
        NSLog("YourPlugin: Running on iOS \(systemVersion)")
        
        // 检查最低版本要求
        if #available(iOS 12.0, *) {
            // iOS 12.0+ 可用的功能
        } else {
            NSLog("YourPlugin: Warning - iOS version may not be fully supported")
        }
    }
    
    private initializeIOSResources(): void {
        // 初始化iOS特定资源
        NSLog("YourPlugin: iOS resources initialized")
    }
    
    /**
     * 触发事件
     */
    private emitEvent(event: string, data: any): void {
        let listeners = self.eventListeners.get(event)
        if (listeners != null && listeners!.count > 0) {
            DispatchQueue.main.async {
                listeners!.forEach { callback in
                    do {
                        try {
                            callback(data)
                        } catch {
                            NSLog("YourPlugin: Error in event callback - \(error)")
                        }
                    }
                }
            }
        }
    }
}

// 导出插件实例
const yourPluginInstance = new YourPluginImpl()

// 导出所有接口方法
export const init = (options: PluginOptions): void => yourPluginInstance.init(options)
export const syncMethod = (params: any): PluginResult => yourPluginInstance.syncMethod(params)
export const asyncMethod = (options: PluginRequestOptions): void => yourPluginInstance.asyncMethod(options)
export const asyncMethodPromise = (params: any): PluginPromise => yourPluginInstance.asyncMethodPromise(params)
export const on = (event: string, callback: (data: any) => void): void => yourPluginInstance.on(event, callback)
export const off = (event: string, callback?: (data: any) => void): void => yourPluginInstance.off(event, callback)
export const getPluginInfo = (): { name: string, version: string, platform: string } => yourPluginInstance.getPluginInfo()

// 兼容默认导出
export default yourPluginInstance
```

### Web平台实现

```typescript
// utssdk/web/index.uts
import { YourPluginType, PluginOptions, PluginResult, PluginRequestOptions, PluginPromise } from '../interface.uts'
import { PluginError, PluginErrorCode, PluginErrorFactory, PluginResultWrapper } from '../unierror.uts'

// 插件Web实现类
class YourPluginImpl implements YourPluginType {
    
    private isInitialized: boolean = false
    private options: PluginOptions | null = null
    private eventListeners: Map<string, Array<(data: any) => void>> = new Map()
    
    constructor() {
        // Web特定初始化
        if (typeof window !== 'undefined') {
            console.log('YourPlugin: Web environment detected')
        }
    }
    
    /**
     * 初始化插件
     */
    init(options: PluginOptions): void {
        try {
            // 执行Web特定的初始化逻辑
            this.performWebInit(options)
            this.options = options
            this.isInitialized = true
            
            console.log('YourPlugin: Plugin initialized successfully')
            
        } catch (error) {
            console.error('YourPlugin: Failed to initialize plugin', error)
            throw PluginErrorFactory.create(PluginErrorCode.INITIALIZATION_FAILED, (error as Error).message)
        }
    }
    
    /**
     * 同步方法实现
     */
    syncMethod(params: any): PluginResult {
        if (!this.isInitialized) {
            return PluginResultWrapper.failureWithCode(
                PluginErrorCode.INITIALIZATION_FAILED,
                "插件未初始化"
            )
        }
        
        try {
            // 执行同步业务逻辑
            const result = this.performSyncOperation(params)
            return PluginResultWrapper.success(result, "操作成功")
            
        } catch (error) {
            console.error('YourPlugin: Sync method error', error)
            return PluginResultWrapper.failure(
                PluginErrorFactory.create(PluginErrorCode.UNKNOWN_ERROR, (error as Error).message)
            )
        }
    }
    
    /**
     * 异步方法实现（回调风格）
     */
    asyncMethod(options: PluginRequestOptions): void {
        if (!this.isInitialized) {
            const error = PluginErrorFactory.create(
                PluginErrorCode.INITIALIZATION_FAILED,
                "插件未初始化"
            )
            
            if (options.fail) {
                options.fail(error)
            }
            if (options.complete) {
                options.complete()
            }
            return
        }
        
        // 使用Web的异步机制
        setTimeout(async () => {
            try {
                const result = await this.performAsyncOperation(options.params)
                
                if (options.success) {
                    options.success(PluginResultWrapper.success(result))
                }
                if (options.complete) {
                    options.complete()
                }
                
            } catch (error) {
                console.error('YourPlugin: Async method error', error)
                
                if (options.fail) {
                    options.fail(PluginErrorFactory.create(PluginErrorCode.UNKNOWN_ERROR, (error as Error).message))
                }
                if (options.complete) {
                    options.complete()
                }
            }
        }, 0)
    }
    
    /**
     * 异步方法实现（Promise风格）
     */
    asyncMethodPromise(params: any): PluginPromise {
        return new Promise((resolve, reject) => {
            this.asyncMethod({
                params: params,
                success: (result) => resolve(result),
                fail: (error) => reject(error)
            })
        })
    }
    
    /**
     * 事件监听
     */
    on(event: string, callback: (data: any) => void): void {
        let listeners = this.eventListeners.get(event)
        if (!listeners) {
            listeners = []
            this.eventListeners.set(event, listeners)
        }
        listeners.push(callback)
        
        console.log(`YourPlugin: Added listener for event: ${event}`)
    }
    
    /**
     * 移除事件监听
     */
    off(event: string, callback?: (data: any) => void): void {
        const listeners = this.eventListeners.get(event)
        if (listeners) {
            if (callback) {
                const index = listeners.indexOf(callback)
                if (index >= 0) {
                    listeners.splice(index, 1)
                }
            } else {
                listeners.length = 0
            }
        }
        
        console.log(`YourPlugin: Removed listener for event: ${event}`)
    }
    
    /**
     * 获取插件信息
     */
    getPluginInfo(): { name: string, version: string, platform: string } {
        return {
            name: "YourPlugin",
            version: "1.0.0",
            platform: "Web"
        }
    }
    
    // === 私有方法 ===
    
    private performWebInit(options: PluginOptions): void {
        // Web特定的初始化逻辑
        if (options.debug) {
            console.log('YourPlugin: Debug mode enabled')
        }
        
        // 检查浏览器兼容性
        this.checkBrowserCompatibility()
        
        // 初始化Web资源
        this.initializeWebResources()
    }
    
    private performSyncOperation(params: any): any {
        // 同步业务逻辑实现
        return {
            processed: true,
            params: params,
            timestamp: Date.now(),
            userAgent: navigator.userAgent
        }
    }
    
    private async performAsyncOperation(params: any): Promise<any> {
        // 模拟异步操作
        return new Promise((resolve) => {
            setTimeout(() => {
                resolve({
                    processed: true,
                    params: params,
                    timestamp: Date.now(),
                    performance: performance.now()
                })
            }, 1000)
        })
    }
    
    private checkBrowserCompatibility(): void {
        const userAgent = navigator.userAgent
        console.log(`YourPlugin: Running in browser - ${userAgent}`)
        
        // 检查必要的Web API支持
        if (!window.fetch) {
            console.warn('YourPlugin: Fetch API not supported')
        }
        
        if (!window.localStorage) {
            console.warn('YourPlugin: LocalStorage not supported')
        }
        
        if (!window.Promise) {
            console.warn('YourPlugin: Promises not supported')
        }
    }
    
    private initializeWebResources(): void {
        // 初始化Web特定资源
        console.log('YourPlugin: Web resources initialized')
        
        // 可以在这里添加CSS样式、加载外部脚本等
    }
    
    /**
     * 触发事件
     */
    private emitEvent(event: string, data: any): void {
        const listeners = this.eventListeners.get(event)
        if (listeners && listeners.length > 0) {
            // 在下一个事件循环中执行回调
            setTimeout(() => {
                listeners.forEach(callback => {
                    try {
                        callback(data)
                    } catch (error) {
                        console.error(`YourPlugin: Error in event callback for ${event}`, error)
                    }
                })
            }, 0)
        }
    }
}

// 导出插件实例
const yourPluginInstance = new YourPluginImpl()

// 导出所有接口方法
export const init = (options: PluginOptions): void => yourPluginInstance.init(options)
export const syncMethod = (params: any): PluginResult => yourPluginInstance.syncMethod(params)
export const asyncMethod = (options: PluginRequestOptions): void => yourPluginInstance.asyncMethod(options)
export const asyncMethodPromise = (params: any): PluginPromise => yourPluginInstance.asyncMethodPromise(params)
export const on = (event: string, callback: (data: any) => void): void => yourPluginInstance.on(event, callback)
export const off = (event: string, callback?: (data: any) => void): void => yourPluginInstance.off(event, callback)
export const getPluginInfo = (): { name: string, version: string, platform: string } => yourPluginInstance.getPluginInfo()

// 兼容默认导出
export default yourPluginInstance
```

## 插件使用

### 在uni-app项目中使用

```typescript
// 在页面或组件中使用插件
import { init, syncMethod, asyncMethod } from '@/uni_modules/your-plugin-name'

export default {
    onLoad() {
        // 初始化插件
        init({
            apiKey: 'your-api-key',
            debug: true,
            timeout: 10000
        })
        
        // 使用同步方法
        try {
            const result = syncMethod('test-parameter')
            console.log('同步方法结果:', result)
        } catch (error) {
            console.error('同步方法错误:', error)
        }
        
        // 使用异步方法
        asyncMethod({
            param: 'test-data',
            success: (res) => {
                console.log('异步方法成功:', res)
            },
            fail: (err) => {
                console.error('异步方法失败:', err)
            },
            complete: () => {
                console.log('异步方法完成')
            }
        })
    }
}
```

## 调试和测试

### 1. 调试配置

在插件开发过程中可以使用以下调试方法：

```typescript
// 调试配置
export class DebugConfig {
    static readonly ENABLE_DEBUG = true
    static readonly LOG_LEVEL = 'debug' // 'debug' | 'info' | 'warn' | 'error'
    
    static log(level: string, message: string, ...args: any[]): void {
        if (!this.ENABLE_DEBUG) return
        
        const timestamp = new Date().toISOString()
        const prefix = `[YourPlugin ${timestamp}]`
        
        switch (level) {
            case 'debug':
                console.debug(prefix, message, ...args)
                break
            case 'info':
                console.info(prefix, message, ...args)
                break
            case 'warn':
                console.warn(prefix, message, ...args)
                break
            case 'error':
                console.error(prefix, message, ...args)
                break
        }
    }
    
    static debug(message: string, ...args: any[]): void {
        this.log('debug', message, ...args)
    }
    
    static info(message: string, ...args: any[]): void {
        this.log('info', message, ...args)
    }
    
    static warn(message: string, ...args: any[]): void {
        this.log('warn', message, ...args)
    }
    
    static error(message: string, ...args: any[]): void {
        this.log('error', message, ...args)
    }
}
```

### 2. 单元测试

```typescript
// 插件单元测试示例
export class PluginTestSuite {
    
    static runAllTests(): void {
        console.log('开始运行插件测试...')
        
        this.testInitialization()
        this.testSyncMethods()
        this.testAsyncMethods()
        this.testEventSystem()
        this.testErrorHandling()
        
        console.log('所有测试完成')
    }
    
    static testInitialization(): void {
        console.log('测试初始化...')
        
        try {
            YourPlugin.init({ debug: true })
            const info = YourPlugin.getPluginInfo()
            
            console.assert(info.name === 'YourPlugin', '插件名称不正确')
            console.assert(info.version === '1.0.0', '插件版本不正确')
            
            console.log('✓ 初始化测试通过')
        } catch (error) {
            console.error('✗ 初始化测试失败:', error)
        }
    }
    
    static testSyncMethods(): void {
        console.log('测试同步方法...')
        
        try {
            const result = YourPlugin.syncMethod({ test: true })
            
            console.assert(result.success === true, '同步方法返回失败')
            console.assert(result.data !== null, '同步方法数据为空')
            
            console.log('✓ 同步方法测试通过')
        } catch (error) {
            console.error('✗ 同步方法测试失败:', error)
        }
    }
    
    static testAsyncMethods(): void {
        console.log('测试异步方法...')
        
        return new Promise((resolve) => {
            YourPlugin.asyncMethod({
                params: { test: true },
                success: (result) => {
                    console.assert(result.success === true, '异步方法返回失败')
                    console.log('✓ 异步方法测试通过')
                    resolve()
                },
                fail: (error) => {
                    console.error('✗ 异步方法测试失败:', error)
                    resolve()
                }
            })
        })
    }
    
    static testEventSystem(): void {
        console.log('测试事件系统...')
        
        let eventReceived = false
        
        const testCallback = (data: any) => {
            eventReceived = true
            console.assert(data.test === true, '事件数据不正确')
        }
        
        YourPlugin.on('testEvent', testCallback)
        
        // 模拟触发事件
        setTimeout(() => {
            // 这里需要插件内部触发事件的方法
            // yourPluginInstance.emitEvent('testEvent', { test: true })
            
            setTimeout(() => {
                YourPlugin.off('testEvent', testCallback)
                
                if (eventReceived) {
                    console.log('✓ 事件系统测试通过')
                } else {
                    console.error('✗ 事件系统测试失败: 未收到事件')
                }
            }, 100)
        }, 100)
    }
    
    static testErrorHandling(): void {
        console.log('测试错误处理...')
        
        try {
            // 测试未初始化时的错误
            const newPlugin = new YourPluginImpl()
            const result = newPlugin.syncMethod({})
            
            console.assert(result.success === false, '应该返回失败结果')
            console.assert(result.error.errCode === PluginErrorCode.INITIALIZATION_FAILED, '错误码不正确')
            
            console.log('✓ 错误处理测试通过')
        } catch (error) {
            console.error('✗ 错误处理测试失败:', error)
        }
    }
}
```

## 最佳实践

### 1. 接口设计原则

- **一致性**: 保持所有平台接口的一致性
- **简洁性**: 接口简单易用，避免过度复杂
- **扩展性**: 预留扩展空间，支持未来功能添加
- **兼容性**: 向后兼容，避免破坏性变更

### 2. 错误处理策略

- **统一错误码**: 使用统一的错误码和错误消息
- **详细日志**: 记录详细的错误信息便于调试
- **优雅降级**: 在功能不可用时提供备选方案
- **用户友好**: 向用户提供易懂的错误提示

### 3. 性能优化

- **懒加载**: 按需加载资源和初始化组件
- **内存管理**: 及时释放不再使用的资源
- **缓存策略**: 合理使用缓存减少重复计算
- **异步优先**: 耗时操作使用异步处理

### 4. 安全考虑

- **参数验证**: 严格验证输入参数
- **权限检查**: 检查必要的系统权限
- **数据加密**: 敏感数据进行加密处理
- **防护机制**: 添加必要的安全防护措施

通过遵循这些指导原则，你可以开发出高质量、可维护的UTS插件。
