# 常见问题与报错汇编（UTS 插件）

> 同步说明（来源：官方文档）
> - 参考页面：UTS 插件介绍 - 常见问题/报错、uts for Android/iOS/Harmony 专章
> - 同步时间：2025-09-13
> - 适用范围：uni-app 与 uni-app x（差异点已标注）

## 快速导航

- 构建/资源：R 资源 unresolved、包名不一致、.so 限制
- 语法/类型：函数重载、泛型、原生类型传参、数组与 JSON 判定
- 回调/异步：持续回调（UTSCallback）、线程与调度
- Android 专项：permissions、targetSdk、过时 API
- iOS 专项：主线程、Framework/entitlement/Info.plist

---

## 构建与资源类问题

### R 资源 unresolved（Android）
- 检查 `utssdk/app-android/res/` 下资源路径是否规范（`layout/`, `values/` 等）。
- 包名需与默认规则一致（详见《Android平台UTS开发增强指南》）。
- 若使用 AndroidX 资源，补充依赖：`androidx.appcompat:appcompat`。

### 包名不一致（混编）
- Kotlin/Swift 文件的 `package`/命名空间必须与默认包名规则一致，否则找不到类/方法。

### .so 库限制（Android）
- 暂不支持直接在插件目录放置 `.so`。改用 Gradle/Maven 远程依赖或 AAR。

---

## 语法与类型限制

### 函数重载（uni-app 限制）
- 在 uni-app（JS 调用）环境中不支持导出函数重载。建议改为不同方法名或使用“选项对象”。

### 原生类型传参
- Android：某些平台 API 需要精确数字类型（如 `Int` 而非 `Number`）。
- iOS：注意 Swift 参数标签（外部参数名）会影响 UTS 调用签名。

### 数组与 JSON 判定
- `typeof` 对数组返回 `object`，判定数组使用 `Array.isArray`。
- UTSJSONObject 可结合断言与 `instanceof`。

### 函数作为参数（限制与建议）
- 一次性回调：用于单次成功/失败的通知，按 `success/fail/complete` 契约执行；
- 持续性回调：用于多次推送的监听，建议导出 `onData`、并提供 `stop()` 停止；
- 在 uni-app（JS）环境中，匿名函数/闭包作为参数时请注意作用域与释放，避免持有导致泄漏。

### 匿名内部类（写法提示）
- 参考官方“匿名内部类”章节的 UTS 写法；
- 当需要实现单方法接口时，优先使用函数式回调而非匿名类，减少样板代码与易错点。

### 泛型参数注意事项
- 编译期类型擦除导致的类型信息丢失需通过显式类型/工厂方法/断言规避；
- 跨语言边界（UTS ↔ Kotlin/Swift）传递泛型时，避免在边界处依赖泛型信息进行分支。

### 函数参数默认值
- 不同平台的默认值展开规则可能存在差异，导出给 JS/UTS 侧的 API 建议显式传参，减少歧义。

### 在 uni-app 上的导出限制（清单）
- 不支持导出函数重载；
- 函数作为参数的闭包能力有限，建议精简签名；
- 泛型/复杂平台类型不建议直接暴露给 JS，改用 UTSJSONObject 或自定义 DTO。

---

## 回调与异步

### 持续回调（UTSCallback 模式）
- 需要“多次回调”时，导出持续回调函数或返回可 `stop()` 的句柄。
- UI 更新需切回主线程（Android `main`、iOS `DispatchQueue.main`）。

### 线程与调度
- Android：`UTSAndroid.getDispatcher('io'|'main')`。
- iOS：`DispatchQueue.main.async { ... }`。

### 线程同步（替代写法）
- Android：避免使用 `synchronized/Lock` 直接在 UTS 中实现，改用线程安全数据结构或将临界区封装至原生侧；
- iOS：避免在回调中长时间占用主线程，必要时将耗时操作放入后台队列。

---

## Android 专项

### 权限与 targetSdk
- 6.0+ 需动态权限；targetSdk 越高限制越严格，注意前后台行为差异。

### 过时 API 处理
- 优先使用替代 API，并用系统版本分支处理；谨慎使用 `@Suppress("DEPRECATION")`。

### 生成原生数组/byte[]
- 使用 UTS 的数组构造或平台提供的 Buffer/Utilities 工具；
- 与三方 SDK 交互时，注意 byte[] 与 UTS 数组之间的互转与编码一致性。

---

## iOS 专项

### 主线程操作
- UI 相关必须在主线程；必要时使用 `DispatchQueue.main.async`。

### Framework/entitlement/Info.plist
- 缺失系统 Framework 在 `app-ios/config.json` 添加；
- 权限用途描述（Info.plist）清晰合规；
- entitlement 需与宿主工程一致，必要时文档说明前置条件。

### 避免闭包循环引用
- 使用 `[weak self]` 捕获列表，进入回调后进行 `guard let self` 或 `if let self` 解包后再使用；
- 回调中更新 UI 请在主线程执行。

---

## 其他高频问答（节选）

- 访问 JSON 对象属性：优先使用安全断言/可选链，避免运行期错误。
- UTS 如何遍历：使用 `for-of`/`forEach` 等，注意平台对象的迭代器支持情况。
- UTS 插件导入边界：仅能 import 插件内部文件，不能跨插件或工程外部路径。

### 三方 SDK 集成策略（概述）
- Android：使用 Gradle 远程依赖或本地 AAR；遵循官方“远程/本地依赖”章节，处理 Manifest 与资源合并；
- iOS：优先用 CocoaPods（见 14），确保与打包环境工具链一致；
- HarmonyOS：使用 ohpm/HAR；
- 签名与包名：与宿主工程保持一致，避免类查找失败；
- 示例：以官方 Hello UTS 与插件示例作为模板进行裁剪。

### 示例项目
- Hello UTS 与各平台示例：参见官方示例项目与本仓文档中的链接。

> 若你的问题不在以上清单，建议先查阅对应平台专章与 API 参考，再在本项目提 issue 补充案例。
