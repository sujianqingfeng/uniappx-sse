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
  - Android: `libs/android-lib-debug.aar` - Kotlin implementation
  - iOS: `Frameworks/SSEFramework.framework` - Swift implementation
- **Configuration**: Platform-specific `config.json` files for dependencies and permissions

## Development Guidelines

### UTS Language Rules
- **Language**: UTS compiles to JavaScript (Web), Kotlin (Android), Swift (iOS), ArkTS (HarmonyOS)
- **Types**: Use strong typing with explicit type annotations for exported APIs
- **Exports**: Use named exports (`export function`, `export class`, `export type`) - avoid `export default`
- **Imports**: Only import from within the plugin directory - no external imports allowed
- **Functions**: Use function declarations, not arrow functions for exports that need decorators
- **No Overloading**: Avoid function overloading in Android environment - use different function names

### Callback and Event Handling
- **Single Callback**: Functions starting with `on` and having only one callback parameter can trigger multiple times without decorator
- **Multiple Callbacks**: Use `@UTSJS.keepAlive` decorator for functions that need to trigger callbacks multiple times
- **KeepAlive Rules**: 
  - Apply decorator to both Android and iOS implementations
  - All callback parameters are held long-term when using keepAlive
  - Provide cleanup methods (`offXxx`, `stop`, `unsubscribe`) to release resources

### Error Handling
- **Error Domain**: Define plugin-specific error domain in `unierror.uts`
- **Error Codes**: Use enum for error codes (e.g., `NETWORK`, `ABORTED`)
- **Error Creation**: Provide `createError` function for consistent error object creation
- **Async Patterns**: 
  - Single operations: Promise-based with optional callback support
  - Continuous events: Use `onXxx/offXxx` pattern or keepAlive callbacks

### Platform-Specific Requirements

#### Android (app-android)
- **Dependencies**: Place AAR files in `libs/` directory and declare in `config.json`
- **Permissions**: Declare required permissions in `config.json`
- **ProGuard**: Add ProGuard rules if needed via `config.json`
- **Threading**: Use appropriate threading for long-running operations

#### iOS (app-ios)
- **Dependencies**: Place Framework files in `Frameworks/` directory and declare in `config.json`
- **Info.plist**: Inject required permissions via `config.json`
- **Architectures**: Ensure both simulator and device architectures are supported
- **Threading**: Use DispatchQueue for background operations, return to main queue for UI updates

### API Design Patterns
- **Promise-first**: Use Promise for single operations, support optional callbacks
- **Event Subscription**: Use `onXxx/offXxx` pattern for event streams
- **Resource Management**: Provide start/stop or subscribe/unsubscribe methods
- **Lifecycle**: Clean up native resources when app goes to background or page unloads

### Build and Debug
- **HBuilderX**: Use HBuilderX for device debugging and custom base creation
- **Logging**: Ensure native logs can flow back to HBuilderX console
- **Networking**: For SSE/WebSocket, use proper debugging tools (Charles/Proxyman)
- **Testing**: Test on both Android and iOS platforms

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
│   └── Frameworks/           # iOS frameworks
└── package.json              # Plugin metadata
```

## Development Checklist

- [ ] Interface declarations complete and cross-platform consistent
- [ ] Error domain and codes defined in `unierror.uts`
- [ ] Platform implementations align with interface definitions
- [ ] Only internal imports used (no external plugin directory imports)
- [ ] Callback strategy implemented correctly:
  - [ ] Event APIs use `onXxx/offXxx` pattern
  - [ ] Continuous callbacks use `@UTSJS.keepAlive` or `on` rules
  - [ ] Cleanup methods provided and are idempotent
- [ ] Android-specific requirements met:
  - [ ] No function overloading used
  - [ ] Permissions and dependencies declared in config.json
- [ ] iOS-specific requirements met:
  - [ ] Info.plist permissions injected via config.json
  - [ ] Frameworks properly declared and signed
- [ ] API patterns followed:
  - [ ] Single operations Promise-based
  - [ ] Component APIs not embedded in templates
- [ ] Resource lifecycle management implemented
- [ ] Documentation and examples provided

## Key Reference Documentation
- [UTS Plugin Official Docs](https://doc.dcloud.net.cn/uni-app-x/plugin/uts-plugin.html)
- [UTS Language Guide](https://doc.dcloud.net.cn/uni-app-x/uts/)
- [UTSJSON Object Reference](https://doc.dcloud.net.cn/uni-app-x/uts/buildin-object-api/utsjsonobject.html)