# iOS Framework Linking Issue

## Problem
The iOS demo project shows "No such module 'SSEFramework'" even though the framework file exists in the project directory.

## Root Cause
The framework is copied to the project directory but not properly linked in the Xcode project. The `Frameworks` build phase in the project file is empty.

## Solutions

### Solution 1: Manual Xcode Setup (Recommended)
1. Open `sse-ios-demo/SSEDemo.xcodeproj` in Xcode
2. Select the `SSEDemo` target
3. Go to `General` tab
4. Scroll to `Frameworks, Libraries, and Embedded Content`
5. Click the `+` button
6. Select `Add Other...` -> `Add Files...`
7. Navigate to `SSEDemo/SSEFramework-simulator.framework`
8. Select it and click `Open`
9. Make sure the `Embed` setting is set to `Embed & Sign`

### Solution 2: Update Import Statement
Change the import in `ContentView.swift` from:
```swift
import SSEFramework
```
to:
```swift
import SSEFramework_simulator
```

### Solution 3: Rename Framework
1. Rename `SSEFramework-simulator.framework` to `SSEFramework.framework`
2. Update the Xcode project to reference the renamed framework

### Solution 4: Use Package Dependency
Instead of using a local framework, add the SSEFramework as a local package dependency in Xcode.

## Why This Happens
- Xcode projects need explicit framework linking, not just file presence
- The `Frameworks` build phase must contain the framework reference
- Framework search paths must be configured correctly
- The framework name must match the import statement exactly

## Current Status
- ✅ Framework file exists in project directory
- ✅ Framework search paths are configured
- ❌ Framework is not linked in `Frameworks` build phase
- ❌ Import statement may not match framework name

## Quick Fix
The easiest fix is to open the project in Xcode and manually add the framework to the target's framework dependencies.