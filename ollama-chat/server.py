#!/usr/bin/env python3
"""
Simple HTTP server for the Ollama Chat frontend.
Handles CORS issues when connecting to the local Ollama instance.
"""

import http.server
import socketserver
import os
import json
import time
import socket
from urllib.parse import urlparse, parse_qs
from urllib.request import urlopen, Request
from urllib.error import URLError, HTTPError

class CORSHTTPRequestHandler(http.server.SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        self.ollama_url = self.detect_ollama_url()
        self.connection_status = self.test_ollama_connection()
        super().__init__(*args, **kwargs)
    
    def detect_ollama_url(self):
        """Detect Ollama URL based on environment and Docker status"""
        # Check environment variable first
        ollama_url = os.getenv('OLLAMA_URL', '')
        if ollama_url:
            print(f"📝 Using OLLAMA_URL from environment: {ollama_url}")
            return ollama_url
        
        # Try Docker container name first (for BYO RAG Docker environment)
        docker_candidates = [
            'http://rag-ollama:11434',
            'http://ollama:11434'
        ]
        
        for url in docker_candidates:
            if self.test_connection(url):
                print(f"🐳 Docker Ollama detected at: {url}")
                return url
        
        # Fall back to localhost
        localhost_url = 'http://localhost:11434'
        if self.test_connection(localhost_url):
            print(f"🖥️  Local Ollama detected at: {localhost_url}")
            return localhost_url
        
        # Default fallback
        print(f"⚠️  No Ollama detected, using default: {localhost_url}")
        return localhost_url
    
    def test_connection(self, url, timeout=2):
        """Test if Ollama is reachable at the given URL"""
        try:
            test_req = Request(f"{url}/api/tags")
            with urlopen(test_req, timeout=timeout) as response:
                return response.status == 200
        except (URLError, OSError, socket.timeout):
            return False
    
    def test_ollama_connection(self):
        """Test current Ollama connection and return status info"""
        try:
            req = Request(f"{self.ollama_url}/api/tags")
            with urlopen(req, timeout=5) as response:
                data = json.loads(response.read().decode('utf-8'))
                models = data.get('models', [])
                return {
                    'connected': True,
                    'url': self.ollama_url,
                    'models_count': len(models),
                    'models': [m.get('name', 'unknown') for m in models[:5]]  # First 5 models
                }
        except Exception as e:
            return {
                'connected': False,
                'url': self.ollama_url,
                'error': str(e),
                'models_count': 0,
                'models': []
            }
    
    def end_headers(self):
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Access-Control-Allow-Methods', 'GET, POST, OPTIONS')
        self.send_header('Access-Control-Allow-Headers', 'Content-Type, Authorization')
        super().end_headers()
    
    def do_OPTIONS(self):
        self.send_response(200)
        self.end_headers()
    
    def do_GET(self):
        # Handle Ollama API proxy requests
        if self.path.startswith('/api/'):
            self.proxy_to_ollama()
        else:
            super().do_GET()
    
    def do_POST(self):
        # Handle Ollama API proxy requests
        if self.path.startswith('/api/'):
            self.proxy_to_ollama()
        else:
            super().do_POST()
    
    def proxy_to_ollama(self):
        """Proxy requests to Ollama to avoid CORS issues with retry logic"""
        # Handle special status endpoint
        if self.path == '/api/status':
            self.send_response(200)
            self.send_header('Content-Type', 'application/json')
            self.end_headers()
            status = self.test_ollama_connection()
            self.wfile.write(json.dumps(status).encode('utf-8'))
            return
        
        max_retries = 3
        retry_delay = 1
        
        for attempt in range(max_retries):
            try:
                # Build the target URL
                target_url = f"{self.ollama_url}{self.path}"
                
                # Get request body for POST requests
                content_length = int(self.headers.get('Content-Length', 0))
                request_body = self.rfile.read(content_length) if content_length > 0 else None
                
                # Create the request
                req = Request(target_url, data=request_body)
                
                # Copy relevant headers
                if self.command == 'POST':
                    req.add_header('Content-Type', 'application/json')
                
                # Adjust timeout based on request type
                timeout = 60 if self.path.endswith('/chat') else 10
                
                # Make the request to Ollama
                try:
                    with urlopen(req, timeout=timeout) as response:
                        response_data = response.read()
                        
                        # Send successful response
                        self.send_response(200)
                        self.send_header('Content-Type', 'application/json')
                        self.end_headers()
                        self.wfile.write(response_data)
                        return
                        
                except HTTPError as e:
                    if attempt == max_retries - 1:  # Last attempt
                        # Forward HTTP errors from Ollama
                        self.send_response(e.code)
                        self.send_header('Content-Type', 'application/json')
                        self.end_headers()
                        
                        error_msg = 'Model not found' if e.code == 404 else f'Ollama HTTP {e.code}'
                        if e.code == 404 and self.path.endswith('/chat'):
                            error_msg = 'Model not found. Please ensure the selected model is available in Ollama.'
                        
                        error_response = json.dumps({
                            'error': error_msg,
                            'message': str(e),
                            'suggestion': self._get_error_suggestion(e.code)
                        }).encode('utf-8')
                        self.wfile.write(error_response)
                        return
                    
            except (URLError, socket.timeout) as e:
                if attempt < max_retries - 1:
                    print(f"⚠️  Ollama connection attempt {attempt + 1} failed, retrying in {retry_delay}s...")
                    time.sleep(retry_delay)
                    retry_delay *= 2  # Exponential backoff
                    continue
                
                # Final attempt failed
                self.send_response(503)
                self.send_header('Content-Type', 'application/json')
                self.end_headers()
                
                error_response = json.dumps({
                    'error': 'Service Unavailable',
                    'message': f'Cannot connect to Ollama after {max_retries} attempts: {str(e)}',
                    'suggestion': 'Please ensure Ollama is running and accessible. Check Docker containers if using the BYO RAG system.'
                }).encode('utf-8')
                self.wfile.write(error_response)
                return
                
            except Exception as e:
                # Other errors
                self.send_response(500)
                self.send_header('Content-Type', 'application/json')
                self.end_headers()
                error_response = json.dumps({
                    'error': 'Internal Server Error',
                    'message': str(e),
                    'suggestion': 'Please check server logs for more details.'
                }).encode('utf-8')
                self.wfile.write(error_response)
                return
    
    def _get_error_suggestion(self, status_code):
        """Get helpful suggestions based on error status code"""
        suggestions = {
            404: "Try selecting a different model or pull the model using: docker-compose exec ollama ollama pull <model-name>",
            500: "The model might be loading. Please wait a moment and try again.",
            503: "Ollama service might be starting up. Please wait a moment and try again."
        }
        return suggestions.get(status_code, "Please check the Ollama service status and try again.")

def main():
    port = int(os.getenv('CHAT_PORT', 8888))
    
    # Change to the directory containing the HTML file
    os.chdir(os.path.dirname(os.path.abspath(__file__)))
    
    # Test Ollama connection before starting server
    print("🔍 Testing Ollama connection...")
    handler_class = CORSHTTPRequestHandler
    
    # Create a temporary handler to test connection
    class TempHandler:
        def detect_ollama_url(self):
            return handler_class.detect_ollama_url(self)
        def test_connection(self, url, timeout=2):
            return handler_class.test_connection(self, url, timeout)
        def test_ollama_connection(self):
            return handler_class.test_ollama_connection(self)
    
    temp_handler = TempHandler()
    temp_handler.ollama_url = temp_handler.detect_ollama_url()
    status = temp_handler.test_ollama_connection()
    
    # Display connection status
    if status['connected']:
        models_info = f"📊 Available models: {status['models_count']}"
        if status['models']:
            models_info += f" ({', '.join(status['models'])})"
    else:
        models_info = f"❌ Connection failed: {status.get('error', 'Unknown error')}"
    
    # Create server
    with socketserver.TCPServer(("", port), CORSHTTPRequestHandler) as httpd:
        print(f"""
🦙 Ollama Chat Frontend Server Started!

📍 Access the chat interface at: http://localhost:{port}
🔗 Ollama API proxy: http://localhost:{port}/api/*
📂 Serving files from: {os.getcwd()}

🌐 Ollama Connection:
   🎯 URL: {status['url']}
   {models_info}

💡 Features:
   ✅ CORS handling for browser requests
   ✅ Automatic Docker/localhost detection
   ✅ Retry logic and error recovery
   ✅ Real-time model discovery
   ✅ Enhanced error messages

🛑 Press Ctrl+C to stop the server
        """)
        
        try:
            httpd.serve_forever()
        except KeyboardInterrupt:
            print("\n\n👋 Server stopped. Thanks for using Ollama Chat!")

if __name__ == "__main__":
    main()