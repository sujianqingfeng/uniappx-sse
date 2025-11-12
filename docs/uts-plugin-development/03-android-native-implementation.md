# Android原生实现指南

## 概述

Android平台的UTS组件实现基于Android原生View系统，通过UTS语言调用Android SDK提供原生功能。本章详细介绍如何开发UTS组件的Android原生实现。

## 基础架构

### 项目结构

```
utssdk/app-android/
├── index.uts                 # 主实现文件
├── config.json               # Android平台配置
├── res/                      # Android资源文件
│   ├── layout/               # 布局文件
│   ├── values/               # 值资源
│   ├── drawable/             # 图片资源
│   └── raw/                  # 原始资源
└── libs/                     # 第三方库文件
    ├── xxx.jar               # Java库
    └── xxx.aar               # Android库
```

### config.json配置

```json
{
  "minSdkVersion": 21,
  "compileSdkVersion": 34,
  "targetSdkVersion": 34,
  "dependencies": [
    "androidx.appcompat:appcompat:1.6.1",
    "androidx.recyclerview:recyclerview:1.3.0",
    "com.google.android.material:material:1.9.0"
  ],
  "abis": ["armeabi-v7a", "arm64-v8a"],
  "project": {
    "repositories": [
      "maven { url 'https://jitpack.io' }",
      "maven { url 'https://maven.google.com/' }"
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
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
    
    <!-- 摄像头权限 -->
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-feature android:name="android.hardware.camera" android:required="false" />
    
    <!-- 录音权限 -->
    <uses-permission android:name="android.permission.RECORD_AUDIO" />
    
</manifest>
```

### 动态权限申请

对于Android 6.0+的危险权限，需要在代码中动态申请：

```typescript
// 申请权限示例
function requestPermissions(): void {
    const permissions = ["android.permission.CAMERA"]
    
    // 检查权限
    if (UTSAndroid.checkSystemPermissions(UTSAndroid.getUniActivity()!, permissions)) {
        // 权限已获取，执行相关操作
        startCamera()
    } else {
        // 请求权限
        UTSAndroid.requestSystemPermission(UTSAndroid.getUniActivity()!, permissions, function(allGranted: boolean, grantedList: Array<string>) {
            if (allGranted) {
                startCamera()
            } else {
                console.log('权限被拒绝')
            }
        }, function(doNotAskAgain: boolean) {
            console.log('权限被永久拒绝')
        })
    }
}
```

## 核心实现

### 基本组件类结构

```typescript
import View from "android.view.View"
import ViewGroup from "android.view.ViewGroup"
import Context from "android.content.Context"
import AttributeSet from "android.util.AttributeSet"
import UniNativeViewElement from "io.dcloud.uniapp.framework.UniNativeViewElement"

/**
 * Android原生组件主类
 */
export class YourNativeComponent {
    // 原生视图实例
    private nativeView: View | null = null
    private context: Context | null = null
    private element: UniNativeViewElement | null = null
    
    // 组件属性
    private _text: string = ""
    private _color: string = "#000000"
    private _size: number = 16
    private _disabled: boolean = false
    
    // 事件监听器
    private eventListeners: Map<string, Array<(event: any) => void>> = new Map()
    
    /**
     * 构造函数
     */
    constructor() {
        // 初始化组件
        this.initializeComponent()
    }
    
    /**
     * 绑定原生视图元素
     */
    bindView(element: UniNativeViewElement): void {
        this.element = element
        this.context = element.getUniActivity()
        
        // 创建原生视图
        this.createNativeView()
        
        // 设置视图到元素
        if (this.nativeView != null) {
            element.setNativeView(this.nativeView!)
        }
        
        // 初始化事件监听
        this.setupEventListeners()
    }
    
    /**
     * 创建原生视图
     */
    private createNativeView(): void {
        if (this.context == null) return
        
        // 创建自定义视图
        this.nativeView = new CustomNativeView(this.context!, this)
        
        // 设置初始属性
        this.applyInitialProperties()
    }
    
    /**
     * 应用初始属性
     */
    private applyInitialProperties(): void {
        if (this.nativeView == null) return
        
        // 应用文本
        this.updateText()
        
        // 应用颜色
        this.updateColor()
        
        // 应用大小
        this.updateSize()
        
        // 应用禁用状态
        this.updateDisabledState()
    }
    
    // === 属性设置方法 ===
    
    /**
     * 设置文本
     */
    setText(text: string): void {
        this._text = text
        this.updateText()
    }
    
    private updateText(): void {
        if (this.nativeView != null && this.nativeView instanceof CustomNativeView) {
            (this.nativeView as CustomNativeView).updateText(this._text)
        }
    }
    
    /**
     * 设置颜色
     */
    setColor(color: string): void {
        this._color = color
        this.updateColor()
    }
    
    private updateColor(): void {
        if (this.nativeView != null && this.nativeView instanceof CustomNativeView) {
            const colorInt = android.graphics.Color.parseColor(this._color)
            (this.nativeView as CustomNativeView).updateColor(colorInt)
        }
    }
    
    /**
     * 设置大小
     */
    setSize(size: number): void {
        this._size = size
        this.updateSize()
    }
    
    private updateSize(): void {
        if (this.nativeView != null && this.nativeView instanceof CustomNativeView) {
            (this.nativeView as CustomNativeView).updateSize(this._size)
        }
    }
    
    /**
     * 设置禁用状态
     */
    setDisabled(disabled: boolean): void {
        this._disabled = disabled
        this.updateDisabledState()
    }
    
    private updateDisabledState(): void {
        if (this.nativeView != null) {
            this.nativeView!.setEnabled(!this._disabled)
        }
    }
    
    // === 事件处理 ===
    
    /**
     * 设置事件监听
     */
    private setupEventListeners(): void {
        if (this.nativeView == null || this.element == null) return
        
        // 点击事件
        this.nativeView!.setOnClickListener(new View.OnClickListener() {
            override onClick(view: View): void {
                this.fireEvent("click", {
                    type: "click",
                    target: view
                })
            }
        })
        
        // 长按事件
        this.nativeView!.setOnLongClickListener(new View.OnLongClickListener() {
            override onLongClick(view: View): boolean {
                this.fireEvent("longclick", {
                    type: "longclick",
                    target: view
                })
                return true
            }
        })
    }
    
    /**
     * 触发事件
     */
    private fireEvent(eventType: string, eventData: any): void {
        if (this.element != null) {
            // 发送事件到Vue层
            this.element!.fireEvent(eventType, eventData)
        }
        
        // 调用本地事件监听器
        const listeners = this.eventListeners.get(eventType)
        if (listeners != null) {
            listeners.forEach(listener => {
                try {
                    listener(eventData)
                } catch (error) {
                    console.error("Error in event listener:", error)
                }
            })
        }
    }
    
    /**
     * 添加事件监听器
     */
    addEventListener(eventType: string, listener: (event: any) => void): void {
        let listeners = this.eventListeners.get(eventType)
        if (listeners == null) {
            listeners = []
            this.eventListeners.set(eventType, listeners)
        }
        listeners.push(listener)
    }
    
    /**
     * 移除事件监听器
     */
    removeEventListener(eventType: string, listener: (event: any) => void): void {
        const listeners = this.eventListeners.get(eventType)
        if (listeners != null) {
            const index = listeners.indexOf(listener)
            if (index >= 0) {
                listeners.splice(index, 1)
            }
        }
    }
    
    // === 公开方法 ===
    
    /**
     * 获取当前状态
     */
    getState(): any {
        return {
            text: this._text,
            color: this._color,
            size: this._size,
            disabled: this._disabled
        }
    }
    
    /**
     * 执行动画
     */
    animateToState(targetState: any, duration: number = 300): void {
        if (this.nativeView == null) return
        
        // 创建动画
        const animator = android.animation.ObjectAnimator.ofFloat(
            this.nativeView!,
            "alpha",
            this.nativeView!.getAlpha(),
            targetState.alpha || 1.0
        )
        
        animator.setDuration(duration)
        animator.start()
    }
    
    /**
     * 异步方法示例
     */
    async performAsyncOperation(param: any): Promise<any> {
        return new Promise((resolve, reject) => {
            // 模拟异步操作
            setTimeout(() => {
                try {
                    const result = this.processParam(param)
                    resolve(result)
                } catch (error) {
                    reject(error)
                }
            }, 1000)
        })
    }
    
    private processParam(param: any): any {
        // 处理参数并返回结果
        return {
            processed: true,
            originalParam: param,
            timestamp: Date.now()
        }
    }
    
    /**
     * 销毁组件
     */
    destroy(): void {
        // 清理事件监听器
        this.eventListeners.clear()
        
        // 清理原生视图
        if (this.nativeView != null && this.nativeView instanceof CustomNativeView) {
            (this.nativeView as CustomNativeView).cleanup()
        }
        
        // 重置引用
        this.nativeView = null
        this.context = null
        this.element = null
    }
    
    /**
     * 初始化组件
     */
    private initializeComponent(): void {
        // 组件初始化逻辑
        console.log("YourNativeComponent initialized")
    }
}
```

### 自定义原生视图

```typescript
import View from "android.view.View"
import Canvas from "android.graphics.Canvas"
import Paint from "android.graphics.Paint"
import Context from "android.content.Context"
import AttributeSet from "android.util.AttributeSet"
import Rect from "android.graphics.Rect"
import RectF from "android.graphics.RectF"

/**
 * 自定义原生视图类
 */
class CustomNativeView extends View {
    
    private component: YourNativeComponent
    private paint: Paint
    private textPaint: Paint
    
    // 视图属性
    private text: string = ""
    private textColor: number = android.graphics.Color.BLACK
    private textSize: number = 16
    private backgroundColor: number = android.graphics.Color.TRANSPARENT
    
    // 布局属性
    private contentRect: Rect = new Rect()
    private textBounds: Rect = new Rect()
    
    constructor(context: Context, component: YourNativeComponent) {
        super(context)
        this.component = component
        this.initializePaints()
        this.setupView()
    }
    
    /**
     * 初始化画笔
     */
    private initializePaints(): void {
        // 背景画笔
        this.paint = new Paint(Paint.ANTI_ALIAS_FLAG)
        this.paint.setStyle(Paint.Style.FILL)
        
        // 文本画笔
        this.textPaint = new Paint(Paint.ANTI_ALIAS_FLAG)
        this.textPaint.setTextAlign(Paint.Align.CENTER)
        this.textPaint.setTextSize(this.dp2px(this.textSize))
        this.textPaint.setColor(this.textColor)
    }
    
    /**
     * 设置视图属性
     */
    private setupView(): void {
        this.setClickable(true)
        this.setFocusable(true)
        this.setBackgroundColor(this.backgroundColor)
    }
    
    /**
     * 测量视图大小
     */
    override onMeasure(widthMeasureSpec: number, heightMeasureSpec: number): void {
        const widthMode = View.MeasureSpec.getMode(widthMeasureSpec)
        const widthSize = View.MeasureSpec.getSize(widthMeasureSpec)
        const heightMode = View.MeasureSpec.getMode(heightMeasureSpec)
        const heightSize = View.MeasureSpec.getSize(heightMeasureSpec)
        
        let width: number
        let height: number
        
        // 计算文本尺寸
        if (this.text.length > 0) {
            this.textPaint.getTextBounds(this.text, 0, this.text.length, this.textBounds)
        }
        
        // 计算宽度
        if (widthMode == View.MeasureSpec.EXACTLY) {
            width = widthSize
        } else {
            width = this.textBounds.width() + this.getPaddingLeft() + this.getPaddingRight()
            if (widthMode == View.MeasureSpec.AT_MOST) {
                width = Math.min(width, widthSize)
            }
        }
        
        // 计算高度
        if (heightMode == View.MeasureSpec.EXACTLY) {
            height = heightSize
        } else {
            height = this.textBounds.height() + this.getPaddingTop() + this.getPaddingBottom()
            if (heightMode == View.MeasureSpec.AT_MOST) {
                height = Math.min(height, heightSize)
            }
        }
        
        this.setMeasuredDimension(width, height)
    }
    
    /**
     * 布局子视图
     */
    override onLayout(changed: boolean, left: number, top: number, right: number, bottom: number): void {
        super.onLayout(changed, left, top, right, bottom)
        
        // 更新内容区域
        this.contentRect.set(
            this.getPaddingLeft(),
            this.getPaddingTop(),
            right - left - this.getPaddingRight(),
            bottom - top - this.getPaddingBottom()
        )
    }
    
    /**
     * 绘制视图
     */
    override onDraw(canvas: Canvas): void {
        super.onDraw(canvas)
        
        // 绘制背景
        this.drawBackground(canvas)
        
        // 绘制文本
        this.drawText(canvas)
        
        // 绘制其他装饰
        this.drawDecorations(canvas)
    }
    
    /**
     * 绘制背景
     */
    private drawBackground(canvas: Canvas): void {
        if (this.backgroundColor != android.graphics.Color.TRANSPARENT) {
            this.paint.setColor(this.backgroundColor)
            const rect = new RectF(
                0, 0,
                this.getWidth().toFloat(),
                this.getHeight().toFloat()
            )
            canvas.drawRoundRect(rect, 8.0, 8.0, this.paint)
        }
    }
    
    /**
     * 绘制文本
     */
    private drawText(canvas: Canvas): void {
        if (this.text.length > 0) {
            const centerX = this.contentRect.centerX().toFloat()
            const centerY = this.contentRect.centerY().toFloat()
            
            // 计算文本基线
            const fontMetrics = this.textPaint.getFontMetrics()
            const textHeight = fontMetrics.bottom - fontMetrics.top
            const baseline = centerY + textHeight / 2 - fontMetrics.bottom
            
            canvas.drawText(this.text, centerX, baseline, this.textPaint)
        }
    }
    
    /**
     * 绘制装饰
     */
    private drawDecorations(canvas: Canvas): void {
        // 绘制边框或其他装饰元素
        if (!this.isEnabled()) {
            // 绘制禁用状态的遮罩
            this.paint.setColor(android.graphics.Color.argb(128, 128, 128, 128))
            canvas.drawRect(0, 0, this.getWidth(), this.getHeight(), this.paint)
        }
    }
    
    /**
     * 处理触摸事件
     */
    override onTouchEvent(event: android.view.MotionEvent): boolean {
        if (!this.isEnabled()) {
            return false
        }
        
        when (event.getAction()) {
            android.view.MotionEvent.ACTION_DOWN -> {
                this.onTouchDown(event)
                return true
            }
            android.view.MotionEvent.ACTION_UP -> {
                this.onTouchUp(event)
                return true
            }
            android.view.MotionEvent.ACTION_CANCEL -> {
                this.onTouchCancel(event)
                return true
            }
        }
        
        return super.onTouchEvent(event)
    }
    
    private onTouchDown(event: android.view.MotionEvent): void {
        // 触摸按下效果
        this.setAlpha(0.7)
        this.component.fireEvent("touchstart", {
            type: "touchstart",
            x: event.getX(),
            y: event.getY()
        })
    }
    
    private onTouchUp(event: android.view.MotionEvent): void {
        // 恢复正常状态
        this.setAlpha(1.0)
        this.component.fireEvent("touchend", {
            type: "touchend",
            x: event.getX(),
            y: event.getY()
        })
    }
    
    private onTouchCancel(event: android.view.MotionEvent): void {
        // 恢复正常状态
        this.setAlpha(1.0)
        this.component.fireEvent("touchcancel", {
            type: "touchcancel"
        })
    }
    
    // === 属性更新方法 ===
    
    /**
     * 更新文本
     */
    updateText(text: string): void {
        if (this.text != text) {
            this.text = text
            this.requestLayout()
            this.invalidate()
        }
    }
    
    /**
     * 更新颜色
     */
    updateColor(color: number): void {
        if (this.textColor != color) {
            this.textColor = color
            this.textPaint.setColor(color)
            this.invalidate()
        }
    }
    
    /**
     * 更新大小
     */
    updateSize(size: number): void {
        if (this.textSize != size) {
            this.textSize = size
            this.textPaint.setTextSize(this.dp2px(size))
            this.requestLayout()
            this.invalidate()
        }
    }
    
    /**
     * 设置背景颜色
     */
    updateBackgroundColor(color: number): void {
        if (this.backgroundColor != color) {
            this.backgroundColor = color
            this.invalidate()
        }
    }
    
    /**
     * DP转PX
     */
    private dp2px(dp: number): number {
        const density = this.getContext().getResources().getDisplayMetrics().density
        return (dp * density + 0.5).toInt()
    }
    
    /**
     * 清理资源
     */
    cleanup(): void {
        // 清理画笔和其他资源
        this.paint = null
        this.textPaint = null
        this.component = null
    }
}
```

## 高级功能

### 1. 复杂布局管理

```typescript
/**
 * 复杂布局组件
 */
export class ComplexLayoutComponent {
    private containerView: ViewGroup | null = null
    private childViews: Array<View> = []
    
    private createComplexLayout(): void {
        if (this.context == null) return
        
        // 创建容器布局
        const linearLayout = new android.widget.LinearLayout(this.context!)
        linearLayout.setOrientation(android.widget.LinearLayout.VERTICAL)
        
        // 添加标题视图
        const titleView = this.createTitleView()
        const titleParams = new android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        )
        linearLayout.addView(titleView, titleParams)
        
        // 添加内容视图
        const contentView = this.createContentView()
        const contentParams = new android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1.0 // weight
        )
        linearLayout.addView(contentView, contentParams)
        
        // 添加底部按钮
        const buttonView = this.createButtonView()
        const buttonParams = new android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        )
        linearLayout.addView(buttonView, buttonParams)
        
        this.containerView = linearLayout
        this.nativeView = linearLayout
    }
    
    private createTitleView(): View {
        const textView = new android.widget.TextView(this.context!)
        textView.setText("标题")
        textView.setTextSize(18)
        textView.setGravity(android.view.Gravity.CENTER)
        textView.setPadding(16, 16, 16, 8)
        return textView
    }
    
    private createContentView(): View {
        const scrollView = new android.widget.ScrollView(this.context!)
        const textView = new android.widget.TextView(this.context!)
        textView.setText("这是内容区域...")
        textView.setPadding(16, 8, 16, 8)
        scrollView.addView(textView)
        return scrollView
    }
    
    private createButtonView(): View {
        val button = new android.widget.Button(this.context!)
        button.setText("操作按钮")
        button.setOnClickListener(new View.OnClickListener() {
            override onClick(view: View): void {
                this.handleButtonClick()
            }
        })
        return button
    }
    
    private handleButtonClick(): void {
        this.fireEvent("buttonClick", { type: "buttonClick" })
    }
}
```

### 2. 动画系统

```typescript
/**
 * 动画处理类
 */
class AnimationHelper {
    
    /**
     * 淡入动画
     */
    static fadeIn(view: View, duration: number = 300): void {
        view.setAlpha(0)
        view.animate()
            .alpha(1)
            .setDuration(duration)
            .setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator())
            .start()
    }
    
    /**
     * 滑动动画
     */
    static slideInFromLeft(view: View, duration: number = 300): void {
        view.setTranslationX(-view.getWidth())
        view.animate()
            .translationX(0)
            .setDuration(duration)
            .setInterpolator(new android.view.animation.DecelerateInterpolator())
            .start()
    }
    
    /**
     * 缩放动画
     */
    static scaleAnimation(view: View, fromScale: number, toScale: number, duration: number = 300): void {
        view.setScaleX(fromScale)
        view.setScaleY(fromScale)
        view.animate()
            .scaleX(toScale)
            .scaleY(toScale)
            .setDuration(duration)
            .setInterpolator(new android.view.animation.OvershootInterpolator())
            .start()
    }
    
    /**
     * 组合动画
     */
    static createComplexAnimation(view: View): android.animation.AnimatorSet {
        // 创建多个动画
        const fadeIn = android.animation.ObjectAnimator.ofFloat(view, "alpha", 0, 1)
        val scaleX = android.animation.ObjectAnimator.ofFloat(view, "scaleX", 0.5, 1)
        const scaleY = android.animation.ObjectAnimator.ofFloat(view, "scaleY", 0.5, 1)
        val rotation = android.animation.ObjectAnimator.ofFloat(view, "rotation", 0, 360)
        
        // 创建动画集合
        const animatorSet = new android.animation.AnimatorSet()
        animatorSet.playTogether(fadeIn, scaleX, scaleY)
        animatorSet.play(rotation).after(animatorSet)
        
        return animatorSet
    }
}
```

### 3. 网络请求处理

```typescript
import OkHttpClient from "okhttp3.OkHttpClient"
import Request from "okhttp3.Request"
import Response from "okhttp3.Response"
import Call from "okhttp3.Call"
import Callback from "okhttp3.Callback"

/**
 * 网络请求工具类
 */
class NetworkHelper {
    private static client: OkHttpClient = new OkHttpClient()
    
    /**
     * GET请求
     */
    static async get(url: string): Promise<string> {
        return new Promise((resolve, reject) => {
            const request = new Request.Builder()
                .url(url)
                .get()
                .build()
            
            this.client.newCall(request).enqueue(new Callback() {
                override onResponse(call: Call, response: Response): void {
                    try {
                        if (response.isSuccessful()) {
                            const body = response.body()?.string()
                            resolve(body || "")
                        } else {
                            reject(new Error(`HTTP ${response.code()}: ${response.message()}`))
                        }
                    } catch (error) {
                        reject(error)
                    } finally {
                        response.close()
                    }
                }
                
                override onFailure(call: Call, e: java.io.IOException): void {
                    reject(e)
                }
            })
        })
    }
    
    /**
     * POST请求
     */
    static async post(url: string, jsonData: string): Promise<string> {
        return new Promise((resolve, reject) => {
            const mediaType = okhttp3.MediaType.parse("application/json; charset=utf-8")
            const body = okhttp3.RequestBody.create(mediaType, jsonData)
            
            const request = new Request.Builder()
                .url(url)
                .post(body)
                .build()
            
            this.client.newCall(request).enqueue(new Callback() {
                override onResponse(call: Call, response: Response): void {
                    try {
                        if (response.isSuccessful()) {
                            const responseBody = response.body()?.string()
                            resolve(responseBody || "")
                        } else {
                            reject(new Error(`HTTP ${response.code()}: ${response.message()}`))
                        }
                    } catch (error) {
                        reject(error)
                    } finally {
                        response.close()
                    }
                }
                
                override onFailure(call: Call, e: java.io.IOException): void {
                    reject(e)
                }
            })
        })
    }
}
```

### 4. 数据存储

```typescript
import SharedPreferences from "android.content.SharedPreferences"
import Context from "android.content.Context"

/**
 * 数据存储工具类
 */
class StorageHelper {
    private static readonly PREF_NAME = "YourComponentPrefs"
    
    /**
     * 保存字符串数据
     */
    static saveString(context: Context, key: string, value: string): void {
        const prefs = context.getSharedPreferences(this.PREF_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putString(key, value)
        editor.apply()
    }
    
    /**
     * 获取字符串数据
     */
    static getString(context: Context, key: string, defaultValue: string = ""): string {
        const prefs = context.getSharedPreferences(this.PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(key, defaultValue) || defaultValue
    }
    
    /**
     * 保存对象数据（JSON）
     */
    static saveObject(context: Context, key: string, obj: any): void {
        const jsonString = JSON.stringify(obj)
        this.saveString(context, key, jsonString)
    }
    
    /**
     * 获取对象数据
     */
    static getObject<T>(context: Context, key: string, defaultValue: T): T {
        const jsonString = this.getString(context, key)
        if (jsonString.length > 0) {
            try {
                return JSON.parse(jsonString) as T
            } catch (error) {
                console.error("Error parsing stored object:", error)
            }
        }
        return defaultValue
    }
    
    /**
     * 删除数据
     */
    static remove(context: Context, key: string): void {
        const prefs = context.getSharedPreferences(this.PREF_NAME, Context.MODE_PRIVATE)
        const editor = prefs.edit()
        editor.remove(key)
        editor.apply()
    }
    
    /**
     * 清空所有数据
     */
    static clear(context: Context): void {
        const prefs = context.getSharedPreferences(this.PREF_NAME, Context.MODE_PRIVATE)
        const editor = prefs.edit()
        editor.clear()
        editor.apply()
    }
}
```

## 性能优化

### 1. 视图复用

```typescript
/**
 * 视图复用池
 */
class ViewPool {
    private static pools: Map<string, Array<View>> = new Map()
    
    /**
     * 获取复用视图
     */
    static getView(context: Context, type: string, creator: () => View): View {
        let pool = this.pools.get(type)
        if (pool == null) {
            pool = []
            this.pools.set(type, pool)
        }
        
        if (pool.length > 0) {
            return pool.pop()!
        } else {
            return creator()
        }
    }
    
    /**
     * 回收视图
     */
    static recycleView(type: string, view: View): void {
        // 重置视图状态
        this.resetView(view)
        
        let pool = this.pools.get(type)
        if (pool == null) {
            pool = []
            this.pools.set(type, pool)
        }
        
        if (pool.length < 10) { // 限制池大小
            pool.push(view)
        }
    }
    
    private static resetView(view: View): void {
        view.setVisibility(View.VISIBLE)
        view.setAlpha(1.0)
        view.setScaleX(1.0)
        view.setScaleY(1.0)
        view.setTranslationX(0)
        view.setTranslationY(0)
        view.setRotation(0)
    }
}
```

### 2. 内存管理

```typescript
/**
 * 内存管理助手
 */
class MemoryManager {
    private static weakReferences: Array<java.lang.ref.WeakReference<any>> = []
    
    /**
     * 添加弱引用跟踪
     */
    static track(object: any): void {
        val weakRef = new java.lang.ref.WeakReference(object)
        this.weakReferences.push(weakRef)
    }
    
    /**
     * 清理无效引用
     */
    static cleanup(): void {
        this.weakReferences = this.weakReferences.filter(ref => ref.get() != null)
    }
    
    /**
     * 获取内存使用情况
     */
    static getMemoryInfo(): any {
        const runtime = java.lang.Runtime.getRuntime()
        return {
            totalMemory: runtime.totalMemory(),
            freeMemory: runtime.freeMemory(),
            maxMemory: runtime.maxMemory(),
            usedMemory: runtime.totalMemory() - runtime.freeMemory()
        }
    }
    
    /**
     * 建议垃圾回收
     */
    static suggestGC(): void {
        java.lang.System.gc()
    }
}
```

## 调试和测试

### 1. 日志工具

```typescript
/**
 * 日志工具类
 */
class Logger {
    private static readonly TAG = "YourNativeComponent"
    private static enabled = true
    
    static debug(message: string, ...args: any[]): void {
        if (this.enabled) {
            android.util.Log.d(this.TAG, this.formatMessage(message, args))
        }
    }
    
    static info(message: string, ...args: any[]): void {
        if (this.enabled) {
            android.util.Log.i(this.TAG, this.formatMessage(message, args))
        }
    }
    
    static warn(message: string, ...args: any[]): void {
        if (this.enabled) {
            android.util.Log.w(this.TAG, this.formatMessage(message, args))
        }
    }
    
    static error(message: string, error?: Error, ...args: any[]): void {
        if (this.enabled) {
            const formattedMessage = this.formatMessage(message, args)
            if (error) {
                android.util.Log.e(this.TAG, formattedMessage, error)
            } else {
                android.util.Log.e(this.TAG, formattedMessage)
            }
        }
    }
    
    private static formatMessage(message: string, args: any[]): string {
        let formatted = message
        args.forEach((arg, index) => {
            formatted = formatted.replace(`{${index}}`, String(arg))
        })
        return formatted
    }
    
    static setEnabled(enabled: boolean): void {
        this.enabled = enabled
    }
}
```

### 2. 性能监控

```typescript
/**
 * 性能监控类
 */
class PerformanceMonitor {
    private static measurements: Map<string, number> = new Map()
    
    /**
     * 开始测量
     */
    static start(name: string): void {
        this.measurements.set(name, java.lang.System.currentTimeMillis())
    }
    
    /**
     * 结束测量并返回耗时
     */
    static end(name: string): number {
        const startTime = this.measurements.get(name)
        if (startTime == null) {
            Logger.warn("No start time found for measurement: {0}", name)
            return 0
        }
        
        const endTime = java.lang.System.currentTimeMillis()
        const duration = endTime - startTime
        
        Logger.debug("Performance [{0}]: {1}ms", name, duration)
        this.measurements.delete(name)
        
        return duration
    }
    
    /**
     * 监控方法执行时间
     */
    static measure<T>(name: string, fn: () => T): T {
        this.start(name)
        try {
            return fn()
        } finally {
            this.end(name)
        }
    }
}
```

## 最佳实践

1. **合理使用线程**：UI操作在主线程，耗时操作在后台线程
2. **及时释放资源**：避免内存泄漏和资源浪费
3. **异常处理**：妥善处理可能的异常情况
4. **性能优化**：减少不必要的视图刷新和内存分配
5. **兼容性考虑**：考虑不同Android版本的差异

通过遵循这些指导原则，你可以开发出高质量的Android原生UTS组件。