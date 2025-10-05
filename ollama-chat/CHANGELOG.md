# Ollama Chat - Changelog

## [1.1.0] - 2025-10-01

### Added
- **RAG Integration Support**: Full integration with RAG SpecKit microservices
  - Auth Service (8081) integration for authentication
  - RAG Core Service (8084) for context-aware responses
  - Document Service (8082) for document management
  - Embedding Service (8083) for semantic search

- **New Files**:
  - `config.js` - Centralized configuration for RAG integration
  - `rag-integration-example.html` - Interactive demo comparing Ollama vs RAG modes
  - `stop-chat.sh` - Graceful server shutdown script with cleanup options
  - `INTEGRATION_NOTES.md` - Complete integration documentation
  - `CHANGELOG.md` - This file

- **Enhanced start-chat.sh**:
  - RAG mode detection and service health checks
  - Support for `RAG_MODE`, `RAG_CORE_URL`, `RAG_AUTH_URL` environment variables
  - Better model recommendations (llama3.2:1b, llama3.2:3b, etc.)
  - Service status display for all RAG components
  - Dual URL display (Direct Chat + RAG Integration)

### Changed
- **README.md**:
  - Added complete service endpoint documentation (ports 8081-8085)
  - Added system architecture diagram
  - Documented two operation modes: Direct Ollama and RAG-Enhanced
  - Added integration instructions with environment variables
  - Added reference to Frontend Integration Guide

- **start-chat.sh**:
  - Enhanced with RAG service health checks
  - Better error messages and troubleshooting
  - Updated model recommendations
  - Shows both chat URLs on startup
  - Displays environment configuration

### Fixed
- Server startup from correct working directory
- Model download instructions updated with latest Llama versions

## [1.0.0] - 2025-09-05

### Initial Release
- Basic Ollama chat interface
- Docker integration with BYO RAG system
- Auto-detection of Ollama (Docker/localhost)
- Model discovery and selection
- Enhanced CORS handling
- Connection reliability features
- Mobile-responsive design

---

## Usage Examples

### Direct Ollama Mode (Default)
```bash
cd ollama-chat
./start-chat.sh
# Opens http://localhost:8888/index.html

# Stop when done
./stop-chat.sh
```

### RAG-Enhanced Mode
```bash
export RAG_MODE=enabled
export RAG_CORE_URL=http://localhost:8084
export RAG_AUTH_URL=http://localhost:8081

cd ollama-chat
./start-chat.sh
# Opens both chat interfaces
```

### Custom Port
```bash
export CHAT_PORT=9999
./start-chat.sh
# Opens on http://localhost:9999
```

## Migration Guide

### Upgrading from 1.0.0 to 1.1.0

No breaking changes. All existing functionality preserved.

**New Features Available**:
1. Set `RAG_MODE=enabled` to enable RAG integration
2. Access RAG demo at `/rag-integration-example.html`
3. Configure services via `config.js`

**Recommended Actions**:
1. Review `INTEGRATION_NOTES.md` for integration details
2. Try the RAG integration example
3. Update any custom scripts to use new environment variables

## Contributing

See [CONTRIBUTING.md](../CONTRIBUTING.md) for development guidelines.

## Related Documentation

- [Frontend Integration Guide](../FRONTEND_INTEGRATION_GUIDE.md) - Complete API reference
- [README.md](README.md) - Main documentation
- [INTEGRATION_NOTES.md](INTEGRATION_NOTES.md) - Integration guide
