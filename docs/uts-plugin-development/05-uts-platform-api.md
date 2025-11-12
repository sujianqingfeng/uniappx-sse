# UTS平台API调用指南

## 概述

UTS作为跨平台语言的核心优势之一是能够直接调用各个平台的原生API。本章详细介绍如何在UTS中调用Android、iOS、Web等平台的原生API。

## 平台API导入机制

### 导入语法

UTS使用ES6模块语法导入平台原生API：

```typescript
// 基本导入语法
import ClassName from "package.name.ClassName"
import { Method, Property } from "package.name"

// 导入整个模块
import * as Module from "package.name"

// 带别名导入
import { LongClassName as ShortName } from "package.name.LongClassName"
```

### 条件编译导入

```typescript
// 条件编译导入 - 确保只在对应平台编译
// #ifdef APP-ANDROID
import Context from "android.content.Context"
import View from "android.view.View"
import Intent from "android.content.Intent"
import Bundle from "android.os.Bundle"
// #endif

// #ifdef APP-IOS
import UIView from "UIKit.UIView"
import UIColor from "UIKit.UIColor"
import NSString from "Foundation.NSString"
import UIViewController from "UIKit.UIViewController"
// #endif

// #ifdef WEB
// Web平台使用标准的Web API
declare let window: Window
declare let document: Document
// #endif

// #ifdef MP-WEIXIN
// 微信小程序API
declare let wx: any
// #endif
```

## Android平台API调用

### 基础Android API

```typescript
// #ifdef APP-ANDROID
import Context from "android.content.Context"
import Intent from "android.content.Intent"
import Bundle from "android.os.Bundle"
import Toast from "android.widget.Toast"
import Log from "android.util.Log"
import Color from "android.graphics.Color"
import Build from "android.os.Build"

export class AndroidAPIHelper {
    
    /**
     * 获取应用上下文
     */
    static getContext(): Context {
        // 通过uni-app获取Android上下文
        const context = UTSAndroid.getAppContext() as Context
        return context
    }
    
    /**
     * 显示Toast消息
     */
    static showToast(message: string, duration: number = Toast.LENGTH_SHORT): void {
        const context = this.getContext()
        Toast.makeText(context, message, duration).show()
    }
    
    /**
     * 获取设备信息
     */
    static getDeviceInfo(): any {
        return {
            brand: Build.BRAND,
            model: Build.MODEL,
            version: Build.VERSION.RELEASE,
            sdk: Build.VERSION.SDK_INT,
            manufacturer: Build.MANUFACTURER
        }
    }
    
    /**
     * 启动Activity
     */
    static startActivity(className: string, extras?: any): void {
        const context = this.getContext()
        const intent = new Intent()
        
        try {
            const cls = Class.forName(className)
            intent.setClass(context, cls)
            
            // 添加额外数据
            if (extras) {
                const bundle = new Bundle()
                for (const key in extras) {
                    const value = extras[key]
                    if (typeof value === 'string') {
                        bundle.putString(key, value)
                    } else if (typeof value === 'number') {
                        bundle.putInt(key, value)
                    } else if (typeof value === 'boolean') {
                        bundle.putBoolean(key, value)
                    }
                }
                intent.putExtras(bundle)
            }
            
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            
        } catch (error) {
            Log.e("UTS", "Failed to start activity: " + error.message)
        }
    }
    
    /**
     * 检查权限
     */
    static hasPermission(permission: string): boolean {
        const context = this.getContext()
        const result = context.checkSelfPermission(permission)
        return result == android.content.pm.PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * 获取屏幕尺寸
     */
    static getScreenSize(): { width: number, height: number, density: number } {
        const context = this.getContext()
        const resources = context.getResources()
        const metrics = resources.getDisplayMetrics()
        
        return {
            width: metrics.widthPixels,
            height: metrics.heightPixels,
            density: metrics.density
        }
    }
}

// 文件操作示例
import File from "java.io.File"
import FileInputStream from "java.io.FileInputStream"
import FileOutputStream from "java.io.FileOutputStream"

export class AndroidFileHelper {
    
    /**
     * 读取文件内容
     */
    static readTextFile(filePath: string): string | null {
        try {
            const file = new File(filePath)
            if (!file.exists()) {
                return null
            }
            
            const inputStream = new FileInputStream(file)
            const bytes = new Array<number>(file.length().toInt())
            inputStream.read(bytes)
            inputStream.close()
            
            return new java.lang.String(bytes, "UTF-8").toString()
            
        } catch (error) {
            Log.e("UTS", "Failed to read file: " + error.message)
            return null
        }
    }
    
    /**
     * 写入文件内容
     */
    static writeTextFile(filePath: string, content: string): boolean {
        try {
            const file = new File(filePath)
            const parentDir = file.getParentFile()
            
            if (!parentDir.exists()) {
                parentDir.mkdirs()
            }
            
            const outputStream = new FileOutputStream(file)
            const bytes = content.getBytes("UTF-8")
            outputStream.write(bytes)
            outputStream.close()
            
            return true
            
        } catch (error) {
            Log.e("UTS", "Failed to write file: " + error.message)
            return false
        }
    }
    
    /**
     * 获取外部存储目录
     */
    static getExternalStorageDir(): string {
        const context = AndroidAPIHelper.getContext()
        const dir = context.getExternalFilesDir(null)
        return dir?.getAbsolutePath() || ""
    }
}

// 网络请求示例
import URL from "java.net.URL"
import HttpURLConnection from "java.net.HttpURLConnection"
import InputStream from "java.io.InputStream"
import BufferedReader from "java.io.BufferedReader"
import InputStreamReader from "java.io.InputStreamReader"

export class AndroidNetworkHelper {
    
    /**
     * 发送GET请求
     */
    static async sendGetRequest(urlString: string): Promise<string> {
        return new Promise((resolve, reject) => {
            try {
                const url = new URL(urlString)
                const connection = url.openConnection() as HttpURLConnection
                
                connection.setRequestMethod("GET")
                connection.setConnectTimeout(10000)
                connection.setReadTimeout(10000)
                connection.connect()
                
                const responseCode = connection.getResponseCode()
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    const inputStream = connection.getInputStream()
                    const reader = new BufferedReader(new InputStreamReader(inputStream))
                    const response = StringBuilder()
                    let line: string
                    
                    while ((line = reader.readLine()) != null) {
                        response.append(line)
                    }
                    
                    reader.close()
                    inputStream.close()
                    connection.disconnect()
                    
                    resolve(response.toString())
                } else {
                    reject(new Error(`HTTP Error: ${responseCode}`))
                }
                
            } catch (error) {
                reject(error)
            }
        })
    }
}

// #endif
```

### Android视图系统

```typescript
// #ifdef APP-ANDROID
import View from "android.view.View"
import ViewGroup from "android.view.ViewGroup"
import LinearLayout from "android.widget.LinearLayout"
import TextView from "android.widget.TextView"
import Button from "android.widget.Button"
import ImageView from "android.widget.ImageView"

export class AndroidViewHelper {
    
    /**
     * 创建线性布局
     */
    static createLinearLayout(context: Context, orientation: number = LinearLayout.VERTICAL): LinearLayout {
        const layout = new LinearLayout(context)
        layout.setOrientation(orientation)
        layout.setLayoutParams(new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))
        return layout
    }
    
    /**
     * 创建文本视图
     */
    static createTextView(context: Context, text: string): TextView {
        const textView = new TextView(context)
        textView.setText(text)
        textView.setTextSize(16)
        textView.setPadding(16, 16, 16, 16)
        return textView
    }
    
    /**
     * 创建按钮
     */
    static createButton(context: Context, text: string, onClick: () => void): Button {
        const button = new Button(context)
        button.setText(text)
        button.setOnClickListener(new View.OnClickListener() {
            override onClick(v: View): void {
                onClick()
            }
        })
        return button
    }
    
    /**
     * 设置视图可见性
     */
    static setViewVisibility(view: View, visible: boolean): void {
        view.setVisibility(visible ? View.VISIBLE : View.GONE)
    }
    
    /**
     * 动画效果
     */
    static fadeInView(view: View, duration: number = 300): void {
        view.setAlpha(0)
        view.setVisibility(View.VISIBLE)
        view.animate()
            .alpha(1)
            .setDuration(duration)
            .start()
    }
    
    /**
     * 测量文本尺寸
     */
    static measureText(text: string, textSize: number): { width: number, height: number } {
        const paint = new android.graphics.Paint()
        paint.setTextSize(textSize)
        
        const bounds = new android.graphics.Rect()
        paint.getTextBounds(text, 0, text.length, bounds)
        
        return {
            width: bounds.width(),
            height: bounds.height()
        }
    }
}

// #endif
```

## iOS平台API调用

### 基础iOS API

```typescript
// #ifdef APP-IOS
import UIView from "UIKit.UIView"
import UILabel from "UIKit.UILabel"
import UIButton from "UIKit.UIButton"
import UIColor from "UIKit.UIColor"
import UIScreen from "UIKit.UIScreen"
import UIDevice from "UIKit.UIDevice"
import NSString from "Foundation.NSString"
import NSUserDefaults from "Foundation.NSUserDefaults"
import UIApplication from "UIKit.UIApplication"

export class IOSAPIHelper {
    
    /**
     * 获取设备信息
     */
    static getDeviceInfo(): any {
        const device = UIDevice.currentDevice()
        const screen = UIScreen.mainScreen()
        
        return {
            model: device.model,
            systemName: device.systemName,
            systemVersion: device.systemVersion,
            name: device.name,
            screenWidth: screen.bounds.size.width,
            screenHeight: screen.bounds.size.height,
            scale: screen.scale
        }
    }
    
    /**
     * 显示系统提示
     */
    static showAlert(title: string, message: string): void {
        const alert = UIAlertController.alertControllerWithTitle(title, message, UIAlertControllerStyle.alert)
        
        const okAction = UIAlertAction.actionWithTitle("OK", UIAlertActionStyle.default, (action) => {
            // OK按钮点击处理
        })
        
        alert.addAction(okAction)
        
        // 获取根视图控制器并显示
        const rootVC = UIApplication.sharedApplication().keyWindow?.rootViewController
        rootVC?.presentViewController(alert, true, null)
    }
    
    /**
     * 用户偏好设置操作
     */
    static setUserDefault(key: string, value: any): void {
        const defaults = NSUserDefaults.standardUserDefaults()
        
        if (typeof value === 'string') {
            defaults.setObjectForKey(value, key)
        } else if (typeof value === 'number') {
            defaults.setFloatForKey(value, key)
        } else if (typeof value === 'boolean') {
            defaults.setBoolForKey(value, key)
        }
        
        defaults.synchronize()
    }
    
    static getUserDefault(key: string): any {
        const defaults = NSUserDefaults.standardUserDefaults()
        return defaults.objectForKey(key)
    }
    
    /**
     * 应用状态管理
     */
    static openURL(urlString: string): void {
        const url = NSURL.URLWithString(urlString)
        if (url && UIApplication.sharedApplication().canOpenURL(url)) {
            UIApplication.sharedApplication().openURL(url)
        }
    }
    
    /**
     * 获取应用信息
     */
    static getAppInfo(): any {
        const bundle = NSBundle.mainBundle()
        const infoDictionary = bundle.infoDictionary
        
        return {
            bundleId: bundle.bundleIdentifier,
            version: infoDictionary?.objectForKey("CFBundleShortVersionString"),
            buildNumber: infoDictionary?.objectForKey("CFBundleVersion"),
            displayName: infoDictionary?.objectForKey("CFBundleDisplayName")
        }
    }
}

// iOS视图系统
export class IOSViewHelper {
    
    /**
     * 创建UILabel
     */
    static createLabel(text: string, frame: CGRect): UILabel {
        const label = new UILabel(frame)
        label.text = text
        label.textAlignment = NSTextAlignment.center
        label.textColor = UIColor.blackColor()
        label.font = UIFont.systemFontOfSize(16)
        return label
    }
    
    /**
     * 创建UIButton
     */
    static createButton(title: string, frame: CGRect, target: any, action: string): UIButton {
        const button = UIButton.buttonWithType(UIButtonType.system)
        button.frame = frame
        button.setTitleForState(title, UIControlState.normal)
        button.addTargetActionForControlEvents(target, action, UIControlEvents.touchUpInside)
        return button
    }
    
    /**
     * 创建颜色
     */
    static createColor(red: number, green: number, blue: number, alpha: number = 1.0): UIColor {
        return UIColor.colorWithRed(red/255.0, green/255.0, blue/255.0, alpha)
    }
    
    /**
     * 动画效果
     */
    static animateView(view: UIView, duration: number, animations: () => void): void {
        UIView.animateWithDuration(duration, () => {
            animations()
        })
    }
    
    /**
     * 添加阴影效果
     */
    static addShadow(view: UIView, color: UIColor, offset: CGSize, opacity: number, radius: number): void {
        view.layer.shadowColor = color.CGColor
        view.layer.shadowOffset = offset
        view.layer.shadowOpacity = opacity
        view.layer.shadowRadius = radius
        view.layer.masksToBounds = false
    }
    
    /**
     * 设置圆角
     */
    static setCornerRadius(view: UIView, radius: number): void {
        view.layer.cornerRadius = radius
        view.layer.masksToBounds = true
    }
}

// 文件系统操作
import NSFileManager from "Foundation.NSFileManager"
import NSData from "Foundation.NSData"
import NSURL from "Foundation.NSURL"

export class IOSFileHelper {
    
    /**
     * 获取文档目录
     */
    static getDocumentsDirectory(): string {
        const paths = NSSearchPathForDirectoriesInDomains(
            NSSearchPathDirectory.documentDirectory,
            NSSearchPathDomainMask.userDomainMask,
            true
        )
        return paths.firstObject as string
    }
    
    /**
     * 读取文件
     */
    static readTextFile(filePath: string): string | null {
        const url = NSURL.fileURLWithPath(filePath)
        const data = NSData.dataWithContentsOfURL(url)
        
        if (data) {
            const string = NSString.alloc().initWithDataEncoding(data, NSUTF8StringEncoding)
            return string?.toString() || null
        }
        
        return null
    }
    
    /**
     * 写入文件
     */
    static writeTextFile(filePath: string, content: string): boolean {
        const nsString = NSString.stringWithString(content)
        return nsString.writeToFileAtomicallyEncodingError(filePath, true, NSUTF8StringEncoding, null)
    }
    
    /**
     * 检查文件是否存在
     */
    static fileExists(filePath: string): boolean {
        const fileManager = NSFileManager.defaultManager()
        return fileManager.fileExistsAtPath(filePath)
    }
    
    /**
     * 删除文件
     */
    static deleteFile(filePath: string): boolean {
        const fileManager = NSFileManager.defaultManager()
        return fileManager.removeItemAtPathError(filePath, null)
    }
}

// #endif
```

## Web平台API调用

### Web API封装

```typescript
// #ifdef WEB
export class WebAPIHelper {
    
    /**
     * 本地存储操作
     */
    static setLocalStorage(key: string, value: any): void {
        try {
            const serializedValue = JSON.stringify(value)
            localStorage.setItem(key, serializedValue)
        } catch (error) {
            console.error('Failed to set localStorage:', error)
        }
    }
    
    static getLocalStorage<T>(key: string): T | null {
        try {
            const item = localStorage.getItem(key)
            return item ? JSON.parse(item) : null
        } catch (error) {
            console.error('Failed to get localStorage:', error)
            return null
        }
    }
    
    /**
     * Cookie操作
     */
    static setCookie(name: string, value: string, days: number = 7): void {
        const expires = new Date()
        expires.setTime(expires.getTime() + days * 24 * 60 * 60 * 1000)
        document.cookie = `${name}=${value};expires=${expires.toUTCString()};path=/`
    }
    
    static getCookie(name: string): string | null {
        const nameEQ = name + "="
        const ca = document.cookie.split(';')
        
        for (let i = 0; i < ca.length; i++) {
            let c = ca[i]
            while (c.charAt(0) === ' ') c = c.substring(1, c.length)
            if (c.indexOf(nameEQ) === 0) return c.substring(nameEQ.length, c.length)
        }
        
        return null
    }
    
    /**
     * 网络请求
     */
    static async fetchData(url: string, options?: RequestInit): Promise<any> {
        try {
            const response = await fetch(url, options)
            
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`)
            }
            
            const data = await response.json()
            return data
        } catch (error) {
            console.error('Fetch error:', error)
            throw error
        }
    }
    
    /**
     * 文件上传
     */
    static async uploadFile(url: string, file: File): Promise<any> {
        const formData = new FormData()
        formData.append('file', file)
        
        return this.fetchData(url, {
            method: 'POST',
            body: formData
        })
    }
    
    /**
     * 获取地理位置
     */
    static async getCurrentLocation(): Promise<{ latitude: number, longitude: number }> {
        return new Promise((resolve, reject) => {
            if (!navigator.geolocation) {
                reject(new Error('Geolocation is not supported'))
                return
            }
            
            navigator.geolocation.getCurrentPosition(
                (position) => {
                    resolve({
                        latitude: position.coords.latitude,
                        longitude: position.coords.longitude
                    })
                },
                (error) => {
                    reject(error)
                },
                { enableHighAccuracy: true, timeout: 10000, maximumAge: 600000 }
            )
        })
    }
    
    /**
     * 浏览器信息
     */
    static getBrowserInfo(): any {
        const userAgent = navigator.userAgent
        
        return {
            userAgent: userAgent,
            language: navigator.language,
            platform: navigator.platform,
            cookieEnabled: navigator.cookieEnabled,
            onLine: navigator.onLine,
            screen: {
                width: screen.width,
                height: screen.height,
                availWidth: screen.availWidth,
                availHeight: screen.availHeight
            },
            viewport: {
                width: window.innerWidth,
                height: window.innerHeight
            }
        }
    }
    
    /**
     * DOM操作辅助
     */
    static createElement<K extends keyof HTMLElementTagNameMap>(
        tagName: K, 
        attributes?: Record<string, string>, 
        textContent?: string
    ): HTMLElementTagNameMap[K] {
        const element = document.createElement(tagName)
        
        if (attributes) {
            Object.entries(attributes).forEach(([key, value]) => {
                element.setAttribute(key, value)
            })
        }
        
        if (textContent) {
            element.textContent = textContent
        }
        
        return element
    }
    
    /**
     * 事件处理
     */
    static addEventListener<K extends keyof WindowEventMap>(
        type: K, 
        listener: (event: WindowEventMap[K]) => void, 
        options?: boolean | AddEventListenerOptions
    ): void {
        window.addEventListener(type, listener, options)
    }
    
    static removeEventListener<K extends keyof WindowEventMap>(
        type: K, 
        listener: (event: WindowEventMap[K]) => void, 
        options?: boolean | EventListenerOptions
    ): void {
        window.removeEventListener(type, listener, options)
    }
}

// #endif
```

## 跨平台API抽象

### 统一接口设计

```typescript
// 定义跨平台接口
interface PlatformAPI {
    // 设备信息
    getDeviceInfo(): DeviceInfo
    
    // 存储操作
    setStorage(key: string, value: any): void
    getStorage<T>(key: string): T | null
    
    // 网络请求
    request(options: RequestOptions): Promise<ResponseData>
    
    // 文件操作
    readFile(path: string): Promise<string | null>
    writeFile(path: string, content: string): Promise<boolean>
    
    // 系统交互
    showToast(message: string): void
    showAlert(title: string, message: string): void
}

// 数据类型定义
interface DeviceInfo {
    platform: string
    version: string
    model: string
    screenWidth: number
    screenHeight: number
}

interface RequestOptions {
    url: string
    method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
    headers?: Record<string, string>
    data?: any
    timeout?: number
}

interface ResponseData {
    data: any
    statusCode: number
    headers: Record<string, string>
}

// 平台实现工厂
export class PlatformAPIFactory {
    
    static createAPI(): PlatformAPI {
        // #ifdef APP-ANDROID
        return new AndroidPlatformAPI()
        // #endif
        
        // #ifdef APP-IOS
        return new IOSPlatformAPI()
        // #endif
        
        // #ifdef WEB
        return new WebPlatformAPI()
        // #endif
        
        throw new Error('Unsupported platform')
    }
}

// Android平台实现
// #ifdef APP-ANDROID
class AndroidPlatformAPI implements PlatformAPI {
    
    getDeviceInfo(): DeviceInfo {
        const info = AndroidAPIHelper.getDeviceInfo()
        const screen = AndroidAPIHelper.getScreenSize()
        
        return {
            platform: 'Android',
            version: info.version,
            model: info.model,
            screenWidth: screen.width,
            screenHeight: screen.height
        }
    }
    
    setStorage(key: string, value: any): void {
        const context = AndroidAPIHelper.getContext()
        const sharedPref = context.getSharedPreferences("UTS_Storage", Context.MODE_PRIVATE)
        const editor = sharedPref.edit()
        
        editor.putString(key, JSON.stringify(value))
        editor.apply()
    }
    
    getStorage<T>(key: string): T | null {
        const context = AndroidAPIHelper.getContext()
        const sharedPref = context.getSharedPreferences("UTS_Storage", Context.MODE_PRIVATE)
        const jsonString = sharedPref.getString(key, null)
        
        if (jsonString) {
            try {
                return JSON.parse(jsonString) as T
            } catch {
                return null
            }
        }
        
        return null
    }
    
    async request(options: RequestOptions): Promise<ResponseData> {
        // 使用Android网络请求实现
        const response = await AndroidNetworkHelper.sendGetRequest(options.url)
        return {
            data: JSON.parse(response),
            statusCode: 200,
            headers: {}
        }
    }
    
    async readFile(path: string): Promise<string | null> {
        return AndroidFileHelper.readTextFile(path)
    }
    
    async writeFile(path: string, content: string): Promise<boolean> {
        return AndroidFileHelper.writeTextFile(path, content)
    }
    
    showToast(message: string): void {
        AndroidAPIHelper.showToast(message)
    }
    
    showAlert(title: string, message: string): void {
        // Android Alert实现
        AndroidAPIHelper.showToast(`${title}: ${message}`)
    }
}
// #endif

// iOS平台实现
// #ifdef APP-IOS
class IOSPlatformAPI implements PlatformAPI {
    
    getDeviceInfo(): DeviceInfo {
        const info = IOSAPIHelper.getDeviceInfo()
        
        return {
            platform: 'iOS',
            version: info.systemVersion,
            model: info.model,
            screenWidth: info.screenWidth,
            screenHeight: info.screenHeight
        }
    }
    
    setStorage(key: string, value: any): void {
        IOSAPIHelper.setUserDefault(key, JSON.stringify(value))
    }
    
    getStorage<T>(key: string): T | null {
        const jsonString = IOSAPIHelper.getUserDefault(key) as string
        if (jsonString) {
            try {
                return JSON.parse(jsonString) as T
            } catch {
                return null
            }
        }
        return null
    }
    
    async request(options: RequestOptions): Promise<ResponseData> {
        // iOS网络请求实现
        return {
            data: {},
            statusCode: 200,
            headers: {}
        }
    }
    
    async readFile(path: string): Promise<string | null> {
        return IOSFileHelper.readTextFile(path)
    }
    
    async writeFile(path: string, content: string): Promise<boolean> {
        return IOSFileHelper.writeTextFile(path, content)
    }
    
    showToast(message: string): void {
        // iOS toast实现（可以用alert替代）
        IOSAPIHelper.showAlert("提示", message)
    }
    
    showAlert(title: string, message: string): void {
        IOSAPIHelper.showAlert(title, message)
    }
}
// #endif

// Web平台实现
// #ifdef WEB
class WebPlatformAPI implements PlatformAPI {
    
    getDeviceInfo(): DeviceInfo {
        const info = WebAPIHelper.getBrowserInfo()
        
        return {
            platform: 'Web',
            version: navigator.userAgent,
            model: info.platform,
            screenWidth: info.viewport.width,
            screenHeight: info.viewport.height
        }
    }
    
    setStorage(key: string, value: any): void {
        WebAPIHelper.setLocalStorage(key, value)
    }
    
    getStorage<T>(key: string): T | null {
        return WebAPIHelper.getLocalStorage<T>(key)
    }
    
    async request(options: RequestOptions): Promise<ResponseData> {
        const response = await WebAPIHelper.fetchData(options.url, {
            method: options.method || 'GET',
            headers: options.headers,
            body: options.data ? JSON.stringify(options.data) : undefined
        })
        
        return {
            data: response,
            statusCode: 200,
            headers: {}
        }
    }
    
    async readFile(path: string): Promise<string | null> {
        // Web环境文件读取实现
        return null
    }
    
    async writeFile(path: string, content: string): Promise<boolean> {
        // Web环境文件写入实现
        return false
    }
    
    showToast(message: string): void {
        // 简单的Toast实现
        const toast = document.createElement('div')
        toast.textContent = message
        toast.style.cssText = `
            position: fixed;
            top: 50%;
            left: 50%;
            transform: translate(-50%, -50%);
            background: rgba(0,0,0,0.8);
            color: white;
            padding: 10px 20px;
            border-radius: 5px;
            z-index: 10000;
        `
        
        document.body.appendChild(toast)
        
        setTimeout(() => {
            document.body.removeChild(toast)
        }, 2000)
    }
    
    showAlert(title: string, message: string): void {
        alert(`${title}\n${message}`)
    }
}
// #endif

// 使用示例
export class UniversalApp {
    private platformAPI: PlatformAPI
    
    constructor() {
        this.platformAPI = PlatformAPIFactory.createAPI()
    }
    
    async initialize(): Promise<void> {
        // 获取设备信息
        const deviceInfo = this.platformAPI.getDeviceInfo()
        console.log('Device Info:', deviceInfo)
        
        // 存储用户配置
        this.platformAPI.setStorage('userConfig', {
            theme: 'dark',
            language: 'zh-CN'
        })
        
        // 读取用户配置
        const config = this.platformAPI.getStorage('userConfig')
        console.log('User Config:', config)
        
        // 显示欢迎消息
        this.platformAPI.showToast('应用初始化完成')
    }
    
    async loadData(): Promise<void> {
        try {
            const response = await this.platformAPI.request({
                url: 'https://api.example.com/data',
                method: 'GET'
            })
            
            console.log('API Response:', response.data)
            
        } catch (error) {
            console.error('Failed to load data:', error)
            this.platformAPI.showAlert('错误', '数据加载失败')
        }
    }
}
```

## 最佳实践

### 1. 错误处理

```typescript
// 平台API调用错误处理
export class SafePlatformAPI {
    
    static safeCall<T>(fn: () => T, fallback: T, errorMessage?: string): T {
        try {
            return fn()
        } catch (error) {
            if (errorMessage) {
                console.error(errorMessage, error)
            }
            return fallback
        }
    }
    
    static async safeAsyncCall<T>(
        fn: () => Promise<T>, 
        fallback: T, 
        errorMessage?: string
    ): Promise<T> {
        try {
            return await fn()
        } catch (error) {
            if (errorMessage) {
                console.error(errorMessage, error)
            }
            return fallback
        }
    }
}

// 使用示例
const deviceInfo = SafePlatformAPI.safeCall(
    () => AndroidAPIHelper.getDeviceInfo(),
    { model: 'Unknown', version: 'Unknown' },
    'Failed to get device info'
)
```

### 2. 性能优化

```typescript
// 平台API调用缓存
export class PlatformAPICache {
    private static cache = new Map<string, { value: any, timestamp: number }>()
    private static TTL = 5 * 60 * 1000 // 5分钟缓存
    
    static get<T>(key: string, fetcher: () => T): T {
        const cached = this.cache.get(key)
        const now = Date.now()
        
        if (cached && (now - cached.timestamp) < this.TTL) {
            return cached.value as T
        }
        
        const value = fetcher()
        this.cache.set(key, { value, timestamp: now })
        
        return value
    }
    
    static clear(): void {
        this.cache.clear()
    }
}

// 使用缓存的API调用
const cachedDeviceInfo = PlatformAPICache.get(
    'deviceInfo',
    () => AndroidAPIHelper.getDeviceInfo()
)
```

### 3. 类型安全

```typescript
// 类型安全的平台API调用
type PlatformType = 'android' | 'ios' | 'web'

export class TypeSafePlatformAPI {
    
    static getCurrentPlatform(): PlatformType {
        // #ifdef APP-ANDROID
        return 'android'
        // #endif
        
        // #ifdef APP-IOS
        return 'ios'
        // #endif
        
        // #ifdef WEB
        return 'web'
        // #endif
        
        throw new Error('Unknown platform')
    }
    
    static isPlatform(platform: PlatformType): boolean {
        return this.getCurrentPlatform() === platform
    }
    
    static runOnPlatform<T>(
        platform: PlatformType, 
        fn: () => T, 
        fallback?: () => T
    ): T | undefined {
        if (this.isPlatform(platform)) {
            return fn()
        } else if (fallback) {
            return fallback()
        }
        return undefined
    }
}
```

通过这种方式，UTS能够充分利用各个平台的原生能力，同时保持代码的跨平台兼容性和类型安全。