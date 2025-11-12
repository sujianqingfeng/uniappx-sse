# 打包、发布与维护（UTS 插件）

> 同步说明（参考官方文档与市场规范）
> - 适用范围：uni_modules（uts-plugin）上架 DCloud 插件市场的常规流程

## 发布前检查清单

- 功能完整：最小功能闭环可真机运行；
- 兼容矩阵：Android/iOS/HarmonyOS（按需）最小设备/系统验证；
- 权限与合规：收集个人信息/敏感权限前置授权与隐私说明；
- 性能与稳定：主线程避免耗时、回调释放、内存无明显泄漏；
- 文档：README/使用说明/示例代码/变更日志齐全。

## package.json 与 dcloudext

- `id/displayName/version/description/keywords/engines.HBuilderX` 填写规范；
- `dcloudext.type=uts-plugin`；
- `dcloudext.declaration`：广告、数据采集、权限用途如实申报；
- 销售策略（sale）：源码授权与常规售价按需配置；
- 联系方式（contact）：可选但建议填写，便于沟通反馈。

## 版本管理与变更日志

- 遵循 SemVer：破坏性变更 → 主版本；向后兼容特性 → 次版本；修补 → 修订号；
- 维护 `CHANGELOG`：记录修复/新增/变更/兼容性影响；
- 标明最低 HBuilderX 版本与平台要求变化。

## 提交与审核要点

- 市场资料：清晰的功能描述、截图/GIF、示例代码链接；
- 合规：权限用途描述、隐私采集说明、第三方 SDK 授权合规；
- 依赖：Android Gradle/Maven、iOS CocoaPods、Harmony ohpm 的声明与版本；
- 示例项目：可选，提供可运行 Demo 提升审核通过率与用户体验。

## 维护与支持

- Issue/反馈：明确渠道与响应 SLA；
- 兼容性：跟进 HBuilderX/Kotlin/Swift/ArkTS/SDK 升级；
- 文档演进：与官方文档版本保持对齐，更新同步说明与版本矩阵；
- 弃用策略：标注废弃 API 的替代方案与过渡期。

