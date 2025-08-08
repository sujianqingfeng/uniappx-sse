# Uni-App X Native Plugin - Say Hi

## Project Overview

This project is a starter template for creating a native plugin for Uni-App X. It includes a simple "Say Hi" plugin that demonstrates how to bridge native code (Swift for iOS, Kotlin for Android) with a Uni-App X frontend.

The project is structured into several main parts:

*   **`sse-uniapp-demo`**: A Uni-App X project that serves as a testbed for the native plugin. It includes a simple UI to invoke the plugin's functionality.
*   **`sse-android`**: An Android Studio project that contains the native Android implementation of the plugin.
*   **`sse-ios-framework`**: An Xcode project that contains the native iOS implementation of the plugin.
*   **`sse-ios-demo`**: An iOS project that serves as a testbed for the iOS native plugin.
*   **`sse-server`**: A Node.js server that provides Server-Sent Events (SSE) for testing.

## Building and Running

### Uni-App X Playground

To run the Uni-App X project, you will need to use HBuilderX.

1.  Open HBuilderX and import the `sse-uniapp-demo` directory.
2.  Connect a device or start an emulator.
3.  Run the project on the desired platform (iOS or Android).

### Android Playground

To build the Android plugin, you can use Gradle.

```bash
cd sse-android
./gradlew build
```

### iOS Framework

To build the iOS framework, you can use the provided script:

```bash
cd sse-ios-framework
./build-framework.sh
```

Alternatively, you can build it manually using Xcode:

1.  Open the `SSEFramework.xcodeproj` file in Xcode.
2.  Select the `SSEFramework` scheme.
3.  Build the project.

### SSE Server

The SSE server is a Node.js application. To run it, use the following commands. The package manager is `pnpm`.

**Note**: The server is not started automatically. You need to start it manually.

```bash
cd sse-server
pnpm install
pnpm start
```

## Development Conventions

### Plugin Development

The core logic for the plugin is located in the following files:

*   **Android**: 
    * Plugin implementation: `sse-android/sse-lib/src/main/java/com/example/android_lib/SayHiModule.kt`
    * Plugin interface: `sse-uniapp-demo/uni_modules/sse-plugin/utssdk/app-android/index.uts`
*   **iOS**: 
    * Plugin implementation: `sse-ios-framework/SSEFramework/SSEFramework.swift`
    * Plugin interface: `sse-uniapp-demo/uni_modules/sse-plugin/utssdk/app-ios/index.uts`

When making changes to the native code, you will need to:
1. Rebuild the respective projects (Android or iOS)
2. Update the plugin files in `sse-uniapp-demo/uni_modules/sse-plugin/utssdk/` if needed
3. Re-run the Uni-App X project to see the changes

### Uni-App X Development

The Uni-App X code is located in the `sse-uniapp-demo` directory. The main files to look at are:

*   `main.uts`: The entry point of the application.
*   `pages/index/index.uvue`: The main page of the application, which contains the UI for interacting with the plugin.
*   `uni_modules/sse-plugin`: The plugin directory, which contains the interface definitions for both Android and iOS platforms.

The plugin is imported and used in the `pages/index/index.uvue` file.
