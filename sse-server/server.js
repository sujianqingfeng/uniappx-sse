const express = require('express');
const cors = require('cors');
const path = require('path');
const crypto = require('crypto');

const app = express();
const PORT = process.env.PORT || 3000;
const RETRY_INTERVAL = 3000;
const BASE64_BYTES = 2048;

const generateBase64Payload = () => {
  const randomBase64 = crypto.randomBytes(BASE64_BYTES).toString('base64');
  return {
    type: 'money',
    data: randomBase64,
    msg: '成功'
  };
};

// Enable CORS for all routes
app.use(cors());

// Serve static files from the 'public' directory
app.use(express.static(path.join(__dirname, 'public')));

// SSE endpoint
app.get('/sse', (req, res) => {
  // Set headers for SSE
  res.setHeader('Content-Type', 'text/event-stream');
  res.setHeader('Cache-Control', 'no-cache');
  res.setHeader('Connection', 'keep-alive');
  res.setHeader('Access-Control-Allow-Origin', '*');
  
  // Set a unique ID for this connection
  const clientId = Date.now();
  console.log(`New SSE connection: ${clientId}`);
  
  // Check for custom headers
  const authHeader = req.headers.authorization;
  const userAgent = req.headers['user-agent'];
  console.log(`Client connected with headers - Auth: ${authHeader}, User-Agent: ${userAgent}`);
  
  // Function to send events
  const sendEvent = (data, options = {}) => {
    const { event = null, retry = null } = options;
    const payload = typeof data === 'string' ? data : JSON.stringify(data);
    let eventData = '';
    eventData += `data: ${payload}\n`;
    if (event) eventData += `event: ${event}\n`;
    if (retry) eventData += `retry: ${retry}\n`;
    eventData += `\n`;
    res.write(eventData);
  };

  // Initial handshake payload expected by clients
  sendEvent({
    code: 200,
    msg: 'SSE连接成功',
    clientId: String(clientId)
  }, { event: 'message', retry: RETRY_INTERVAL });
  
  const sendBase64Payload = () => {
    try {
      const payload = generateBase64Payload();
      sendEvent(payload, { event: 'message', retry: RETRY_INTERVAL });
    } catch (error) {
      console.error('Failed to generate base64 payload for SSE:', error);
      sendEvent({
        type: 'error',
        msg: 'base64_generator_failed',
        detail: error.message
      }, { event: 'message', retry: RETRY_INTERVAL });
    }
  };
  
  sendBase64Payload();
  const interval = setInterval(sendBase64Payload, 5000);
  
  // Handle client disconnect
  req.on('close', () => {
    console.log(`SSE connection closed: ${clientId}`);
    clearInterval(interval);
  });
  
  // Handle errors
  req.on('error', (err) => {
    console.error(`SSE connection error for ${clientId}:`, err);
    clearInterval(interval);
  });
});

// Health check endpoint
app.get('/health', (req, res) => {
  res.json({ status: 'OK', timestamp: new Date().toISOString() });
});

// Main page
app.get('/', (req, res) => {
  res.sendFile(path.join(__dirname, 'public', 'index.html'));
});

// Start the server
app.listen(PORT, () => {
  console.log(`SSE Server is running on http://localhost:${PORT}`);
  console.log(`SSE endpoint: http://localhost:${PORT}/sse`);
});
