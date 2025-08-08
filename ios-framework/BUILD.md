# iOS Framework 构建说明

本文档说明如何构建 iOS Framework 并将其集成到 UniApp X 项目中。

## 🚀 快速构建

### 使用构建脚本 (推荐)

```bash
# 进入 iOS Framework 项目目录
cd ios-framework

# 构建 Release 通用版本 (默认)
./build-framework.sh

# 构建 Debug 通用版本
./build-framework.sh -c Debug

# 仅构建模拟器版本
./build-framework.sh -s

# 仅构建设备版本
./build-framework.sh -d

# 构建通用版本 (模拟器+设备)
./build-framework.sh -u

# 清理缓存后构建
./build-framework.sh --clean

# 查看帮助
./build-framework.sh -h
```

### 使用 Xcode 命令

```bash
# 进入 iOS Framework 项目目录
cd ios-framework

# 构建 Release 版本
xcodebuild build -project ios-framework.xcodeproj -scheme ios-framework -configuration Release -sdk iphoneos

# 构建 Debug 版本
xcodebuild build -project ios-framework.xcodeproj -scheme ios-framework -configuration Debug -sdk iphoneos

# 构建模拟器版本
xcodebuild build -project ios-framework.xcodeproj -scheme ios-framework -configuration Release -sdk iphonesimulator

# 清理构建缓存
xcodebuild clean -project ios-framework.xcodeproj -scheme ios-framework
```

## 📁 构建输出

构建完成后，Framework 文件将生成在：
```
ios-framework/build/Release/
├── ios_framework.framework           # 通用版本 (模拟器+设备)
├── ios_framework-simulator.framework # 模拟器版本
└── ios_framework-device.framework    # 设备版本
```

## 🔄 自动复制

构建脚本会自动将生成的 Framework 文件复制到 UniApp 项目中：
```
uniapp-x-playground/uni_modules/say-hi/utssdk/app-ios/Frameworks/
```

## ⚙️ 构建配置

### 构建配置选项
- **Debug**: 调试版本，包含调试信息，文件较大，适合开发阶段使用
- **Release**: 发布版本，优化后的代码，文件较小，适合生产环境

### 构建类型选项
- **simulator**: 仅构建模拟器版本，支持 x86_64 和 arm64 架构
- **device**: 仅构建设备版本，支持 arm64 架构
- **universal**: 构建通用版本，同时支持模拟器和设备，文件较大但兼容性最好

## 🛠️ 环境要求

- macOS 系统
- Xcode 12.0+
- iOS SDK 12.0+

## 🔧 故障排除

### 常见问题

1. **xcodebuild 命令未找到**
   - 确保已安装 Xcode
   - 确保 Xcode 命令行工具已安装：`xcode-select --install`

2. **构建失败**
   - 清理构建缓存：`./build-framework.sh --clean`
   - 检查 Xcode 项目配置是否正确
   - 确保 iOS SDK 版本兼容

3. **Framework 复制失败**
   - 检查 UTS 插件目录是否存在
   - 确保有足够的磁盘空间
   - 检查文件权限

### 手动复制 Framework

如果自动复制失败，可以手动复制：

```bash
# 复制到 UTS 插件目录
cp -R ios-framework/build/Release/ios_framework.framework \
     ../uniapp-x-playground/uni_modules/say-hi/utssdk/app-ios/Frameworks/
```

## 📝 注意事项

1. **架构支持**：通用版本支持模拟器和设备，但文件较大
2. **版本兼容**：确保 Framework 版本与 UniApp X 项目兼容
3. **调试信息**：Debug 版本包含调试信息，Release 版本已优化
4. **备份机制**：构建脚本会自动备份原有的 Framework 文件

## 🔗 相关链接

- [UTS iOS 插件开发](https://uniapp.dcloud.net.cn/plugin/uts-plugin.html)
- [iOS Framework 开发指南](https://developer.apple.com/documentation/xcode/creating-a-framework)
- [Xcode 命令行工具](https://developer.apple.com/library/archive/technotes/tn2339/_index.html)
