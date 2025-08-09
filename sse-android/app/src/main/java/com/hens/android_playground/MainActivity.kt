package com.hens.sse.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.hens.sse.demo.ui.theme.AndroidplaygroundTheme
import com.hens.sse.library.SSEManager
import org.json.JSONObject

// SSE Manager helper
class SSEManagerHelper {
    private val sseManager = SSEManager.getInstance()
    private val currentRequestId = "sse-request-1"
    private var callback: ((String, Boolean) -> Unit)? = null
    
    private val sseCallback = object : SSEManager.SSECallback {
        override fun onOpen(requestId: String) {
            callback?.invoke("✅ SSE 连接已建立 (ID: $requestId)", true)
        }
        
        override fun onMessage(message: String, requestId: String) {
            try {
                val json = JSONObject(message)
                when (val eventType = json.optString("type")) {
                    "notification" -> {
                        val title = json.optString("title", "")
                        val body = json.optString("body", "")
                        callback?.invoke("🔔 通知: $title - $body", true)
                    }
                    "status" -> {
                        val user = json.optString("user", "")
                        val status = json.optString("status", "")
                        callback?.invoke("👤 用户 $user 状态更新为: $status", true)
                    }
                    "data" -> {
                        val value = json.optDouble("value", 0.0)
                        val unit = json.optString("unit", "")
                        callback?.invoke("📊 数据更新: ${"%.2f".format(value)} $unit", true)
                    }
                    else -> {
                        val title = json.optString("title")
                        if (title.isNotEmpty()) {
                            callback?.invoke("🔔 通知: $title", true)
                        } else {
                            callback?.invoke("📥 $message", true)
                        }
                    }
                }
            } catch (e: Exception) {
                callback?.invoke("📥 $message", true)
            }
        }
        
        override fun onError(error: Exception, requestId: String) {
            callback?.invoke("❌ 连接错误 (ID: $requestId): ${error.message}", false)
        }
        
        override fun onClose(requestId: String) {
            callback?.invoke("⏹️ 连接已关闭 (ID: $requestId)", false)
        }
    }
    
    fun setCallback(callback: (String, Boolean) -> Unit) {
        this.callback = callback
    }
    
    fun startSSEConnection() {
        val url = "http://10.0.2.2:3000/sse"
        val headers = mapOf(
            "Authorization" to "Bearer your-token-here",
            "User-Agent" to "SSE-Demo-App/1.0"
        )
        
        callback?.invoke("正在连接到 SSE 服务器...", false)
        sseManager.startConnection(url, headers, currentRequestId, sseCallback)
    }
    
    fun disconnect() {
        sseManager.cancelConnection(currentRequestId)
        callback?.invoke("已断开 SSE 连接", false)
    }
    
    fun disconnectAll() {
        sseManager.cancelAllConnections()
        callback?.invoke("已断开所有 SSE 连接", false)
    }
    
    fun reset() {
        disconnectAll()
        callback?.invoke("点击按钮开始 SSE 连接", false)
    }
}

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      AndroidplaygroundTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          SSEScreen(
            modifier = Modifier.padding(innerPadding)
          )
        }
      }
    }
  }
}

@Composable
fun SSEScreen(modifier: Modifier = Modifier) {
    val sseHelper = remember { SSEManagerHelper() }
    var resultText by remember { mutableStateOf("点击按钮开始 SSE 连接") }
    var isConnected by remember { mutableStateOf(false) }
    
    // Set up callback
    LaunchedEffect(sseHelper) {
        sseHelper.setCallback { text, connected ->
            resultText = text
            isConnected = connected
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "SSE Framework 调用示例",
            fontSize = 20.sp,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .background(Color.Gray.copy(alpha = 0.1f))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = resultText,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = {
                    if (isConnected) {
                        sseHelper.disconnect()
                    } else {
                        sseHelper.startSSEConnection()
                    }
                },
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = if (isConnected) Color.Red else Color.Blue
                )
            ) {
                Text(
                    text = if (isConnected) "断开 SSE 连接" else "开始 SSE 连接",
                    color = Color.White
                )
            }
            
            Button(
                onClick = { sseHelper.disconnectAll() },
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = Color.Gray
                )
            ) {
                Text(
                    text = "断开所有连接",
                    color = Color.White
                )
            }
            
            Button(
                onClick = { sseHelper.reset() },
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFA500)
                )
            ) {
                Text(
                    text = "重置",
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
  Text(
    text = "Hello $name!",
    modifier = modifier
  )
}

@Preview(showBackground = true)
@Composable
fun SSEScreenPreview() {
  AndroidplaygroundTheme {
    SSEScreen()
  }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
  AndroidplaygroundTheme {
    Greeting("Android")
  }
}