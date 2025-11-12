# Vue组件开发详解

## 概述

Vue组件层是UTS组件的前端接口，负责处理业务逻辑、状态管理、事件处理以及与原生层的通信。本章详细介绍如何开发UTS组件的Vue层。

## 基本结构

### 标准模板结构

```vue
<template>
  <!-- 使用native-view作为原生视图容器 -->
  <native-view 
    @init="onViewInit" 
    @customEvent="handleCustomEvent"
    :prop1="prop1"
    :prop2="prop2">
  </native-view>
</template>

<script setup lang="uts">
// 导入类型和工具
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { NativeComponentClass } from '@/uni_modules/your-component'

// 组件属性定义
interface Props {
  prop1?: string
  prop2?: number
  disabled?: boolean
}

// 组件事件定义
interface Emits {
  change: [value: any]
  click: [event: any]
  customEvent: [data: any]
}

// 定义props和emits
const props = withDefaults(defineProps<Props>(), {
  prop1: 'default value',
  prop2: 0,
  disabled: false
})

const emits = defineEmits<Emits>()

// 组件状态
const nativeInstance = ref<NativeComponentClass | null>(null)
const isReady = ref(false)

// 初始化原生视图
function onViewInit(nativeView: any) {
  // 创建原生组件实例
  nativeInstance.value = new NativeComponentClass()
  nativeInstance.value.bindView(nativeView)
  
  // 设置初始属性
  updateNativeProps()
  
  isReady.value = true
}

// 更新原生属性
function updateNativeProps() {
  if (!nativeInstance.value) return
  
  nativeInstance.value.setProp1(props.prop1)
  nativeInstance.value.setProp2(props.prop2)
  nativeInstance.value.setDisabled(props.disabled)
}

// 监听属性变化
watch(() => props.prop1, (newValue) => {
  nativeInstance.value?.setProp1(newValue)
})

watch(() => props.prop2, (newValue) => {
  nativeInstance.value?.setProp2(newValue)
})

watch(() => props.disabled, (newValue) => {
  nativeInstance.value?.setDisabled(newValue)
})

// 处理原生事件
function handleCustomEvent(event: any) {
  emits('customEvent', event.detail)
}

// 公开方法供外部调用
function publicMethod(param: any) {
  if (!nativeInstance.value || !isReady.value) {
    console.warn('Component not ready')
    return
  }
  
  return nativeInstance.value.callMethod(param)
}

// 生命周期
onMounted(() => {
  // 组件挂载后的初始化
})

onUnmounted(() => {
  // 清理资源
  if (nativeInstance.value) {
    nativeInstance.value.destroy()
    nativeInstance.value = null
  }
})

// 暴露给父组件的方法
defineExpose({
  publicMethod
})
</script>
```

## 核心概念详解

### 1. native-view组件

`native-view` 是连接Vue组件与原生视图的桥梁组件：

#### 基本用法

```vue
<template>
  <native-view 
    @init="onViewInit"
    :style="{ width: '100%', height: '200px' }">
  </native-view>
</template>
```

#### 重要事件

- `@init`：原生视图初始化完成时触发
- `@resize`：视图大小改变时触发
- `@destroy`：视图销毁时触发

#### 传递属性

```vue
<native-view 
  :data="componentData"
  :options="componentOptions"
  @init="onViewInit">
</native-view>
```

### 2. 属性系统

#### Props定义

使用TypeScript接口定义组件属性：

```typescript
interface ComponentProps {
  // 基础属性
  text?: string
  color?: string
  size?: number
  disabled?: boolean
  
  // 复杂属性
  options?: {
    key1: string
    key2: number
  }
  
  // 数组属性
  items?: Array<any>
  
  // 函数属性
  onClick?: (event: any) => void
}
```

#### 属性默认值

```typescript
const props = withDefaults(defineProps<ComponentProps>(), {
  text: '',
  color: '#000000',
  size: 16,
  disabled: false,
  options: () => ({
    key1: 'default',
    key2: 0
  }),
  items: () => []
})
```

#### 属性验证

```typescript
// 自定义验证逻辑
const validateProps = (props: ComponentProps) => {
  if (props.size && (props.size < 10 || props.size > 100)) {
    console.warn('Size should be between 10 and 100')
  }
  
  if (props.color && !props.color.startsWith('#')) {
    console.warn('Color should start with #')
  }
}

// 在onViewInit中调用验证
function onViewInit(nativeView: any) {
  validateProps(props)
  // ... 其他初始化代码
}
```

### 3. 事件系统

#### 定义事件

```typescript
interface ComponentEmits {
  // 基础事件
  click: [event: ClickEvent]
  change: [value: any, oldValue: any]
  
  // 自定义事件
  customEvent: [data: CustomEventData]
  
  // 异步事件
  asyncComplete: [result: any]
}

const emits = defineEmits<ComponentEmits>()
```

#### 发射事件

```typescript
// 简单事件
function handleClick(event: any) {
  emits('click', event)
}

// 带数据的事件
function handleChange(newValue: any) {
  const oldValue = currentValue.value
  currentValue.value = newValue
  emits('change', newValue, oldValue)
}

// 异步事件
async function performAsyncOperation() {
  try {
    const result = await nativeInstance.value?.asyncMethod()
    emits('asyncComplete', result)
  } catch (error) {
    emits('asyncComplete', { error })
  }
}
```

#### 处理原生事件

```typescript
function onViewInit(nativeView: any) {
  nativeInstance.value = new NativeComponentClass()
  nativeInstance.value.bindView(nativeView)
  
  // 监听原生事件
  nativeInstance.value.addEventListener('nativeClick', (event: any) => {
    emits('click', event)
  })
  
  nativeInstance.value.addEventListener('nativeChange', (event: any) => {
    emits('change', event.detail.value)
  })
}
```

### 4. 状态管理

#### 响应式状态

```typescript
import { ref, reactive, computed } from 'vue'

// 基础响应式数据
const count = ref(0)
const text = ref('')
const isLoading = ref(false)

// 复杂对象
const state = reactive({
  user: {
    name: '',
    age: 0
  },
  settings: {
    theme: 'light',
    language: 'zh-CN'
  }
})

// 计算属性
const displayText = computed(() => {
  return `${text.value} (${count.value})`
})

const isEnabled = computed(() => {
  return !props.disabled && !isLoading.value
})
```

#### 状态同步

```typescript
// 将Vue状态同步到原生层
function syncStateToNative() {
  if (!nativeInstance.value) return
  
  nativeInstance.value.updateState({
    count: count.value,
    text: text.value,
    isEnabled: isEnabled.value
  })
}

// 监听状态变化并同步
watch([count, text, isEnabled], () => {
  syncStateToNative()
}, { deep: true })

// 从原生层同步状态
function syncStateFromNative() {
  if (!nativeInstance.value) return
  
  const nativeState = nativeInstance.value.getState()
  count.value = nativeState.count
  text.value = nativeState.text
}
```

### 5. 生命周期管理

#### Vue生命周期集成

```typescript
import { onMounted, onUpdated, onUnmounted, onBeforeUnmount } from 'vue'

onMounted(() => {
  console.log('Vue component mounted')
  // 执行挂载后的初始化
  initializeComponent()
})

onUpdated(() => {
  console.log('Vue component updated')
  // 更新后同步状态
  syncStateToNative()
})

onBeforeUnmount(() => {
  console.log('Vue component before unmount')
  // 清理前的准备工作
  cleanupBeforeDestroy()
})

onUnmounted(() => {
  console.log('Vue component unmounted')
  // 清理资源
  cleanup()
})
```

#### 原生视图生命周期

```typescript
function onViewInit(nativeView: any) {
  console.log('Native view initialized')
  
  nativeInstance.value = new NativeComponentClass()
  nativeInstance.value.bindView(nativeView)
  
  // 监听原生生命周期
  nativeInstance.value.onReady(() => {
    console.log('Native component ready')
    isReady.value = true
  })
  
  nativeInstance.value.onDestroy(() => {
    console.log('Native component destroyed')
    isReady.value = false
  })
}

function cleanup() {
  if (nativeInstance.value) {
    nativeInstance.value.destroy()
    nativeInstance.value = null
  }
}
```

## 高级功能

### 1. 组件通信

#### 父子组件通信

```typescript
// 子组件向父组件传递数据
const emitToParent = (data: any) => {
  emits('childData', data)
}

// 接收父组件的方法调用
const parentMethod = (param: any) => {
  // 处理父组件调用
  return processParentRequest(param)
}

defineExpose({
  parentMethod
})
```

#### 兄弟组件通信

```typescript
// 使用事件总线
import { EventBus } from '@/utils/event-bus'

// 发送事件
const sendToSibling = (data: any) => {
  EventBus.emit('siblingEvent', data)
}

// 接收事件
onMounted(() => {
  EventBus.on('siblingEvent', handleSiblingEvent)
})

onUnmounted(() => {
  EventBus.off('siblingEvent', handleSiblingEvent)
})
```

### 2. 异步操作处理

#### Promise集成

```typescript
// 包装原生异步方法
const asyncMethod = async (param: any): Promise<any> => {
  if (!nativeInstance.value) {
    throw new Error('Component not ready')
  }
  
  try {
    isLoading.value = true
    const result = await nativeInstance.value.performAsyncOperation(param)
    return result
  } catch (error) {
    console.error('Async operation failed:', error)
    throw error
  } finally {
    isLoading.value = false
  }
}

// 批量异步操作
const batchAsync = async (operations: Array<any>): Promise<Array<any>> => {
  const results = await Promise.all(
    operations.map(op => asyncMethod(op))
  )
  return results
}
```

#### 取消机制

```typescript
let currentOperation: AbortController | null = null

const cancelableAsync = async (param: any) => {
  // 取消之前的操作
  if (currentOperation) {
    currentOperation.abort()
  }
  
  currentOperation = new AbortController()
  
  try {
    const result = await nativeInstance.value?.cancelableOperation(
      param, 
      currentOperation.signal
    )
    return result
  } catch (error) {
    if (error.name === 'AbortError') {
      console.log('Operation cancelled')
    } else {
      throw error
    }
  }
}
```

### 3. 性能优化

#### 防抖和节流

```typescript
import { debounce, throttle } from '@/utils/performance'

// 防抖处理用户输入
const debouncedUpdate = debounce((value: string) => {
  nativeInstance.value?.updateText(value)
}, 300)

watch(() => props.text, (newValue) => {
  debouncedUpdate(newValue)
})

// 节流处理频繁事件
const throttledScroll = throttle((event: any) => {
  nativeInstance.value?.handleScroll(event)
}, 100)
```

#### 虚拟化

```typescript
// 大量数据的虚拟化处理
const virtualizedData = computed(() => {
  const start = scrollTop.value / itemHeight
  const end = start + visibleCount.value
  return allData.value.slice(Math.floor(start), Math.ceil(end))
})

// 更新虚拟化视图
watch(virtualizedData, (newData) => {
  nativeInstance.value?.updateVirtualizedData(newData)
})
```

## 调试和测试

### 1. 调试技巧

```typescript
// 开发环境调试信息
const DEBUG = process.env.NODE_ENV === 'development'

function debugLog(message: string, data?: any) {
  if (DEBUG) {
    console.log(`[${componentName}] ${message}`, data)
  }
}

// 状态跟踪
watch(() => [props, state], (newValue, oldValue) => {
  if (DEBUG) {
    console.log('State changed:', { old: oldValue, new: newValue })
  }
}, { deep: true })
```

### 2. 错误处理

```typescript
// 全局错误处理
const handleError = (error: Error, context: string) => {
  console.error(`Error in ${context}:`, error)
  
  // 发送错误事件
  emits('error', {
    message: error.message,
    context,
    stack: error.stack
  })
}

// 安全的方法调用
const safeCall = (fn: Function, context: string, ...args: any[]) => {
  try {
    return fn(...args)
  } catch (error) {
    handleError(error, context)
    return null
  }
}
```

## 最佳实践

### 1. 代码组织

```typescript
// 按功能分组组织代码
// === Props and Emits ===
interface Props { /* ... */ }
interface Emits { /* ... */ }

// === State Management ===
const state = reactive({ /* ... */ })
const computed = computed(() => { /* ... */ })

// === Native Integration ===
const nativeInstance = ref<NativeClass | null>(null)
function onViewInit() { /* ... */ }

// === Event Handlers ===
function handleClick() { /* ... */ }
function handleChange() { /* ... */ }

// === Public Methods ===
function publicMethod() { /* ... */ }

// === Lifecycle ===
onMounted(() => { /* ... */ })
onUnmounted(() => { /* ... */ })

// === Expose ===
defineExpose({ publicMethod })
```

### 2. 类型安全

```typescript
// 严格的类型定义
interface StrictComponentProps {
  readonly id: string
  readonly type: 'primary' | 'secondary' | 'danger'
  readonly size: 'small' | 'medium' | 'large'
  readonly data: ReadonlyArray<DataItem>
}

// 类型守卫
function isValidProps(props: any): props is StrictComponentProps {
  return (
    typeof props.id === 'string' &&
    ['primary', 'secondary', 'danger'].includes(props.type) &&
    ['small', 'medium', 'large'].includes(props.size) &&
    Array.isArray(props.data)
  )
}
```

### 3. 性能优化策略

- 使用 `shallowRef` 和 `shallowReactive` 减少响应式开销
- 合理使用 `computed` 缓存计算结果
- 避免在模板中使用复杂计算
- 使用 `v-memo` 缓存复杂的模板片段
- 及时清理事件监听器和定时器

通过遵循这些实践，你可以构建出高性能、可维护的UTS组件Vue层。