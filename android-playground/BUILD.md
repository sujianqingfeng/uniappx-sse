# Android AAR 构建说明

本文档详细说明了如何构建 Android AAR 包，支持不同的构建环境。

## 🎯 构建环境

项目支持三种构建环境：

| 环境 | 描述 | 特点 | 适用场景 |
|------|------|------|----------|
| `debug` | 调试版本 | 包含调试信息，文件较大 | 开发阶段使用 |
| `release` | 发布版本 | 优化后的代码，文件较小 | 生产环境 |
| `release-minified` | 混淆版本 | 代码经过混淆和压缩，文件最小 | 最终发布 |

## 🛠️ 构建方式

### 1. Shell 脚本 (推荐)

**Linux/macOS:**
```bash
./build-aar.sh -e debug          # 构建 debug 版本
./build-aar.sh -e release        # 构建 release 版本
./build-aar.sh -e release-minified -c  # 构建混淆版本并清理缓存
```

**Windows:**
```cmd
build-aar.bat -e debug           # 构建 debug 版本
build-aar.bat -e release         # 构建 release 版本
build-aar.bat -e release-minified -c  # 构建混淆版本并清理缓存
```

### 2. Gradle 命令

```bash
./gradlew :android-lib:assembleDebug    # 构建 debug 版本
./gradlew :android-lib:assembleRelease  # 构建 release 版本
./gradlew clean                          # 清理构建缓存
```

## 📁 输出文件

构建完成后，AAR 文件将生成在：
```
android-playground/android-lib/build/outputs/aar/
├── android-lib-debug.aar      # Debug 版本
├── android-lib-release.aar    # Release 版本
└── android-lib-release.aar    # Release-minified 版本 (混淆后)
```

## 🔄 自动复制

构建脚本会自动将生成的 AAR 文件复制到 UniApp 项目中：
```
uniapp-x-playground/uni_modules/say-hi/utssdk/app-android/libs/
```

## ⚙️ 配置说明

### build.gradle.kts

Android 库模块的构建配置：

```kotlin
android {
  buildTypes {
    release {
      isMinifyEnabled = false  // 默认不启用混淆
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }
}
```

## 🚀 快速开始

1. **选择构建方式**：推荐使用 Shell 脚本或 Node.js 脚本
2. **选择构建环境**：开发时使用 `debug`，发布时使用 `release` 或 `release-minified`
3. **执行构建**：运行相应的构建命令
4. **检查结果**：查看生成的 AAR 文件和复制到 UniApp 项目的情况

## 🔧 故障排除

### 常见问题

1. **权限问题**：确保脚本有执行权限
   ```bash
   chmod +x build-aar.sh
   chmod +x build-aar.js
   ```

2. **Gradle 问题**：确保 Gradle 环境正确配置
   ```bash
   ./gradlew --version
   ```

3. **路径问题**：确保在正确的目录下执行命令
   ```bash
   cd android-playground
   ```

### 调试模式

使用 `-v` 或 `--verbose` 参数获取详细输出：
```bash
./build-aar.sh -e debug -v
```

## 📚 相关文档

- [Gradle 用户指南](https://docs.gradle.org/current/userguide/userguide.html)
- [Android 库开发](https://developer.android.com/studio/projects/android-library)
- [ProGuard 混淆](https://www.guardsquare.com/manual/configuration/usage)
