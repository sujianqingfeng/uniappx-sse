//
//  ContentView.swift
//  SSEDemo
//
//

import SwiftUI
import SSEFramework

class ContentViewModel: NSObject, ObservableObject, SSEManagerDelegate {
    // 创建 SSEManager 实例
    private let sseManager: SSEManager
    
    // 状态变量来存储结果
    @Published var resultText: String = "点击按钮开始 SSE 连接"
    @Published var isConnected: Bool = false
    @Published var requestId: String = "sse-request-1"
    
    override init() {
        self.sseManager = SSEManager()
        super.init()
        // 设置代理
        self.sseManager.delegate = self
    }
    
    /// 开始 SSE 连接
    func startSSEConnection() {
        // 使用本地运行的 SSE 服务器地址
        guard let url = URL(string: "http://localhost:3000/sse") else {
            resultText = "无效的 URL"
            return
        }
        
        // 示例请求头
        let headers = [
            "Authorization": "Bearer your-token-here",
            "User-Agent": "SSE-Demo-App/1.0"
        ]
        
        // 开始连接
        sseManager.startConnection(to: url, headers: headers, requestId: requestId)
        resultText = "正在连接到 SSE 服务器..."
    }
    
    func disconnect() {
        // 断开连接
        sseManager.cancelConnection(for: requestId)
        resultText = "已断开 SSE 连接"
        isConnected = false
    }
    
    func disconnectAll() {
        sseManager.cancelAllConnections()
        resultText = "已断开所有 SSE 连接"
        isConnected = false
    }
    
    func reset() {
        resultText = "点击按钮开始 SSE 连接"
        isConnected = false
    }
    
    // MARK: - SSEManagerDelegate
    func sseManager(_ manager: SSEManager, didReceiveMessage message: String, requestId: String) {
        DispatchQueue.main.async {
            // Try to parse as JSON
            if let data = message.data(using: .utf8),
               let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any] {
                // Handle different event types
                if let eventType = json["type"] as? String {
                    switch eventType {
                    case "notification":
                        if let title = json["title"] as? String, let body = json["body"] as? String {
                            self.resultText = "🔔 通知: \(title) - \(body)"
                        }
                    case "status":
                        if let user = json["user"] as? String, let status = json["status"] as? String {
                            self.resultText = "👤 用户 \(user) 状态更新为: \(status)"
                        }
                    case "data":
                        if let value = json["value"] as? Double, let unit = json["unit"] as? String {
                            self.resultText = "📊 数据更新: \(String(format: "%.2f", value)) \(unit)"
                        }
                    default:
                        self.resultText = "📥 \(message)"
                    }
                } else if let title = json["title"] as? String {
                    self.resultText = "🔔 通知: \(title)"
                } else {
                    self.resultText = "📥 \(message)"
                }
            } else {
                // Plain text message
                self.resultText = "📥 \(message)"
            }
        }
    }
    
    func sseManager(_ manager: SSEManager, didOpenWithRequestId requestId: String) {
        DispatchQueue.main.async {
            self.isConnected = true
            self.resultText = "✅ SSE 连接已建立 (ID: \(requestId))"
        }
    }
    
    func sseManager(_ manager: SSEManager, didFailWithError error: Error, requestId: String) {
        DispatchQueue.main.async {
            self.isConnected = false
            self.resultText = "❌ 连接错误 (ID: \(requestId)): \(error.localizedDescription)"
        }
    }
    
    func sseManager(_ manager: SSEManager, didCloseWithRequestId requestId: String) {
        DispatchQueue.main.async {
            self.isConnected = false
            self.resultText = "⏹️ 连接已关闭 (ID: \(requestId))"
        }
    }
}

struct ContentView: View {
    @StateObject private var viewModel = ContentViewModel()
    
    var body: some View {
        VStack(spacing: 20) {
            Text("SSE Framework 调用示例")
                .font(.title)
                .fontWeight(.bold)
            
            Text(viewModel.resultText)
                .padding()
                .background(Color.gray.opacity(0.1))
                .cornerRadius(8)
                .multilineTextAlignment(.center)
                .frame(minHeight: 120)
            
            VStack(spacing: 10) {
                Button(viewModel.isConnected ? "断开 SSE 连接" : "开始 SSE 连接") {
                    if viewModel.isConnected {
                        viewModel.disconnect()
                    } else {
                        viewModel.startSSEConnection()
                    }
                }
                .padding()
                .background(viewModel.isConnected ? Color.red : Color.blue)
                .foregroundColor(.white)
                .cornerRadius(8)
                
                Button("断开所有连接") {
                    viewModel.disconnectAll()
                }
                .padding()
                .background(Color.gray)
                .foregroundColor(.white)
                .cornerRadius(8)
                
                Button("重置") {
                    viewModel.reset()
                }
                .padding()
                .background(Color.orange)
                .foregroundColor(.white)
                .cornerRadius(8)
            }
        }
        .padding()
    }
}

#Preview {
    ContentView()
}