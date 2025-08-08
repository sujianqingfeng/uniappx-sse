package com.hens.android_playground

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
import com.hens.android_playground.ui.theme.AndroidplaygroundTheme
import com.hens.android_lib.SayHiLib

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      AndroidplaygroundTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          SayHiScreen(
            modifier = Modifier.padding(innerPadding)
          )
        }
      }
    }
  }
}

@Composable
fun SayHiScreen(modifier: Modifier = Modifier) {
  val sayHiLib = remember { SayHiLib() }
  var resultText by remember { mutableStateOf("点击按钮调用 SayHiLib 函数") }
  
  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Text(
      text = "SayHiLib 测试",
      modifier = Modifier.padding(bottom = 32.dp)
    )
    
    Button(
      onClick = {
        resultText = sayHiLib.say("来自 Android 的问候！")
      }
    ) {
      Text("调用 say() 函数")
    }
    
    Spacer(modifier = Modifier.height(16.dp))
    
    Button(
      onClick = {
        resultText = "SayHiLib 版本: ${sayHiLib.getVersion()}"
      }
    ) {
      Text("获取版本信息")
    }
    
    Spacer(modifier = Modifier.height(32.dp))
    
    Text(
      text = resultText,
      modifier = Modifier.padding(16.dp)
    )
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
fun SayHiScreenPreview() {
  AndroidplaygroundTheme {
    SayHiScreen()
  }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
  AndroidplaygroundTheme {
    Greeting("Android")
  }
}