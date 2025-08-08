//
//  ContentView.swift
//  ios-playground
//
//

import SwiftUI
import ios_framework

struct ContentView: View {
    // 创建 framework 实例
    private let sayHiFramework = SayHiFramework()
    
    // 状态变量来存储结果
    @State private var resultText: String = "点击按钮调用 Framework 方法"
    
    var body: some View {
        VStack(spacing: 20) {
            Text("iOS Framework 调用示例")
                .font(.title)
                .fontWeight(.bold)
            
            Text(resultText)
                .padding()
                .background(Color.gray.opacity(0.1))
                .cornerRadius(8)
                .multilineTextAlignment(.center)
                .frame(minHeight: 80)
            
            VStack(spacing: 10) {
                Button("调用 say 方法") {
                    // 调用 framework 中的 say 方法
                    let result = sayHiFramework.say("Hello from iOS!")
                    resultText = result
                    print("Framework say result: \(result)")
                }
                .padding()
                .background(Color.blue)
                .foregroundColor(.white)
                .cornerRadius(8)
                
                Button("获取版本信息") {
                    // 调用 framework 中的 getVersion 方法
                    let version = sayHiFramework.getVersion()
                    resultText = "Framework 版本: \(version)"
                    print("Framework version: \(version)")
                }
                .padding()
                .background(Color.green)
                .foregroundColor(.white)
                .cornerRadius(8)
                
                Button("重置") {
                    resultText = "点击按钮调用 Framework 方法"
                }
                .padding()
                .background(Color.gray)
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
