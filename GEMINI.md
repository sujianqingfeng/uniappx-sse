# Uni-App X Native Plugin - Say Hi

## Project Overview

This project is a starter template for creating a native plugin for Uni-App X. It includes a simple "Say Hi" plugin that demonstrates how to bridge native code (Swift for iOS, Kotlin for Android) with a Uni-App X frontend.

The project is structured into several main parts:

*   **`sse-uniapp-project`**: A Uni-App X project that serves as a testbed for the native plugin. It includes a simple UI to invoke the plugin's functionality.
*   **`sse-android-plugin`**: An Android Studio project that contains the native Android implementation of the plugin.
*   **`SSEFramework`**: An Xcode project that contains the native iOS implementation of the plugin.
*   **`SSEApp`**: An iOS project that serves as a testbed for the iOS native plugin.

## Building and Running

### Uni-App X Playground

To run the Uni-App X project, you will need to use HBuilderX.

1.  Open HBuilderX and import the `sse-uniapp-project` directory.
2.  Connect a device or start an emulator.
3.  Run the project on the desired platform (iOS or Android).

### Android Playground

To build the Android plugin, you can use Gradle.

```bash
cd sse-android-plugin
./gradlew build
```

### iOS Framework

To build the iOS framework, you can use the provided script:

```bash
cd SSEFramework
./build-framework.sh
```

Alternatively, you can build it manually using Xcode:

1.  Open the `SSEFramework.xcodeproj` file in Xcode.
2.  Select the `SSEFramework` scheme.
3.  Build the project.

## Development Conventions

### Plugin Development

The core logic for the plugin is located in the following files:

*   **Android**: 
    * Plugin implementation: `sse-android-plugin/sse-sse-lib/src/main/java/com/example/android_lib/SayHiModule.kt`
    * Plugin interface: `sse-uniapp-project/uni_modules//utssdk/app-android/index.uts`
*   **iOS**: 
    * Plugin implementation: `SSEFramework/SSEFramework/ios_framework.swift`
    * Plugin interface: `sse-uniapp-project/uni_modules//utssdk/app-ios/index.uts`

When making changes to the native code, you will need to:
1. Rebuild the respective projects (Android or iOS)
2. Update the plugin files in `sse-uniapp-project/uni_modules//utssdk/` if needed
3. Re-run the Uni-App X project to see the changes

### Uni-App X Development

The Uni-App X code is located in the `sse-uniapp-project` directory. The main files to look at are:

*   `main.uts`: The entry point of the application.
*   `pages/index/index.uvue`: The main page of the application, which contains the UI for interacting with the plugin.
*   `uni_modules//`: The plugin directory, which contains the interface definitions for both Android and iOS platforms.

The plugin is imported and used in the `pages/index/index.uvue` file.
