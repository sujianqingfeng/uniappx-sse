//
//  ios_framework.swift
//  ios-framework
//
//

import Foundation

public class SayHiFramework {
    public init() {
    }
    
    public func say(_ str: String) -> String {
        let currentTime = Date()
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd HH:mm:ss"
        let timeString = formatter.string(from: currentTime)
        
        return "Hi! 你发送的消息是: '\(str)'\n当前时间: \(timeString)"
    }
    
    public func getVersion() -> String {
        return "1.0.0"
    }
}

