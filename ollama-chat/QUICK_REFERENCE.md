# Ollama Chat - Quick Reference

## 🚀 Common Commands

### Start Server
```bash
./start-chat.sh
```

### Stop Server
```bash
./stop-chat.sh
```

### Start with RAG Mode
```bash
export RAG_MODE=enabled
./start-chat.sh
```

### Custom Port
```bash
export CHAT_PORT=9999
./start-chat.sh
```

## 🌐 URLs

| Interface | URL |
|-----------|-----|
| **Main Chat** | http://localhost:8888/index.html |
| **RAG Demo** | http://localhost:8888/rag-integration-example.html |
| **Config** | [config.js](config.js) |

## 📦 Download Models

### Recommended Models
```bash
# Small & Fast (1.3GB)
docker-compose exec ollama ollama pull llama3.2:1b

# Balanced (4.7GB)
docker-compose exec ollama ollama pull llama3.2:3b

# High Quality (4.7GB)
docker-compose exec ollama ollama pull llama2:7b-chat

# Code Focused (3.8GB)
docker-compose exec ollama ollama pull codellama:7b
```

### List Models
```bash
docker-compose exec ollama ollama list
```

### Remove Model
```bash
docker-compose exec ollama ollama rm <model-name>
```

## 🔧 Troubleshooting

### Check if Server Running
```bash
lsof -i :8888
```

### Check Ollama
```bash
curl http://localhost:11434/api/tags
```

### Kill Stuck Server
```bash
pkill -f 'python.*server.py'
```

### View Logs
```bash
tail -f chat-server.log
```

### Check RAG Services
```bash
# Auth Service
curl http://localhost:8081/actuator/health

# Core Service
curl http://localhost:8084/actuator/health

# Document Service
curl http://localhost:8082/actuator/health

# Embedding Service
curl http://localhost:8083/actuator/health
```

## 🎯 Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `CHAT_PORT` | 8888 | Server port |
| `RAG_MODE` | disabled | Enable RAG integration |
| `RAG_CORE_URL` | http://localhost:8084 | RAG Core Service URL |
| `RAG_AUTH_URL` | http://localhost:8081 | Auth Service URL |
| `OLLAMA_URL` | auto-detect | Ollama API URL |

## 📚 Service Ports

| Service | Port | Purpose |
|---------|------|---------|
| **Ollama Chat** | 8888 | Chat frontend |
| **Auth Service** | 8081 | Authentication |
| **Document Service** | 8082 | Document management |
| **Embedding Service** | 8083 | Vector embeddings |
| **Core Service** | 8084 | RAG queries |
| **Admin Service** | 8085 | Administration |
| **Ollama** | 11434 | LLM API |

## 🔍 Health Checks

### Quick Check All Services
```bash
echo "Ollama:"; curl -s http://localhost:11434/api/tags | jq -r '.models | length'
echo "Auth:"; curl -s http://localhost:8081/actuator/health | jq -r .status
echo "Document:"; curl -s http://localhost:8082/actuator/health | jq -r .status
echo "Embedding:"; curl -s http://localhost:8083/actuator/health | jq -r .status
echo "Core:"; curl -s http://localhost:8084/actuator/health | jq -r .status
echo "Admin:"; curl -s http://localhost:8085/admin/api/actuator/health | jq -r .status
```

### Start All RAG Services
```bash
cd .. && docker-compose up -d
```

### Check Docker Containers
```bash
docker-compose ps
```

## 📖 Documentation

| Document | Purpose |
|----------|---------|
| [README.md](README.md) | Main documentation |
| [INTEGRATION_NOTES.md](INTEGRATION_NOTES.md) | RAG integration guide |
| [CHANGELOG.md](CHANGELOG.md) | Version history |
| [QUICK_REFERENCE.md](QUICK_REFERENCE.md) | This file |
| [Frontend Guide](../FRONTEND_INTEGRATION_GUIDE.md) | API documentation |

## 🎨 Files

| File | Purpose |
|------|---------|
| `index.html` | Main chat UI |
| `rag-integration-example.html` | RAG comparison demo |
| `server.py` | Python CORS proxy |
| `config.js` | Configuration |
| `start-chat.sh` | Start script |
| `stop-chat.sh` | Stop script |

## 💡 Tips

- Use **llama3.2:1b** for fast testing (1.3GB)
- Use **llama2:7b-chat** for quality conversations (4.7GB)
- Use **codellama:7b** for code-related tasks (3.8GB)
- First message may be slow (model loading)
- Press `Enter` to send, `Shift+Enter` for new line
- Check logs if something doesn't work: `cat chat-server.log`

## 🐛 Common Issues

### "No models found"
```bash
docker-compose exec ollama ollama pull llama3.2:1b
```

### "Connection refused"
```bash
# Check if Ollama is running
docker-compose ps ollama

# Restart if needed
docker-compose restart ollama
```

### "Port already in use"
```bash
./stop-chat.sh
# or
pkill -f 'python.*server.py'
```

### "RAG services not available"
```bash
# Start all services
cd .. && docker-compose up -d

# Wait a minute for services to start
sleep 60

# Check status
docker-compose ps
```

## 🚦 Quick Start Checklist

- [ ] Start Docker services: `cd .. && docker-compose up -d`
- [ ] Pull a model: `docker-compose exec ollama ollama pull llama3.2:1b`
- [ ] Start chat: `./start-chat.sh`
- [ ] Open browser: http://localhost:8888/index.html
- [ ] Select model from dropdown
- [ ] Start chatting!

---

**Need help?** Check [README.md](README.md) or [INTEGRATION_NOTES.md](INTEGRATION_NOTES.md)
