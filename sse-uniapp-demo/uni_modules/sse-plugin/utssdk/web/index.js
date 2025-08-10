// Web implementation using Fetch streaming to support custom headers
// Aligns with UTS interface exports

const controllersByRequestId = new Map();
let listenersByRequestId = new Map();
let globalListeners = [];

function notifyOpen(requestId) {
  const evt = { requestId, message: 'Connection opened' };
  const scoped = listenersByRequestId.get(requestId);
  if (scoped) scoped.forEach(o => o.onOpen && o.onOpen(evt));
  globalListeners.forEach(o => o.onOpen && o.onOpen(evt));
}

function notifyMessage(requestId, message) {
  const evt = { requestId, message };
  const scoped = listenersByRequestId.get(requestId);
  if (scoped) scoped.forEach(o => o.onMessage && o.onMessage(evt));
  globalListeners.forEach(o => o.onMessage && o.onMessage(evt));
}

function notifyError(requestId, errorMessage) {
  const evt = { requestId, error: errorMessage };
  const scoped = listenersByRequestId.get(requestId);
  if (scoped) scoped.forEach(o => o.onError && o.onError(evt));
  globalListeners.forEach(o => o.onError && o.onError(evt));
}

function notifyClose(requestId) {
  const evt = { requestId };
  const scoped = listenersByRequestId.get(requestId);
  if (scoped) scoped.forEach(o => o.onClose && o.onClose(evt));
  globalListeners.forEach(o => o.onClose && o.onClose(evt));
  listenersByRequestId.delete(requestId);
}

function isForbiddenHeaderName(name) {
  const n = String(name || '').toLowerCase();
  if (n.startsWith('proxy-') || n.startsWith('sec-')) return true;
  switch (n) {
    case 'accept-charset':
    case 'accept-encoding':
    case 'access-control-request-headers':
    case 'access-control-request-method':
    case 'connection':
    case 'content-length':
    case 'cookie':
    case 'cookie2':
    case 'date':
    case 'dnt':
    case 'expect':
    case 'host':
    case 'keep-alive':
    case 'origin':
    case 'referer':
    case 'te':
    case 'trailer':
    case 'transfer-encoding':
    case 'upgrade':
    case 'via':
      return true;
    default:
      return false;
  }
}

function buildHeaders(custom) {
  const headers = new Headers();
  headers.set('Accept', 'text/event-stream');
  headers.set('Cache-Control', 'no-cache');
  if (custom && typeof custom === 'object') {
    for (const k of Object.keys(custom)) {
      if (isForbiddenHeaderName(k)) continue;
      const v = custom[k];
      if (v == null) continue;
      try { headers.set(k, String(v)); } catch (_) {}
    }
  }
  return headers;
}

async function readSSEStream(reader, requestId) {
  const decoder = new TextDecoder('utf-8');
  let buffer = '';
  let dataBuffer = '';
  while (true) {
    const { value, done } = await reader.read();
    if (done) break;
    buffer += decoder.decode(value, { stream: true });
    let nl;
    while ((nl = buffer.indexOf('\n')) !== -1) {
      let line = buffer.slice(0, nl);
      buffer = buffer.slice(nl + 1);
      if (line.endsWith('\r')) line = line.slice(0, -1);
      if (line === '') {
        if (dataBuffer.length > 0) {
          const msg = dataBuffer.endsWith('\n') ? dataBuffer.slice(0, -1) : dataBuffer;
          notifyMessage(requestId, msg);
          dataBuffer = '';
        }
        continue;
      }
      if (line.startsWith(':')) continue;
      const idx = line.indexOf(':');
      const field = idx === -1 ? line : line.slice(0, idx);
      let valueStr = idx === -1 ? '' : line.slice(idx + 1);
      if (valueStr.startsWith(' ')) valueStr = valueStr.slice(1);
      switch (field) {
        case 'data':
          dataBuffer += valueStr + '\n';
          break;
        case 'event':
        case 'id':
        case 'retry':
        default:
          // ignore for now
          break;
      }
    }
  }
  if (dataBuffer.length > 0) {
    const msg = dataBuffer.endsWith('\n') ? dataBuffer.slice(0, -1) : dataBuffer;
    notifyMessage(requestId, msg);
  }
}

function isAbortError(err) {
  return (
    (typeof DOMException !== 'undefined' && err instanceof DOMException && err.name === 'AbortError') ||
    (typeof err === 'object' && err && String(err.name || '').toLowerCase() === 'aborterror') ||
    String(err && err.message || err).toLowerCase().includes('aborted')
  );
}

export function sseConnectApi(options) {
  try {
    const requestId = options.requestId != null ? options.requestId : `sse_${Date.now()}`;

    const existing = controllersByRequestId.get(requestId);
    if (existing) {
      try { existing.abort(); } catch (_) {}
      controllersByRequestId.delete(requestId);
    }

    const controller = new AbortController();
    controllersByRequestId.set(requestId, controller);

    const result = { requestId, message: 'Connection started' };
    options.success && options.success(result);
    options.complete && options.complete(result);

    (async () => {
      try {
        const headers = buildHeaders(options.headers);
        const resp = await fetch(options.url, {
          method: 'GET',
          headers,
          signal: controller.signal,
          cache: 'no-cache',
          mode: 'cors'
        });

        if (!resp.ok) {
          throw new Error(`HTTP ${resp.status}`);
        }
        const body = resp.body;
        if (!body || typeof body.getReader !== 'function') {
          throw new Error('ReadableStream is not supported in this environment');
        }

        notifyOpen(requestId);
        await readSSEStream(body.getReader(), requestId);
      } catch (err) {
        if (!isAbortError(err)) {
          const msg = err instanceof Error ? err.message : String(err);
          notifyError(requestId, msg);
        }
      } finally {
        if (controllersByRequestId.has(requestId)) {
          controllersByRequestId.delete(requestId);
          notifyClose(requestId);
        }
      }
    })();
  } catch (e) {
    const msg = e instanceof Error ? e.message : 'Failed to start SSE connection';
    const err = { errSubject: 'sse-plugin-api', errCode: 9020001, errMsg: msg };
    options.fail && options.fail(err);
    options.complete && options.complete(err);
  }
}

export function sseCloseApi(requestId) {
  try {
    const controller = controllersByRequestId.get(requestId);
    if (controller) {
      try { controller.abort(); } catch (_) {}
      controllersByRequestId.delete(requestId);
      notifyClose(requestId);
    }
  } catch (e) {
    // noop
  }
}

export function sseAddEventListenerApi(options) {
  const reqId = options.requestId;
  if (reqId != null) {
    const list = listenersByRequestId.get(reqId);
    if (list == null) listenersByRequestId.set(reqId, [options]);
    else listenersByRequestId.set(reqId, [...list, options]);
  } else {
    globalListeners.push(options);
  }
}

export function sseRemoveEventListenerApi(requestId) {
  if (requestId != null) {
    if (listenersByRequestId.has(requestId)) listenersByRequestId.delete(requestId);
  } else {
    globalListeners = [];
  }
}

export function closeAllSSEConnections() {
  for (const [id, controller] of controllersByRequestId.entries()) {
    try { controller.abort(); } catch (_) {}
    controllersByRequestId.delete(id);
    notifyClose(id);
  }
  listenersByRequestId.clear();
  globalListeners = [];
}
