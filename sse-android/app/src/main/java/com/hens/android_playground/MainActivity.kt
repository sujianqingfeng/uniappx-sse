package com.hens.sse.demo

import android.os.Bundle
import android.os.Build
import android.Manifest
import android.content.pm.PackageManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hens.sse.demo.ui.theme.AndroidplaygroundTheme
import com.hens.android_playground.service.SSEForegroundService
import org.json.JSONObject

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    requestPostNotificationsIfNeeded()
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

  private fun requestPostNotificationsIfNeeded() {
    if (Build.VERSION.SDK_INT >= 33) {
      val granted = ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
      if (!granted) {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
      }
    }
  }
}

@Composable
fun SSEScreen(modifier: Modifier = Modifier) {
    var resultText by remember { mutableStateOf("点击按钮开始 SSE 连接") }
    var isConnected by remember { mutableStateOf(false) }
    var sseUrl by remember { mutableStateOf("http://10.0.2.2:3000/sse") }
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current
    var service by remember { mutableStateOf<SSEForegroundService?>(null) }

    val listener = remember {
        object : SSEForegroundService.StatusListener {
            override fun onStatusChanged(message: String, connected: Boolean) {
                resultText = tryFormatMessage(message)
                isConnected = connected
            }
        }
    }

    if (!isPreview) {
        val connection = remember {
            object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                    val srv = (binder as? SSEForegroundService.LocalBinder)?.getService()
                    service = srv
                    srv?.registerListener(listener)
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    service?.unregisterListener(listener)
                    service = null
                }
            }
        }

        DisposableEffect(context) {
            SSEForegroundService.startService(context)
            val intent = Intent(context, SSEForegroundService::class.java)
            context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
            onDispose {
                try {
                    service?.unregisterListener(listener)
                } catch (_: Exception) { }
                context.unbindService(connection)
                service = null
            }
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
        
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = sseUrl,
            onValueChange = { sseUrl = it },
            label = { Text("SSE 服务地址") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = {
                    if (isConnected) {
                        service?.stopSse() ?: SSEForegroundService.stopConnection(context)
                    } else {
                        service?.startSse(sseUrl) ?: SSEForegroundService.startConnection(context, sseUrl)
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
                onClick = { service?.stopSse() ?: SSEForegroundService.stopConnection(context) },
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = Color.Gray
                )
            ) {
                Text(
                    text = "断开连接",
                    color = Color.White
                )
            }
            
            Button(
                onClick = {
                    service?.stopSse() ?: SSEForegroundService.stopConnection(context)
                    resultText = "点击按钮开始 SSE 连接"
                    isConnected = false
                },
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

private fun tryFormatMessage(raw: String): String {
    return try {
        val json = JSONObject(raw)
        when (val eventType = json.optString("type")) {
            "notification" -> {
                val title = json.optString("title", "")
                val body = json.optString("body", "")
                "🔔 通知: $title - $body"
            }
            "status" -> {
                val user = json.optString("user", "")
                val status = json.optString("status", "")
                "👤 用户 $user 状态更新为: $status"
            }
            "data" -> {
                val value = json.optDouble("value", 0.0)
                val unit = json.optString("unit", "")
                "📊 数据更新: ${"%.2f".format(value)} $unit"
            }
            else -> raw
        }
    } catch (_: Exception) {
        raw
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
  AndroidplaygroundTheme {
    Greeting("Android")
  }
}
