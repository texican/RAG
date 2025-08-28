# 🦙 Ollama Chat Frontend

A lightweight, responsive web interface for chatting with your local Ollama AI models.

## ✨ Features

- 🎨 **Modern UI**: Clean, responsive design that works on desktop and mobile
- 🚀 **Real-time Chat**: Interactive conversation with your Ollama models  
- 🔄 **Model Selection**: Switch between available Ollama models
- ⚡ **Fast & Local**: Direct connection to your local Ollama instance
- 🛠 **CORS Handling**: Built-in proxy server to handle browser security
- 📱 **Mobile Friendly**: Responsive design for all screen sizes

## 🚀 Quick Start

### Option 1: Python Server (Recommended)

```bash
# Navigate to the chat directory
cd /Users/stryfe/Projects/RAG/ollama-chat

# Start the server (includes CORS handling)
python3 server.py

# Open your browser to: http://localhost:8888
```

### Option 2: Direct File Access

```bash
# Simply open the HTML file in your browser
open index.html
```

> **Note**: Direct file access may have CORS issues. The Python server is recommended.

## 📋 Prerequisites

1. **Ollama Running**: Make sure your Ollama instance is running
   ```bash
   # Check if Ollama is running
   curl http://localhost:11434/api/tags
   ```

2. **Models Available**: Ensure you have models downloaded
   ```bash
   # Check available models
   docker-compose exec ollama ollama list
   
   # Pull a model if needed
   docker-compose exec ollama ollama pull llama2:7b
   ```

## 🎯 Usage

1. **Start the Frontend**:
   ```bash
   python3 server.py
   ```

2. **Open Browser**: Navigate to `http://localhost:8888`

3. **Select Model**: Use the dropdown to choose your preferred model

4. **Start Chatting**: Type your question and press Enter or click Send

## 🔧 Configuration

### Change Ollama URL
Edit the `ollamaUrl` in `index.html` or modify the `ollama_url` in `server.py`:

```javascript
// In index.html
this.ollamaUrl = 'http://your-ollama-host:11434';
```

```python
# In server.py
self.ollama_url = 'http://your-ollama-host:11434'
```

### Change Server Port
Modify the `port` variable in `server.py`:

```python
port = 8888  # Change to your preferred port
```

## 🎨 Interface Features

### Chat Interface
- **User messages**: Blue bubbles on the right
- **AI responses**: White bubbles on the left with avatar
- **Typing indicator**: Shows when Ollama is processing
- **Auto-scroll**: Automatically scrolls to latest messages

### Controls
- **Model Selector**: Dropdown in the header to switch models
- **Status Indicator**: Shows connection status and current activity
- **Send Button**: Click or press Enter to send messages
- **Multi-line Input**: Use Shift+Enter for line breaks

### Responsive Design
- **Desktop**: Full-featured chat interface
- **Mobile**: Optimized layout for smaller screens
- **Auto-resize**: Input field adjusts to content

## 🔍 Troubleshooting

### Ollama Not Connected
```bash
# Check if Ollama is running
docker-compose ps ollama

# Check Ollama status
curl http://localhost:11434/api/tags
```

### No Models Available
```bash
# List current models
docker-compose exec ollama ollama list

# Pull a model
docker-compose exec ollama ollama pull llama2:7b
```

### CORS Issues
Use the Python server instead of opening the HTML file directly:
```bash
python3 server.py
```

### Port Already in Use
Change the port in `server.py` or kill the process:
```bash
# Find process using port 8888
lsof -i :8888

# Kill the process
kill -9 <PID>
```

## 🛠 Development

### File Structure
```
ollama-chat/
├── index.html      # Main chat interface
├── server.py       # Python server with CORS handling
└── README.md       # This file
```

### Customization
- **Styling**: Edit the CSS in the `<style>` section of `index.html`
- **Functionality**: Modify the JavaScript `OllamaChat` class
- **Server Logic**: Update `server.py` for proxy behavior

## 📝 API Reference

The frontend uses these Ollama API endpoints:

### Check Available Models
```bash
GET /api/tags
```

### Chat with Model
```bash
POST /api/chat
Content-Type: application/json

{
  "model": "llama2:7b",
  "messages": [
    {"role": "user", "content": "Hello!"}
  ],
  "stream": false
}
```

## 🎉 Tips

1. **Keyboard Shortcuts**:
   - `Enter`: Send message
   - `Shift+Enter`: New line
   
2. **Model Selection**: 
   - Try different models for different tasks
   - Smaller models respond faster
   - Larger models give better quality

3. **Performance**:
   - First message may be slow (model loading)
   - Subsequent messages are typically faster
   - Monitor Ollama logs for troubleshooting

Enjoy chatting with your local AI! 🚀