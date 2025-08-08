package com.hens.android_lib

import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

public class SayHiLib {
  companion object {
    private const val TAG = "SayHiLib"
  }

  public constructor() {
    Log.d(TAG, "SayHiLib 构造函数被调用")
  }

  public fun say(str: String): String {
    Log.d(TAG, "say() 方法被调用，参数: $str")
    
    val currentTime = Date()
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    val timeString = formatter.format(currentTime)
    
    val result = "Hi! 你发送的消息是: '$str'\n当前时间: $timeString"
    Log.i(TAG, "say() 方法返回结果: $result")
    
    return result
  }

  public fun getVersion(): String {
    Log.d(TAG, "getVersion() 方法被调用")
    val version = "1.0.0"
    Log.i(TAG, "getVersion() 返回版本: $version")
    return version
  }
}