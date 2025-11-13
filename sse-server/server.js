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
  log(`New SSE connection: ${clientId}`);
  
  // Check for custom headers
  const authHeader = req.headers.authorization;
  const userAgent = req.headers['user-agent'];
  log(`Client connected with headers - Auth: ${authHeader}, User-Agent: ${userAgent}`);
  
  // Send a welcome message
  res.write(`data: Welcome to the SSE server! Your connection ID is ${clientId}\n\n`);
  
  // Function to send events
  const sendEvent = (data, event = null, id = null) => {
    let eventData = '';
    if (id) eventData += `id: ${id}\n`;
    if (event) eventData += `event: ${event}\n`;
    eventData += `data: ${JSON.stringify(data)}\n\n`;
    res.write(eventData);
  };
  
  // Send different types of test data
  let counter = 1;
  const interval = setInterval(() => {
    // Randomly send different types of events
    const eventType = Math.floor(Math.random() * 4);
    
    switch (eventType) {
      case 0:
        // Regular message
        const message = {
          timestamp: new Date().toISOString(),
          message: `Server time is ${new Date().toLocaleTimeString()}`,
          clientId: clientId
        };
        sendEvent(message, 'message');
        break;
        
      case 1:
        // Notification event
        const notification = {
          id: counter++,
          title: "New Notification",
          body: "This is a test notification from the server",
          timestamp: new Date().toISOString()
        };
        sendEvent(notification, 'notification');
        break;
        
      case 2:
        // Status update event
        const status = {
          id: counter++,
          user: "user" + Math.floor(Math.random() * 100),
          status: ["online", "offline", "away"][Math.floor(Math.random() * 3)],
          timestamp: new Date().toISOString()
        };
        sendEvent(status, 'status');
        break;
        
      case 3:
        // Data update event
        const dataUpdate = {
          id: counter++,
          type: "data_update",
          value: Math.random() * 100,
          unit: ["kb", "mb", "gb"][Math.floor(Math.random() * 3)],
          timestamp: new Date().toISOString()
        };
        sendEvent(dataUpdate, 'data');
        break;
    }
  }, 3000);
  
  // Handle client disconnect
  req.on('close', () => {
    log(`SSE connection closed: ${clientId}`);
    clearInterval(interval);
  });
  
  // Handle errors
  req.on('error', (err) => {
    console.error(`[${new Date().toISOString()}] SSE connection error for ${clientId}:`, err);
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
app.listen(PORT, HOST, () => {
  log(`SSE Server is running on http://${HOST}:${PORT}`);
  log(`SSE endpoint: http://${HOST}:${PORT}/sse`);
});
