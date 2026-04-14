<template>
  <scroll-view class="page" scroll-y>
    <view class="container">
      <view class="hero">
        <text class="title">WangAI Agent SSE 验证</text>
        <text class="subtitle">在 uniapp-sse-playground 里模拟 agent-new 的 start、resume、retry、stop 链路</text>
        <view class="hero-actions">
          <view class="hero-btn" @tap="goToGenericLab">
            <text class="hero-btn-text">通用实验页</text>
          </view>
          <view class="hero-btn ghost" @tap="restoreDefaults">
            <text class="hero-btn-text ghost-text">恢复默认</text>
          </view>
        </view>
      </view>

      <view class="card">
        <text class="section-title">01 / 地址与鉴权</text>

        <text class="label">Base URL</text>
        <textarea
          :value="form.baseUrl"
          class="textarea mini-textarea"
          auto-height
          placeholder="例如 https://wangkeapp.test.wangxiaobao.com"
          @input="setFormField('baseUrl', $event.detail.value)"
        />

        <text class="label">Token</text>
        <textarea
          :value="form.token"
          class="textarea token-textarea"
          auto-height
          placeholder="粘贴 x-auth-token / Bearer token"
          @input="setFormField('token', $event.detail.value)"
        />

        <view class="split-fields">
          <view class="field-half">
            <text class="label">x-source-platform</text>
            <textarea
              :value="form.sourcePlatform"
              class="textarea mini-textarea"
              auto-height
              placeholder="IWANGKE"
              @input="setFormField('sourcePlatform', $event.detail.value)"
            />
          </view>
          <view class="field-half">
            <text class="label">x-platform-client</text>
            <textarea
              :value="form.platformClient"
              class="textarea mini-textarea"
              auto-height
              placeholder="iwangke"
              @input="setFormField('platformClient', $event.detail.value)"
            />
          </view>
        </view>

        <view class="split-fields">
          <view class="field-half">
            <text class="label">templateType</text>
            <textarea
              :value="form.templateType"
              class="textarea mini-textarea"
              auto-height
              placeholder="2"
              @input="setFormField('templateType', $event.detail.value)"
            />
          </view>
          <view class="field-half">
            <text class="label">timeout(ms)</text>
            <textarea
              :value="form.timeoutMs"
              class="textarea mini-textarea"
              auto-height
              placeholder="1800000"
              @input="setFormField('timeoutMs', $event.detail.value)"
            />
          </view>
        </view>

        <text class="tip">流地址会自动追加 `/wang-ai/...`，Android 模拟器里 `localhost` 会自动映射成 `10.0.2.2`。</text>
      </view>

      <view class="card">
        <text class="section-title">02 / 业务参数</text>

        <text class="label">Prompt</text>
        <textarea
          :value="form.prompt"
          class="textarea"
          auto-height
          placeholder="输入 agent-new 的提问内容"
          @input="setFormField('prompt', $event.detail.value)"
        />

        <text class="label">threadId</text>
        <textarea
          :value="form.threadId"
          class="textarea mini-textarea"
          auto-height
          placeholder="start 可先创建 thread，再发起流"
          @input="setFormField('threadId', $event.detail.value)"
        />

        <view class="split-fields">
          <view class="field-half">
            <text class="label">runId</text>
            <textarea
              :value="form.runId"
              class="textarea mini-textarea"
              auto-height
              placeholder="resume / retry 使用"
              @input="setFormField('runId', $event.detail.value)"
            />
          </view>
          <view class="field-half">
            <text class="label">reportId</text>
            <textarea
              :value="form.reportId"
              class="textarea mini-textarea"
              auto-height
              placeholder="retry 使用"
              @input="setFormField('reportId', $event.detail.value)"
            />
          </view>
        </view>

        <text class="label">checkpointId</text>
        <textarea
          :value="form.checkpointId"
          class="textarea mini-textarea"
          auto-height
          placeholder="retry 使用，可留空"
          @input="setFormField('checkpointId', $event.detail.value)"
        />
      </view>

      <view class="card">
        <text class="section-title">03 / 模式与执行</text>

        <text class="label">先选模式</text>
        <view class="tabs">
          <view
            v-for="(item, index) in actionLabels"
            :key="item"
            class="tab"
            :class="{ active: actionIndex === index }"
            @tap="pickAction(index)"
          >
            <text class="tab-text" :class="{ 'tab-text-active': actionIndex === index }">{{ item }}</text>
          </view>
        </view>
        <text class="tip action-mode-tip">这里只是切换模式，不会发请求。真正发请求请点下面的“执行”按钮。</text>

        <view class="actions-column">
          <view class="action-row">
            <view class="btn accent full-btn" @click="handleCreateThread">
              <text class="btn-text accent-text">创建 Thread</text>
            </view>
          </view>
          <view class="action-row">
            <view class="btn primary full-btn" @click="runCurrentAction">
              <text class="btn-text primary-text">{{ currentActionButtonLabel }}</text>
            </view>
          </view>
          <view class="action-row">
            <view class="btn full-btn" @click="handleFetchRunId">
              <text class="btn-text">查询 RunId</text>
            </view>
          </view>
          <view class="action-row">
            <view class="btn danger full-btn" @click="handleStop">
              <text class="btn-text">断开当前连接</text>
            </view>
          </view>
          <view class="action-row">
            <view class="btn full-btn" @click="handleAutoResumeTest">
              <text class="btn-text">自动测 Resume</text>
            </view>
          </view>
          <view class="action-row">
            <view class="btn full-btn" @click="handleAutoRetryTest">
              <text class="btn-text">自动测 Retry</text>
            </view>
          </view>
        </view>
        <text class="tip action-mode-tip">“断开当前连接”只会关闭本地 SSE，不会取消服务端任务，适合用来验证 resume。</text>
        <text class="tip action-mode-tip">自动化按钮会新开一轮 start，然后自动执行 resume 或 retry。</text>

        <view class="status-box">
          <view class="status-row">
            <text class="status-label">状态</text>
            <text class="status-value">{{ statusText }}</text>
          </view>
          <view class="divider"></view>
          <view class="status-row">
            <text class="status-label">动作</text>
            <text class="status-value">{{ currentActionLabel }}</text>
          </view>
          <view class="status-row">
            <text class="status-label">结束原因</text>
            <text class="status-value">{{ closeReason || '-' }}</text>
          </view>
          <view class="status-row">
            <text class="status-label">统计</text>
            <text class="status-value">chunk {{ chunkCount }} / message {{ messageCount }}</text>
          </view>
          <view class="status-row">
            <text class="status-label">提取结果</text>
            <text class="status-value small-value">runId: {{ form.runId || '-' }} / reportId: {{ form.reportId || '-' }}</text>
          </view>
          <view class="status-row">
            <text class="status-label">checkpointId</text>
            <text class="status-value small-value">{{ form.checkpointId || '-' }}</text>
          </view>
          <view class="status-row">
            <text class="status-label">自动化</text>
            <text class="status-value">{{ automationName || '-' }}</text>
          </view>
        </view>
      </view>

      <view class="card">
        <text class="section-title">04 / 请求预览</text>

        <text class="preview-label">URL</text>
        <text class="preview-text">{{ currentRequestUrl || '请选择动作并填写参数' }}</text>

        <text class="preview-label">Headers</text>
        <text class="payload-text">{{ currentRequestHeaders }}</text>

        <text class="preview-label">Body</text>
        <text class="payload-text">{{ currentRequestBody }}</text>
      </view>

      <view class="card">
        <text class="section-title">05 / AI 输出</text>

        <text class="preview-label">最近事件</text>
        <text class="preview-text">{{ latestEventName || '暂无' }}</text>

        <text class="preview-label">累计文本</text>
        <text class="response-text">{{ assistantText || '等待流式文本...' }}</text>

        <text class="preview-label">最近 payload</text>
        <text class="payload-text">{{ latestPayloadText || '暂无' }}</text>
      </view>
    </view>
  </scroll-view>
</template>

<script>
import {
  WANGAI_STREAM_TIMEOUT,
  buildAgentHeaders,
  buildCreateThreadUrl,
  buildCurrentRunIdUrl,
  buildResumeStreamUrl,
  buildThreadHistoryUrl,
  buildRetryPayload,
  buildStartPayload,
  buildStartStreamUrl,
  createWangaiAgentStreamBridge,
  normalizeRuntimeUrl
} from '@/utils/wangai-agent-stream'

const STORAGE_KEY = 'wangai-agent-playground-config-v1'
const DEFAULT_PROXY_BASE_URL = 'https://wangkeapp.test.wangxiaobao.com'
const DEFAULT_DEBUG_TOKEN = 'ed287c55-67e4-41eb-bb18-19711983c7d1'
const LOG_TAG = '[wangai-agent-playground]'

function createDefaultForm() {
  return {
    baseUrl: DEFAULT_PROXY_BASE_URL,
    token: DEFAULT_DEBUG_TOKEN,
    sourcePlatform: 'IWANGKE',
    platformClient: 'iwangke',
    prompt: '帮我说明一下这个流式接口的事件结构',
    threadId: '',
    runId: '',
    reportId: '',
    checkpointId: '',
    timeoutMs: `${WANGAI_STREAM_TIMEOUT}`,
    templateType: '2'
  }
}

function normalizeStoredValue(value, fallback = '') {
  const text = `${value == null ? '' : value}`.trim()
  return text || fallback
}

function mergeStoredForm(stored = {}) {
  const defaults = createDefaultForm()
  return {
    ...defaults,
    ...stored,
    baseUrl: normalizeStoredValue(stored?.baseUrl, defaults.baseUrl),
    token: normalizeStoredValue(stored?.token, defaults.token),
    sourcePlatform: normalizeStoredValue(stored?.sourcePlatform, defaults.sourcePlatform),
    platformClient: normalizeStoredValue(stored?.platformClient, defaults.platformClient),
    prompt: normalizeStoredValue(stored?.prompt, defaults.prompt),
    timeoutMs: normalizeStoredValue(stored?.timeoutMs, defaults.timeoutMs),
    templateType: normalizeStoredValue(stored?.templateType, defaults.templateType),
    threadId: normalizeStoredValue(stored?.threadId, ''),
    runId: normalizeStoredValue(stored?.runId, ''),
    reportId: normalizeStoredValue(stored?.reportId, ''),
    checkpointId: normalizeStoredValue(stored?.checkpointId, '')
  }
}

export default {
  data() {
    return {
      actionLabels: ['选择 Start 模式', '选择 Resume 模式', '选择 Retry 模式'],
      actionValues: ['start', 'resume', 'retry'],
      actionIndex: 0,
      form: createDefaultForm(),
      statusText: '未连接',
      closeReason: '',
      chunkCount: 0,
      messageCount: 0,
      latestEventName: '',
      latestPayloadText: '',
      assistantText: '',
      pendingAction: '',
      automationName: '',
      automationToken: '',
      activeBridge: null,
      activeStreamToken: '',
      streamTokenSeed: 0,
      automationTokenSeed: 0
    }
  },
  computed: {
    currentAction() {
      return this.actionValues[this.actionIndex] || 'start'
    },
    currentActionLabel() {
      return this.actionLabels[this.actionIndex] || 'Start'
    },
    currentActionButtonLabel() {
      if (this.currentAction === 'resume') {
        return '执行 Resume 请求'
      }
      if (this.currentAction === 'retry') {
        return '执行 Retry 请求'
      }
      return '执行 Start 请求'
    },
    currentRequestUrl() {
      try {
        if (this.currentAction === 'resume') {
          if (!this.form.threadId || !this.form.runId) {
            return ''
          }
          return normalizeRuntimeUrl(
            buildResumeStreamUrl(this.form.baseUrl, this.form.threadId, this.form.runId)
          )
        }
        return normalizeRuntimeUrl(buildStartStreamUrl(this.form.baseUrl))
      } catch (error) {
        return ''
      }
    },
    currentRequestHeaders() {
      return this.stringifyPretty(this.createRequestHeaders())
    },
    currentRequestBody() {
      if (this.currentAction === 'resume') {
        return 'GET 请求，无 body'
      }

      const prompt = `${this.form.prompt || ''}`.trim()
      const threadId = `${this.form.threadId || ''}`.trim()
      if (!prompt || !threadId) {
        return '填写 prompt 和 threadId 后可预览'
      }

      const payload = this.currentAction === 'retry'
        ? buildRetryPayload({
          prompt,
          threadId,
          runId: this.form.runId,
          checkpointId: this.form.checkpointId,
          reportId: this.form.reportId,
          templateType: this.normalizeTemplateType()
        })
        : buildStartPayload({
          prompt,
          threadId,
          templateType: this.normalizeTemplateType()
        })

      return this.stringifyPretty(payload)
    }
  },
  onLoad() {
    this.restoreConfig()
    this.pushLog('config', this.stringifyPretty({
      baseUrl: this.form.baseUrl,
      hasToken: !!this.form.token,
      sourcePlatform: this.form.sourcePlatform,
      platformClient: this.form.platformClient
    }, ''))
  },
  onUnload() {
    this.persistConfig()
    this.automationName = ''
    this.automationToken = ''
    this.stopActiveStream({
      silent: true,
      reason: 'dispose-abort'
    })
  },
  methods: {
    pickAction(index) {
      this.pushLog('tap', `select mode -> ${this.actionValues[index] || index}`)
      this.actionIndex = index
    },
    goToGenericLab() {
      this.pushLog('tap', 'go to generic lab')
      uni.navigateTo({
        url: '/pages/index/index'
      })
    },
    restoreDefaults() {
      this.pushLog('tap', 'restore defaults')
      this.form = createDefaultForm()
      this.persistConfig()
      this.pushLog('config', '已恢复默认配置')
    },
    restoreConfig() {
      try {
        const stored = uni.getStorageSync(STORAGE_KEY)
        if (stored && typeof stored === 'object') {
          this.form = mergeStoredForm(stored)
        } else {
          this.form = createDefaultForm()
        }
        this.persistConfig()
      } catch (error) {
        this.form = createDefaultForm()
      }
    },
    persistConfig() {
      try {
        uni.setStorageSync(STORAGE_KEY, this.form)
      } catch (error) {
      }
    },
    setFormField(key, value) {
      this.form = {
        ...this.form,
        [key]: value
      }
    },
    normalizeTemplateType() {
      const numeric = Number(this.form.templateType)
      return Number.isFinite(numeric) && numeric > 0 ? numeric : 2
    },
    normalizeTimeout() {
      const numeric = Number(this.form.timeoutMs)
      return Number.isFinite(numeric) && numeric > 0 ? numeric : WANGAI_STREAM_TIMEOUT
    },
    createRequestHeaders() {
      return buildAgentHeaders({
        token: this.form.token,
        sourcePlatform: this.form.sourcePlatform,
        platformClient: this.form.platformClient
      })
    },
    nowText() {
      const now = new Date()
      const hh = now.getHours().toString().padStart(2, '0')
      const mm = now.getMinutes().toString().padStart(2, '0')
      const ss = now.getSeconds().toString().padStart(2, '0')
      return `${hh}:${mm}:${ss}`
    },
    stringifySafe(value, fallback = '') {
      if (typeof value === 'string') {
        return value
      }
      try {
        const text = JSON.stringify(value)
        return text == null ? fallback : text
      } catch (error) {
        return fallback
      }
    },
    stringifyPretty(value, fallback = '') {
      if (typeof value === 'string') {
        return value
      }
      try {
        const text = JSON.stringify(value, null, 2)
        return text == null ? fallback : text
      } catch (error) {
        return fallback
      }
    },
    trimLogText(value, maxLength = 1800) {
      const text = typeof value === 'string' ? value : this.stringifySafe(value, '')
      if (text.length <= maxLength) {
        return text
      }
      return `${text.slice(0, maxLength)}...`
    },
    shouldLogKind(kind) {
      const value = `${kind || ''}`
      if (value === 'config' || value === 'action' || value === 'action:prepare' || value === 'open' || value === 'complete' || value === 'automation') {
        return true
      }
      if (value === 'http:create-thread' || value === 'http:run-id' || value === 'http:history' || value === 'context') {
        return true
      }
      if (value === 'error' || value === 'warn' || value === 'abort') {
        return true
      }
      if (value === 'tap') {
        return true
      }
      if (value === 'message:metadata' || value === 'message:messages') {
        return true
      }
      return false
    },
    pushLog(kind, text) {
      if (!this.shouldLogKind(kind)) {
        return
      }
      const prefix = `${LOG_TAG}[${this.nowText()}][${kind}]`
      const message = this.trimLogText(text)
      if (`${kind}`.indexOf('error') !== -1) {
        console.error(prefix, message)
        return
      }
      if (`${kind}`.indexOf('abort') !== -1 || `${kind}`.indexOf('warn') !== -1) {
        console.warn(prefix, message)
        return
      }
      console.log(prefix, message)
    },
    showToast(title) {
      uni.showToast({
        title,
        icon: 'none'
      })
    },
    beginPendingAction(name) {
      if (this.pendingAction) {
        this.pushLog('warn', `忽略重复操作，当前进行中: ${this.pendingAction}`)
        return false
      }
      this.pendingAction = name
      return true
    },
    endPendingAction(name = '') {
      if (!name || this.pendingAction === name) {
        this.pendingAction = ''
      }
    },
    createAutomationToken() {
      this.automationTokenSeed += 1
      return `wangai-auto-${Date.now()}-${this.automationTokenSeed}`
    },
    beginAutomation(name) {
      if (this.automationName) {
        const message = `已有自动化执行中: ${this.automationName}`
        this.pushLog('warn', message)
        this.showToast(message)
        return ''
      }

      const token = this.createAutomationToken()
      this.automationName = name
      this.automationToken = token
      this.pushLog('automation', `开始 ${name}`)
      return token
    },
    finishAutomation(token, message = '') {
      if (token && this.automationToken && token !== this.automationToken) {
        return
      }
      if (message) {
        this.pushLog('automation', message)
      }
      this.automationName = ''
      this.automationToken = ''
    },
    ensureAutomationActive(token) {
      if (!token || token !== this.automationToken) {
        throw new Error('自动化流程已取消')
      }
    },
    sleep(ms = 0) {
      return new Promise((resolve) => {
        setTimeout(resolve, Math.max(0, Number(ms) || 0))
      })
    },
    async waitForCondition(checker, {
      timeoutMs = 15000,
      intervalMs = 120,
      token = '',
      label = 'condition'
    } = {}) {
      const startedAt = Date.now()
      while (Date.now() - startedAt < timeoutMs) {
        if (token) {
          this.ensureAutomationActive(token)
        }

        let result = false
        try {
          result = checker()
        } catch (error) {
          result = false
        }

        if (result) {
          return result
        }

        await this.sleep(intervalMs)
      }

      throw new Error(`${label} 超时`)
    },
    setActionValue(action) {
      const index = this.actionValues.findIndex((item) => item === action)
      if (index >= 0) {
        this.actionIndex = index
      }
    },
    resetRunContext() {
      this.form = {
        ...this.form,
        threadId: '',
        runId: '',
        reportId: '',
        checkpointId: ''
      }
    },
    async requestJson({ url, method = 'GET', data = null, headers = null }) {
      const runtimeUrl = normalizeRuntimeUrl(url)
      if (runtimeUrl !== url) {
        this.pushLog('config', `runtime url -> ${runtimeUrl}`)
      }

      return new Promise((resolve, reject) => {
        uni.request({
          url: runtimeUrl,
          method,
          data,
          header: headers || {},
          timeout: this.normalizeTimeout(),
          success: (response) => {
            resolve(response)
          },
          fail: (error) => {
            reject(error)
          }
        })
      })
    },
    isBusinessSuccess(response) {
      const code = response?.code
      if (code == null) {
        return true
      }
      return `${code}` === '0'
    },
    createStreamToken() {
      this.streamTokenSeed += 1
      return `wangai-agent-${Date.now()}-${this.streamTokenSeed}`
    },
    stopActiveStream({ silent = false, reason = '' } = {}) {
      const bridge = this.activeBridge
      if (!bridge) {
        if (silent) {
          this.activeStreamToken = ''
        }
        return
      }

      this.activeBridge = null
      const activeToken = this.activeStreamToken

      if (silent) {
        bridge.abort({
          silent: true,
          reason: reason || 'dispose-abort'
        })
        if (this.activeStreamToken === activeToken) {
          this.activeStreamToken = ''
        }
        return
      }

      bridge.abort({
        silent: false,
        reason: reason || 'manual-abort'
      })
    },
    resetStreamSnapshot(action) {
      this.statusText = `准备 ${action}`
      this.closeReason = ''
      this.chunkCount = 0
      this.messageCount = 0
      this.latestEventName = ''
      this.latestPayloadText = ''
      this.assistantText = ''
      this.pushLog('action', `开始 ${action}`)
    },
    appendAssistantText(text) {
      if (typeof text !== 'string' || !text) {
        return
      }
      this.assistantText += text
    },
    tryParsePayload(payload) {
      if (typeof payload !== 'string') {
        return payload
      }
      const text = payload.trim()
      if (!text) {
        return ''
      }
      try {
        return JSON.parse(text)
      } catch (error) {
        return text
      }
    },
    parseEventName(name = '') {
      const parts = `${name || ''}`.split('|')
      return {
        base: parts[0] || '',
        namespace: parts.slice(1)
      }
    },
    extractReportId(payload) {
      if (!payload || typeof payload !== 'object') {
        return ''
      }
      const candidates = [
        payload.reportId,
        payload.report_id,
        payload.data?.reportId,
        payload.data?.report_id,
        payload.metadata?.reportId,
        payload.metadata?.report_id
      ]
      const matched = candidates.find((item) => item !== undefined && item !== null && `${item}`)
      return matched ? `${matched}` : ''
    },
    extractMessageLikeReportId(payload) {
      if (!payload || typeof payload !== 'object') {
        return ''
      }
      const candidates = [
        payload.biz_id,
        payload.bizId,
        payload.additional_kwargs?.biz_id,
        payload.additional_kwargs?.bizId,
        payload.response_metadata?.biz_id,
        payload.response_metadata?.bizId,
        payload.metadata?.biz_id,
        payload.metadata?.bizId
      ]
      const matched = candidates.find((item) => item !== undefined && item !== null && `${item}`)
      return matched ? `${matched}` : ''
    },
    extractRunId(payload) {
      if (!payload || typeof payload !== 'object') {
        return ''
      }
      const candidates = [
        payload.run_id,
        payload.runId,
        payload.metadata?.run_id,
        payload.metadata?.runId,
        payload.response_metadata?.run_id,
        payload.response_metadata?.runId,
        payload.additional_kwargs?.run_id,
        payload.additional_kwargs?.runId
      ]
      const matched = candidates.find((item) => item !== undefined && item !== null && `${item}`)
      return matched ? `${matched}` : ''
    },
    extractCheckpointId(payload) {
      if (!payload || typeof payload !== 'object') {
        return ''
      }
      const candidates = [
        payload.checkpointId,
        payload.checkpoint_id,
        payload.data?.checkpointId,
        payload.data?.checkpoint_id,
        payload.metadata?.checkpointId,
        payload.metadata?.checkpoint_id
      ]
      const matched = candidates.find((item) => item !== undefined && item !== null && `${item}`)
      return matched ? `${matched}` : ''
    },
    extractHistoryEntries(responseData) {
      const content = responseData?.data?.content
      return Array.isArray(content) ? content : []
    },
    pickLatestHistoryEntry(entries = [], preferredRunId = '') {
      if (!Array.isArray(entries) || entries.length === 0) {
        return null
      }

      const normalizedRunId = `${preferredRunId || ''}`.trim()
      if (normalizedRunId) {
        const exactMatch = entries.find((item) => `${item?.runId || ''}`.trim() === normalizedRunId)
        if (exactMatch) {
          return exactMatch
        }
      }

      const sortedEntries = [...entries].sort((left, right) => {
        const leftTime = Date.parse(left?.updatedAt || left?.createdAt || '') || 0
        const rightTime = Date.parse(right?.updatedAt || right?.createdAt || '') || 0
        return rightTime - leftTime
      })

      return sortedEntries[0] || null
    },
    extractHistoryReportId(entry) {
      if (!entry || typeof entry !== 'object') {
        return ''
      }
      const candidates = [
        entry.metadata?.biz_id,
        entry.metadata?.bizId,
        entry.reportId,
        entry.report_id,
        entry.lastMessage?.metadata?.biz_id,
        entry.lastMessage?.metadata?.bizId
      ]
      const matched = candidates.find((item) => item !== undefined && item !== null && `${item}`)
      return matched ? `${matched}` : ''
    },
    extractHistoryCheckpointId(entry) {
      if (!entry || typeof entry !== 'object') {
        return ''
      }
      const candidates = [
        entry.checkpoint_id,
        entry.checkpointId,
        entry.metadata?.checkpoint_id,
        entry.metadata?.checkpointId
      ]
      const matched = candidates.find((item) => item !== undefined && item !== null && `${item}`)
      return matched ? `${matched}` : ''
    },
    async fetchThreadHistory(threadId) {
      const normalizedThreadId = `${threadId || ''}`.trim()
      if (!normalizedThreadId) {
        throw new Error('缺少 threadId，无法查询 history')
      }

      const response = await this.requestJson({
        url: buildThreadHistoryUrl(this.form.baseUrl, normalizedThreadId),
        method: 'POST',
        headers: this.createRequestHeaders(),
        data: {
          size: 100
        }
      })

      if (response.statusCode < 200 || response.statusCode >= 300) {
        throw new Error(`HTTP ${response.statusCode}`)
      }

      if (!this.isBusinessSuccess(response.data)) {
        throw new Error(response.data?.msg || '查询 history 失败')
      }

      return response
    },
    async syncRunContext({ threadId = '', preferredRunId = '', fetchLatestRunId = false } = {}) {
      const normalizedThreadId = `${threadId || this.form.threadId || ''}`.trim()
      if (!normalizedThreadId) {
        throw new Error('缺少 threadId，无法同步上下文')
      }

      let resolvedRunId = `${preferredRunId || this.form.runId || ''}`.trim()

      if (fetchLatestRunId) {
        const runIdResponse = await this.requestJson({
          url: buildCurrentRunIdUrl(this.form.baseUrl, normalizedThreadId),
          method: 'GET',
          headers: this.createRequestHeaders()
        })

        this.pushLog('http:run-id', this.stringifyPretty(runIdResponse.data, ''))

        if (runIdResponse.statusCode < 200 || runIdResponse.statusCode >= 300) {
          throw new Error(`HTTP ${runIdResponse.statusCode}`)
        }

        if (!this.isBusinessSuccess(runIdResponse.data)) {
          throw new Error(runIdResponse.data?.msg || '查询 runId 失败')
        }

        const latestRunId = runIdResponse.data?.data?.runId || runIdResponse.data?.data || ''
        if (latestRunId) {
          resolvedRunId = `${latestRunId}`
        }
      }

      const historyResponse = await this.fetchThreadHistory(normalizedThreadId)
      const entries = this.extractHistoryEntries(historyResponse.data)
      const targetEntry = this.pickLatestHistoryEntry(entries, resolvedRunId)

      const context = {
        threadId: normalizedThreadId,
        total: entries.length,
        runId: `${targetEntry?.runId || resolvedRunId || ''}`.trim(),
        reportId: this.extractHistoryReportId(targetEntry),
        checkpointId: this.extractHistoryCheckpointId(targetEntry)
      }

      this.pushLog('http:history', this.stringifyPretty({
        total: context.total,
        runId: context.runId,
        reportId: context.reportId,
        checkpointId: context.checkpointId
      }, ''))

      if (context.runId) {
        this.form.runId = context.runId
      }
      if (context.reportId) {
        this.form.reportId = context.reportId
      }
      if (context.checkpointId) {
        this.form.checkpointId = context.checkpointId
      }

      this.pushLog('context', this.stringifyPretty(context, ''))
      return context
    },
    collectChunkText(content) {
      if (typeof content === 'string') {
        return content
      }
      if (Array.isArray(content)) {
        return content
          .map((item) => {
            if (typeof item === 'string') {
              return item
            }
            if (typeof item?.text === 'string') {
              return item.text
            }
            if (typeof item?.content === 'string') {
              return item.content
            }
            return ''
          })
          .join('')
      }
      if (content && typeof content === 'object') {
        if (typeof content.text === 'string') {
          return content.text
        }
        if (typeof content.content === 'string') {
          return content.content
        }
      }
      return ''
    },
    extractFallbackText(payload) {
      if (!payload || typeof payload !== 'object') {
        return ''
      }
      const candidates = [
        payload.text,
        payload.content,
        payload.message,
        payload.delta?.text,
        payload.delta?.content,
        payload.data?.text,
        payload.data?.content,
        payload.output_text,
        payload.answer
      ]
      const matched = candidates.find((item) => typeof item === 'string' && item)
      return matched || ''
    },
    extractMessageChunkLogText(payload) {
      const chunks = Array.isArray(payload) ? payload : [payload]
      const texts = []
      let hasFinalChunk = false

      chunks.forEach((item) => {
        if (!item || typeof item !== 'object') {
          if (typeof item === 'string' && item) {
            texts.push(item)
          }
          return
        }

        if (item.type === 'AIMessageChunk') {
          const text = this.collectChunkText(item.content)
          if (text) {
            texts.push(text)
          }
          if (
            item.chunk_position === 'last' ||
            item.response_metadata?.finish_reason === 'stop'
          ) {
            hasFinalChunk = true
          }
        }
      })

      const merged = texts.join('')
      if (merged) {
        return merged
      }
      if (hasFinalChunk) {
        return '[messages:end]'
      }
      return ''
    },
    summarizeStreamLog(payload, eventName = '') {
      const { base } = this.parseEventName(eventName)

      if (eventName === 'metadata' || base === 'metadata') {
        return this.stringifyPretty(payload, this.stringifySafe(payload, ''))
      }

      if (eventName === 'messages' || base === 'messages') {
        return this.extractMessageChunkLogText(payload)
      }

      if (eventName === 'error' || base === 'error') {
        return this.stringifyPretty(payload, this.stringifySafe(payload, ''))
      }

      return ''
    },
    processStreamPayload(payload, eventName = '') {
      if (Array.isArray(payload)) {
        payload.forEach((item) => this.processStreamPayload(item, eventName))
        return
      }

      const { base } = this.parseEventName(eventName)

      if (eventName === 'error' || base === 'error') {
        const errorMessage = typeof payload === 'string'
          ? payload
          : payload?.message || payload?.error || '服务端返回错误事件'
        this.statusText = errorMessage
        return
      }

      if (eventName === 'done' || eventName === 'close' || base === 'done' || base === 'close' || payload === '[DONE]') {
        this.statusText = '服务端标记完成'
        return
      }

      if (!payload) {
        return
      }

      if (typeof payload === 'string') {
        if (!base || base === 'messages') {
          this.appendAssistantText(payload)
        }
        return
      }

      if (typeof payload !== 'object') {
        return
      }

      const reportId = this.extractMessageLikeReportId(payload) || this.extractReportId(payload)
      if (reportId) {
        this.form.reportId = reportId
      }

      const runId = this.extractRunId(payload)
      if (runId) {
        this.form.runId = runId
      }

      const checkpointId = this.extractCheckpointId(payload)
      if (checkpointId) {
        this.form.checkpointId = checkpointId
      }

      const normalizedType = String(
        payload.type || payload.message_type || payload.event || payload.data?.type || ''
      ).toLowerCase()

      if (normalizedType === 'metadata') {
        return
      }

      if (base && base !== 'messages') {
        return
      }

      let hasChunkText = false
      const appendMessageChunk = (messageLike) => {
        if (!messageLike || typeof messageLike !== 'object') {
          return
        }

        const nestedRunId = this.extractRunId(messageLike)
        if (nestedRunId && !this.form.runId) {
          this.form.runId = nestedRunId
        }

        const nestedReportId = this.extractMessageLikeReportId(messageLike)
        if (nestedReportId && !this.form.reportId) {
          this.form.reportId = nestedReportId
        }

        if (messageLike.type !== 'AIMessageChunk') {
          return
        }

        const text = this.collectChunkText(messageLike.content)
        if (!text) {
          return
        }

        hasChunkText = true
        this.appendAssistantText(text)
      }

      if (Array.isArray(payload)) {
        payload.forEach((item) => appendMessageChunk(item))
      } else {
        appendMessageChunk(payload)
      }

      if (!hasChunkText) {
        const fallbackText = this.extractFallbackText(payload)
        if (fallbackText) {
          this.appendAssistantText(fallbackText)
        }
      }
    },
    handleStreamMessage(message) {
      this.messageCount += 1
      const eventName = message?.event || 'message'
      this.latestEventName = eventName

      const payload = this.tryParsePayload(message?.data)
      const summary = this.summarizeStreamLog(payload, eventName)
      if (summary) {
        this.pushLog(`message:${eventName}`, summary)
      }
      this.latestPayloadText = this.stringifyPretty(payload, this.stringifySafe(payload, ''))
      this.processStreamPayload(payload, eventName)
    },
    async createThreadRequest({ toastOnSuccess = false } = {}) {
      const prompt = `${this.form.prompt || ''}`.trim()
      if (!prompt) {
        throw new Error('请先填写 prompt')
      }

      const response = await this.requestJson({
        url: buildCreateThreadUrl(this.form.baseUrl),
        method: 'POST',
        headers: this.createRequestHeaders(),
        data: {
          metadata: {},
          prompt,
          templateType: this.normalizeTemplateType()
        }
      })

      this.pushLog('http:create-thread', this.stringifyPretty(response.data, ''))

      if (response.statusCode < 200 || response.statusCode >= 300) {
        throw new Error(`HTTP ${response.statusCode}`)
      }

      if (!this.isBusinessSuccess(response.data)) {
        throw new Error(response.data?.msg || '创建 thread 失败')
      }

      const threadId = response.data?.data?.threadId || response.data?.data?.id || response.data?.data?.sessionId || ''
      if (!threadId) {
        throw new Error('接口返回成功，但缺少 threadId')
      }

      this.form.threadId = `${threadId}`
      this.statusText = 'thread 已创建'
      if (toastOnSuccess) {
        this.showToast('thread 已创建')
      }
      return this.form.threadId
    },
    async handleCreateThread() {
      this.pushLog('tap', 'click create-thread')
      this.persistConfig()
      if (!this.beginPendingAction('create-thread')) {
        return
      }

      try {
        await this.createThreadRequest({
          toastOnSuccess: true
        })
      } catch (error) {
        const message = error?.message || '创建 thread 失败'
        this.statusText = message
        this.pushLog('error', message)
        this.showToast(message)
      } finally {
        this.endPendingAction('create-thread')
      }
    },
    async handleFetchRunId() {
      this.pushLog('tap', 'click fetch-run-id')
      this.persistConfig()
      if (!this.beginPendingAction('fetch-run-id')) {
        return
      }

      const threadId = `${this.form.threadId || ''}`.trim()
      if (!threadId) {
        this.showToast('请先填写 threadId')
        this.endPendingAction('fetch-run-id')
        return
      }

      try {
        const response = await this.requestJson({
          url: buildCurrentRunIdUrl(this.form.baseUrl, threadId),
          method: 'GET',
          headers: this.createRequestHeaders()
        })

        this.pushLog('http:run-id', this.stringifyPretty(response.data, ''))

        if (response.statusCode < 200 || response.statusCode >= 300) {
          throw new Error(`HTTP ${response.statusCode}`)
        }

        if (!this.isBusinessSuccess(response.data)) {
          throw new Error(response.data?.msg || '查询 runId 失败')
        }

        const runId = response.data?.data?.runId || response.data?.data || ''
        if (!runId) {
          throw new Error('接口返回成功，但缺少 runId')
        }

        this.form.runId = `${runId}`
        const context = await this.syncRunContext({
          threadId,
          preferredRunId: this.form.runId,
          fetchLatestRunId: false
        })

        this.statusText = context.checkpointId ? 'runId / retry 参数已更新' : 'runId 已更新'
        this.showToast(context.checkpointId ? 'runId / retry 参数已更新' : 'runId 已更新')
      } catch (error) {
        const message = error?.message || '查询 runId 失败'
        this.statusText = message
        this.pushLog('error', message)
        this.showToast(message)
      } finally {
        this.endPendingAction('fetch-run-id')
      }
    },
    async runCurrentAction() {
      this.pushLog('tap', `click run-current-action -> ${this.currentAction}`)
      this.persistConfig()

      const action = this.currentAction
      if (!this.beginPendingAction(`stream-${action}`)) {
        return
      }

      const prompt = `${this.form.prompt || ''}`.trim()
      let threadId = `${this.form.threadId || ''}`.trim()
      let runId = `${this.form.runId || ''}`.trim()
      let reportId = `${this.form.reportId || ''}`.trim()
      let checkpointId = `${this.form.checkpointId || ''}`.trim()

      if ((action === 'start' || action === 'retry') && !prompt) {
        this.showToast('请先填写 prompt')
        this.endPendingAction(`stream-${action}`)
        return
      }

      if (action === 'start' && !threadId) {
        try {
          threadId = await this.createThreadRequest({
            toastOnSuccess: false
          })
        } catch (error) {
          const message = error?.message || '创建 thread 失败'
          this.statusText = message
          this.pushLog('error', message)
          this.showToast(message)
          this.endPendingAction(`stream-${action}`)
          return
        }
      }

      if (!threadId) {
        this.showToast('请先创建或填写 threadId')
        this.endPendingAction(`stream-${action}`)
        return
      }

      if ((action === 'resume' || action === 'retry') && !runId) {
        this.showToast('请先填写或查询 runId')
        this.endPendingAction(`stream-${action}`)
        return
      }

      if (action === 'retry') {
        try {
          const context = await this.syncRunContext({
            threadId,
            preferredRunId: runId,
            fetchLatestRunId: true
          })
          runId = context.runId || runId
          reportId = context.reportId || reportId
          checkpointId = context.checkpointId || checkpointId
        } catch (error) {
          const message = error?.message || '同步 retry 参数失败'
          this.statusText = message
          this.pushLog('error', message)
          this.showToast(message)
          this.endPendingAction(`stream-${action}`)
          return
        }

        if (!reportId) {
          const message = '缺少 reportId，无法发起 retry'
          this.statusText = message
          this.pushLog('error', message)
          this.showToast(message)
          this.endPendingAction(`stream-${action}`)
          return
        }

        if (!checkpointId) {
          const message = '缺少 checkpointId，无法发起 retry'
          this.statusText = message
          this.pushLog('error', message)
          this.showToast(message)
          this.endPendingAction(`stream-${action}`)
          return
        }
      }

      this.pushLog('action:prepare', this.stringifyPretty({
        action,
        baseUrl: this.form.baseUrl,
        hasToken: !!this.form.token,
        threadId,
        runId,
        reportId,
        checkpointId
      }, ''))

      let url = ''
      let method = 'GET'
      let body = null

      if (action === 'resume') {
        url = buildResumeStreamUrl(this.form.baseUrl, threadId, runId)
        method = 'GET'
      } else if (action === 'retry') {
        url = buildStartStreamUrl(this.form.baseUrl)
        method = 'POST'
        body = buildRetryPayload({
          prompt,
          threadId,
          runId,
          checkpointId,
          reportId,
          templateType: this.normalizeTemplateType()
        })
      } else {
        url = buildStartStreamUrl(this.form.baseUrl)
        method = 'POST'
        body = buildStartPayload({
          prompt,
          threadId,
          templateType: this.normalizeTemplateType()
        })
      }

      this.stopActiveStream({
        silent: true,
        reason: 'replace-connection'
      })
      this.resetStreamSnapshot(action)

      const token = this.createStreamToken()
      this.activeStreamToken = token
      this.latestPayloadText = body ? this.stringifyPretty(body, '') : 'GET 请求，无 body'
      this.endPendingAction(`stream-${action}`)

      const bridge = createWangaiAgentStreamBridge({
        url,
        method,
        headers: this.createRequestHeaders(),
        body,
        timeout: this.normalizeTimeout(),
        debug: false,
        onOpen: (event) => {
          if (this.activeStreamToken !== token) {
            return
          }
          this.statusText = event?.statusCode ? `已连接 HTTP ${event.statusCode}` : '已连接'
          this.pushLog('open', this.stringifyPretty(event, ''))
        },
        onChunk: (event) => {
          if (this.activeStreamToken !== token) {
            return
          }
          this.chunkCount += 1
        },
        onMessage: (event) => {
          if (this.activeStreamToken !== token) {
            return
          }
          this.handleStreamMessage(event)
        },
        onError: (error) => {
          if (this.activeStreamToken !== token) {
            return
          }
          this.statusText = error?.message || '流式请求失败'
          this.pushLog('error', this.stringifyPretty(error, error?.message || ''))
        },
        onClose: (event) => {
          if (this.activeStreamToken !== token) {
            return
          }

          this.activeBridge = null
          this.activeStreamToken = ''
          this.closeReason = event?.reason || ''

          if (event?.reason === 'normal-complete') {
            this.statusText = '已完成'
          } else if (event?.reason === 'error') {
            this.statusText = '连接结束（带错误）'
          } else {
            this.statusText = `连接结束: ${event?.reason || 'unknown'}`
          }

          this.pushLog('complete', this.stringifyPretty(event, ''))

          if (event?.reason === 'normal-complete' && threadId) {
            this.syncRunContext({
              threadId,
              preferredRunId: this.form.runId,
              fetchLatestRunId: false
            }).catch(() => {})
          }
        }
      })

      this.activeBridge = bridge
    },
    async handleAutoResumeTest() {
      this.pushLog('tap', 'click auto-resume')
      this.persistConfig()

      const token = this.beginAutomation('auto-resume')
      if (!token) {
        return
      }

      try {
        this.resetRunContext()
        this.setActionValue('start')
        this.pushLog('automation', 'resume: 先发起 start')
        await this.runCurrentAction()

        const resumeStartState = await this.waitForCondition(() => {
          if (this.closeReason === 'normal-complete') {
            return 'completed-too-fast'
          }
          if (this.closeReason === 'error') {
            return 'start-error'
          }
          if (!!this.activeBridge && !!`${this.form.threadId || ''}`.trim() && !!`${this.form.runId || ''}`.trim()) {
            return 'ready'
          }
          return ''
        }, {
          token,
          timeoutMs: 15000,
          label: '等待 start 建立连接并拿到 runId'
        })

        if (resumeStartState === 'completed-too-fast') {
          throw new Error('start 在断开前已完成，换一个更长的 prompt 再测 resume')
        }
        if (resumeStartState === 'start-error') {
          throw new Error('start 阶段已经异常结束，无法继续测 resume')
        }

        this.pushLog('automation', this.stringifyPretty({
          step: 'resume:start-ready',
          threadId: this.form.threadId,
          runId: this.form.runId
        }, ''))

        await this.sleep(400)
        this.ensureAutomationActive(token)

        this.pushLog('automation', 'resume: 断开本地连接，准备重连')
        this.statusText = '自动化：准备 resume'
        this.closeReason = 'auto-resume-disconnect'
        this.stopActiveStream({
          silent: true,
          reason: 'auto-resume-disconnect'
        })

        await this.waitForCondition(() => !this.activeBridge && !this.activeStreamToken, {
          token,
          timeoutMs: 3000,
          label: '等待本地连接断开'
        })

        this.setActionValue('resume')
        this.pushLog('automation', 'resume: 发起 resume 请求')
        await this.runCurrentAction()

        await this.waitForCondition(() => {
          return !this.activeBridge && !this.activeStreamToken && !!this.closeReason
        }, {
          token,
          timeoutMs: 180000,
          label: '等待 resume 完成'
        })

        if (this.closeReason !== 'normal-complete') {
          throw new Error(`resume 结束异常: ${this.closeReason || 'unknown'}`)
        }

        this.showToast('自动 Resume 已完成')
        this.finishAutomation(token, 'auto-resume 完成')
      } catch (error) {
        const message = error?.message || '自动 Resume 失败'
        this.pushLog('error', message)
        this.showToast(message)
        this.finishAutomation(token, `auto-resume 结束: ${message}`)
      }
    },
    async handleAutoRetryTest() {
      this.pushLog('tap', 'click auto-retry')
      this.persistConfig()

      const token = this.beginAutomation('auto-retry')
      if (!token) {
        return
      }

      try {
        this.resetRunContext()
        this.setActionValue('start')
        this.pushLog('automation', 'retry: 先跑一轮 start')
        await this.runCurrentAction()

        const startCompletionState = await this.waitForCondition(() => {
          if (!this.activeBridge && !this.activeStreamToken && !!this.closeReason) {
            return this.closeReason
          }
          return ''
        }, {
          token,
          timeoutMs: 180000,
          label: '等待 start 完成'
        })

        if (startCompletionState !== 'normal-complete') {
          throw new Error(`start 结束异常: ${startCompletionState || 'unknown'}`)
        }

        const beforeRetryContext = await this.syncRunContext({
          threadId: this.form.threadId,
          preferredRunId: this.form.runId,
          fetchLatestRunId: false
        })

        this.pushLog('automation', this.stringifyPretty({
          step: 'retry:start-complete',
          threadId: beforeRetryContext.threadId,
          runId: beforeRetryContext.runId,
          reportId: beforeRetryContext.reportId,
          checkpointId: beforeRetryContext.checkpointId
        }, ''))

        this.ensureAutomationActive(token)
        this.setActionValue('retry')
        this.pushLog('automation', 'retry: 发起 retry 请求')
        await this.runCurrentAction()

        await this.waitForCondition(() => {
          return !this.activeBridge && !this.activeStreamToken && !!this.closeReason
        }, {
          token,
          timeoutMs: 180000,
          label: '等待 retry 完成'
        })

        if (this.closeReason !== 'normal-complete') {
          throw new Error(`retry 结束异常: ${this.closeReason || 'unknown'}`)
        }

        this.pushLog('automation', this.stringifyPretty({
          step: 'retry:completed',
          threadId: this.form.threadId,
          runId: this.form.runId,
          reportId: this.form.reportId,
          checkpointId: this.form.checkpointId
        }, ''))

        this.showToast('自动 Retry 已完成')
        this.finishAutomation(token, 'auto-retry 完成')
      } catch (error) {
        const message = error?.message || '自动 Retry 失败'
        this.pushLog('error', message)
        this.showToast(message)
        this.finishAutomation(token, `auto-retry 结束: ${message}`)
      }
    },
    handleStop() {
      this.pushLog('tap', 'click stop')
      if (!this.activeBridge) {
        this.statusText = '当前没有活动连接'
        return
      }

      this.pushLog('abort', '用户手动断开本地连接')
      this.statusText = '已断开本地连接'
      this.closeReason = 'user-stop'
      this.stopActiveStream({
        silent: true,
        reason: 'user-stop'
      })
    }
  }
}
</script>

<style>
.page {
  height: 100vh;
  background-color: #f5f2ed;
}

.container {
  padding: 32rpx 24rpx 64rpx;
}

.hero {
  margin-bottom: 40rpx;
}

.title {
  display: block;
  font-size: 32rpx;
  font-weight: 400;
  color: #1a1714;
  letter-spacing: 1rpx;
}

.subtitle {
  display: block;
  margin-top: 12rpx;
  font-size: 15px;
  line-height: 24px;
  color: #6b665c;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  margin-top: 20rpx;
}

.hero-btn {
  margin-right: 16rpx;
  margin-bottom: 12rpx;
  padding: 10rpx 18rpx;
  border-radius: 999px;
  background-color: #2d2a25;
}

.hero-btn.ghost {
  background-color: transparent;
  border: 1px solid #d9d1c7;
}

.hero-btn-text {
  color: #f5f2ed;
  font-size: 13px;
}

.ghost-text {
  color: #2d2a25;
}

.card {
  margin-bottom: 24rpx;
  padding: 24rpx;
  border-radius: 8rpx;
  background-color: #f5f2ed;
  border: 1px solid #ede6de;
}

.section-title {
  display: block;
  margin-bottom: 24rpx;
  font-size: 14px;
  color: #8a8175;
  letter-spacing: 2rpx;
}

.label {
  display: block;
  margin-bottom: 8rpx;
  font-size: 13px;
  color: #6b665c;
}

.tip {
  display: block;
  font-size: 12px;
  line-height: 20px;
  color: #8a8175;
}

.action-mode-tip {
  margin-top: -4rpx;
  margin-bottom: 20rpx;
}

.input,
.textarea {
  box-sizing: border-box;
  width: 100%;
  padding: 12px 14px;
  font-size: 15px;
  color: #2d2a25;
  border: 1px solid #ede6de;
  border-radius: 4rpx;
  background-color: #f5f2ed;
  margin-bottom: 24rpx;
}

.textarea {
  min-height: 96rpx;
}

.token-textarea {
  min-height: 140rpx;
}

.mini-textarea {
  min-height: 72rpx;
}

.split-fields {
  display: flex;
  margin-right: -8rpx;
  margin-left: -8rpx;
}

.field-half {
  flex: 1;
  padding: 0 8rpx;
}

.tabs {
  display: flex;
  flex-wrap: wrap;
  margin-bottom: 16rpx;
}

.tab {
  padding: 8rpx 16rpx;
  margin-right: 12rpx;
  margin-bottom: 12rpx;
  border-radius: 4rpx;
  border: 1px solid #ede6de;
  background-color: transparent;
}

.tab.active {
  border-color: #2d2a25;
  background-color: #2d2a25;
}

.tab-text {
  font-size: 14px;
  color: #6b665c;
}

.tab-text-active {
  color: #f5f2ed;
}

.actions-column {
  margin-bottom: 24rpx;
}

.action-row {
  margin-bottom: 12rpx;
}

.btn {
  box-sizing: border-box;
  display: flex;
  padding: 12px 0;
  border-radius: 4rpx;
  align-items: center;
  justify-content: center;
  border: 1px solid #ede6de;
  background-color: transparent;
  line-height: 1.2;
}

.full-btn {
  width: 100%;
}

.btn.primary {
  border-color: #2d2a25;
  background-color: #2d2a25;
}

.btn.accent {
  border-color: #b16d16;
  background-color: #f1d3a9;
}

.btn.danger {
  border-color: #c78787;
  background-color: #f7ebeb;
}

.btn-text {
  color: #2d2a25;
  font-size: 14px;
}

.primary-text {
  color: #f5f2ed;
}

.accent-text {
  color: #6f4309;
}

.status-box {
  padding: 16px;
  border-radius: 4rpx;
  background-color: #ede6de;
}

.status-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 6rpx 0;
}

.divider {
  height: 1px;
  background-color: transparent;
  border-bottom: 1px solid #dfd8ce;
  margin: 12rpx 0;
}

.status-label {
  font-size: 14px;
  color: #6b665c;
}

.status-value {
  flex: 1;
  text-align: right;
  font-size: 14px;
  color: #2d2a25;
}

.small-value {
  font-size: 12px;
  line-height: 18px;
}

.preview-label {
  display: block;
  margin-bottom: 8rpx;
  font-size: 13px;
  color: #8a8175;
}

.preview-text,
.response-text,
.payload-text {
  display: block;
  margin-bottom: 20rpx;
  padding: 14rpx 16rpx;
  border-radius: 4rpx;
  background-color: #ebe6df;
  color: #2d2a25;
  font-size: 13px;
  line-height: 20px;
  white-space: pre-wrap;
  word-break: break-all;
}

.response-text {
  min-height: 120rpx;
}

</style>
