package uts.sdk.modules.hensSse

import android.os.Handler
import android.os.Looper
import com.alibaba.fastjson.JSONObject
import io.dcloud.uts.UTSJSONObject
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okio.ByteString
import java.io.IOException
import java.io.InputStreamReader
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object SSEStreamManager {
  private val mainHandler = Handler(Looper.getMainLooper())
  private val activeCalls = ConcurrentHashMap<String, Call>()

  private fun postToMain(block: () -> Unit) {
    if (Looper.myLooper() == Looper.getMainLooper()) {
      block()
    } else {
      mainHandler.post { block() }
    }
  }

  private fun createClient(timeoutMs: Number?): OkHttpClient {
    val timeout = (timeoutMs?.toLong() ?: 60_000L)
    return OkHttpClient.Builder()
      .connectTimeout(timeout, TimeUnit.MILLISECONDS)
      .readTimeout(0, TimeUnit.MILLISECONDS)
      .writeTimeout(timeout, TimeUnit.MILLISECONDS)
      .protocols(Collections.singletonList(Protocol.HTTP_1_1))
      .build()
  }

  private fun createRequest(
    url: String,
    method: String,
    protocol: String,
    headers: UTSJSONObject?,
    bodyText: String?
  ): Request? {
    val builder = Request.Builder()
    try {
      builder.url(url)
    } catch (_: Exception) {
      return null
    }

    builder.header("Accept", if (protocol == "sse") "text/event-stream" else "text/plain")

    if (headers != null) {
      val headerJson = headers.toJSONObject() as JSONObject
      for (key in headerJson.keys) {
        builder.header(key, "${headerJson[key]}")
      }
    }

    if (method == "POST" || method == "PUT" || method == "PATCH" || method == "DELETE") {
      var contentType = "application/json; charset=utf-8"
      if (headers != null) {
        val headerContentType = headers.getString("Content-Type")
        if (!headerContentType.isNullOrEmpty()) {
          contentType = headerContentType
        }
      }
      val requestBody = RequestBody.create(
        MediaType.parse(contentType),
        ByteString.encodeUtf8(bodyText ?: "")
      )
      builder.method(method, requestBody)
    } else if (method == "HEAD") {
      builder.head()
    } else {
      builder.method(method, null)
    }

    return builder.build()
  }

  fun connect(
    requestId: String,
    url: String,
    method: String,
    protocol: String,
    timeoutMs: Number?,
    headers: UTSJSONObject?,
    bodyText: String?,
    onOpen: (Number, Array<String>, Array<String>) -> Unit,
    onChunk: (String) -> Unit,
    onError: (String) -> Unit,
    onComplete: () -> Unit
  ): Boolean {
    val request = createRequest(url, method, protocol, headers, bodyText) ?: return false
    val client = createClient(timeoutMs)
    val call = client.newCall(request)

    activeCalls[requestId]?.cancel()
    activeCalls[requestId] = call

    call.enqueue(object : Callback {
      override fun onFailure(call: Call, e: IOException) {
        activeCalls.remove(requestId, call)
        postToMain {
          onError(e.message ?: "Network request failed")
          onComplete()
        }
      }

      override fun onResponse(call: Call, response: Response) {
        try {
          val headerNames = response.headers().names().toTypedArray()
          val headerValues = Array(headerNames.size) { index ->
            response.headers(headerNames[index]).firstOrNull() ?: ""
          }

          postToMain {
            onOpen(response.code(), headerNames, headerValues)
          }

          if (!response.isSuccessful) {
            postToMain {
              onError("HTTP ${response.code()}")
              onComplete()
            }
            return
          }

          val body = response.body()
          if (body == null) {
            postToMain {
              onError("Response body is empty")
              onComplete()
            }
            return
          }

          InputStreamReader(body.byteStream(), Charsets.UTF_8).use { reader ->
            val buffer = CharArray(4096)
            while (true) {
              val len = reader.read(buffer, 0, buffer.size)
              if (len == -1) break
              val text = String(buffer, 0, len)
              if (text.isEmpty()) continue

              postToMain {
                onChunk(text)
              }
            }
          }

          postToMain {
            onComplete()
          }
        } catch (e: Exception) {
          postToMain {
            onError(e.message ?: "Stream decode failed")
            onComplete()
          }
        } finally {
          activeCalls.remove(requestId, call)
          try {
            response.close()
          } catch (_: Exception) {
          }
        }
      }
    })

    return true
  }

  fun abort(requestId: String) {
    activeCalls.remove(requestId)?.cancel()
  }
}
