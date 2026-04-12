function createStreamError(code, message, data) {
  return {
    errSubject: 'sse-plugin-stream-api',
    errCode: code,
    errMsg: message || 'Stream error',
    data: data || null,
  }
}

class WebStreamConnection {
  constructor() {
    this.openCallback = null
    this.chunkCallback = null
    this.messageCallback = null
    this.errorCallback = null
    this.completeCallback = null
    this.completed = false
    this.aborter = null
  }

  setAborter(aborter) {
    this.aborter = aborter
  }

  abort() {
    if (this.completed) return
    try { this.aborter && this.aborter() } catch (_) {}
    this.finishOnce()
  }

  onOpen(callback) { this.openCallback = callback || null }
  offOpen() { this.openCallback = null }
  onChunk(callback) { this.chunkCallback = callback || null }
  offChunk() { this.chunkCallback = null }
  onMessage(callback) { this.messageCallback = callback || null }
  offMessage() { this.messageCallback = null }
  onError(callback) { this.errorCallback = callback || null }
  offError() { this.errorCallback = null }
  onComplete(callback) { this.completeCallback = callback || null }
  offComplete() { this.completeCallback = null }

  emitOpen(evt) { if (!this.completed && this.openCallback) this.openCallback(evt) }
  emitChunk(text) { if (!this.completed && text && this.chunkCallback) this.chunkCallback({ text }) }
  emitMessage(evt) { if (!this.completed && this.messageCallback) this.messageCallback(evt) }
  emitError(err) { if (!this.completed && this.errorCallback) this.errorCallback(err) }
  finishOnce() {
    if (this.completed) return
    this.completed = true
    if (this.completeCallback) this.completeCallback()
  }
}

function normalizeProtocol(protocol) {
  return protocol || 'sse'
}

function parseDataText(dataText) {
  if (!dataText) return ''
  try {
    const parsed = JSON.parse(dataText)
    return parsed == null ? dataText : parsed
  } catch (_) {
    return dataText
  }
}

function parseLineBuffer(buffer, final) {
  const messages = []
  let rest = buffer
  while (true) {
    const idx = rest.indexOf('\n')
    if (idx === -1) break
    let line = rest.slice(0, idx)
    if (line.endsWith('\r')) line = line.slice(0, -1)
    rest = rest.slice(idx + 1)
    messages.push({ data: line, rawText: line })
  }
  if (final && rest.length > 0) {
    let line = rest
    if (line.endsWith('\r')) line = line.slice(0, -1)
    messages.push({ data: line, rawText: line })
    rest = ''
  }
  return { messages, rest }
}

function parseJsonlBuffer(buffer, final) {
  const out = parseLineBuffer(buffer, final)
  out.messages = out.messages.map((msg) => ({
    data: parseDataText(msg.rawText),
    rawText: msg.rawText
  }))
  return out
}

function parseSseBlock(block) {
  const lines = block.split('\n')
  let event = null
  let id = null
  const dataLines = []
  for (let i = 0; i < lines.length; i++) {
    let line = lines[i]
    if (line.endsWith('\r')) line = line.slice(0, -1)
    if (!line || line.startsWith(':')) continue
    const idx = line.indexOf(':')
    const field = idx === -1 ? line : line.slice(0, idx)
    let value = idx === -1 ? '' : line.slice(idx + 1)
    if (value.startsWith(' ')) value = value.slice(1)
    if (field === 'event') event = value
    else if (field === 'id') id = value
    else if (field === 'data') dataLines.push(value)
  }
  if (!event && !id && dataLines.length === 0) return null
  const rawText = dataLines.join('\n')
  const msg = { data: parseDataText(rawText), rawText }
  if (event) msg.event = event
  if (id) msg.id = id
  return msg
}

function parseSseBuffer(buffer, final) {
  const messages = []
  let rest = buffer
  while (true) {
    let idx = rest.indexOf('\n\n')
    let sepLen = 2
    const altIdx = rest.indexOf('\r\n\r\n')
    if (altIdx !== -1 && (idx === -1 || altIdx < idx)) {
      idx = altIdx
      sepLen = 4
    }
    if (idx === -1) break
    const block = rest.slice(0, idx)
    rest = rest.slice(idx + sepLen)
    const msg = parseSseBlock(block)
    if (msg) messages.push(msg)
  }
  if (final && rest.length > 0) {
    const msg = parseSseBlock(rest)
    if (msg) messages.push(msg)
    rest = ''
  }
  return { messages, rest }
}

function parseBuffer(protocol, buffer, final) {
  switch (protocol) {
    case 'line':
      return parseLineBuffer(buffer, final)
    case 'jsonl':
      return parseJsonlBuffer(buffer, final)
    case 'raw':
      return { messages: [], rest: final ? '' : buffer }
    case 'sse':
    default:
      return parseSseBuffer(buffer, final)
  }
}

function buildHeaders(custom, protocol) {
  const headers = new Headers()
  headers.set('Accept', protocol === 'sse' ? 'text/event-stream' : 'text/plain')
  if (custom && typeof custom === 'object') {
    Object.keys(custom).forEach((key) => {
      const value = custom[key]
      if (value == null) return
      headers.set(key, String(value))
    })
  }
  return headers
}

function isAbortError(err) {
  return (
    (typeof DOMException !== 'undefined' && err instanceof DOMException && err.name === 'AbortError') ||
    String((err && err.message) || err).toLowerCase().includes('aborted')
  )
}

export function connectStream(options) {
  const connection = new WebStreamConnection()
  const protocol = normalizeProtocol(options && options.protocol)
  const controller = new AbortController()
  connection.setAborter(() => controller.abort())

  setTimeout(async () => {
    try {
      if (!options || !options.url || String(options.url).trim().length === 0) {
        connection.emitError(createStreamError(9030002, 'Invalid stream URL'))
        connection.finishOnce()
        return
      }

      const response = await fetch(options.url, {
        method: options.method || 'GET',
        headers: buildHeaders(options.headers, protocol),
        body: options.body == null ? undefined : (typeof options.body === 'string' ? options.body : JSON.stringify(options.body)),
        signal: controller.signal,
        cache: 'no-cache',
        mode: 'cors'
      })

      const headerObj = {}
      response.headers.forEach((value, key) => {
        headerObj[key] = value
      })
      connection.emitOpen({ statusCode: response.status, headers: headerObj })

      if (!response.ok) {
        connection.emitError(createStreamError(9030004, `HTTP ${response.status}`, { statusCode: response.status }))
        connection.finishOnce()
        return
      }

      if (!response.body || typeof response.body.getReader !== 'function') {
        connection.emitError(createStreamError(9030007, 'ReadableStream is not supported in this environment'))
        connection.finishOnce()
        return
      }

      const reader = response.body.getReader()
      const decoder = new TextDecoder('utf-8')
      let buffer = ''
      while (true) {
        const { value, done } = await reader.read()
        if (done) break
        const text = decoder.decode(value, { stream: true })
        if (text.length > 0) {
          connection.emitChunk(text)
          if (protocol === 'raw') continue
          buffer += text
          const parsed = parseBuffer(protocol, buffer, false)
          buffer = parsed.rest
          parsed.messages.forEach((msg) => connection.emitMessage(msg))
        }
      }

      const tail = decoder.decode()
      if (tail.length > 0) {
        connection.emitChunk(tail)
        if (protocol !== 'raw') buffer += tail
      }
      if (protocol !== 'raw') {
        const parsed = parseBuffer(protocol, buffer, true)
        parsed.messages.forEach((msg) => connection.emitMessage(msg))
      }
    } catch (err) {
      if (!isAbortError(err)) {
        connection.emitError(createStreamError(9030003, err instanceof Error ? err.message : String(err)))
      }
    } finally {
      connection.finishOnce()
    }
  }, 0)

  return connection
}

export { createStreamError }
