# UTS原生混编开发指南

## 概述

UTS原生混编（Native Hybrid）是uni-app-x中一项强大的技术，允许开发者在UTS插件中直接集成和使用原生代码（Kotlin、Swift、ArkTS）。这种方式大大简化了原生代码的集成流程，同时保持了跨平台的一致性。

## 核心概念

### 什么是UTS原生混编

UTS原生混编是一种允许在UTS插件目录中直接放置原生代码，并在UTS代码中无缝调用原生功能的技术。与传统的原生插件开发方式不同，混编模式下原生代码与UTS代码共存，编译时自动整合。

### 主要优势

1. **零封装成本** - 无需将原生代码打包成独立的库或框架
2. **直接调用** - UTS代码可以直接调用原生代码中的函数和对象
3. **统一调试** - 原生代码可以直接在HBuilderX中运行和调试
4. **性能优化** - 编译过程无性能损耗，直接编译为目标平台代码
5. **开发效率** - 减少了原生代码的封装和桥接工作

### 支持平台

- **Android** - 支持Kotlin原生代码混编
- **iOS** - 支持Swift原生代码混编  
- **HarmonyOS** - 支持ArkTS原生代码混编

## 环境要求

### 开发工具版本
- HBuilderX 4.25 以上版本
- uni-app-x 4.0+ 项目

### 前置条件
- 具备UTS插件基本开发经验
- 了解目标平台的原生开发语言（Kotlin/Swift/ArkTS）
- 熟悉UTS语言基础语法

## 项目结构

### 基本目录结构

```
uni_modules/your-plugin/
├─utssdk/
│ ├─app-android/
│ │ ├─index.uts                    // UTS入口文件
│ │ ├─YourNativeClass.kt           // Kotlin原生代码
│ │ └─config.json                  // Android配置
│ ├─app-ios/
│ │ ├─index.uts                    // UTS入口文件
│ │ ├─YourNativeClass.swift        // Swift原生代码
│ │ └─config.json                  // iOS配置
│ └─app-harmony/
│   ├─index.uts                    // UTS入口文件
│   ├─YourNativeClass.ets          // ArkTS原生代码
│   └─config.json                  // HarmonyOS配置
├─interface.uts                     // 插件接口定义
├─unierror.uts                     // 错误类型定义
└─package.json                     // 插件配置
```

### 关键目录说明

- **原生代码文件** - 直接放在平台目录下（如 app-android/YourNativeClass.kt）
- **index.uts** - UTS入口文件，负责调用原生代码
- **config.json** - 平台特定配置，包含编译选项和依赖管理
- **AndroidManifest.xml** - Android平台权限和配置（可选）
- **Info.plist** - iOS平台权限和配置（可选）
- **module.json5** - HarmonyOS平台权限和配置（可选）

## Android平台混编开发

### 1. Kotlin代码集成

#### 创建原生代码文件
```kotlin
// utssdk/app-android/ImageProcessor.kt
// 注意：包名应与UTS插件默认包名保持一致
// 如果插件目录为 uni-image-processor，则包名为：uts.sdk.modules.uniImageProcessor
package uts.sdk.modules.uniImageProcessor

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream

class ImageProcessor {
    
    fun compressImage(imagePath: String, quality: Int): ByteArray? {
        return try {
            val bitmap = BitmapFactory.decodeFile(imagePath)
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            outputStream.toByteArray()
        } catch (e: Exception) {
            null
        }
    }
    
    fun getImageSize(imagePath: String): Map<String, Int> {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(imagePath, options)
        
        return mapOf(
            "width" to options.outWidth,
            "height" to options.outHeight
        )
    }
    
    companion object {
        fun createInstance(): ImageProcessor {
            return ImageProcessor()
        }
    }
}
```

#### UTS调用代码
```typescript
// utssdk/app-android/index.uts
// #ifdef APP-ANDROID
import { ImageProcessor } from './ImageProcessor.kt'
// #endif

interface ImageSize {
    width: number
    height: number
}

interface CompressOptions {
    quality: number
}

// #ifdef APP-ANDROID
export function compressImage(imagePath: string, options: CompressOptions): Promise<Uint8Array | null> {
    return new Promise((resolve, reject) => {
        try {
            const processor = ImageProcessor.createInstance()
            const result = processor.compressImage(imagePath, options.quality)

            if (result != null) {
                const uint8Array = new Uint8Array(result.size)
                for (let i = 0; i < result.size; i++) {
                    uint8Array[i] = result[i]
                }
                resolve(uint8Array)
            } else {
                resolve(null)
            }
        } catch (e) {
            reject(e)
        }
    })
}

export function getImageSize(imagePath: string): ImageSize {
    const processor = ImageProcessor.createInstance()
    const sizeMap = processor.getImageSize(imagePath)

    return {
        width: sizeMap.get("width") as number,
        height: sizeMap.get("height") as number
    }
}
// #endif
```

### 2. Android配置管理

#### config.json 配置
```json
{
    "name": "uni-image-processor",
    "version": "1.0.0",
    "description": "图片处理插件",
    "dependencies": [
        "androidx.exifinterface:exifinterface:1.3.3"
    ],
    "minSdkVersion": 21,
    "targetSdkVersion": 33,
    "compileSdkVersion": 33,
    "abis": ["armeabi-v7a", "arm64-v8a"]
}
```

#### AndroidManifest.xml 权限配置
权限需要在AndroidManifest.xml中配置：
```xml
<!-- utssdk/app-android/AndroidManifest.xml -->
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />

    <application>
        <!-- 其他配置 -->
    </application>

</manifest>
```

## iOS平台混编开发

### 1. Swift代码集成

#### 创建原生代码文件
```swift
// utssdk/app-ios/ImageProcessor.swift
import UIKit
import Foundation

@objc public class ImageProcessor: NSObject {
    
    @objc public func compressImage(_ imagePath: String, quality: CGFloat) -> Data? {
        guard let image = UIImage(contentsOfFile: imagePath) else {
            return nil
        }
        
        return image.jpegData(compressionQuality: quality)
    }
    
    @objc public func getImageSize(_ imagePath: String) -> [String: NSNumber] {
        guard let image = UIImage(contentsOfFile: imagePath) else {
            return ["width": 0, "height": 0]
        }
        
        return [
            "width": NSNumber(value: Float(image.size.width)),
            "height": NSNumber(value: Float(image.size.height))
        ]
    }
    
    @objc public static func createInstance() -> ImageProcessor {
        return ImageProcessor()
    }
}
```

#### UTS调用代码
```typescript
// utssdk/app-ios/index.uts
// #ifdef APP-IOS
import { ImageProcessor } from './ImageProcessor.swift'
// #endif

interface ImageSize {
    width: number
    height: number
}

interface CompressOptions {
    quality: number
}

// #ifdef APP-IOS
export function compressImage(imagePath: string, options: CompressOptions): Promise<Uint8Array | null> {
    return new Promise((resolve, reject) => {
        try {
            const processor = ImageProcessor.createInstance()
            const result = processor.compressImage(imagePath, options.quality)

            if (result != null) {
                const uint8Array = new Uint8Array(result.length)
                for (let i = 0; i < result.length; i++) {
                    uint8Array[i] = result[i]
                }
                resolve(uint8Array)
            } else {
                resolve(null)
            }
        } catch (e) {
            reject(e)
        }
    })
}

export function getImageSize(imagePath: string): ImageSize {
    const processor = ImageProcessor.createInstance()
    const sizeMap = processor.getImageSize(imagePath)

    return {
        width: sizeMap["width"]!.intValue,
        height: sizeMap["height"]!.intValue
    }
}
// #endif
```

### 2. iOS配置管理

#### config.json 配置
```json
{
    "name": "uni-image-processor",
    "version": "1.0.0",
    "description": "图片处理插件",
    "frameworks": [
        "UIKit.framework",
        "Foundation.framework",
        "CoreGraphics.framework"
    ],
    "deploymentTarget": "12.0"
}
```

#### Info.plist 权限配置
权限和信息需要在Info.plist中配置：
```xml
<!-- utssdk/app-ios/Info.plist -->
<plist version="1.0">
<dict>
    <key>NSPhotoLibraryUsageDescription</key>
    <string>需要访问相册来处理图片</string>
    <key>NSCameraUsageDescription</key>
    <string>需要访问相机来拍摄图片</string>
</dict>
</plist>
```

## HarmonyOS平台混编开发

### 1. ArkTS代码集成

#### 创建原生代码文件
```typescript
// utssdk/app-harmony/ImageProcessor.ets
import { image } from '@kit.ImageKit'

export class ImageProcessor {
    
    static async compressImage(imagePath: string, quality: number): Promise<ArrayBuffer | null> {
        try {
            const imageSource = image.createImageSource(imagePath)
            const packOpts: image.PackingOption = {
                format: "image/jpeg",
                quality: quality
            }
            
            const pixelMap = await imageSource.createPixelMap()
            const imagePacker = image.createImagePacker()
            
            return await imagePacker.packing(pixelMap, packOpts)
        } catch (err) {
            console.error('图片压缩失败:', err)
            return null
        }
    }
    
    static async getImageSize(imagePath: string): Promise<{ width: number, height: number }> {
        try {
            const imageSource = image.createImageSource(imagePath)
            const imageInfo = await imageSource.getImageInfo()
            
            return {
                width: imageInfo.size.width,
                height: imageInfo.size.height
            }
        } catch (err) {
            console.error('获取图片尺寸失败:', err)
            return { width: 0, height: 0 }
        }
    }
}
```

#### UTS调用代码
```typescript
// utssdk/app-harmony/index.uts
// #ifdef APP-HARMONY
import { ImageProcessor } from './ImageProcessor.ets'
// #endif

interface ImageSize {
    width: number
    height: number
}

interface CompressOptions {
    quality: number
}

// #ifdef APP-HARMONY
export async function compressImage(imagePath: string, options: CompressOptions): Promise<Uint8Array | null> {
    try {
        const result = await ImageProcessor.compressImage(imagePath, options.quality)

        if (result != null) {
            return new Uint8Array(result)
        } else {
            return null
        }
    } catch (e) {
        throw e
    }
}

export async function getImageSize(imagePath: string): Promise<ImageSize> {
    try {
        return await ImageProcessor.getImageSize(imagePath)
    } catch (e) {
        throw e
    }
}
// #endif
```

### 2. HarmonyOS配置管理

#### config.json 配置
```json
{
    "name": "uni-image-processor",
    "version": "1.0.0",
    "description": "图片处理插件",
    "dependencies": [
        "@ohos.multimedia.image",
        "@ohos.file.fs"
    ]
}
```

#### module.json5 权限配置
权限需要在module.json5中配置：
```json
{
    "module": {
        "name": "uni-image-processor",
        "type": "har",
        "requestPermissions": [
            {
                "name": "ohos.permission.READ_MEDIA",
                "reason": "需要读取媒体文件",
                "usedScene": {
                    "ability": ["MainAbility"],
                    "when": "always"
                }
            },
            {
                "name": "ohos.permission.WRITE_MEDIA",
                "reason": "需要写入媒体文件",
                "usedScene": {
                    "ability": ["MainAbility"],
                    "when": "always"
                }
            }
        ]
    }
}
```

## 重要注意事项

### 1. 条件编译
必须使用条件编译指令来确保代码只在对应的平台上编译：

```typescript
// #ifdef APP-ANDROID
// Android平台特有代码
// #endif

// #ifdef APP-IOS
// iOS平台特有代码
// #endif

// #ifdef APP-HARMONY
// HarmonyOS平台特有代码
// #endif
```

### 2. 包名规范
- Android Kotlin代码的包名必须与UTS插件默认包名保持一致
- 包名格式：`uts.sdk.modules.{插件目录名}`（去掉横线，驼峰命名）
- 例如：插件目录 `uni-image-processor` → 包名 `uts.sdk.modules.uniImageProcessor`

### 3. 原生代码位置
原生代码文件直接放在平台目录下：
- Android: `app-android/YourNativeClass.kt`
- iOS: `app-ios/YourNativeClass.swift`
- HarmonyOS: `app-harmony/YourNativeClass.ets`

### 4. Swift函数参数规则
当Swift函数被UTS调用时，函数参数不应该需要外部参数名（参数别名）：

```swift
// 错误示例：需要外部参数名
@objc public func compressImage(imagePath: String, quality: CGFloat) -> Data? {
    // UTS调用时需要使用：processor.compressImage(imagePath: path, quality: value)
}

// 正确示例：不需要外部参数名
@objc public func compressImage(_ imagePath: String, quality: CGFloat) -> Data? {
    // UTS调用时可以直接使用：processor.compressImage(path, value)
}
```

### 5. 开发和调试
- 原生代码可以直接在HBuilderX中真机运行调试
- 无需打包自定义基座
- 支持console.log输出日志
- 支持热重载

## 跨平台接口统一

### 统一接口定义
```typescript
// interface.uts
export interface CompressOptions {
    /**
     * 压缩质量 0-100
     */
    quality: number
}

export interface ImageSize {
    width: number
    height: number
}

/**
 * 压缩图片
 */
export type CompressImage = (imagePath: string, options: CompressOptions) => Promise<Uint8Array | null>

/**
 * 获取图片尺寸
 */
export type GetImageSize = (imagePath: string) => Promise<ImageSize>
```

### 错误处理统一
```typescript
// unierror.uts
export type ImageProcessorErrorCode = 1001001 | 1001002 | 1001003

export interface ImageProcessorFail extends IUniError {
    errCode: ImageProcessorErrorCode
}

export const ImageProcessorErrors: Map<ImageProcessorErrorCode, string> = new Map([
    [1001001, "文件路径无效"],
    [1001002, "图片格式不支持"], 
    [1001003, "压缩处理失败"]
])
```

## 开发最佳实践

### 1. 代码组织原则

#### 保持接口一致性
```typescript
// 确保所有平台实现相同的接口签名
export function processImage(path: string, options: ProcessOptions): Promise<ProcessResult>
```

#### 错误处理统一化
```typescript
// 统一的错误处理模式
try {
    const result = await nativeFunction()
    return { success: true, data: result }
} catch (error) {
    return { 
        success: false, 
        error: new UniError("uni-image-processor", 1001001, "处理失败")
    }
}
```

### 2. 性能优化策略

#### 异步操作处理
```typescript
// Android - 使用协程处理耗时操作
import kotlinx.coroutines.*

class ImageProcessor {
    suspend fun processImageAsync(path: String): ByteArray = withContext(Dispatchers.IO) {
        // 耗时的图片处理操作
        processImageInternal(path)
    }
}
```

```typescript
// iOS - 使用GCD处理异步操作
@objc public func processImageAsync(_ path: String, completion: @escaping (Data?) -> Void) {
    DispatchQueue.global(qos: .userInitiated).async {
        let result = self.processImageInternal(path)
        DispatchQueue.main.async {
            completion(result)
        }
    }
}
```

#### 内存管理
```swift
// iOS - 适当的内存管理
@objc public func processLargeImage(_ path: String) -> Data? {
    autoreleasepool {
        guard let image = UIImage(contentsOfFile: path) else { return nil }
        return processImageInternal(image)
    } // 自动释放临时对象
}
```

### 3. 调试和测试

#### 原生代码调试
```typescript
// 在UTS中添加调试信息传递
export function debugProcessImage(path: string): Promise<any> {
    if (__PLATFORM__ == "app-android") {
        // Android调试信息
        const processor = ImageProcessor.createInstance()
        processor.enableDebugMode(true)
        return processor.processWithDebugInfo(path)
    } else if (__PLATFORM__ == "app-ios") {
        // iOS调试信息  
        const processor = ImageProcessor.createInstance()
        processor.debugMode = true
        return processor.processWithDebugInfo(path)
    }
}
```

#### 单元测试支持
```kotlin
// Android - 添加测试支持
class ImageProcessorTest {
    @Test
    fun testImageCompression() {
        val processor = ImageProcessor()
        val result = processor.compressImage("/test/image.jpg", 80)
        assertNotNull(result)
        assertTrue(result.isNotEmpty())
    }
}
```

### 4. 版本兼容性

#### 平台版本检查
```typescript
// 检查平台特性支持
export function isFeatureSupported(): boolean {
    if (__PLATFORM__ == "app-android") {
        return android.os.Build.VERSION.SDK_INT >= 21
    } else if (__PLATFORM__ == "app-ios") {
        return parseFloat(UIDevice.current.systemVersion) >= 12.0
    }
    return false
}
```

## 常见问题和解决方案

### 1. 编译问题

#### 问题：原生代码找不到
```
错误：Cannot find native class 'YourNativeClass'
```

**解决方案：**
- 确保原生代码文件直接放在平台目录下（如 app-android/YourNativeClass.kt）
- 检查import路径是否正确
- 确认类名和文件名一致

#### 问题：依赖库缺失
```
错误：Package 'com.example.library' not found
```

**解决方案：**
- 在config.json中添加正确的dependencies配置
- 检查库版本兼容性
- 确保网络可以访问依赖仓库

### 2. 运行时问题

#### 问题：调用原生方法失败
```
错误：Native method invocation failed
```

**解决方案：**
- 检查方法签名是否匹配
- 确认参数类型转换正确
- 添加必要的异常处理

#### 问题：内存泄漏
```
警告：Memory leak detected in native code
```

**解决方案：**
- 及时释放原生对象引用
- 使用weak引用打破循环引用
- 在适当时机清理资源

### 3. 平台差异处理

#### 不同平台API差异
```typescript
export function getPlatformSpecificValue(): number {
    if (__PLATFORM__ == "app-android") {
        return AndroidSpecific.getValue()
    } else if (__PLATFORM__ == "app-ios") {
        return iOSSpecific.getValue()
    } else if (__PLATFORM__ == "app-harmony") {
        return HarmonySpecific.getValue()
    }
    return 0
}
```

### 4. UTS原生混编特有的常见问题

#### 原生代码无法找到
```typescript
// 错误：Cannot resolve native class 'ImageProcessor'

// 解决方案：检查import路径和文件位置
// 1. 确保原生代码文件直接放在平台目录下（如 app-android/ImageProcessor.kt）
// 2. 检查包名是否正确（Android）
// 3. 检查类名和文件名是否一致
// 4. 确保使用了正确的条件编译指令
```

#### 包名不匹配
```kotlin
// Android Kotlin代码中的包名必须与UTS插件包名一致
// 错误示例：
package com.example.imageprocessor  // ❌ 错误

// 正确示例：
package uts.sdk.modules.uniImageProcessor  // ✅ 正确
```

#### 条件编译遗漏
```typescript
// 错误：代码在错误平台编译
// 缺少条件编译会导致在不支持的平台上编译失败

// 正确做法：
export function callNativeMethod(): void {
    // #ifdef APP-ANDROID
    const result = NativeAndroid.method()
    // #endif

    // #ifdef APP-IOS
    const result = NativeIOS.method()
    // #endif
}
```

#### 权限配置错误
```json
// Android config.json 权限配置
{
    "permissions": [
        "android.permission.READ_EXTERNAL_STORAGE",
        "android.permission.WRITE_EXTERNAL_STORAGE"
    ]
}

// iOS config.json 权限配置
{
    "capabilities": {
        "photoLibraryUsageDescription": "需要访问相册来处理图片"
    }
}
```

## 总结

UTS原生混编技术为uni-app-x开发者提供了一个强大而灵活的原生功能集成方案。通过合理的项目结构组织、统一的接口设计和良好的开发实践，可以高效地实现跨平台原生功能，同时保持代码的可维护性和扩展性。

### 核心优势总结

1. **零封装成本** - 直接集成原生代码，无需创建独立的库或框架
2. **无缝调用** - UTS代码可以直接调用原生代码，无性能损耗
3. **统一调试** - 原生代码直接在HBuilderX中运行和调试
4. **热重载支持** - 修改原生代码后可直接运行，无需重新打包
5. **跨平台一致性** - 统一的开发模式，学习成本低

### 关键要点

- ✅ **严格的包名规范** - Android Kotlin代码必须使用UTS插件默认包名
- ✅ **条件编译指令** - 必须使用`#ifdef APP-XXX`来隔离平台代码
- ✅ **配置文件正确性** - 各平台config.json配置必须准确无误
- ✅ **权限配置** - 正确配置各平台的权限（AndroidManifest.xml、Info.plist、module.json5）
- ✅ **原生代码位置** - 直接放在平台目录下，无需native子目录
- ✅ **统一接口设计** - 确保跨平台API一致性
- ✅ **错误处理机制** - 完善的异常处理和错误反馈

### 开发建议

1. **从简单开始** - 先从单个平台开始，熟悉开发流程后再扩展到多平台
2. **包名一致性** - 严格按照规范设置包名，避免不必要的错误
3. **充分测试** - 在真机上充分测试所有平台的功能
4. **文档记录** - 记录平台差异和特殊处理逻辑
5. **版本控制** - 妥善管理原生代码的版本和依赖

通过遵循这些指导原则和最佳实践，开发者可以充分发挥UTS原生混编的优势，创建高质量、性能优异的跨平台插件。