# UTS语言基础指南

## 概述

UTS (Uni Type Script) 是DCloud为uni-app生态系统开发的跨平台、高性能、强类型的现代编程语言。它能够编译到多个目标平台的原生语言，实现真正的跨平台开发。

### 编译目标

- **Web/小程序**: 编译为JavaScript
- **Android**: 编译为Kotlin
- **iOS**: 编译为Swift
- **HarmonyOS**: 编译为ArkTS

### 核心特性

1. **强类型系统**: 提供编译时类型检查和推断
2. **跨平台兼容**: 统一语法，多平台编译
3. **高性能**: 编译为原生代码，减少运行时开销
4. **TypeScript兼容**: 语法类似TypeScript，学习成本低
5. **原生互操作**: 可直接调用平台原生API

## 数据类型系统

### 基本类型

#### 1. 原始类型

```typescript
// 布尔类型
let isDone: boolean = false
let isActive: boolean = true

// 数字类型 - 支持整数和浮点数
let decimal: number = 6
let hex: number = 0xf00d
let binary: number = 0b1010
let octal: number = 0o744
let float: number = 3.14

// 字符串类型
let color: string = "blue"
let fullName: string = `Bob Bobbington`
let age: number = 37
let sentence: string = `Hello, my name is ${ fullName }.
I'll be ${ age + 1 } years old next month.`

// null 和 undefined
let u: undefined = undefined
let n: null = null

// void 类型 - 通常用于函数无返回值
function warnUser(): void {
    console.log("This is my warning message")
}

// any 类型 - 动态类型，可以是任何值
let notSure: any = 4
notSure = "maybe a string instead"
notSure = false
```

#### 2. 类型推断

UTS支持类型推断，可以自动推导变量类型：

```typescript
// 类型推断
let message = "Hello World"  // 推断为 string 类型
let count = 42              // 推断为 number 类型
let isReady = true          // 推断为 boolean 类型

// 函数返回值推断
function add(a: number, b: number) {  // 返回值推断为 number
    return a + b
}

// 数组类型推断
let numbers = [1, 2, 3, 4, 5]     // 推断为 number[]
let mixed = [1, "hello", true]     // 推断为 (string | number | boolean)[]
```

### 复杂类型

#### 1. 数组类型

```typescript
// 数组声明方式1：类型[]
let list: number[] = [1, 2, 3]
let fruits: string[] = ["apple", "banana", "orange"]

// 数组声明方式2：Array<类型>
let numbers: Array<number> = [1, 2, 3, 4]
let names: Array<string> = ["Alice", "Bob", "Charlie"]

// 多维数组
let matrix: number[][] = [[1, 2], [3, 4]]

// 只读数组
let readonlyList: ReadonlyArray<number> = [1, 2, 3]

// 数组操作
let arr: number[] = []
arr.push(1, 2, 3)
arr.pop()
let length: number = arr.length
let first: number | undefined = arr[0]

// 数组遍历
arr.forEach((item, index) => {
    console.log(`Index ${index}: ${item}`)
})

let doubled = arr.map(x => x * 2)
let filtered = arr.filter(x => x > 1)
```

#### 2. 对象类型

```typescript
// 对象类型声明
interface User {
    name: string
    age: number
    email?: string  // 可选属性
    readonly id: number  // 只读属性
}

let user: User = {
    name: "Alice",
    age: 30,
    id: 1
}

// 内联对象类型
let point: { x: number, y: number } = { x: 10, y: 20 }

// 索引签名
interface StringDictionary {
    [key: string]: string
}

let dict: StringDictionary = {
    "key1": "value1",
    "key2": "value2"
}

// UTSJSONObject - UTS特有的JSON对象类型
let jsonObj: UTSJSONObject = {
    name: "test",
    count: 10,
    active: true,
    data: {
        nested: "value"
    }
}
```

#### 3. 函数类型

```typescript
// 函数声明
function greet(name: string): string {
    return `Hello, ${name}!`
}

// 函数表达式
let add = function(a: number, b: number): number {
    return a + b
}

// 箭头函数
let multiply = (a: number, b: number): number => a * b

// 可选参数
function buildName(firstName: string, lastName?: string): string {
    if (lastName) {
        return firstName + " " + lastName
    } else {
        return firstName
    }
}

// 默认参数
function greetWithDefault(name: string, greeting: string = "Hello"): string {
    return `${greeting}, ${name}!`
}

// 剩余参数
function sum(...numbers: number[]): number {
    return numbers.reduce((total, num) => total + num, 0)
}

// 函数重载
function processValue(value: string): string
function processValue(value: number): number
function processValue(value: string | number): string | number {
    if (typeof value === "string") {
        return value.toUpperCase()
    } else {
        return value * 2
    }
}

// 高阶函数
function createMultiplier(factor: number): (value: number) => number {
    return (value: number) => value * factor
}

let double = createMultiplier(2)
let result = double(5)  // 10
```

#### 4. 类和接口

```typescript
// 接口定义
interface Shape {
    area(): number
    perimeter(): number
}

interface ColoredShape extends Shape {
    color: string
}

// 类实现接口
class Circle implements ColoredShape {
    private radius: number
    color: string
    
    constructor(radius: number, color: string) {
        this.radius = radius
        this.color = color
    }
    
    area(): number {
        return Math.PI * this.radius * this.radius
    }
    
    perimeter(): number {
        return 2 * Math.PI * this.radius
    }
    
    // Getter 和 Setter
    get diameter(): number {
        return this.radius * 2
    }
    
    set diameter(value: number) {
        this.radius = value / 2
    }
    
    // 静态方法
    static createUnit(): Circle {
        return new Circle(1, "white")
    }
}

// 抽象类
abstract class Animal {
    protected name: string
    
    constructor(name: string) {
        this.name = name
    }
    
    abstract makeSound(): void
    
    move(): void {
        console.log(`${this.name} is moving`)
    }
}

class Dog extends Animal {
    makeSound(): void {
        console.log("Woof! Woof!")
    }
    
    fetch(): void {
        console.log(`${this.name} is fetching`)
    }
}

// 类的使用
let myDog = new Dog("Buddy")
myDog.makeSound()
myDog.move()
myDog.fetch()

let circle = new Circle(5, "red")
console.log(`Area: ${circle.area()}`)
console.log(`Perimeter: ${circle.perimeter()}`)
console.log(`Diameter: ${circle.diameter}`)

let unitCircle = Circle.createUnit()
```

### 联合类型和类型守卫

```typescript
// 联合类型
type StringOrNumber = string | number
type Status = "pending" | "completed" | "failed"

let value: StringOrNumber = "hello"
value = 42  // 也可以是数字

let currentStatus: Status = "pending"

// 类型守卫
function processValue(input: string | number): string {
    if (typeof input === "string") {
        // 这里 TypeScript 知道 input 是 string 类型
        return input.toUpperCase()
    } else {
        // 这里 TypeScript 知道 input 是 number 类型
        return input.toString()
    }
}

// 自定义类型守卫
function isString(value: any): value is string {
    return typeof value === "string"
}

function handleValue(value: string | number) {
    if (isString(value)) {
        // value 被推断为 string
        console.log(value.length)
    } else {
        // value 被推断为 number
        console.log(value.toFixed(2))
    }
}

// instanceof 类型守卫
class Dog {
    bark() { console.log("Woof!") }
}

class Cat {
    meow() { console.log("Meow!") }
}

function makeSound(animal: Dog | Cat) {
    if (animal instanceof Dog) {
        animal.bark()
    } else {
        animal.meow()
    }
}
```

### 泛型

```typescript
// 泛型函数
function identity<T>(arg: T): T {
    return arg
}

let result1 = identity<string>("hello")
let result2 = identity<number>(42)
let result3 = identity("world")  // 类型推断

// 泛型接口
interface GenericIdentity<T> {
    (arg: T): T
}

let myIdentity: GenericIdentity<number> = identity

// 泛型类
class GenericNumber<T> {
    zeroValue: T
    add: (x: T, y: T) => T
    
    constructor(zeroValue: T, addFn: (x: T, y: T) => T) {
        this.zeroValue = zeroValue
        this.add = addFn
    }
}

let myGenericNumber = new GenericNumber<number>(0, (x, y) => x + y)

// 泛型约束
interface Lengthwise {
    length: number
}

function loggingIdentity<T extends Lengthwise>(arg: T): T {
    console.log(arg.length)  // 现在知道 arg 有 length 属性
    return arg
}

// 在泛型约束中使用类型参数
function getProperty<T, K extends keyof T>(obj: T, key: K): T[K] {
    return obj[key]
}

let person = { name: "Alice", age: 25, city: "New York" }
let personName = getProperty(person, "name")  // string
let personAge = getProperty(person, "age")    // number

// 条件类型
type MessageType<T> = T extends string ? string : number

type StringMessage = MessageType<string>  // string
type NumberMessage = MessageType<boolean> // number
```

## 平台特定类型

### 条件编译

```typescript
// 条件编译指令
// #ifdef APP-ANDROID
import Context from "android.content.Context"
import View from "android.view.View"
// #endif

// #ifdef APP-IOS
import UIView from "UIKit.UIView"
import NSString from "Foundation.NSString"
// #endif

// 平台特定函数
function getPlatformSpecificData(): any {
    // #ifdef APP-ANDROID
    // Android 特定代码
    return getAndroidData()
    // #endif
    
    // #ifdef APP-IOS
    // iOS 特定代码
    return getIOSData()
    // #endif
    
    // #ifdef WEB
    // Web 特定代码
    return getWebData()
    // #endif
    
    return null
}

// 平台类型别名
// #ifdef APP-ANDROID
type PlatformView = android.view.View
type PlatformString = string
// #endif

// #ifdef APP-IOS
type PlatformView = UIView
type PlatformString = NSString
// #endif

// 使用平台类型
function createPlatformView(): PlatformView {
    // #ifdef APP-ANDROID
    return new android.view.View(getAndroidContext())
    // #endif
    
    // #ifdef APP-IOS
    return new UIView()
    // #endif
}
```

### 原生类型映射

```typescript
// Android 平台类型映射示例
// #ifdef APP-ANDROID
class AndroidHelper {
    static convertToNative(jsString: string): java.lang.String {
        return new java.lang.String(jsString)
    }
    
    static convertFromNative(javaString: java.lang.String): string {
        return javaString.toString()
    }
    
    static createIntent(context: android.content.Context, cls: any): android.content.Intent {
        return new android.content.Intent(context, cls)
    }
}
// #endif

// iOS 平台类型映射示例
// #ifdef APP-IOS
class IOSHelper {
    static convertToNative(jsString: string): NSString {
        return NSString.stringWithString(jsString)
    }
    
    static convertFromNative(nsString: NSString): string {
        return nsString.toString()
    }
    
    static createColor(red: number, green: number, blue: number, alpha: number): UIColor {
        return UIColor.colorWithRed(red, green, blue, alpha)
    }
}
// #endif
```

## 类型检查和安全性

### null 安全

```typescript
// 可空类型
let nullableString: string | null = null
let maybeNumber: number | undefined = undefined

// 安全调用操作符
let length: number | undefined = nullableString?.length
let doubled: number | undefined = maybeNumber && maybeNumber * 2

// null 合并操作符
let defaultValue: string = nullableString ?? "default"
let safeNumber: number = maybeNumber ?? 0

// 非空断言操作符 (谨慎使用)
let definitelyString: string = nullableString!

// 类型保护函数
function isNotNull<T>(value: T | null | undefined): value is T {
    return value !== null && value !== undefined
}

function processValue(input: string | null) {
    if (isNotNull(input)) {
        // 这里 input 被缩小为 string 类型
        console.log(input.toUpperCase())
    }
}
```

### 类型断言

```typescript
// 类型断言
let someValue: any = "this is a string"
let strLength1: number = (someValue as string).length
let strLength2: number = (<string>someValue).length

// 双重断言（谨慎使用）
let someUnknown: unknown = "hello world"
let strValue: string = someUnknown as any as string

// 常量断言
const directions = ["north", "south", "east", "west"] as const
type Direction = typeof directions[number]  // "north" | "south" | "east" | "west"

// 非空断言
function processArray(arr: number[] | undefined) {
    // 确信 arr 不为 undefined 时使用
    console.log(arr!.length)
}
```

## 最佳实践

### 1. 类型声明最佳实践

```typescript
// ✅ 好的实践
// 明确声明函数参数和返回值类型
function calculateArea(width: number, height: number): number {
    return width * height
}

// 使用接口定义复杂对象结构
interface UserConfig {
    readonly id: number
    name: string
    email?: string
    settings: {
        theme: 'light' | 'dark'
        notifications: boolean
    }
}

// 使用联合类型表示枚举值
type HttpMethod = 'GET' | 'POST' | 'PUT' | 'DELETE'

// ❌ 避免的实践
// 避免使用 any (除非必要)
let badExample: any = "could be anything"

// 避免过度使用类型断言
let risky = (unknownValue as any).someProperty
```

### 2. 性能优化建议

```typescript
// 类型守卫优化
function isValidUser(user: any): user is User {
    return user && 
           typeof user.name === 'string' && 
           typeof user.age === 'number'
}

// 缓存类型检查结果
class OptimizedClass {
    private _typeCache = new Map<any, string>()
    
    getType(value: any): string {
        if (this._typeCache.has(value)) {
            return this._typeCache.get(value)!
        }
        
        const type = typeof value
        this._typeCache.set(value, type)
        return type
    }
}

// 使用常量枚举减少运行时开销
const enum Color {
    Red,
    Green,
    Blue
}

let favoriteColor = Color.Red  // 编译时被替换为 0
```

### 3. 错误处理

```typescript
// 结果类型模式
type Result<T, E = Error> = 
    | { success: true; data: T }
    | { success: false; error: E }

function safeParseNumber(input: string): Result<number> {
    const num = parseFloat(input)
    
    if (isNaN(num)) {
        return { 
            success: false, 
            error: new Error(`Invalid number: ${input}`) 
        }
    }
    
    return { success: true, data: num }
}

// 使用结果类型
const parseResult = safeParseNumber("42")
if (parseResult.success) {
    console.log(`Parsed number: ${parseResult.data}`)
} else {
    console.error(`Parse error: ${parseResult.error.message}`)
}

// Option 类型模式
type Option<T> = T | null

function findUser(id: number): Option<User> {
    // 查找逻辑
    return users.find(u => u.id === id) || null
}

// 处理可选值
const user = findUser(123)
if (user !== null) {
    console.log(`Found user: ${user.name}`)
} else {
    console.log("User not found")
}
```

UTS的类型系统设计注重性能和跨平台兼容性，通过强类型约束在编译时捕获错误，同时在运行时提供最佳性能。理解和正确使用这些类型特性是编写高质量UTS代码的关键。