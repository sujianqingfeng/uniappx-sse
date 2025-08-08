//
//  SSEFramework.swift
//  SSEFramework
//

import Foundation

// MARK: - SSEManagerDelegate
/// SSE 事件回调协议
public protocol SSEManagerDelegate: AnyObject {
    /// 接收到消息
    /// - Parameters:
    ///   - manager: SSE 管理器实例
    ///   - message: 消息内容
    ///   - requestId: 请求 ID
    func sseManager(_ manager: SSEManager, didReceiveMessage message: String, requestId: String)
    
    /// 连接打开
    /// - Parameters:
    ///   - manager: SSE 管理器实例
    ///   - requestId: 请求 ID
    func sseManager(_ manager: SSEManager, didOpenWithRequestId requestId: String)
    
    /// 连接错误
    /// - Parameters:
    ///   - manager: SSE 管理器实例
    ///   - error: 错误信息
    ///   - requestId: 请求 ID
    func sseManager(_ manager: SSEManager, didFailWithError error: Error, requestId: String)
    
    /// 连接关闭
    /// - Parameters:
    ///   - manager: SSE 管理器实例
    ///   - requestId: 请求 ID
    func sseManager(_ manager: SSEManager, didCloseWithRequestId requestId: String)
}

// MARK: - SSEManager
/// SSE 管理器
public class SSEManager {
    
    // MARK: - Properties
    /// 代理
    public weak var delegate: SSEManagerDelegate?
    
    /// 存储所有活动的连接
    private var connections: [String: URLSessionDataTask] = [:]
    
    /// URLSession 配置
    private let session: URLSession = {
        let config = URLSessionConfiguration.default
        // 可根据需要自定义配置
        return URLSession(configuration: config)
    }()
    
    /// 串行队列，用于保护 connections 字典的线程安全
    private let connectionsQueue = DispatchQueue(label: "com.sseframework.connections.queue")
    
    // MARK: - Initializer
    public init() {}
    
    // MARK: - Public Methods
    
    /// 开始 SSE 连接
    /// - Parameters:
    ///   - url: SSE 服务器地址
    ///   - headers: 请求头
    ///   - requestId: 请求 ID，用于标识和管理连接
    public func startConnection(
        to url: URL,
        headers: [String: String] = [:],
        requestId: String
    ) {
        // 检查是否已存在相同 requestId 的连接
        connectionsQueue.sync {
            if connections[requestId] != nil {
                print("SSEManager: 已存在 requestId 为 \\(requestId) 的连接")
                return
            }
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = "GET"
        request.setValue("text/event-stream", forHTTPHeaderField: "Accept")
        request.setValue("no-cache", forHTTPHeaderField: "Cache-Control")
        request.setValue("keep-alive", forHTTPHeaderField: "Connection")
        
        // 添加自定义请求头
        for (key, value) in headers {
            request.setValue(value, forHTTPHeaderField: key)
        }
        
        let task = session.dataTask(with: request) { [weak self] data, response, error in
            guard let self = self else { return }
            
            // 处理连接错误
            if let error = error {
                DispatchQueue.main.async {
                    self.delegate?.sseManager(self, didFailWithError: error, requestId: requestId)
                }
                self.removeConnection(for: requestId)
                return
            }
            
            // 检查 HTTP 响应状态码
            if let httpResponse = response as? HTTPURLResponse {
                guard 200...299 ~= httpResponse.statusCode else {
                    let statusError = NSError(
                        domain: "SSEManagerErrorDomain",
                        code: httpResponse.statusCode,
                        userInfo: [NSLocalizedDescriptionKey: "HTTP \\(httpResponse.statusCode)"]
                    )
                    DispatchQueue.main.async {
                        self.delegate?.sseManager(self, didFailWithError: statusError, requestId: requestId)
                    }
                    self.removeConnection(for: requestId)
                    return
                }
            }
            
            // 通知连接已打开
            DispatchQueue.main.async {
                self.delegate?.sseManager(self, didOpenWithRequestId: requestId)
            }
            
            // 处理接收到的数据
            guard let data = data else {
                self.removeConnection(for: requestId)
                return
            }
            
            // 简单的 SSE 数据解析
            // SSE 格式: data: <content>\n\n
            let dataString = String(data: data, encoding: .utf8) ?? ""
            let lines = dataString.components(separatedBy: "\n")
            
            for line in lines {
                if line.hasPrefix("data:") {
                    let message = String(line.dropFirst(5)).trimmingCharacters(in: .whitespacesAndNewlines)
                    DispatchQueue.main.async {
                        self.delegate?.sseManager(self, didReceiveMessage: message, requestId: requestId)
                    }
                }
                // 可以在这里处理其他 SSE 事件类型，如 event:, id:, retry: 等
            }
        }
        
        // 存储连接
        connectionsQueue.async(flags: .barrier) {
            self.connections[requestId] = task
        }
        
        // 启动任务
        task.resume()
    }
    
    /// 取消指定的 SSE 连接
    /// - Parameter requestId: 请求 ID
    public func cancelConnection(for requestId: String) {
        connectionsQueue.sync {
            if let task = connections[requestId] {
                task.cancel()
                removeConnection(for: requestId)
                DispatchQueue.main.async {
                    self.delegate?.sseManager(self, didCloseWithRequestId: requestId)
                }
            }
        }
    }
    
    /// 取消所有 SSE 连接
    public func cancelAllConnections() {
        connectionsQueue.sync {
            let requestIds = Array(connections.keys)
            for requestId in requestIds {
                if let task = connections[requestId] {
                    task.cancel()
                }
            }
            connections.removeAll()
            
            // 通知所有连接已关闭
            DispatchQueue.main.async {
                for requestId in requestIds {
                    self.delegate?.sseManager(self, didCloseWithRequestId: requestId)
                }
            }
        }
    }
    
    // MARK: - Private Methods
    
    /// 从连接字典中移除指定的连接
    /// - Parameter requestId: 请求 ID
    private func removeConnection(for requestId: String) {
        connectionsQueue.async(flags: .barrier) {
            self.connections[requestId] = nil
        }
    }
}