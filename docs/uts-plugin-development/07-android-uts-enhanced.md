# Android平台UTS开发增强指南

## Android特有开发细节补充

### UTSAndroid工具类详解

UTSAndroid是UTS提供的Android平台专用工具类，提供了丰富的原生能力访问接口：

```typescript
// #ifdef APP-ANDROID

/**
 * UTSAndroid核心工具类使用
 */
export class UTSAndroidHelper {
    
    /**
     * 获取应用上下文
     */
    static getAppContext(): Context {
        return UTSAndroid.getAppContext() as Context
    }
    
    /**
     * 获取当前Activity
     */
    static getUniActivity(): Activity {
        return UTSAndroid.getUniActivity()!
    }
    
    /**
     * 线程调度器使用
     */
    static runOnIOThread(task: () => void): void {
        UTSAndroid.getDispatcher("io").async(function(_) {
            task()
        }, null)
    }
    
    static runOnMainThread(task: () => void): void {
        UTSAndroid.getDispatcher("main").async(function(_) {
            task()
        }, null)
    }
    
    static runOnDefaultThread(task: () => void): void {
        UTSAndroid.getDispatcher("default").async(function(_) {
            task()
        }, null)
    }
    
    /**
     * 权限管理
     */
    static requestPermission(
        permissions: Array<string>, 
        callback: (allGranted: boolean, grantedList: Array<string>) => void
    ): void {
        UTSAndroid.requestSystemPermission(
            UTSAndroid.getUniActivity()!,
            permissions,
            function(allGranted: boolean, grantedList: Array<string>) {
                callback(allGranted, grantedList)
            },
            function(doNotAskAgain: boolean, grantedList: Array<string>) {
                callback(false, grantedList)
            }
        )
    }
    
    /**
     * 检查单个权限
     */
    static hasPermission(permission: string): boolean {
        const context = this.getAppContext()
        return context.checkSelfPermission(permission) == 
            android.content.pm.PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * 检查多个权限
     */
    static hasPermissions(permissions: Array<string>): boolean {
        return permissions.every(permission => this.hasPermission(permission))
    }
    
    /**
     * 获取应用信息
     */
    static getAppInfo(): any {
        const context = this.getAppContext()
        const packageManager = context.getPackageManager()
        const packageInfo = packageManager.getPackageInfo(context.getPackageName(), 0)
        
        return {
            packageName: context.getPackageName(),
            versionName: packageInfo.versionName,
            versionCode: packageInfo.versionCode,
            targetSdkVersion: packageInfo.applicationInfo?.targetSdkVersion || 0
        }
    }
    
    /**
     * 获取设备信息
     */
    static getDeviceInfo(): any {
        return {
            brand: android.os.Build.BRAND,
            model: android.os.Build.MODEL,
            device: android.os.Build.DEVICE,
            product: android.os.Build.PRODUCT,
            hardware: android.os.Build.HARDWARE,
            manufacturer: android.os.Build.MANUFACTURER,
            version: {
                release: android.os.Build.VERSION.RELEASE,
                sdkInt: android.os.Build.VERSION.SDK_INT,
                codename: android.os.Build.VERSION.CODENAME
            }
        }
    }
    
    /**
     * 获取屏幕信息
     */
    static getScreenInfo(): any {
        const context = this.getAppContext()
        const resources = context.getResources()
        const metrics = resources.getDisplayMetrics()
        const configuration = resources.getConfiguration()
        
        return {
            widthPixels: metrics.widthPixels,
            heightPixels: metrics.heightPixels,
            density: metrics.density,
            densityDpi: metrics.densityDpi,
            scaledDensity: metrics.scaledDensity,
            orientation: configuration.orientation,
            screenLayout: configuration.screenLayout
        }
    }
}

// #endif
```

### Android资源管理

```typescript
// #ifdef APP-ANDROID

/**
 * Android资源管理工具类
 */
export class AndroidResourceManager {
    
    private static context = UTSAndroid.getAppContext() as Context
    private static resources = this.context.getResources()
    
    /**
     * 获取字符串资源
     */
    static getString(resId: number): string {
        return this.resources.getString(resId)
    }
    
    /**
     * 获取字符串资源（带参数）
     */
    static getStringWithArgs(resId: number, ...args: any[]): string {
        return this.resources.getString(resId, ...args)
    }
    
    /**
     * 获取颜色资源
     */
    static getColor(resId: number): number {
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            return this.resources.getColor(resId, this.context.getTheme())
        } else {
            return this.resources.getColor(resId)
        }
    }
    
    /**
     * 获取尺寸资源
     */
    static getDimension(resId: number): number {
        return this.resources.getDimension(resId)
    }
    
    /**
     * 获取尺寸像素值
     */
    static getDimensionPixelSize(resId: number): number {
        return this.resources.getDimensionPixelSize(resId)
    }
    
    /**
     * 获取Drawable资源
     */
    static getDrawable(resId: number): android.graphics.drawable.Drawable | null {
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            return this.resources.getDrawable(resId, this.context.getTheme())
        } else {
            return this.resources.getDrawable(resId)
        }
    }
    
    /**
     * 获取Assets文件内容
     */
    static getAssetsFile(fileName: string): string | null {
        try {
            const assetManager = this.context.getAssets()
            const inputStream = assetManager.open(fileName)
            const bytes = inputStream.readBytes()
            inputStream.close()
            
            return new java.lang.String(bytes, "UTF-8").toString()
        } catch (error) {
            android.util.Log.e("ResourceManager", "Failed to read assets file: " + error.message)
            return null
        }
    }
    
    /**
     * DP转PX
     */
    static dp2px(dp: number): number {
        const density = this.resources.getDisplayMetrics().density
        return (dp * density + 0.5).toInt()
    }
    
    /**
     * SP转PX
     */
    static sp2px(sp: number): number {
        const scaledDensity = this.resources.getDisplayMetrics().scaledDensity
        return (sp * scaledDensity + 0.5).toInt()
    }
    
    /**
     * PX转DP
     */
    static px2dp(px: number): number {
        const density = this.resources.getDisplayMetrics().density
        return (px / density + 0.5).toInt()
    }
}

// #endif
```

### Android生命周期管理

```typescript
// #ifdef APP-ANDROID

import Application from "android.app.Application"
import Activity from "android.app.Activity"
import Bundle from "android.os.Bundle"

/**
 * Android生命周期监听器
 */
export class AndroidLifecycleManager {
    
    private static lifecycleCallbacks: Map<string, Array<Function>> = new Map()
    private static isRegistered = false
    
    /**
     * 注册生命周期监听
     */
    static registerLifecycleCallbacks(): void {
        if (this.isRegistered) return
        
        const context = UTSAndroid.getAppContext() as Context
        const application = context.getApplicationContext() as Application
        
        const callbacks = new Application.ActivityLifecycleCallbacks() {
            override onActivityCreated(activity: Activity, savedInstanceState: Bundle | null): void {
                AndroidLifecycleManager.triggerCallbacks('onCreate', activity, savedInstanceState)
            }
            
            override onActivityStarted(activity: Activity): void {
                AndroidLifecycleManager.triggerCallbacks('onStart', activity)
            }
            
            override onActivityResumed(activity: Activity): void {
                AndroidLifecycleManager.triggerCallbacks('onResume', activity)
            }
            
            override onActivityPaused(activity: Activity): void {
                AndroidLifecycleManager.triggerCallbacks('onPause', activity)
            }
            
            override onActivityStopped(activity: Activity): void {
                AndroidLifecycleManager.triggerCallbacks('onStop', activity)
            }
            
            override onActivitySaveInstanceState(activity: Activity, outState: Bundle): void {
                AndroidLifecycleManager.triggerCallbacks('onSaveInstanceState', activity, outState)
            }
            
            override onActivityDestroyed(activity: Activity): void {
                AndroidLifecycleManager.triggerCallbacks('onDestroy', activity)
            }
        }
        
        application.registerActivityLifecycleCallbacks(callbacks)
        this.isRegistered = true
    }
    
    /**
     * 添加生命周期监听器
     */
    static addLifecycleListener(event: string, callback: Function): void {
        let callbacks = this.lifecycleCallbacks.get(event)
        if (!callbacks) {
            callbacks = []
            this.lifecycleCallbacks.set(event, callbacks)
        }
        callbacks.push(callback)
        
        // 确保已注册生命周期监听
        this.registerLifecycleCallbacks()
    }
    
    /**
     * 移除生命周期监听器
     */
    static removeLifecycleListener(event: string, callback: Function): void {
        const callbacks = this.lifecycleCallbacks.get(event)
        if (callbacks) {
            const index = callbacks.indexOf(callback)
            if (index >= 0) {
                callbacks.splice(index, 1)
            }
        }
    }
    
    /**
     * 触发回调
     */
    private static triggerCallbacks(event: string, ...args: any[]): void {
        const callbacks = this.lifecycleCallbacks.get(event)
        if (callbacks) {
            callbacks.forEach(callback => {
                try {
                    callback(...args)
                } catch (error) {
                    android.util.Log.e("LifecycleManager", "Error in lifecycle callback: " + error.message)
                }
            })
        }
    }
    
    /**
     * 获取当前Activity状态
     */
    static getCurrentActivityState(): string {
        const activity = UTSAndroid.getUniActivity()
        if (!activity) return 'unknown'
        
        return activity.isDestroyed() ? 'destroyed' : 
               activity.isFinishing() ? 'finishing' : 'active'
    }
}

// #endif
```

### 详细config.json配置

```json
{
    "minSdkVersion": 21,
    "compileSdkVersion": 34,
    "targetSdkVersion": 34,
    "dependencies": [
        "androidx.appcompat:appcompat:1.6.1",
        "androidx.recyclerview:recyclerview:1.3.0", 
        "com.google.android.material:material:1.9.0",
        "com.squareup.okhttp3:okhttp:4.10.0",
        "com.google.code.gson:gson:2.10.1"
    ],
    "abis": ["armeabi-v7a", "arm64-v8a", "x86", "x86_64"],
    "project": {
        "plugins": [
            "kotlin-android",
            "kotlin-parcelize"
        ],
        "dependencies": [
            "org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.8.0"
        ],
        "repositories": [
            "maven { url 'https://repo1.maven.org/maven2/' }",
            "maven { url 'https://jcenter.bintray.com/' }",
            "maven { url 'https://maven.google.com/' }",
            "maven { url 'https://developer.huawei.com/repo/' }",
            "maven { url 'https://jitpack.io' }"
        ]
    }
}
```

### AndroidManifest.xml 权限配置

权限需要在AndroidManifest.xml中进行配置：

```xml
<!-- utssdk/app-android/AndroidManifest.xml -->
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    
    <!-- 网络访问权限 -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    
    <!-- 存储权限（API 23+需要动态申请） -->
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
    
    <!-- 摄像头权限 -->
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-feature android:name="android.hardware.camera" android:required="false" />
    
    <!-- 位置权限 -->
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
    
</manifest>
```

### Android系统服务访问

```typescript
// #ifdef APP-ANDROID

/**
 * Android系统服务访问工具类
 */
export class AndroidSystemService {
    
    private static context = UTSAndroid.getAppContext() as Context
    
    /**
     * 连接管理器服务
     */
    static getConnectivityManager(): android.net.ConnectivityManager {
        return this.context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
    }
    
    /**
     * WiFi管理器服务
     */
    static getWifiManager(): android.net.wifi.WifiManager {
        return this.context.getApplicationContext().getSystemService(Context.WIFI_SERVICE) as android.net.wifi.WifiManager
    }
    
    /**
     * 电话管理器服务
     */
    static getTelephonyManager(): android.telephony.TelephonyManager {
        return this.context.getSystemService(Context.TELEPHONY_SERVICE) as android.telephony.TelephonyManager
    }
    
    /**
     * 位置管理器服务
     */
    static getLocationManager(): android.location.LocationManager {
        return this.context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
    }
    
    /**
     * 音频管理器服务
     */
    static getAudioManager(): android.media.AudioManager {
        return this.context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
    }
    
    /**
     * 传感器管理器服务
     */
    static getSensorManager(): android.hardware.SensorManager {
        return this.context.getSystemService(Context.SENSOR_SERVICE) as android.hardware.SensorManager
    }
    
    /**
     * 振动器服务
     */
    static getVibrator(): android.os.Vibrator {
        return this.context.getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
    }
    
    /**
     * 通知管理器服务
     */
    static getNotificationManager(): android.app.NotificationManager {
        return this.context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
    }
    
    /**
     * 剪贴板管理器服务
     */
    static getClipboardManager(): android.content.ClipboardManager {
        return this.context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    }
    
    /**
     * 检查网络连接状态
     */
    static isNetworkAvailable(): boolean {
        const connectivityManager = this.getConnectivityManager()
        const networkInfo = connectivityManager.getActiveNetworkInfo()
        return networkInfo != null && networkInfo.isConnected()
    }
    
    /**
     * 检查WiFi状态
     */
    static isWifiEnabled(): boolean {
        const wifiManager = this.getWifiManager()
        return wifiManager.isWifiEnabled()
    }
    
    /**
     * 获取网络类型
     */
    static getNetworkType(): string {
        const connectivityManager = this.getConnectivityManager()
        const networkInfo = connectivityManager.getActiveNetworkInfo()
        
        if (networkInfo == null || !networkInfo.isConnected()) {
            return 'none'
        }
        
        when (networkInfo.getType()) {
            android.net.ConnectivityManager.TYPE_WIFI -> return 'wifi'
            android.net.ConnectivityManager.TYPE_MOBILE -> return 'mobile'
            android.net.ConnectivityManager.TYPE_ETHERNET -> return 'ethernet'
            else -> return 'unknown'
        }
    }
}

// #endif
```

### Android Intent处理

```typescript
// #ifdef APP-ANDROID

import Intent from "android.content.Intent"
import ComponentName from "android.content.ComponentName"

/**
 * Android Intent处理工具类
 */
export class AndroidIntentHelper {
    
    private static context = UTSAndroid.getAppContext() as Context
    
    /**
     * 启动Activity
     */
    static startActivity(packageName: string, className: string, extras?: any): boolean {
        try {
            const intent = new Intent()
            intent.setComponent(new ComponentName(packageName, className))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            
            if (extras) {
                this.addExtrasToIntent(intent, extras)
            }
            
            this.context.startActivity(intent)
            return true
        } catch (error) {
            android.util.Log.e("IntentHelper", "Failed to start activity: " + error.message)
            return false
        }
    }
    
    /**
     * 启动应用
     */
    static launchApp(packageName: string): boolean {
        try {
            const packageManager = this.context.getPackageManager()
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                this.context.startActivity(intent)
                return true
            }
            
            return false
        } catch (error) {
            android.util.Log.e("IntentHelper", "Failed to launch app: " + error.message)
            return false
        }
    }
    
    /**
     * 检查应用是否安装
     */
    static isAppInstalled(packageName: string): boolean {
        try {
            val packageManager = this.context.getPackageManager()
            packageManager.getPackageInfo(packageName, android.content.pm.PackageManager.GET_ACTIVITIES)
            return true
        } catch (error) {
            return false
        }
    }
    
    /**
     * 打开系统设置
     */
    static openSystemSettings(action: string = android.provider.Settings.ACTION_SETTINGS): boolean {
        try {
            const intent = new Intent(action)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            this.context.startActivity(intent)
            return true
        } catch (error) {
            android.util.Log.e("IntentHelper", "Failed to open settings: " + error.message)
            return false
        }
    }
    
    /**
     * 发送分享Intent
     */
    static shareText(text: string, title?: string): boolean {
        try {
            const intent = new Intent(Intent.ACTION_SEND)
            intent.setType("text/plain")
            intent.putExtra(Intent.EXTRA_TEXT, text)
            
            if (title) {
                intent.putExtra(Intent.EXTRA_TITLE, title)
            }
            
            val chooserIntent = Intent.createChooser(intent, title || "分享到")
            chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            
            this.context.startActivity(chooserIntent)
            return true
        } catch (error) {
            android.util.Log.e("IntentHelper", "Failed to share text: " + error.message)
            return false
        }
    }
    
    /**
     * 拨打电话
     */
    static makePhoneCall(phoneNumber: string): boolean {
        try {
            const intent = new Intent(Intent.ACTION_CALL)
            intent.setData(android.net.Uri.parse("tel:" + phoneNumber))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            
            this.context.startActivity(intent)
            return true
        } catch (error) {
            android.util.Log.e("IntentHelper", "Failed to make phone call: " + error.message)
            return false
        }
    }
    
    /**
     * 发送短信
     */
    static sendSMS(phoneNumber: string, message: string): boolean {
        try {
            const intent = new Intent(Intent.ACTION_SENDTO)
            intent.setData(android.net.Uri.parse("smsto:" + phoneNumber))
            intent.putExtra("sms_body", message)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            
            this.context.startActivity(intent)
            return true
        } catch (error) {
            android.util.Log.e("IntentHelper", "Failed to send SMS: " + error.message)
            return false
        }
    }
    
    /**
     * 打开浏览器
     */
    static openUrl(url: string): boolean {
        try {
            val intent = new Intent(Intent.ACTION_VIEW)
            intent.setData(android.net.Uri.parse(url))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            
            this.context.startActivity(intent)
            return true
        } catch (error) {
            android.util.Log.e("IntentHelper", "Failed to open URL: " + error.message)
            return false
        }
    }
    
    /**
     * 向Intent添加额外数据
     */
    private static addExtrasToIntent(intent: Intent, extras: any): void {
        for (const key in extras) {
            const value = extras[key]
            
            when (typeof value) {
                "string" -> intent.putExtra(key, value as string)
                "number" -> {
                    if (Number.isInteger(value)) {
                        intent.putExtra(key, value as Int)
                    } else {
                        intent.putExtra(key, value as Double)
                    }
                }
                "boolean" -> intent.putExtra(key, value as Boolean)
                else -> intent.putExtra(key, value.toString())
            }
        }
    }
}

// #endif
```

### 最佳实践和注意事项

#### 1. 线程处理最佳实践

```typescript
// ✅ 正确的异步处理方式
function performAsyncOperation(callback: (result: any) => void): void {
    UTSAndroid.getDispatcher("io").async(function(_) {
        // 耗时操作在IO线程执行
        const result = doLongRunningTask()
        
        // 切换到主线程更新UI
        UTSAndroid.getDispatcher("main").async(function(_) {
            callback(result)
        }, null)
    }, null)
}

// ❌ 错误的做法 - 在主线程执行耗时操作
function badAsyncOperation(): void {
    // 这会阻塞UI线程
    val result = doLongRunningTask()
    updateUI(result)
}
```

#### 2. 权限处理最佳实践

```typescript
function requestCameraPermission(callback: (granted: boolean) => void): void {
    const permissions = ["android.permission.CAMERA"]
    
    if (UTSAndroidHelper.hasPermissions(permissions)) {
        callback(true)
        return
    }
    
    UTSAndroidHelper.requestPermission(permissions, (allGranted, grantedList) => {
        callback(allGranted)
    })
}
```

#### 3. 资源管理最佳实践

```typescript
// ✅ 正确的资源管理
class ResourceHandler {
    private inputStream: java.io.InputStream | null = null
    
    openResource(fileName: string): boolean {
        try {
            this.inputStream = UTSAndroid.getAppContext().getAssets().open(fileName)
            return true
        } catch (error) {
            return false
        }
    }
    
    closeResource(): void {
        try {
            this.inputStream?.close()
            this.inputStream = null
        } catch (error) {
            android.util.Log.e("ResourceHandler", "Failed to close resource: " + error.message)
        }
    }
}
```

这些增强内容基于DCloud官方文档，提供了更完整和实用的Android UTS开发指导。