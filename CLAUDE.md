# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Uni-App X native plugin starter template that demonstrates how to bridge native code (Swift for iOS, Kotlin for Android) with a Uni-App X frontend. The project consists of several main components:

- **sse-uniapp-demo**: Uni-App X project that serves as a testbed for the native plugin
- **sse-android**: Android Studio project containing the native Android implementation
- **sse-ios-framework**: Xcode project containing the native iOS implementation
- **sse-ios-demo**: iOS project that serves as a testbed for the iOS native plugin
- **sse-server**: Node.js server that provides Server-Sent Events (SSE) for testing

## Development Environment

### Prerequisites
- HBuilderX for Uni-App X development
- Android Studio for Android native development
- Xcode for iOS native development
- Node.js and pnpm for SSE server

### Building and Running

#### Uni-App X Playground
```bash
# Open HBuilderX and import the sse-uniapp-demo directory
# Connect a device or start an emulator
# Run the project on desired platform (iOS or Android)
```

#### Android Plugin
```bash
cd sse-android
./gradlew build
```

#### iOS Framework
```bash
cd sse-ios-framework
./build-framework.sh
```

Alternatively, build manually using Xcode:
1. Open `SSEFramework.xcodeproj` in Xcode
2. Select `SSEFramework` scheme
3. Build the project

#### SSE Server
```bash
cd sse-server
pnpm install
pnpm start
```

## Architecture

### Plugin Structure
The plugin follows UTS (Uni-App TypeScript) plugin architecture:

- **Interface Definition**: `uni_modules/sse-plugin/utssdk/interface.uts` - TypeScript interface definitions
- **Android Implementation**: `uni_modules/sse-plugin/utssdk/app-android/index.uts` - Android-specific implementation
- **iOS Implementation**: `uni_modules/sse-plugin/utssdk/app-ios/index.uts` - iOS-specific implementation
- **Native Libraries**: 
  - Android: `sse-android/sse-lib/src/main/java/com/hens/android_lib/SayHiLib.kt`
  - iOS: `sse-ios-framework/SSEFramework/SSEFramework.swift`

### Configuration Files
- **Android**: `uni_modules/sse-plugin/utssdk/app-android/config.json` - Android SDK version config
- **iOS**: `uni_modules/sse-plugin/utssdk/app-ios/config.json` - iOS deployment target config

### Key Implementation Details

#### Native Code Integration
- **Android**: Uses AAR library (`android-lib-debug.aar`) located in `libs/` directory
- **iOS**: Uses framework (`SSEFramework.framework`) located in `Frameworks/` directory

#### API Interface
The plugin exposes a single async API `sayHiApi` with the following structure:
```typescript
export type SayHiApiOptions = {
  say: string
  success?: (res: SayHiApiResult) => void
  fail?: (res: SayHiApiFail) => void
  complete?: (res: any) => void
}
```

#### Error Handling
Error codes follow Uni-App convention (90xxxx series):
- 9010001: General error
- 9010002: Additional error types

## Development Workflow

1. **Native Code Changes**: Modify Swift/Kotlin files in respective native projects
2. **Rebuild Libraries**: Build native projects to update AAR/Framework files
3. **UTS Implementation**: Update platform-specific UTS files if API changes
4. **Interface Updates**: Modify `interface.uts` if API signature changes
5. **Testing**: Run Uni-App X playground to test integration

## Native Code Guidelines

### Android Library Development
- **Language**: All Android native code MUST be written in Kotlin (.kt files)
- **Location**: Android native code should be placed in `sse-android/sse-lib/src/main/java/com/hens/sse/library/`
- **Build Configuration**: Use Kotlin DSL for build.gradle.kts files
- **Dependencies**: Include OkHttp for network operations when needed
- **Threading**: Use coroutines for asynchronous operations
- **Architecture**: Follow Android architecture best practices with proper separation of concerns

### iOS Framework Development
- **Language**: All iOS native code MUST be written in Swift (.swift files)
- **Location**: iOS native code should be placed in `sse-ios-framework/SSEFramework/`
- **Build Configuration**: Use Xcode project settings for framework configuration
- **Dependencies**: Use Swift Package Manager or manual framework inclusion
- **Threading**: Use DispatchQueue for asynchronous operations
- **Architecture**: Follow iOS architecture best practices with proper separation of concerns

## File Structure

```
sse-uniapp-demo/
├── uni_modules/sse-plugin/
│   ├── utssdk/
│   │   ├── interface.uts          # TypeScript interface definitions
│   │   ├── app-android/
│   │   │   ├── index.uts          # Android UTS implementation
│   │   │   ├── config.json        # Android config
│   │   │   └── libs/              # Android AAR libraries
│   │   └── app-ios/
│   │       ├── index.uts          # iOS UTS implementation
│   │       ├── config.json        # iOS config
│   │       └── Frameworks/        # iOS frameworks
│   └── package.json              # Plugin metadata
├── pages/index/index.uvue         # Main UI page
└── main.uts                       # App entry point

sse-android/
└── sse-lib/
    └── src/main/java/com/hens/sse/library/
        ├── SayHiLib.kt            # Android native implementation
        └── SSEManager.kt          # SSE Manager implementation

sse-ios-framework/
└── SSEFramework/
    └── SSEFramework.swift         # iOS native implementation

sse-ios-demo/
└── SSEDemo/
    └── ContentView.swift         # iOS demo app

sse-server/
├── server.js                      # SSE server implementation
├── package.json                   # Server dependencies
└── public/
    └── index.html                 # Web interface
```

## Testing

The main testing interface is in `sse-uniapp-demo/pages/index/index.uvue` which provides a UI to test the plugin functionality. The plugin supports both sync and async calls through the UTS bridge.

## Platform Support

- **Android**: Minimum SDK 21
- **iOS**: Deployment target 12.0
- **Uni-App X**: Requires HBuilderX 3.6.8+ and uni-app-x 3.1.0+