// Minimal JS implementation for Web using EventSource
// Aligns with UTS interface exports

let sourcesByRequestId = new Map();
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

export function sseConnectApi(options) {
  try {
    const requestId = options.requestId != null ? options.requestId : `sse_${Date.now()}`;
    if (typeof window.EventSource === 'undefined') {
      const err = { errSubject: 'sse-plugin-api', errCode: 9020001, errMsg: 'EventSource is not supported in this environment' };
      options.fail && options.fail(err);
      options.complete && options.complete(err);
      return;
    }
    const existing = sourcesByRequestId.get(requestId);
    if (existing) {
      try { existing.close(); } catch (_) {}
      sourcesByRequestId.delete(requestId);
    }
    if (options.headers != null) {
      console.log('[SSE][WEB] headers are not supported by EventSource and will be ignored');
    }
    const url = options.url;
    const src = new window.EventSource(url);
    sourcesByRequestId.set(requestId, src);
    src.onopen = function() { notifyOpen(requestId); };
    src.onmessage = function(e) {
      const dataStr = e && e.data != null ? String(e.data) : '';
      notifyMessage(requestId, dataStr);
    };
    src.onerror = function() { notifyError(requestId, 'EventSource error'); };
    const result = { requestId, message: 'Connection started' };
    options.success && options.success(result);
    options.complete && options.complete(result);
  } catch (e) {
    const msg = e instanceof Error ? e.message : 'Failed to start SSE connection';
    const err = { errSubject: 'sse-plugin-api', errCode: 9020001, errMsg: msg };
    options.fail && options.fail(err);
    options.complete && options.complete(err);
  }
}

export function sseCloseApi(requestId) {
  try {
    const src = sourcesByRequestId.get(requestId);
    if (src) {
      try { src.close(); } catch (_) {}
      sourcesByRequestId.delete(requestId);
      notifyClose(requestId);
    }
  } catch (e) {
    console.error('[SSE][WEB] close error', e);
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
  for (const [id, src] of sourcesByRequestId.entries()) {
    try { src.close(); } catch (_) {}
    notifyClose(id);
  }
  sourcesByRequestId.clear();
  listenersByRequestId.clear();
  globalListeners = [];
}
