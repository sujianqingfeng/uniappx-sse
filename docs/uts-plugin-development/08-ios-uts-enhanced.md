# iOS平台UTS开发增强指南

## iOS特有开发细节补充

### UTSiOS工具类详解

UTSiOS是UTS提供的iOS平台专用工具类，提供了丰富的原生能力访问接口：

```typescript
// #ifdef APP-IOS

/**
 * UTSiOS核心工具类使用
 */
export class UTSiOSHelper {
    
    /**
     * 获取当前UIViewController
     */
    static getCurrentViewController(): UIViewController | null {
        return UTSiOS.getCurrentViewController()
    }
    
    /**
     * 字符串转UIColor
     */
    static colorWithString(colorString: string): UIColor {
        return UTSiOS.colorWithString(colorString)
    }
    
    /**
     * 获取插件资源路径
     */
    static getResourcePath(fileName: string): string {
        return UTSiOS.getResourcePath(fileName)
    }
    
    /**
     * 销毁实例（内存管理）
     */
    static destroyInstance(instance: any): void {
        UTSiOS.destroyInstance(instance)
    }
    
    /**
     * 获取指针（类似Swift的&操作符）
     */
    static getPointer(object: any): any {
        return UTSiOS.getPointer(object)
    }
    
    /**
     * 获取应用信息
     */
    static getAppInfo(): any {
        const bundle = Bundle.main
        const infoDictionary = bundle.infoDictionary
        
        return {
            bundleIdentifier: bundle.bundleIdentifier,
            version: infoDictionary?["CFBundleShortVersionString"] as? string,
            buildNumber: infoDictionary?["CFBundleVersion"] as? string,
            displayName: infoDictionary?["CFBundleDisplayName"] as? string || infoDictionary?["CFBundleName"] as? string
        }
    }
    
    /**
     * 获取设备信息
     */
    static getDeviceInfo(): any {
        const device = UIDevice.current
        const screen = UIScreen.main
        
        return {
            model: device.model,
            systemName: device.systemName,
            systemVersion: device.systemVersion,
            name: device.name,
            identifierForVendor: device.identifierForVendor?.uuidString,
            screenBounds: {
                width: screen.bounds.size.width,
                height: screen.bounds.size.height
            },
            screenScale: screen.scale,
            nativeBounds: {
                width: screen.nativeBounds.size.width,
                height: screen.nativeBounds.size.height
            }
        }
    }
    
    /**
     * 获取系统版本信息
     */
    static getSystemVersion(): any {
        let systemVersion = UIDevice.current.systemVersion
        let versionComponents = systemVersion.components(separatedBy: ".")
        
        return {
            fullVersion: systemVersion,
            majorVersion: versionComponents.count > 0 ? Int(versionComponents[0]) ?? 0 : 0,
            minorVersion: versionComponents.count > 1 ? Int(versionComponents[1]) ?? 0 : 0,
            patchVersion: versionComponents.count > 2 ? Int(versionComponents[2]) ?? 0 : 0
        }
    }
    
    /**
     * 检查iOS版本兼容性
     */
    static isVersionSupported(minVersion: string): boolean {
        if #available(iOS 13.0, *) {
            return UIDevice.current.systemVersion.compare(minVersion, options: .numeric) != .orderedAscending
        }
        return false
    }
}

// #endif
```

### iOS线程管理

```typescript
// #ifdef APP-IOS

/**
 * iOS线程管理工具类
 */
export class iOSThreadManager {
    
    /**
     * 在主线程执行操作
     */
    static runOnMainThread(block: () => void): void {
        DispatchQueue.main.async {
            block()
        }
    }
    
    /**
     * 在后台线程执行操作
     */
    static runOnBackgroundThread(block: () => void): void {
        DispatchQueue.global(qos: .background).async {
            block()
        }
    }
    
    /**
     * 在用户交互级别的线程执行操作
     */
    static runOnUserInteractiveThread(block: () => void): void {
        DispatchQueue.global(qos: .userInteractive).async {
            block()
        }
    }
    
    /**
     * 在用户发起级别的线程执行操作
     */
    static runOnUserInitiatedThread(block: () => void): void {
        DispatchQueue.global(qos: .userInitiated).async {
            block()
        }
    }
    
    /**
     * 延时执行操作
     */
    static runAfterDelay(delay: Double, block: () -> void): void {
        DispatchQueue.main.asyncAfter(deadline: .now() + delay) {\n            block()\n        }\n    }\n    \n    /**\n     * 异步执行操作并在主线程回调结果\n     */\n    static performAsync<T>(\n        backgroundWork: () -> T,\n        mainCallback: (T) -> void\n    ): void {\n        DispatchQueue.global().async {\n            let result = backgroundWork()\n            \n            DispatchQueue.main.async {\n                mainCallback(result)\n            }\n        }\n    }\n    \n    /**\n     * 同步执行操作（谨慎使用）\n     */\n    static runSynchronously<T>(block: () -> T): T {\n        var result: T?\n        let semaphore = DispatchSemaphore(value: 0)\n        \n        DispatchQueue.global().async {\n            result = block()\n            semaphore.signal()\n        }\n        \n        semaphore.wait()\n        return result!\n    }\n    \n    /**\n     * 检查是否在主线程\n     */\n    static isMainThread(): boolean {\n        return Thread.isMainThread\n    }\n}\n\n// #endif\n```\n\n### iOS内存管理\n\n```typescript\n// #ifdef APP-IOS\n\n/**\n * iOS内存管理工具类\n */\nexport class iOSMemoryManager {\n    \n    private static strongReferences: Array<any> = []\n    private static weakReferences: Array<WeakRef<any>> = []\n    \n    /**\n     * 强引用对象（防止被释放）\n     */\n    static retainObject(object: any): void {\n        this.strongReferences.push(object)\n    }\n    \n    /**\n     * 释放强引用\n     */\n    static releaseObject(object: any): void {\n        let index = this.strongReferences.indexOf(object)\n        if (index >= 0) {\n            this.strongReferences.splice(index, 1)\n        }\n    }\n    \n    /**\n     * 弱引用对象\n     */\n    static weakReference(object: any): void {\n        this.weakReferences.push(new WeakRef(object))\n    }\n    \n    /**\n     * 清理无效的弱引用\n     */\n    static cleanupWeakReferences(): void {\n        this.weakReferences = this.weakReferences.filter(ref => ref.deref() !== undefined)\n    }\n    \n    /**\n     * 手动销毁对象\n     */\n    static destroyObject(object: any): void {\n        UTSiOS.destroyInstance(object)\n        this.releaseObject(object)\n    }\n    \n    /**\n     * 创建弱引用闭包\n     */\n    static createWeakClosure<T>(target: T, closure: (weakTarget: T?) -> void): () -> void {\n        return { [weak target] in\n            closure(target)\n        }\n    }\n    \n    /**\n     * 获取内存使用信息\n     */\n    static getMemoryInfo(): any {\n        let info = mach_task_basic_info()\n        var count = mach_msg_type_number_t(MemoryLayout<mach_task_basic_info>.size)/4\n        \n        let result = withUnsafeMutablePointer(to: &info) {\n            $0.withMemoryRebound(to: integer_t.self, capacity: 1) {\n                task_info(mach_task_self_,\n                         task_flavor_t(MACH_TASK_BASIC_INFO),\n                         $0,\n                         &count)\n            }\n        }\n        \n        if result == KERN_SUCCESS {\n            return {\n                residentSize: info.resident_size,\n                virtualSize: info.virtual_size\n            }\n        }\n        \n        return null\n    }\n    \n    /**\n     * 监控内存警告\n     */\n    static setupMemoryWarningObserver(callback: () -> void): void {\n        NotificationCenter.default.addObserver(\n            forName: UIApplication.didReceiveMemoryWarningNotification,\n            object: nil,\n            queue: .main\n        ) { _ in\n            callback()\n        }\n    }\n}\n\n// #endif\n```\n\n### iOS资源管理\n\n```typescript\n// #ifdef APP-IOS\n\n/**\n * iOS资源管理工具类\n */\nexport class iOSResourceManager {\n    \n    /**\n     * 获取Bundle资源\n     */\n    static getBundleResource(name: string, type: string): string? {\n        return Bundle.main.path(forResource: name, ofType: type)\n    }\n    \n    /**\n     * 获取插件资源\n     */\n    static getPluginResource(fileName: string): string {\n        return UTSiOS.getResourcePath(fileName)\n    }\n    \n    /**\n     * 读取文本文件\n     */\n    static readTextFile(filePath: string): string? {\n        do {\n            return try String(contentsOfFile: filePath, encoding: .utf8)\n        } catch {\n            NSLog(\"Failed to read file: \\(error)\")\n            return null\n        }\n    }\n    \n    /**\n     * 写入文本文件\n     */\n    static writeTextFile(filePath: string, content: string): boolean {\n        do {\n            try content.write(toFile: filePath, atomically: true, encoding: .utf8)\n            return true\n        } catch {\n            NSLog(\"Failed to write file: \\(error)\")\n            return false\n        }\n    }\n    \n    /**\n     * 获取Documents目录\n     */\n    static getDocumentsDirectory(): string {\n        let paths = NSSearchPathForDirectoriesInDomains(.documentDirectory, .userDomainMask, true)\n        return paths[0]\n    }\n    \n    /**\n     * 获取Cache目录\n     */\n    static getCacheDirectory(): string {\n        let paths = NSSearchPathForDirectoriesInDomains(.cachesDirectory, .userDomainMask, true)\n        return paths[0]\n    }\n    \n    /**\n     * 获取临时目录\n     */\n    static getTemporaryDirectory(): string {\n        return NSTemporaryDirectory()\n    }\n    \n    /**\n     * 检查文件是否存在\n     */\n    static fileExists(filePath: string): boolean {\n        return FileManager.default.fileExists(atPath: filePath)\n    }\n    \n    /**\n     * 删除文件\n     */\n    static deleteFile(filePath: string): boolean {\n        do {\n            try FileManager.default.removeItem(atPath: filePath)\n            return true\n        } catch {\n            NSLog(\"Failed to delete file: \\(error)\")\n            return false\n        }\n    }\n    \n    /**\n     * 创建目录\n     */\n    static createDirectory(directoryPath: string): boolean {\n        do {\n            try FileManager.default.createDirectory(\n                atPath: directoryPath,\n                withIntermediateDirectories: true,\n                attributes: nil\n            )\n            return true\n        } catch {\n            NSLog(\"Failed to create directory: \\(error)\")\n            return false\n        }\n    }\n    \n    /**\n     * 获取文件大小\n     */\n    static getFileSize(filePath: string): number {\n        do {\n            let attributes = try FileManager.default.attributesOfItem(atPath: filePath)\n            let fileSize = attributes[.size] as? NSNumber\n            return fileSize?.intValue ?? 0\n        } catch {\n            return 0\n        }\n    }\n    \n    /**\n     * 加载图片资源\n     */\n    static loadImage(imageName: string): UIImage? {\n        if let imagePath = UTSiOS.getResourcePath(imageName) {\n            return UIImage(contentsOfFile: imagePath)\n        }\n        return UIImage(named: imageName)\n    }\n    \n    /**\n     * 从URL加载图片\n     */\n    static loadImageFromURL(urlString: string, completion: (UIImage?) -> void): void {\n        guard let url = URL(string: urlString) else {\n            completion(null)\n            return\n        }\n        \n        URLSession.shared.dataTask(with: url) { data, response, error in\n            if let data = data, let image = UIImage(data: data) {\n                DispatchQueue.main.async {\n                    completion(image)\n                }\n            } else {\n                DispatchQueue.main.async {\n                    completion(null)\n                }\n            }\n        }.resume()\n    }\n}\n\n// #endif\n```\n\n### iOS系统服务访问\n\n```typescript\n// #ifdef APP-IOS\n\n/**\n * iOS系统服务访问工具类\n */\nexport class iOSSystemService {\n    \n    /**\n     * 用户偏好设置管理\n     */\n    static class UserDefaults {\n        \n        static setValue(key: string, value: any): void {\n            UserDefaults.standard.set(value, forKey: key)\n            UserDefaults.standard.synchronize()\n        }\n        \n        static getValue(key: string): any? {\n            return UserDefaults.standard.object(forKey: key)\n        }\n        \n        static removeValue(key: string): void {\n            UserDefaults.standard.removeObject(forKey: key)\n            UserDefaults.standard.synchronize()\n        }\n        \n        static clear(): void {\n            let domain = Bundle.main.bundleIdentifier!\n            UserDefaults.standard.removePersistentDomain(forName: domain)\n            UserDefaults.standard.synchronize()\n        }\n    }\n    \n    /**\n     * 钥匙串管理\n     */\n    static class Keychain {\n        \n        static setValue(key: string, value: string): boolean {\n            let data = value.data(using: .utf8)!\n            \n            let query: [String: Any] = [\n                kSecClass as String: kSecClassGenericPassword,\n                kSecAttrAccount as String: key,\n                kSecValueData as String: data\n            ]\n            \n            SecItemDelete(query as CFDictionary)\n            let status = SecItemAdd(query as CFDictionary, nil)\n            \n            return status == errSecSuccess\n        }\n        \n        static getValue(key: string): string? {\n            let query: [String: Any] = [\n                kSecClass as String: kSecClassGenericPassword,\n                kSecAttrAccount as String: key,\n                kSecReturnData as String: kCFBooleanTrue!,\n                kSecMatchLimit as String: kSecMatchLimitOne\n            ]\n            \n            var dataTypeRef: AnyObject? = nil\n            let status: OSStatus = SecItemCopyMatching(query as CFDictionary, &dataTypeRef)\n            \n            if status == errSecSuccess {\n                if let retrievedData = dataTypeRef as? Data {\n                    return String(data: retrievedData, encoding: .utf8)\n                }\n            }\n            \n            return null\n        }\n        \n        static removeValue(key: string): boolean {\n            let query: [String: Any] = [\n                kSecClass as String: kSecClassGenericPassword,\n                kSecAttrAccount as String: key\n            ]\n            \n            let status = SecItemDelete(query as CFDictionary)\n            return status == errSecSuccess\n        }\n    }\n    \n    /**\n     * 网络状态监控\n     */\n    static class NetworkMonitor {\n        \n        /**\n         * 检查网络连接状态\n         */\n        static isNetworkAvailable(): boolean {\n            var zeroAddress = sockaddr_in(sin_len: 0, sin_family: 0, sin_port: 0, sin_addr: in_addr(s_addr: 0), sin_zero: (0, 0, 0, 0, 0, 0, 0, 0))\n            zeroAddress.sin_len = UInt8(MemoryLayout.size(ofValue: zeroAddress))\n            zeroAddress.sin_family = sa_family_t(AF_INET)\n            \n            let defaultRouteReachability = withUnsafePointer(to: &zeroAddress) {\n                $0.withMemoryRebound(to: sockaddr.self, capacity: 1) {zeroSockAddress in\n                    SCNetworkReachabilityCreateWithAddress(nil, zeroSockAddress)\n                }\n            }\n            \n            var flags: SCNetworkReachabilityFlags = SCNetworkReachabilityFlags(rawValue: 0)\n            if SCNetworkReachabilityGetFlags(defaultRouteReachability!, &flags) == false {\n                return false\n            }\n            \n            let isReachable = (flags.rawValue & UInt32(kSCNetworkFlagsReachable)) != 0\n            let needsConnection = (flags.rawValue & UInt32(kSCNetworkFlagsConnectionRequired)) != 0\n            let ret = (isReachable && !needsConnection)\n            \n            return ret\n        }\n        \n        /**\n         * 获取网络类型\n         */\n        static getNetworkType(): string {\n            // 这里需要引入网络检测框架或使用第三方库\n            // 简化实现\n            if self.isNetworkAvailable() {\n                return \"available\"\n            } else {\n                return \"unavailable\"\n            }\n        }\n    }\n    \n    /**\n     * 位置服务\n     */\n    static class LocationService {\n        \n        /**\n         * 检查位置权限\n         */\n        static checkLocationPermission(): string {\n            let status = CLLocationManager.authorizationStatus()\n            \n            switch status {\n                case .authorizedWhenInUse:\n                    return \"authorizedWhenInUse\"\n                case .authorizedAlways:\n                    return \"authorizedAlways\"\n                case .denied:\n                    return \"denied\"\n                case .restricted:\n                    return \"restricted\"\n                case .notDetermined:\n                    return \"notDetermined\"\n                @unknown default:\n                    return \"unknown\"\n            }\n        }\n        \n        /**\n         * 请求位置权限\n         */\n        static requestLocationPermission(always: boolean = false): void {\n            let locationManager = CLLocationManager()\n            \n            if always {\n                locationManager.requestAlwaysAuthorization()\n            } else {\n                locationManager.requestWhenInUseAuthorization()\n            }\n        }\n    }\n    \n    /**\n     * 通知服务\n     */\n    static class NotificationService {\n        \n        /**\n         * 请求通知权限\n         */\n        static requestNotificationPermission(completion: (boolean) -> void): void {\n            UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge]) { granted, error in\n                DispatchQueue.main.async {\n                    completion(granted)\n                }\n            }\n        }\n        \n        /**\n         * 发送本地通知\n         */\n        static scheduleLocalNotification(title: string, body: string, delay: TimeInterval): void {\n            let content = UNMutableNotificationContent()\n            content.title = title\n            content.body = body\n            content.sound = .default\n            \n            let trigger = UNTimeIntervalNotificationTrigger(timeInterval: delay, repeats: false)\n            let request = UNNotificationRequest(identifier: UUID().uuidString, content: content, trigger: trigger)\n            \n            UNUserNotificationCenter.current().add(request)\n        }\n    }\n}\n\n// #endif\n```\n\n### iOS Framework集成\n\n```typescript\n// #ifdef APP-IOS\n\n/**\n * iOS Framework集成管理\n */\nexport class iOSFrameworkManager {\n    \n    /**\n     * 检查Framework是否可用\n     */\n    static isFrameworkAvailable(frameworkName: string): boolean {\n        let bundle = Bundle(identifier: frameworkName)\n        return bundle != nil\n    }\n    \n    /**\n     * 动态加载Framework\n     */\n    static loadFramework(frameworkPath: string): boolean {\n        guard let bundle = Bundle(path: frameworkPath) else {\n            return false\n        }\n        \n        return bundle.load()\n    }\n    \n    /**\n     * 获取Framework版本\n     */\n    static getFrameworkVersion(frameworkName: string): string? {\n        guard let bundle = Bundle(identifier: frameworkName) else {\n            return null\n        }\n        \n        return bundle.infoDictionary?[\"CFBundleShortVersionString\"] as? String\n    }\n    \n    /**\n     * 配置第三方Framework\n     */\n    static configureThirdPartyFramework(frameworkName: string, configBlock: () -> void): void {\n        if self.isFrameworkAvailable(frameworkName) {\n            configBlock()\n        } else {\n            NSLog(\"Framework \\(frameworkName) is not available\")\n        }\n    }\n}\n\n/**\n * 常用第三方Framework集成示例\n */\nexport class ThirdPartyIntegration {\n    \n    /**\n     * 集成网络请求框架（示例）\n     */\n    static setupNetworkFramework(): void {\n        // 配置网络框架\n        // 这里需要根据具体使用的第三方框架进行配置\n    }\n    \n    /**\n     * 集成图片加载框架（示例）\n     */\n    static setupImageFramework(): void {\n        // 配置图片加载框架\n        // 例如：SDWebImage, Kingfisher等\n    }\n    \n    /**\n     * 集成分析框架（示例）\n     */\n    static setupAnalyticsFramework(): void {\n        // 配置分析框架\n        // 例如：Firebase Analytics, 友盟等\n    }\n}\n\n// #endif\n```\n\n### 详细config.json和Info.plist配置\n\n#### config.json配置\n```json\n{\n    \"deploymentTarget\": \"12.0\",\n    \"frameworks\": [\n        \"UIKit.framework\",\n        \"Foundation.framework\",\n        \"CoreLocation.framework\",\n        \"UserNotifications.framework\"\n    ],\n    \"dependencies\": [\n        {\n            \"name\": \"Alamofire\",\n            \"version\": \"5.6.4\",\n            \"source\": \"https://github.com/Alamofire/Alamofire.git\"\n        }\n    ],\n    \"capabilities\": [\n        \"com.apple.developer.location.push\",\n        \"com.apple.developer.usernotifications.communication\"\n    ],\n    \"buildSettings\": {\n        \"SWIFT_VERSION\": \"5.0\",\n        \"ENABLE_BITCODE\": \"NO\",\n        \"OTHER_LDFLAGS\": \"-ObjC\"\n    }\n}\n```\n\n#### Info.plist配置示例\n```xml\n<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n<plist version=\"1.0\">\n<dict>\n    <key>NSLocationWhenInUseUsageDescription</key>\n    <string>此应用需要访问位置信息以提供相关服务</string>\n    <key>NSLocationAlwaysAndWhenInUseUsageDescription</key>\n    <string>此应用需要持续访问位置信息</string>\n    <key>NSCameraUsageDescription</key>\n    <string>此应用需要访问相机以拍摄照片</string>\n    <key>NSPhotoLibraryUsageDescription</key>\n    <string>此应用需要访问相册以选择照片</string>\n    <key>NSMicrophoneUsageDescription</key>\n    <string>此应用需要访问麦克风以录制音频</string>\n    <key>NSContactsUsageDescription</key>\n    <string>此应用需要访问通讯录</string>\n    <key>NSCalendarsUsageDescription</key>\n    <string>此应用需要访问日历</string>\n    <key>NSRemindersUsageDescription</key>\n    <string>此应用需要访问提醒事项</string>\n    <key>NSUserNotificationsUsageDescription</key>\n    <string>此应用需要发送通知</string>\n    <key>LSApplicationQueriesSchemes</key>\n    <array>\n        <string>wechat</string>\n        <string>weixin</string>\n        <string>alipay</string>\n    </array>\n    <key>CFBundleURLTypes</key>\n    <array>\n        <dict>\n            <key>CFBundleURLName</key>\n            <string>com.example.app</string>\n            <key>CFBundleURLSchemes</key>\n            <array>\n                <string>yourapp</string>\n            </array>\n        </dict>\n    </array>\n</dict>\n</plist>\n```\n\n### iOS最佳实践和注意事项\n\n#### 1. 内存管理最佳实践\n\n```typescript\n// ✅ 正确的内存管理\nclass ViewControllerManager {\n    private viewController: UIViewController?\n    \n    func setupViewController() {\n        self.viewController = MyViewController()\n        \n        // 使用弱引用避免循环引用\n        self.viewController?.onComplete = { [weak self] in\n            self?.handleComplete()\n        }\n    }\n    \n    func cleanup() {\n        UTSiOS.destroyInstance(self.viewController)\n        self.viewController = nil\n    }\n}\n\n// ❌ 避免的做法\nclass BadViewControllerManager {\n    private viewController: UIViewController?\n    \n    func setupViewController() {\n        self.viewController = MyViewController()\n        \n        // 强引用会导致循环引用\n        self.viewController?.onComplete = {\n            self.handleComplete() // 这里会造成循环引用\n        }\n    }\n}\n```\n\n#### 2. 线程处理最佳实践\n\n```typescript\n// ✅ 正确的线程处理\nfunc performAsyncOperation(completion: @escaping (Result) -> Void) {\n    DispatchQueue.global(qos: .userInitiated).async {\n        // 后台线程执行耗时操作\n        let result = performLongRunningTask()\n        \n        // 切换到主线程更新UI\n        DispatchQueue.main.async {\n            completion(result)\n        }\n    }\n}\n\n// ❌ 避免的做法\nfunc badAsyncOperation(completion: @escaping (Result) -> Void) {\n    // 在主线程执行耗时操作会阻塞UI\n    let result = performLongRunningTask()\n    completion(result)\n}\n```\n\n#### 3. 资源管理最佳实践\n\n```typescript\n// ✅ 正确的资源管理\nclass ResourceManager {\n    private var fileHandle: FileHandle?\n    \n    func openResource() -> Bool {\n        do {\n            self.fileHandle = try FileHandle(forReadingFrom: url)\n            return true\n        } catch {\n            NSLog(\"Failed to open resource: \\(error)\")\n            return false\n        }\n    }\n    \n    func closeResource() {\n        self.fileHandle?.closeFile()\n        self.fileHandle = nil\n    }\n    \n    deinit {\n        closeResource()\n    }\n}\n```\n\n#### 4. 版本兼容性处理\n\n```typescript\n// ✅ 正确的版本检查\nfunc useNewFeature() {\n    if #available(iOS 14.0, *) {\n        // 使用iOS 14+的新特性\n        useNewAPI()\n    } else {\n        // 使用旧版本的兼容方案\n        useLegacyAPI()\n    }\n}\n\n// 运行时版本检查\nfunc checkSystemVersion() {\n    let systemVersion = UTSiOSHelper.getSystemVersion()\n    \n    if systemVersion.majorVersion >= 15 {\n        // iOS 15+ 特定功能\n    } else if systemVersion.majorVersion >= 14 {\n        // iOS 14+ 特定功能\n    } else {\n        // 更旧版本的兼容处理\n    }\n}\n```\n\n这些增强内容基于DCloud官方iOS UTS文档，提供了更完整和实用的iOS UTS开发指导。