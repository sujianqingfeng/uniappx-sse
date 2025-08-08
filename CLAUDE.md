# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Uni-App X native plugin starter template that demonstrates how to bridge native code (Swift for iOS, Kotlin for Android) with a Uni-App X frontend. The project consists of three main components:

- **sse-uniapp-project**: Uni-App X project that serves as a testbed for the native plugin
- **sse-android-plugin**: Android Studio project containing the native Android implementation
- **SSEFramework**: Xcode project containing the native iOS implementation

## Development Environment

### Prerequisites
- HBuilderX for Uni-App X development
- Android Studio for Android native development
- Xcode for iOS native development

### Building and Running

#### Uni-App X Playground
```bash
# Open HBuilderX and import the sse-uniapp-project directory
# Run the project on desired platform (iOS or Android)
```

#### Android Playground
```bash
cd sse-android-plugin
./gradlew build
```

#### iOS Framework
```bash
# Open SSEFramework.xcodeproj in Xcode
# Select SSEFramework scheme and build
```

## Architecture

### Plugin Structure
The plugin follows UTS (Uni-App TypeScript) plugin architecture:

- **Interface Definition**: `uni_modules//utssdk/interface.uts` - TypeScript interface definitions
- **Android Implementation**: `uni_modules//utssdk/app-android/index.uts` - Android-specific implementation
- **iOS Implementation**: `uni_modules//utssdk/app-ios/index.uts` - iOS-specific implementation
- **Native Libraries**: 
  - Android: `sse-android-plugin/sse-sse-lib/src/main/java/com/hens/android_lib/SayHiLib.kt`
  - iOS: `SSEFramework/SSEFramework/ios_framework.swift`

### Configuration Files
- **Android**: `uni_modules//utssdk/app-android/config.json` - Android SDK version config
- **iOS**: `uni_modules//utssdk/app-ios/config.json` - iOS deployment target config

### Key Implementation Details

#### Native Code Integration
- **Android**: Uses AAR library (`sse-sse-lib-debug.aar`) located in `libs/` directory
- **iOS**: Uses framework (`ios_framework.framework`) located in `Frameworks/` directory

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

## File Structure

```
sse-uniapp-project/
├── uni_modules//
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

sse-android-plugin/
└── sse-sse-lib/
    └── src/main/java/com/hens/android_lib/
        └── SayHiLib.kt            # Android native implementation

SSEFramework/
└── SSEFramework/
    └── ios_framework.swift        # iOS native implementation
```

## Testing

The main testing interface is in `sse-uniapp-project/pages/index/index.uvue` which provides a UI to test the plugin functionality. The plugin supports both sync and async calls through the UTS bridge.

## Platform Support

- **Android**: Minimum SDK 21
- **iOS**: Deployment target 12.0
- **Uni-App X**: Requires HBuilderX 3.6.8+ and uni-app-x 3.1.0+