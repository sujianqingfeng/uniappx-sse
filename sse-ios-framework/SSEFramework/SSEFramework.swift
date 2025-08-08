//
//  SSEFramework.swift
//  SSEFramework
//

import Foundation

// MARK: - SSEManagerDelegate
/// SSE 事件回调协议
public protocol SSEManagerDelegate: AnyObject {
    /// 接收到消息
    func sseManager(_ manager: SSEManager, didReceiveMessage message: String, requestId: String)
    /// 连接打开
    func sseManager(_ manager: SSEManager, didOpenWithRequestId requestId: String)
    /// 连接错误
    func sseManager(_ manager: SSEManager, didFailWithError error: Error, requestId: String)
    /// 连接关闭
    func sseManager(_ manager: SSEManager, didCloseWithRequestId requestId: String)
}

// MARK: - SSEManager
/// SSE 管理器
public class SSEManager: NSObject, URLSessionDataDelegate {
    
    // MARK: - Properties
    public weak var delegate: SSEManagerDelegate?
    private var session: URLSession!
    private var tasks: [String: URLSessionDataTask] = [:]
    private let connectionsQueue = DispatchQueue(label: "com.sseframework.connections.queue")

    // MARK: - Initializer
    public override init() {
        super.init()
        let configuration = URLSessionConfiguration.default
        configuration.timeoutIntervalForRequest = TimeInterval(10)
        self.session = URLSession(configuration: configuration, delegate: self, delegateQueue: nil)
    }
    
    // MARK: - Public Methods
    public func startConnection(to url: URL, headers: [String: String] = [:], requestId: String) {
        connectionsQueue.sync {
            if tasks[requestId] != nil {
                print("SSEManager: Connection with requestId \(requestId) already exists.")
                return
            }
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = "GET"
        request.setValue("text/event-stream", forHTTPHeaderField: "Accept")
        request.setValue("no-cache", forHTTPHeaderField: "Cache-Control")
        
        for (key, value) in headers {
            request.setValue(value, forHTTPHeaderField: key)
        }
        
        let task = session.dataTask(with: request)
        task.taskDescription = requestId
        
        connectionsQueue.async(flags: .barrier) {
            self.tasks[requestId] = task
        }
        
        task.resume()
    }
    
    public func cancelConnection(for requestId: String) {
        connectionsQueue.sync {
            if let task = tasks[requestId] {
                task.cancel()
                tasks.removeValue(forKey: requestId)
                DispatchQueue.main.async {
                    self.delegate?.sseManager(self, didCloseWithRequestId: requestId)
                }
            }
        }
    }
    
    public func cancelAllConnections() {
        connectionsQueue.sync {
            let allRequestIds = Array(tasks.keys)
            for requestId in allRequestIds {
                if let task = tasks[requestId] {
                    task.cancel()
                }
            }
            tasks.removeAll()
            
            DispatchQueue.main.async {
                for requestId in allRequestIds {
                    self.delegate?.sseManager(self, didCloseWithRequestId: requestId)
                }
            }
        }
    }
    
    // MARK: - URLSessionDataDelegate
    public func urlSession(_ session: URLSession, dataTask: URLSessionDataTask, didReceive response: URLResponse, completionHandler: @escaping (URLSession.ResponseDisposition) -> Void) {
        guard let httpResponse = response as? HTTPURLResponse, 200...299 ~= httpResponse.statusCode else {
            completionHandler(.cancel)
            return
        }
        
        if let requestId = dataTask.taskDescription {
            DispatchQueue.main.async {
                self.delegate?.sseManager(self, didOpenWithRequestId: requestId)
            }
        }
        completionHandler(.allow)
    }
    
    public func urlSession(_ session: URLSession, dataTask: URLSessionDataTask, didReceive data: Data) {
        guard let requestId = dataTask.taskDescription else { return }
        
        let dataString = String(data: data, encoding: .utf8) ?? ""
        let lines = dataString.components(separatedBy: "\n")
        
        for line in lines {
            if line.hasPrefix("data:") {
                let message = String(line.dropFirst(5)).trimmingCharacters(in: .whitespacesAndNewlines)
                if !message.isEmpty {
                    DispatchQueue.main.async {
                        self.delegate?.sseManager(self, didReceiveMessage: message, requestId: requestId)
                    }
                }
            }
        }
    }
    
    public func urlSession(_ session: URLSession, task: URLSessionTask, didCompleteWithError error: Error?) {
        guard let requestId = task.taskDescription else { return }
        
        connectionsQueue.async(flags: .barrier) {
            self.tasks.removeValue(forKey: requestId)
        }
        
        if let error = error {
            DispatchQueue.main.async {
                self.delegate?.sseManager(self, didFailWithError: error, requestId: requestId)
            }
        } else {
            DispatchQueue.main.async {
                self.delegate?.sseManager(self, didCloseWithRequestId: requestId)
            }
        }
    }
}
