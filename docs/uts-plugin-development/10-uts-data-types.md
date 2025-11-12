# UTS 数据类型

## 概述

UTS (UniApp TypeScript) 是一种强类型语言，具有完善的类型系统。UTS 的类型系统不仅提供了传统 TypeScript 的类型安全特性，还针对跨平台开发进行了优化，特别是在 Web、Android 和 iOS 平台上的类型转换。

## 基础类型

### boolean

布尔类型表示真或假的值。

```typescript
let a: boolean = true
let b = false // 自动类型推断
```

### number

数字类型，包含整数和浮点数。

```typescript
let a: number = 42
let b: number = 3.14159
let c = 100 // 自动类型推断为 number
```

### string

字符串类型。

```typescript
let s1: string = "hello"
let s2 = "world" // 自动类型推断
let s3: string = `template ${s1} ${s2}` // 模板字符串
```

### any

任意类型，允许赋值为任何类型的值。

```typescript
let notSure: any = 4
notSure = "maybe a string"
notSure = false
```

**注意**: 尽量避免使用 `any` 类型，因为它会丢失类型检查的优势。

### null

空值类型。

```typescript
let value: string | null = "abc"
value = null // 通过联合类型允许 null
```

## 平台特定数字类型

### Kotlin 数字类型

在 Android 平台上，UTS 支持 Kotlin 的原生数字类型：

```typescript
// 对应 Kotlin 的数字类型
let byteValue: Byte = 127
let shortValue: Short = 32767
let intValue: Int = 2147483647
let longValue: Long = 9223372036854775807
let floatValue: Float = 3.14
let doubleValue: Double = 3.141592653589793
```

### Swift 数字类型

在 iOS 平台上，UTS 支持 Swift 的原生数字类型：

```typescript
// 对应 Swift 的数字类型
let int8Value: Int8 = 127
let int16Value: Int16 = 32767
let int32Value: Int32 = 2147483647
let int64Value: Int64 = 9223372036854775807
let floatValue: Float = 3.14
let doubleValue: Double = 3.141592653589793
```

## 高级类型

### 联合类型

联合类型表示一个值可以是几种类型中的一种。

```typescript
type StringOrNumber = string | number
type Direction = 'up' | 'down' | 'left' | 'right'

function move(direction: Direction) {
    console.log(`Moving ${direction}`)
}

move('up') // 正确
// move('diagonal') // 错误：不在允许的值中
```

### 类型窄化

通过条件判断缩小类型范围。

```typescript
function padLeft(padding: number | string, input: string): string {
    if (typeof padding === "number") {
        // 在这个分支中，padding 被窄化为 number 类型
        return " ".repeat(padding) + input
    }
    // 在这个分支中，padding 被窄化为 string 类型
    return padding + input
}
```

### 自定义类型

使用 `type` 关键字定义自定义类型。

```typescript
type PersonType = {
    id: number
    name: string
    age: number | null
    email?: string // 可选属性
}

let person: PersonType = {
    id: 1,
    name: "张三",
    age: 30
}
```

### 接口类型

使用 `interface` 定义对象的结构。

```typescript
interface User {
    readonly id: number // 只读属性
    name: string
    age?: number // 可选属性
    greet(): string // 方法
}

class UserImpl implements User {
    readonly id: number = 1
    name: string = "用户"
    
    greet(): string {
        return `Hello, ${this.name}`
    }
}
```

## 特殊对象类型

### UTSJSONObject

UTS 中的 JSON 对象类型，用于处理动态数据结构。

```typescript
let config: UTSJSONObject = {
    apiUrl: "https://api.example.com",
    timeout: 5000,
    headers: {
        "Content-Type": "application/json",
        "Authorization": "Bearer token"
    },
    retryCount: 3
}

// 访问属性
let url = config["apiUrl"] as string
let timeout = config["timeout"] as number
```

### Map 类型

键值对映射类型。

```typescript
const userMap: Map<string, User> = new Map()
userMap.set("user1", { id: 1, name: "张三" })
userMap.set("user2", { id: 2, name: "李四" })

// 获取值
let user = userMap.get("user1")
if (user != null) {
    console.log(user.name)
}
```

### Set 类型

唯一值集合类型。

```typescript
const tagSet: Set<string> = new Set()
tagSet.add("javascript")
tagSet.add("typescript")
tagSet.add("uts")

// 检查是否存在
if (tagSet.has("typescript")) {
    console.log("包含 TypeScript 标签")
}
```

### Array 类型

数组类型的定义和操作。

```typescript
// 基础数组
let numbers: number[] = [1, 2, 3, 4, 5]
let names: Array<string> = ["Alice", "Bob", "Charlie"]

// 多维数组
let matrix: number[][] = [
    [1, 2, 3],
    [4, 5, 6],
    [7, 8, 9]
]

// 混合类型数组
let mixed: (string | number)[] = ["hello", 42, "world", 100]
```

## 函数类型

### 函数声明

```typescript
// 普通函数
function add(a: number, b: number): number {
    return a + b
}

// 可选参数
function greet(name: string, title?: string): string {
    return title ? `${title} ${name}` : `Hello, ${name}`
}

// 默认参数
function createUser(name: string, age: number = 18): User {
    return { id: Date.now(), name, age }
}

// 剩余参数
function sum(...numbers: number[]): number {
    return numbers.reduce((total, num) => total + num, 0)
}
```

### 函数类型

```typescript
// 函数类型定义
type MathOperation = (a: number, b: number) => number

let multiply: MathOperation = (x, y) => x * y
let divide: MathOperation = (x, y) => x / y
```

### 回调函数类型

```typescript
// 定义回调函数类型
type EventCallback<T> = (data: T) => void
type ErrorCallback = (error: Error) => void

// 使用回调函数
function fetchData(
    onSuccess: EventCallback<string>,
    onError: ErrorCallback
): void {
    // 模拟异步操作
    setTimeout(() => {
        try {
            onSuccess("数据获取成功")
        } catch (e) {
            onError(new Error("获取数据失败"))
        }
    }, 1000)
}
```

## 类和继承

### 类定义

```typescript
class Animal {
    protected name: string
    private age: number
    
    constructor(name: string, age: number) {
        this.name = name
        this.age = age
    }
    
    public speak(): string {
        return `${this.name} makes a sound`
    }
    
    protected getAge(): number {
        return this.age
    }
}

class Dog extends Animal {
    private breed: string
    
    constructor(name: string, age: number, breed: string) {
        super(name, age)
        this.breed = breed
    }
    
    public speak(): string {
        return `${this.name} barks`
    }
    
    public getInfo(): string {
        return `${this.name} is a ${this.breed}, age ${this.getAge()}`
    }
}
```

### 抽象类

```typescript
abstract class Shape {
    abstract calculateArea(): number
    
    public describe(): string {
        return `This shape has an area of ${this.calculateArea()}`
    }
}

class Circle extends Shape {
    constructor(private radius: number) {
        super()
    }
    
    calculateArea(): number {
        return Math.PI * this.radius * this.radius
    }
}
```

## 泛型

### 基础泛型

```typescript
// 泛型函数
function identity<T>(arg: T): T {
    return arg
}

let stringResult = identity<string>("hello")
let numberResult = identity<number>(42)

// 泛型接口
interface GenericResponse<T> {
    success: boolean
    data: T
    message?: string
}

type UserResponse = GenericResponse<User>
type ListResponse<T> = GenericResponse<T[]>
```

### 约束泛型

```typescript
// 泛型约束
interface Lengthwise {
    length: number
}

function logLength<T extends Lengthwise>(arg: T): T {
    console.log(`Length: ${arg.length}`)
    return arg
}

logLength("hello") // 字符串有 length 属性
logLength([1, 2, 3]) // 数组有 length 属性
// logLength(123) // 错误：数字没有 length 属性
```

### 条件类型

```typescript
// 条件类型
type NonNullable<T> = T extends null | undefined ? never : T

type StringOrNumber = NonNullable<string | null> // string
```

## 类型保护和类型断言

### 类型保护

```typescript
// 用户定义的类型保护
function isString(value: any): value is string {
    return typeof value === "string"
}

function processValue(value: string | number) {
    if (isString(value)) {
        // 在这个分支中，TypeScript 知道 value 是 string
        console.log(value.toUpperCase())
    } else {
        // 在这个分支中，TypeScript 知道 value 是 number
        console.log(value.toFixed(2))
    }
}
```

### 类型断言

```typescript
// 尖括号语法（在 .uts 文件中推荐）
let someValue: any = "this is a string"
let strLength: number = (<string>someValue).length

// as 语法
let strLength2: number = (someValue as string).length

// 非空断言操作符
let user: User | null = getUser()
let userName: string = user!.name // 断言 user 不为 null
```

## 模块和命名空间

### 模块导出导入

```typescript
// math.uts - 导出模块
export function add(a: number, b: number): number {
    return a + b
}

export function subtract(a: number, b: number): number {
    return a - b
}

export default class Calculator {
    multiply(a: number, b: number): number {
        return a * b
    }
}

// main.uts - 导入模块
import Calculator, { add, subtract } from './math'

let calc = new Calculator()
let result1 = add(5, 3)
let result2 = calc.multiply(4, 7)
```

### 命名空间

```typescript
namespace Validation {
    export interface StringValidator {
        isAcceptable(s: string): boolean
    }
    
    export class LettersOnlyValidator implements StringValidator {
        isAcceptable(s: string): boolean {
            return /^[A-Za-z]+$/.test(s)
        }
    }
}

// 使用命名空间
let validator = new Validation.LettersOnlyValidator()
```

## 平台特定注意事项

### Android 平台

```typescript
// Android 特定的类型转换
import { UTSAndroid } from "io.dcloud.uts"

// Kotlin 互操作
let kotlinInt: Int = 42
let utsNumber: number = kotlinInt.toInt()

// 处理 Android 上下文
let context = UTSAndroid.getAppContext()
```

### iOS 平台

```typescript
// iOS 特定的类型转换
import { UTSiOS } from "io.dcloud.uts"

// Swift 互操作
let swiftInt: Int32 = 42
let utsNumber: number = Number(swiftInt)

// 处理 iOS 对象
let view = UTSiOS.getCurrentViewController()
```

## 最佳实践

### 1. 类型安全

```typescript
// 好的做法：使用具体类型
interface ApiResponse {
    status: 'success' | 'error'
    data?: any
    error?: string
}

// 更好的做法：使用泛型
interface ApiResponse<T> {
    status: 'success' | 'error'
    data?: T
    error?: string
}
```

### 2. 空值处理

```typescript
// 好的做法：明确处理 null/undefined
function processUser(user: User | null): string {
    if (user == null) {
        return "未知用户"
    }
    return user.name
}

// 使用可选链
function getUserEmail(user?: User): string | undefined {
    return user?.email
}
```

### 3. 错误处理

```typescript
// 使用 Result 类型模式
type Result<T, E = Error> = {
    success: true
    data: T
} | {
    success: false
    error: E
}

function parseNumber(input: string): Result<number> {
    const num = Number(input)
    if (isNaN(num)) {
        return { success: false, error: new Error("Invalid number") }
    }
    return { success: true, data: num }
}
```

### 4. 性能考虑

```typescript
// 避免过度使用 any
let data: any = fetchData() // 不好

// 使用具体类型或泛型
interface FetchResult {
    items: Item[]
    total: number
}
let data: FetchResult = fetchData() // 好
```

## 总结

UTS 的类型系统结合了 TypeScript 的类型安全特性和跨平台开发的实际需求。合理使用类型系统不仅可以提高代码的可维护性和可读性，还能在编译时捕获潜在的错误，提高应用的稳定性。

在开发 UniApp 插件时，特别是涉及到原生平台交互的场景，正确的类型定义和类型转换是确保功能正常运行的关键。
## 类型判断注意事项（重要）

### typeof 与数组/对象

在 UTS（以及 JS）中，`typeof` 对数组返回 `"object"`：

```ts
const a1 = ["uni-app", "uniCloud", "HBuilder"]
console.log(typeof a1) // "object"

// 判定数组类型请使用：
console.log(Array.isArray(a1))      // true
console.log(a1 instanceof Array)    // true
```

### UTSJSONObject 判定

部分 API 返回通用 JSON 对象（UTSJSONObject），可结合断言与 `instanceof` 进行判定：

```ts
uni.request({
  url: "https://api.example",
  success: (data) => {
    const result = data.data as UTSJSONObject
    console.log(result instanceof UTSJSONObject) // true
  }
})
```

### Android 强类型注意

当平台 API 明确要求特定数字类型时（如 Android `Service.onStartCommand` 的 Int 参数），需使用平台要求的精确类型，而非 `Number`。详细示例见《Android平台UTS开发增强指南》（07 章）。
