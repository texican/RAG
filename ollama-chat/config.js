/**
 * Ollama Chat Configuration
 *
 * This file defines the integration settings for the RAG SpecKit services.
 * You can enable RAG mode to get document-aware responses with semantic search.
 */

const ChatConfig = {
    // Operation Mode
    mode: 'ollama', // 'ollama' for direct chat, 'rag' for RAG-enhanced mode

    // Service URLs (automatically detected by server.py)
    services: {
        ollama: 'http://localhost:11434',
        auth: 'http://localhost:8081/api/v1',
        document: 'http://localhost:8082/api/v1',
        embedding: 'http://localhost:8083/api/v1',
        core: 'http://localhost:8084/api/v1',
        admin: 'http://localhost:8085/admin/api'
    },

    // RAG Configuration (when mode = 'rag')
    rag: {
        enabled: false,
        requireAuth: true,
        includeContext: true,
        maxContextChunks: 5,
        contextThreshold: 0.7,
        streaming: true,
        showSources: true
    },

    // Ollama Configuration (when mode = 'ollama')
    ollama: {
        model: 'llama3.2:1b',
        streaming: false,
        temperature: 0.7,
        maxTokens: 2000
    },

    // UI Settings
    ui: {
        showDebugPanel: false,
        theme: 'light', // 'light' or 'dark'
        showModelSelector: true,
        enableMarkdown: true
    },

    // Authentication (for RAG mode)
    auth: {
        tokenKey: 'rag_access_token',
        refreshKey: 'rag_refresh_token',
        userKey: 'rag_user',
        autoRefresh: true
    }
};

// Make config available globally
if (typeof window !== 'undefined') {
    window.ChatConfig = ChatConfig;
}

// Export for Node.js (if needed)
if (typeof module !== 'undefined' && module.exports) {
    module.exports = ChatConfig;
}
