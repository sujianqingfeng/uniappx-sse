const express = require('express');
const cors = require('cors');
const path = require('path');

const app = express();
const PORT = process.env.PORT || 3000;
const HOST = process.env.HOST || '0.0.0.0';

const log = (...args) => {
  const timestamp = new Date().toISOString()
  console.log(`[${timestamp}]`, ...args)
}

const readRequestBody = (req) => {
  return new Promise((resolve, reject) => {
    let body = ''
    req.setEncoding('utf8')
    req.on('data', (chunk) => {
      body += chunk
    })
    req.on('end', () => {
      resolve(body)
    })
    req.on('error', (err) => {
      reject(err)
    })
  })
}

const parseRequestBody = (bodyText, contentType) => {
  const text = bodyText.trim()
  if (text.length == 0) return null

  const shouldTryJson = contentType.includes('application/json') || contentType.includes('+json') || text.startsWith('{') || text.startsWith('[')
  if (shouldTryJson) {
    try {
      return JSON.parse(text)
    } catch (_) {
    }
  }
  return bodyText
}

const createRequestInfo = (req, bodyText = '') => {
  const contentType = `${req.headers['content-type'] || ''}`
  return {
    method: req.method,
    contentType,
    bodyText,
    body: parseRequestBody(bodyText, contentType)
  }
}

const createRequestPreview = (requestInfo) => {
  if (requestInfo.body == null) return '(empty)'
  const text = typeof requestInfo.body == 'string' ? requestInfo.body : (JSON.stringify(requestInfo.body) || '')
  const compact = text.replace(/\r/g, '\\r').replace(/\n/g, '\\n')
  return compact.length > 160 ? `${compact.slice(0, 157)}...` : compact
}

const logRequestInfo = (route, req, requestInfo) => {
  const authHeader = req.headers.authorization
  const userAgent = req.headers['user-agent']
  log(`${route} ${req.method} headers - Auth: ${authHeader}, User-Agent: ${userAgent}, Content-Type: ${requestInfo.contentType || '(empty)'}`)
  if (requestInfo.body != null) {
    log(`${route} ${req.method} body:`, requestInfo.body)
  } else {
    log(`${route} ${req.method} body: (empty)`)
  }
}

const setStreamHeaders = (res, contentType) => {
  res.setHeader('Content-Type', contentType)
  res.setHeader('Cache-Control', 'no-cache')
  res.setHeader('Connection', 'keep-alive')
  res.setHeader('Access-Control-Allow-Origin', '*')
}

const createStreamRequestEvent = (clientId, requestInfo) => {
  return {
    clientId,
    method: requestInfo.method,
    contentType: requestInfo.contentType || null,
    body: requestInfo.body,
    timestamp: new Date().toISOString()
  }
}

const attachStreamLifecycle = (res, interval, onClose, onError) => {
  let finished = false
  const cleanup = () => {
    if (finished) return false
    finished = true
    clearInterval(interval)
    return true
  }

  res.on('close', () => {
    if (!cleanup()) return
    onClose()
  })

  res.on('error', (err) => {
    if (!cleanup()) return
    onError(err)
  })
}

const registerStreamRoute = (path, handler) => {
  app.get(path, (req, res) => {
    handler(req, res, createRequestInfo(req))
  })

  app.post(path, async (req, res) => {
    try {
      const bodyText = await readRequestBody(req)
      handler(req, res, createRequestInfo(req, bodyText))
    } catch (err) {
      log(`${path} POST read body failed:`, err)
      res.status(400).json({
        error: 'Failed to read request body',
        message: err instanceof Error ? err.message : String(err)
      })
    }
  })
}

// Enable CORS for all routes
app.use(cors());

// Serve static files from the 'public' directory
app.use(express.static(path.join(__dirname, 'public')));

const handleSseStream = (req, res, requestInfo) => {
  setStreamHeaders(res, 'text/event-stream')

  const clientId = Date.now()
  log(`New SSE connection: ${clientId}`)
  logRequestInfo('/sse', req, requestInfo)

  res.write(`data: Welcome to the SSE server! Your connection ID is ${clientId}\n\n`)

  const sendEvent = (data, event = null, id = null) => {
    let eventData = ''
    if (id) eventData += `id: ${id}\n`
    if (event) eventData += `event: ${event}\n`
    eventData += `data: ${JSON.stringify(data)}\n\n`
    res.write(eventData)
  }

  sendEvent(createStreamRequestEvent(clientId, requestInfo), 'request')

  let counter = 1
  const interval = setInterval(() => {
    const eventType = Math.floor(Math.random() * 4)

    switch (eventType) {
      case 0: {
        const message = {
          timestamp: new Date().toISOString(),
          message: `Server time is ${new Date().toLocaleTimeString()}`,
          clientId
        }
        sendEvent(message, 'message')
        break
      }

      case 1: {
        const notification = {
          id: counter++,
          title: 'New Notification',
          body: 'This is a test notification from the server',
          timestamp: new Date().toISOString()
        }
        sendEvent(notification, 'notification')
        break
      }

      case 2: {
        const status = {
          id: counter++,
          user: `user${Math.floor(Math.random() * 100)}`,
          status: ['online', 'offline', 'away'][Math.floor(Math.random() * 3)],
          timestamp: new Date().toISOString()
        }
        sendEvent(status, 'status')
        break
      }

      default: {
        const dataUpdate = {
          id: counter++,
          type: 'data_update',
          value: Math.random() * 100,
          unit: ['kb', 'mb', 'gb'][Math.floor(Math.random() * 3)],
          timestamp: new Date().toISOString()
        }
        sendEvent(dataUpdate, 'data')
        break
      }
    }
  }, 3000)

  attachStreamLifecycle(
    res,
    interval,
    () => {
      log(`SSE connection closed: ${clientId}`)
    },
    (err) => {
      console.error(`[${new Date().toISOString()}] SSE connection error for ${clientId}:`, err)
    }
  )
}

const handleLineStream = (req, res, requestInfo) => {
  setStreamHeaders(res, 'text/plain; charset=utf-8')

  const clientId = Date.now()
  log(`New line stream connection: ${clientId}`)
  logRequestInfo('/line-stream', req, requestInfo)

  res.write(`hello line stream ${clientId}\n`)
  res.write(`request ${requestInfo.method} body=${createRequestPreview(requestInfo)}\n`)

  let counter = 1
  const interval = setInterval(() => {
    res.write(`line ${counter} @ ${new Date().toISOString()}\n`)
    counter += 1
  }, 2000)

  attachStreamLifecycle(
    res,
    interval,
    () => {
      log(`Line stream closed: ${clientId}`)
    },
    (err) => {
      console.error(`[${new Date().toISOString()}] Line stream error for ${clientId}:`, err)
    }
  )
}

const handleJsonlStream = (req, res, requestInfo) => {
  setStreamHeaders(res, 'application/x-ndjson; charset=utf-8')

  const clientId = Date.now()
  log(`New JSONL stream connection: ${clientId}`)
  logRequestInfo('/jsonl-stream', req, requestInfo)

  res.write(`${JSON.stringify({ type: 'request', ...createStreamRequestEvent(clientId, requestInfo) })}\n`)

  let counter = 1
  const send = () => {
    const payload = {
      clientId,
      counter,
      now: new Date().toISOString(),
      random: Number((Math.random() * 100).toFixed(2))
    }
    res.write(`${JSON.stringify(payload)}\n`)
    counter += 1
  }

  send()
  const interval = setInterval(send, 2000)
  attachStreamLifecycle(
    res,
    interval,
    () => {
      log(`JSONL stream closed: ${clientId}`)
    },
    (err) => {
      console.error(`[${new Date().toISOString()}] JSONL stream error for ${clientId}:`, err)
    }
  )
}

const handleRawStream = (req, res, requestInfo) => {
  setStreamHeaders(res, 'text/plain; charset=utf-8')

  const clientId = Date.now()
  log(`New raw stream connection: ${clientId}`)
  logRequestInfo('/raw-stream', req, requestInfo)

  res.write(`request|${requestInfo.method}|${createRequestPreview(requestInfo)}|`)

  let counter = 1
  const interval = setInterval(() => {
    res.write(`chunk-${counter}|${new Date().toISOString()}|`)
    counter += 1
  }, 1500)

  attachStreamLifecycle(
    res,
    interval,
    () => {
      log(`Raw stream closed: ${clientId}`)
    },
    (err) => {
      console.error(`[${new Date().toISOString()}] Raw stream error for ${clientId}:`, err)
    }
  )
}

registerStreamRoute('/sse', handleSseStream)
registerStreamRoute('/line-stream', handleLineStream)
registerStreamRoute('/jsonl-stream', handleJsonlStream)
registerStreamRoute('/raw-stream', handleRawStream)

// Health check endpoint
app.get('/health', (req, res) => {
  res.json({ status: 'OK', timestamp: new Date().toISOString() });
});

// Main page
app.get('/', (req, res) => {
  res.sendFile(path.join(__dirname, 'public', 'index.html'));
});

// Start the server
app.listen(PORT, HOST, () => {
  log(`SSE Server is running on http://${HOST}:${PORT}`);
  log(`SSE endpoint: http://${HOST}:${PORT}/sse`);
});
