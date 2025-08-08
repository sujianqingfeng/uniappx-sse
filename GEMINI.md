# Uni-App X Native Plugin - Say Hi

## Project Overview

This project is a starter template for creating a native plugin for Uni-App X. It includes a simple "Say Hi" plugin that demonstrates how to bridge native code (Swift for iOS, Kotlin for Android) with a Uni-App X frontend.

The project is structured into three main parts:

*   **`uniapp-x-playground`**: A Uni-App X project that serves as a testbed for the native plugin. It includes a simple UI to invoke the plugin's functionality.
*   **`android-playground`**: An Android Studio project that contains the native Android implementation of the plugin.
*   **`ios-framework`**: An Xcode project that contains the native iOS implementation of the plugin.

## Building and Running

### Uni-App X Playground

To run the Uni-App X project, you will need to use HBuilderX.

1.  Open HBuilderX and import the `uniapp-x-playground` directory.
2.  Connect a device or start an emulator.
3.  Run the project on the desired platform (iOS or Android).

### Android Playground

To build the Android plugin, you can use Gradle.

```bash
cd android-playground
./gradlew build
```

### iOS Framework

To build the iOS framework, you can use Xcode.

1.  Open the `ios-framework.xcodeproj` file in Xcode.
2.  Select the `ios-framework` scheme.
3.  Build the project.

## Development Conventions

### Plugin Development

The core logic for the plugin is located in the following files:

*   **Android**: `android-playground/android-lib/src/main/java/com/example/android_lib/SayHiModule.kt`
*   **iOS**: `ios-framework/ios-framework/ios_framework.swift`

When making changes to the native code, you will need to rebuild the respective projects and then re-run the Uni-App X project to see the changes.

### Uni-App X Development

The Uni-App X code is located in the `uniapp-x-playground` directory. The main files to look at are:

*   `main.uts`: The entry point of the application.
*   `pages/index/index.uvue`: The main page of the application, which contains the UI for interacting with the plugin.

The `sayHiApi` is imported from the `uni_modules/say-hi` directory, which is where the plugin's JavaScript interface is defined.
