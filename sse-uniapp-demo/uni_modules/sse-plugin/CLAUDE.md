---
globs: *.uts,*.uvue,*.json
alwaysApply: false
---

# SSE Plugin - Claude Assistant Guide

## Project Overview

This is a Uni-App X native plugin that bridges Server-Sent Events (SSE) functionality between native Swift/iOS and Kotlin/Android code and a Uni-App X frontend using UTS (Uni-App TypeScript).

## Architecture Overview

- **Interface Layer**: `interface.uts` - TypeScript interface definitions for the plugin API
- **Platform Implementations**: 
  - `app-android/index.uts` - Android-specific UTS implementation
  - `app-ios/index.uts` - iOS-specific UTS implementation
- **Native Libraries**:
  - Android: `libs/sse-lib-debug.aar` - Kotlin implementation
  - iOS: `Frameworks/SSEFramework.framework` - Swift implementation
- **Configuration**: Platform-specific `config.json` files for dependencies and permissions
- **Error Handling**: `unierror.uts` - Unified error domain and error creation utilities

## UTS Language Fundamentals

### Core Language Rules
- **Compilation Targets**: UTS compiles to JavaScript (Web), Kotlin (Android), Swift (iOS), ArkTS (HarmonyOS)
- **Strong Typing**: Use explicit type annotations for exported APIs and public interfaces
- **Variable Declaration**: Use `let`/`const` with explicit type annotations; support union types and type inference
- **Module Exports**: Use named exports (`export function`, `export class`, `export type`) - avoid `export default`
- **Import Constraints**: Only import from within the plugin directory - no external imports allowed
- **Function Declarations**: Use function declarations (`export function foo() {}`) for exports needing decorators; avoid `export const fn = () => {}`
- **No Function Overloading**: Avoid function overloading in Android environment - use different function names
- **Semicolons**: Optional for multi-line statements; required for same-line multiple statements

### Types and Objects
- **UTSJSONObject**: Use for dynamic JSON objects; perform type assertions when accessing properties
- **Arrays**: Use `Array.isArray()` or `instanceof Array` for type checking
- **Null Safety**: Use optional chaining `?.` for nullable types; avoid null reference errors
- **Type Checking**: Use `instanceof` for built-in objects (Date, UTSJSONObject, etc.)

## Callback and Event Handling

### Callback Patterns
- **Single Callback**: Functions starting with `on` and having only one callback parameter can trigger multiple times without decorator
- **Multiple Callbacks**: Use `@UTSJS.keepAlive` decorator for functions that need to trigger callbacks multiple times
- **KeepAlive Rules**: 
  - Apply decorator to both Android and iOS implementations
  - All callback parameters are held long-term when using keepAlive
  - Avoid frequent calls to prevent memory pressure
  - Provide cleanup methods (`offXxx`, `stop`, `unsubscribe`) to release resources

### API Design Patterns
- **Event Subscription**: Use `onXxx/offXxx` pattern for event streams with idempotent cleanup
- **Promise-first**: Single operations should be Promise-based with optional callback support
- **Resource Management**: Provide start/stop or subscribe/unsubscribe methods
- **Lifecycle**: Clean up native resources when app goes to background or page unloads

### Examples
```uts
// Event subscription pattern
export function onSSEMessage(callback: (data: string) => void) {
  // on开头且仅一个callback，可多次触发
}

// KeepAlive for complex options
@UTSJS.keepAlive
export function startSSE(options: SSEOptions) {
  // 持续触发options.onMessage
}

export function stopSSE() {
  // 幂等的清理方法
}
```

## Error Handling and Async Patterns

### Error Domain and Codes
- **Error Domain**: Define plugin-specific error domain in `unierror.uts`
- **Error Codes**: Use enum for error codes (e.g., `NETWORK`, `ABORTED`)
- **Error Creation**: Provide `createError` function for consistent error object creation
- **Error Format**: Unified error object with `domain`, `code`, `message`, and optional `extra`

### Async Patterns
- **Single Operations**: Promise-based with optional callback support (`success/fail/complete`)
- **Continuous Events**: Use `onXxx/offXxx` pattern or keepAlive callbacks
- **Cancellation**: Return AbortController-style objects or provide `stop/cancel` methods
- **Resource Cleanup**: Ensure `offXxx`/`stop` methods are idempotent and handle multiple calls

### Error Handling Example
```uts
export enum SSEErrorCode {
  NETWORK = 'NETWORK',
  ABORTED = 'ABORTED',
  TIMEOUT = 'TIMEOUT'
}

export function createError(code: SSEErrorCode, message?: string, extra?: UTSJSONObject): UTSJSONObject {
  return {
    domain: 'sse-plugin',
    code,
    message: message ?? '',
    extra: extra ?? {}
  }
}

export function connect(url: string): Promise<void> {
  return new Promise((resolve, reject) => {
    // 连接逻辑
    // reject(createError(SSEErrorCode.NETWORK, 'Connection failed'))
  })
}
```

## Platform-Specific Requirements

### Android (app-android)
- **Dependencies**: Place AAR files in `libs/` directory and declare in `config.json`
- **Permissions**: Declare required permissions in `config.json` following Android 13+ behavior changes
- **ProGuard**: Add ProGuard rules via `config.json`; keep reflection/entry classes/callback interfaces
- **Threading**: Use appropriate threading for long-running operations; avoid blocking UI thread
- **Network Security**: Configure network security in `config.json`; handle HTTPS and ATS
- **Function Overloading**: Not supported in uni-app Android environment; use different function names

### iOS (app-ios)
- **Dependencies**: Place Framework files in `Frameworks/` directory and declare in `config.json`
- **Info.plist**: Inject required permissions via `config.json` (NSCameraUsageDescription, etc.)
- **Architectures**: Ensure both simulator (arm64/x86_64) and device (arm64) architectures are supported
- **Threading**: Use DispatchQueue for background operations; return to main queue for UI updates
- **Swift Compatibility**: Use Xcode version compatible with packaging; enable "Build Libraries for Distribution"
- **Resource Files**: Use built-in methods to resolve resource paths; avoid hardcoding

### iOS-Specific Development Guidelines

#### Swift and UTS Differences
- **Numeric Types**: UTS uses `Number`; use native types (`Int`, `Float`, `Double`) only when required by protocol signatures
- **Optional Types**: Use optional chaining `?.`; `"[weak self]"` annotation in closures to prevent circular references
- **Protocol Implementation**: Use `implements`; method signatures must match native exactly including parameter labels
- **Parameter Labels**: Compatible with Swift external parameter label rules
- **Closures**: Add `"[weak self]"` as first line when closure holds `this` and accesses `this`

#### iOS Built-in Library (DCloudUTSFoundation)
- `getCurrentViewController(): UIViewController` - Get current visible view controller
- `colorWithString(value: string): UIColor` - Convert color string to UIColor
- `getResourcePath(resourceName: string): string` - Resolve plugin resource paths
- UI updates must be dispatched to main thread

#### Memory Management
- Explicitly destroy native instances: `UTSiOS.destroyInstance(this)`
- Handle lifecycle events for resource cleanup
- Use weak references in closures to prevent retain cycles

## Native Hybrid Development

### Directory Structure for Native Code
```
uni_modules/sse-plugin/utssdk/
├── app-android/
│   ├── index.uts              # UTS entry point
│   ├── config.json           # Android configuration
│   ├── libs/                 # AAR libraries
│   └── YourNativeCode.kt     # Kotlin/Java native code
├── app-ios/
│   ├── index.uts              # UTS entry point
│   ├── config.json           # iOS configuration
│   ├── Frameworks/           # Third-party frameworks
│   ├── Info.plist            # iOS permissions/injection
│   └── YourNativeCode.swift  # Swift native code
├── interface.uts              # API definitions
├── unierror.uts              # Error handling
└── package.json              # Plugin metadata
```

### Native Hybrid Guidelines
- **Package Names**: Use default package `uts.sdk.modules.<pluginNameCamelCase>` or explicit imports
- **File Naming**: Avoid using `index.*` for native files (reserved for UTS entry)
- **Condition Compilation**: Wrap platform-specific code with `#ifdef APP-ANDROID` / `#ifdef APP-IOS`
- **Direct Interop**: UTS compiles to native languages; no serialization overhead for same-language calls
- **ArkTS/ETS Bridge**: Use JS intermediate files for ets ↔ uts communication

### Development Requirements
- **HBuilderX Version**: ≥ 4.25 for native hybrid support
- **Native IDE**: Use Android Studio/Xcode for complex native development
- **Custom Base**: Required for Java code; Kotlin/Swift can run directly on device
- **Framework Integration**: Place third-party frameworks in `Frameworks/` directory

## Build and Debug

### Development Environment
- **HBuilderX**: Use for device debugging and custom base creation
- **Logging**: Ensure native logs flow back to HBuilderX console
- **Breakpoints**: UTS supports breakpoint debugging; use platform tools for native layer debugging
- **Networking**: Use Charles/Proxyman for SSE/WebSocket debugging; handle HTTPS certificates

### Build Configuration
- **Android**: Declare permissions, AAR dependencies, ProGuard rules, network security in `config.json`
- **iOS**: Inject Info.plist permissions, Framework dependencies via `config.json`
- **Release Builds**: 
  - Android: Obfuscate while preserving external APIs and callback interfaces
  - iOS: Ensure correct symbols and signing; verify third-party library compliance

### Testing
- **Platform Testing**: Test on both Android and iOS platforms
- **Network Testing**: Test with real network conditions; handle connectivity issues
- **Memory Testing**: Verify proper resource cleanup and no memory leaks
- **Compatibility**: Test across different OS versions and device types

## File Structure
```
uni_modules/sse-plugin/
├── interface.uts              # API type definitions and exports
├── unierror.uts              # Error codes and error creation utilities
├── app-android/
│   ├── index.uts             # Android UTS implementation
│   ├── config.json           # Android configuration
│   └── libs/                 # Android AAR libraries
├── app-ios/
│   ├── index.uts             # iOS UTS implementation
│   ├── config.json           # iOS configuration
│   ├── Frameworks/           # iOS frameworks
│   └── Info.plist            # iOS permissions injection
└── package.json              # Plugin metadata
```

## Development Checklist

### Core Requirements
- [ ] `interface.uts`: Method/type declarations complete and cross-platform consistent; named exports only
- [ ] `unierror.uts`: Error domain and error codes defined; `createError` utility available
- [ ] Platform implementations align with interface definitions
- [ ] Only internal imports used (no external plugin directory imports)
- [ ] HBuilderX version ≥ 4.25 for native hybrid development

### Callback and Event Handling
- [ ] Event APIs use `onXxx/offXxx` pattern
- [ ] Continuous callbacks use `@UTSJS.keepAlive` or `on` rules (both platforms)
- [ ] Cleanup methods provided and are idempotent
- [ ] Avoid callback memory pressure; provide release paths

### Android-Specific
- [ ] No function overloading used; different function names for different signatures
- [ ] Permissions and dependencies declared in config.json
- [ ] AAR files placed in `libs/` directory
- [ ] ProGuard rules configured if needed
- [ ] Network security configuration complete

### iOS-Specific
- [ ] Info.plist permissions injected via config.json
- [ ] Frameworks properly declared and signed
- [ ] Simulator and device architectures supported
- [ ] Swift/UTS differences handled correctly
- [ ] Memory management: weak references in closures, proper instance destruction
- [ ] Threading: background operations with UI updates on main thread

### API and Design Patterns
- [ ] Single operations Promise-based with optional callback support
- [ ] Component APIs not embedded in templates (except component plugins)
- [ ] Resource lifecycle management implemented
- [ ] Error handling consistent across platforms
- [ ] Native hybrid files not named `index.*`

### Build and Quality
- [ ] Platform-specific code wrapped in condition compilation blocks
- [ ] Documentation and examples provided
- [ ] Testing completed on both platforms
- [ ] Memory leaks and resource cleanup verified
- [ ] Native hybrid integration tested (if applicable)

## Key Reference Documentation
- [UTS Plugin Official Docs](https://doc.dcloud.net.cn/uni-app-x/plugin/uts-plugin.html)
- [UTS Language Guide](https://doc.dcloud.net.cn/uni-app-x/uts/)
- [UTSJSON Object Reference](https://doc.dcloud.net.cn/uni-app-x/uts/buildin-object-api/utsjsonobject.html)
- [UTS Native Hybrid Development](https://doc.dcloud.net.cn/uni-app-x/plugin/uts-plugin-hybrid.html)
- [UTS for iOS Development](https://doc.dcloud.net.cn/uni-app-x/plugin/uts-for-ios.html)