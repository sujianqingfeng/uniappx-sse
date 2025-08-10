<template>
  <view class="container">
    <!-- 头部 -->
    <view class="header">
      <text class="app-title">SSE 多连接 Demo</text>
      <text class="app-subtitle">SSE Plugin - Vue 3</text>
    </view>

    <!-- 中间可滚动区域 -->
    <scroll-view class="page-scroll" scroll-y="true">
      <view class="content">
        <!-- 多连接管理卡片 -->
        <view class="card">
          <text class="card-title">多连接管理</text>
          <text class="card-description">为每个 URL 生成独立连接，可分别连接/断开，消息分别显示</text>

          <view class="input-group">
            <text class="input-label">服务器 URL:</text>
            <input class="input-field" v-model="serverUrl" :placeholder="placeholderText" />
          </view>

          <view class="button-group">
            <view class="btn btn-primary" @click="addConnection">
              <text class="btn-text text-strong">添加连接</text>
            </view>
            <view class="btn btn-secondary" @click="connectAll" v-if="connections.length > 0">
              <text class="btn-text text-strong">全部连接</text>
            </view>
            <view class="btn btn-danger" @click="disconnectAll" v-if="connections.length > 0">
              <text class="btn-text text-strong">全部断开</text>
            </view>
          </view>
        </view>

        <!-- 连接列表 -->
        <view class="card" v-for="item in connections" :key="item.requestId">
          <text class="card-title">连接 {{ shortId(item.requestId) }}</text>
          <text class="card-description text-wrap">{{ item.url }}</text>

          <view class="button-group">
            <view class="btn btn-primary" @click="connectConnection(item.requestId)" v-if="!item.isConnected">
              <text class="btn-text text-strong">连接</text>
            </view>
            <view class="btn btn-danger" @click="disconnectConnection(item.requestId)" v-else>
              <text class="btn-text text-strong">断开</text>
            </view>
            <view class="btn" style="background:#999" @click="removeConnection(item.requestId)">
              <text class="btn-text text-strong">移除</text>
            </view>
          </view>

          <view class="status-card" v-if="item.status">
            <text class="status-title text-strong">状态</text>
            <text class="status-content text-wrap">{{ item.status }}</text>
          </view>

          <text class="card-title" style="margin-top: 5px;">消息</text>
          <view class="message-container">
            <view class="message-item" v-for="(msg, index) in item.messages" :key="index">
              <text class="message-time">{{ msg.time }}</text>
              <text class="message-content text-wrap">{{ msg.content }}</text>
            </view>
            <text class="no-messages" v-if="item.messages.length === 0">等待消息...</text>
          </view>
        </view>

        <!-- 结果显示区域 -->
        <view class="result-card" v-if="resultMessage">
          <text class="result-title text-strong">提示</text>
          <text class="result-content text-wrap">{{ resultMessage }}</text>
        </view>
      </view>
    </scroll-view>

    <!-- 底部信息 -->
    <view class="footer">
      <text class="footer-text">基于 uni-app（Vue 3）与 UTS 插件</text>
    </view>
  </view>
  
</template>

<script>
import { sseConnectApi, sseCloseApi, sseAddEventListenerApi, sseRemoveEventListenerApi } from '@/uni_modules/sse-plugin'

export default {
  data() {
    // 平台默认 URL
    // #ifdef APP-ANDROID
    const defaultUrl = 'http://10.0.2.2:3000/sse'
    // #endif
    // #ifndef APP-ANDROID
    const defaultUrl = 'http://localhost:3000/sse'
    // #endif

    return {
      resultMessage: '',
      serverUrl: defaultUrl,
      placeholderText: defaultUrl,
      connections: []
    }
  },
  onLoad() {
    this.setupSSEEventListeners()
  },
  onUnload() {
    this.cleanupSSE()
  },
  methods: {
    shortId(id) {
      if (!id) return ''
      return id.length > 8 ? id.slice(0, 4) + '…' + id.slice(-3) : id
    },
    addConnection() {
      if (!this.serverUrl || this.serverUrl.length === 0) {
        this.resultMessage = '请输入服务器 URL'
        return
      }
      const requestId = `sse_${Date.now()}_${Math.floor(Math.random() * 1000)}`
      this.connections.unshift({
        requestId,
        url: this.serverUrl,
        isConnected: false,
        status: '待连接',
        messages: []
      })
      this.resultMessage = `已添加连接：${requestId}`
    },
    resolveUrlForAndroid(url) {
      let resolved = url
      // #ifdef APP-ANDROID
      if (resolved && resolved.length > 0) {
        const patterns = [
          '://localhost',
          '://127.0.0.1',
          '://[::1]'
        ]
        for (let i = 0; i < patterns.length; i++) {
          const p = patterns[i]
          if (resolved.indexOf(p) !== -1) {
            resolved = resolved.replace(p, '://10.0.2.2')
          }
        }
      }
      // #endif
      return resolved
    },
    connectConnection(requestId) {
      const idx = this.connections.findIndex(c => c.requestId === requestId)
      if (idx < 0) return
      const conn = this.connections[idx]
      conn.status = '连接中…'
      const finalUrl = this.resolveUrlForAndroid(conn.url)
      if (finalUrl !== conn.url) {
        this.connections[idx].url = finalUrl
        this.connections[idx].status = '检测到 Android 环境，已将 localhost 映射为 10.0.2.2'
      }
      sseConnectApi({
        url: finalUrl,
        requestId: conn.requestId,
        headers: { 'User-Agent': 'UniApp-Vue3-SSE-Plugin' },
        fail: (err) => {
          conn.status = `连接失败: ${JSON.stringify(err)}`
        },
        complete: () => {}
      })
    },
    disconnectConnection(requestId) {
      sseCloseApi(requestId)
    },
    connectAll() {
      for (let i = 0; i < this.connections.length; i++) {
        this.connectConnection(this.connections[i].requestId)
      }
    },
    disconnectAll() {
      for (let i = 0; i < this.connections.length; i++) {
        sseCloseApi(this.connections[i].requestId)
      }
    },
    removeConnection(requestId) {
      const idx = this.connections.findIndex(c => c.requestId === requestId)
      if (idx < 0) return
      if (this.connections[idx].isConnected) {
        sseCloseApi(requestId)
      }
      this.connections.splice(idx, 1)
    },
    setupSSEEventListeners() {
      sseAddEventListenerApi({
        onOpen: (event) => {
          const idx = this.connections.findIndex(c => c.requestId === event.requestId)
          if (idx < 0) return
          this.connections[idx].isConnected = true
          this.connections[idx].status = `连接成功: ${event.requestId}`
        },
        onMessage: (event) => {
          const idx = this.connections.findIndex(c => c.requestId === event.requestId)
          if (idx < 0) return
          this.addMessage(event.requestId, event.message)
        },
        onError: (event) => {
          const idx = this.connections.findIndex(c => c.requestId === event.requestId)
          if (idx < 0) return
          this.connections[idx].status = `错误: ${event.error}`
        },
        onClose: (event) => {
          const idx = this.connections.findIndex(c => c.requestId === event.requestId)
          if (idx < 0) return
          this.connections[idx].isConnected = false
          this.connections[idx].status = `已关闭: ${event.requestId}`
        }
      })
    },
    addMessage(requestId, content) {
      const idx = this.connections.findIndex(c => c.requestId === requestId)
      if (idx < 0) return
      const now = new Date()
      const hh = now.getHours().toString().padStart(2, '0')
      const mm = now.getMinutes().toString().padStart(2, '0')
      const ss = now.getSeconds().toString().padStart(2, '0')
      this.connections[idx].messages.unshift({ time: `${hh}:${mm}:${ss}`, content })
      if (this.connections[idx].messages.length > 50) {
        this.connections[idx].messages = this.connections[idx].messages.slice(0, 50)
      }
    },
    cleanupSSE() {
      this.disconnectAll()
      // 传 null/undefined 以清空全局监听
      sseRemoveEventListenerApi(null)
    }
  }
}
</script>

<style>
  .container {
    background: #f5f5f5;
    display: flex;
    flex-direction: column;
    position: relative;
    height: 100vh;
    overflow: hidden;
  }

  .header {
    padding: 12px 20px 10px;
    text-align: center;
    background: #f5f5f5;
    border-bottom: 1px solid #eaeaea;
    flex-shrink: 0;
  }

  .app-title {
    font-size: 24px;
    font-weight: bold;
    color: #333333;
    margin-bottom: 5px;
  }

  .app-subtitle {
    font-size: 14px;
    color: #666666;
  }

  .content {
    padding: 0 20px;
    box-sizing: border-box;
    display: flex;
    flex-direction: column;
  }
  .page-scroll {
    flex: 1 1 auto;
    min-height: 0;
  }

  .card {
    background: #ffffff;
    border-radius: 8px;
    padding: 25px 20px;
    margin-bottom: 15px;
    box-shadow: 0 2px 10px rgba(0, 0, 0, 0.08);
  }

  .card-title {
    font-size: 18px;
    font-weight: bold;
    color: #333333;
    text-align: center;
    margin-bottom: 10px;
  }

  .card-description {
    font-size: 14px;
    color: #666666;
    text-align: center;
    line-height: 1.6;
    margin-bottom: 25px;
  }

  .button-group {
    display: flex;
    flex-direction: column;
  }

  .btn {
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 15px;
    border-radius: 6px;
    transition: all 0.2s ease;
    margin-bottom: 10px;
  }

  .btn:last-child { margin-bottom: 0; }
  .btn:active { transform: scale(0.98); }
  .btn-primary { background: #007aff; box-shadow: 0 4rpx 12rpx rgba(0, 122, 255, 0.3); }
  .btn-secondary { background: #34c759; box-shadow: 0 4rpx 12rpx rgba(52, 199, 89, 0.3); }
  .btn-danger { background: #ff3b30; box-shadow: 0 4rpx 12rpx rgba(255, 59, 48, 0.3); }

  .btn-text {
    font-size: 16px;
    font-weight: 700;
    color: #ffffff;
  }

  .result-card {
    background: #ffffff;
    border-radius: 8px;
    padding: 15px;
    margin-bottom: 15px;
    border-left: 3px solid #007aff;
    box-shadow: 0 2px 10px rgba(0, 0, 0, 0.08);
  }

  .result-title {
    font-size: 14px;
    font-weight: 700;
    color: #333333;
    margin-bottom: 8px;
  }

  .result-content {
    font-size: 13px;
    color: #666666;
    line-height: 1.5;
  }

  .footer {
    padding: 12px 20px;
    text-align: center;
    background: #f5f5f5;
    border-top: 1px solid #eaeaea;
    flex-shrink: 0;
  }
  .footer-text { font-size: 12px; color: #999999; }

  .input-group { margin-bottom: 20px; }
  .input-label { font-size: 14px; color: #333333; margin-bottom: 8px; display: flex; }
  .input-field {
    width: 100%;
    padding: 12px;
    border: 1px solid #e0e0e0;
    border-radius: 4px;
    font-size: 14px;
    background: #ffffff;
  }
  .input-field:focus { border-color: #007aff; }

  .message-container { max-height: 200px; overflow: hidden; }
  .message-item { padding: 10px 0; border-bottom: 1px solid #f0f0f0; }
  .message-item:last-child { border-bottom: none; }
  .message-time { font-size: 12px; color: #999999; margin-bottom: 4px; display: flex; }
  .message-content { font-size: 13px; color: #333333; line-height: 1.4; }
  .no-messages { font-size: 13px; color: #999999; text-align: center; padding: 20px 0; }

  .status-card {
    background: #ffffff;
    border-radius: 8px;
    padding: 15px;
    margin-bottom: 15px;
    border-left: 3px solid #ff9500;
    box-shadow: 0 2px 10px rgba(0, 0, 0, 0.08);
  }
  .status-title { font-size: 14px; font-weight: 700; color: #333333; margin-bottom: 8px; }
  .status-content { font-size: 13px; color: #666666; line-height: 1.5; }
</style>
