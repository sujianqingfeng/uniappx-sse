import Foundation
import DCloudUTSFoundation

// MARK: - SSEManagerDelegate
/// SSE 事件回调协议
public protocol SSEManagerDelegate: AnyObject {
    /// 接收到消息
    func sseManagerMessage(_ manager: SSEManager, _ message: String, _ requestId: String)
    /// 连接打开
    func sseManagerOpen(_ manager: SSEManager, _ requestId: String)
    /// 连接错误
    func sseManagerError(_ manager: SSEManager, _ errorMessage: String, _ requestId: String)
    /// 连接关闭
    func sseManagerClose(_ manager: SSEManager, _ requestId: String)
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
        self.session = URLSession(configuration: configuration, delegate: self, delegateQueue: OperationQueue())
    }
    
    deinit {
        cancelAllConnections()
        session.invalidateAndCancel()
    }
    
    // MARK: - Public Methods
    public func startConnection(_ urlString: String, _ headers: Any = [:], _ requestId: String) {
        print("SSEManager: startConnection called for requestId: \(requestId), URL: \(urlString)")
        console.log("SSEManager: startConnection called for requestId: \(requestId), URL: \(urlString)")
        
        connectionsQueue.sync {
            if tasks[requestId] != nil {
                print("SSEManager: Connection with requestId \(requestId) already exists.")
                console.log("SSEManager: Connection with requestId \(requestId) already exists.")
                return
            }
        }
        
        guard let url = URL(string: urlString) else {
            print("SSEManager: Invalid URL string: \(urlString)")
            console.log("SSEManager: Invalid URL string: \(urlString)")
            return
        }
        
        print("SSEManager: URL created successfully: \(url)")
        console.log("SSEManager: URL created successfully: \(url)")
        var request = URLRequest(url: url)
        request.httpMethod = "GET"
        request.setValue("text/event-stream", forHTTPHeaderField: "Accept")
        request.setValue("no-cache", forHTTPHeaderField: "Cache-Control")
        
        // 处理 Any 类型的 headers，转换为 [String: String]
        if let headersDict = headers as? [String: String] {
            for (key, value) in headersDict {
                request.setValue(value, forHTTPHeaderField: key)
            }
        } else if let headersAny = headers as? [String: Any] {
            for (key, value) in headersAny {
                if let stringValue = value as? String {
                    request.setValue(stringValue, forHTTPHeaderField: key)
                }
            }
        }
        
        let task = session.dataTask(with: request)
        task.taskDescription = requestId
                
        print("SSEManager: URLSessionDataTask created for requestId: \(requestId)")
        console.log("SSEManager: URLSessionDataTask created for requestId: \(requestId)")

        connectionsQueue.async(flags: .barrier) {
            self.tasks[requestId] = task
            print("SSEManager: Task stored for requestId: \(requestId)")
            console.log("SSEManager: Task stored for requestId: \(requestId)")
        }
        
        print("SSEManager: Resuming task for requestId: \(requestId)")
        console.log("SSEManager: Resuming task for requestId: \(requestId)")
        task.resume()
    }
    
    public func cancelConnection(_ requestId: String) {
        connectionsQueue.sync { [weak self] in
            guard let self = self else { return }
            if let task = self.tasks[requestId] {
                task.cancel()
                self.tasks.removeValue(forKey: requestId)
                DispatchQueue.main.async { [weak self] in
                    guard let self = self else { return }
                    self.delegate?.sseManagerClose(self, requestId)
                }
            }
        }
    }
    
    public func cancelAllConnections() {
        connectionsQueue.sync { [weak self] in
            guard let self = self else { return }
            let allRequestIds = Array(self.tasks.keys)
            for requestId in allRequestIds {
                if let task = self.tasks[requestId] {
                    task.cancel()
                }
            }
            self.tasks.removeAll()
            
            DispatchQueue.main.async { [weak self] in
                guard let self = self else { return }
                for requestId in allRequestIds {
                    self.delegate?.sseManagerClose(self, requestId)
                }
            }
        }
    }
    
    // MARK: - URLSessionDataDelegate
    public func urlSession(_ session: URLSession, dataTask: URLSessionDataTask, didReceive response: URLResponse, completionHandler: @escaping (URLSession.ResponseDisposition) -> Void) {
        print("SSEManager: urlSession:didReceiveResponse called")
        console.log("SSEManager: urlSession:didReceiveResponse called")
        print("SSEManager: Response: \(response)")
        console.log("SSEManager: Response: \(response)")
        
        guard let httpResponse = response as? HTTPURLResponse, 200...299 ~= httpResponse.statusCode else {
            print("SSEManager: Invalid HTTP response: \(response)")
            console.log("SSEManager: Invalid HTTP response: \(response)")
            completionHandler(.cancel)
            return
        }
        
        print("SSEManager: Valid HTTP response with status: \(httpResponse.statusCode)")
        console.log("SSEManager: Valid HTTP response with status: \(httpResponse.statusCode)")

        if let requestId = dataTask.taskDescription {
            print("SSEManager: Calling sseManagerOpen for requestId: \(requestId)")
            console.log("SSEManager: Calling sseManagerOpen for requestId: \(requestId)")

            DispatchQueue.main.async { [weak self] in
                guard let self = self else { return }
                print("SSEManager: Dispatching sseManagerOpen to main thread for requestId: \(requestId)")
                console.log("SSEManager: Dispatching sseManagerOpen to main thread for requestId: \(requestId)")
                self.delegate?.sseManagerOpen(self, requestId)
            }
        } else {
            print("SSEManager: No requestId found in taskDescription")
            console.log("SSEManager: No requestId found in taskDescription")
        }
        completionHandler(.allow)
    }
    
    public func urlSession(_ session: URLSession, dataTask: URLSessionDataTask, didReceive data: Data) {
        guard let requestId = dataTask.taskDescription else { 
            print("SSEManager: No requestId found in taskDescription for data reception")
            console.log("SSEManager: No requestId found in taskDescription for data reception")
            return 
        }
        
        let dataString = String(data: data, encoding: .utf8) ?? ""
        print("SSEManager: Received data for requestId \(requestId): \(dataString)")
        console.log("SSEManager: Received data for requestId \(requestId): \(dataString)")
        let lines = dataString.components(separatedBy: "\n")
        
        for line in lines {
            if line.hasPrefix("data:") {
                let message = String(line.dropFirst(5)).trimmingCharacters(in: .whitespacesAndNewlines)
                if !message.isEmpty {
                    print("SSEManager: Processing message for requestId \(requestId): \(message)")
                    console.log("SSEManager: Processing message for requestId \(requestId): \(message)")
                    DispatchQueue.main.async { [weak self] in
                        guard let self = self else { return }
                        print("SSEManager: Dispatching sseManagerMessage to main thread for requestId: \(requestId)")
                        console.log("SSEManager: Dispatching sseManagerMessage to main thread for requestId: \(requestId)")
                        self.delegate?.sseManagerMessage(self, message, requestId)
                    }
                }
            }
        }
    }
    
    public func urlSession(_ session: URLSession, task: URLSessionTask, didCompleteWithError error: Error?) {
        guard let requestId = task.taskDescription else { return }
        
        connectionsQueue.async(flags: .barrier) { [weak self] in
            self?.tasks.removeValue(forKey: requestId)
        }
        
        if let error = error {
            DispatchQueue.main.async { [weak self] in
                guard let self = self else { return }
                self.delegate?.sseManagerError(self, (error as NSError).localizedDescription, requestId)
            }
        } else {
            DispatchQueue.main.async { [weak self] in
                guard let self = self else { return }
                self.delegate?.sseManagerClose(self, requestId)
            }
        }
    }
}

