#!/usr/bin/env python3
"""
Simple HTTP server for the Ollama Chat frontend.
Handles CORS issues when connecting to the local Ollama instance.
"""

import http.server
import socketserver
import os
import json
from urllib.parse import urlparse, parse_qs
from urllib.request import urlopen, Request
from urllib.error import URLError, HTTPError

class CORSHTTPRequestHandler(http.server.SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        self.ollama_url = 'http://localhost:11434'
        super().__init__(*args, **kwargs)
    
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
        """Proxy requests to Ollama to avoid CORS issues"""
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
            
            # Make the request to Ollama
            try:
                with urlopen(req, timeout=30) as response:
                    response_data = response.read()
                    
                    # Send successful response
                    self.send_response(200)
                    self.send_header('Content-Type', 'application/json')
                    self.end_headers()
                    self.wfile.write(response_data)
                    
            except HTTPError as e:
                # Forward HTTP errors from Ollama
                self.send_response(e.code)
                self.send_header('Content-Type', 'application/json')
                self.end_headers()
                error_response = json.dumps({
                    'error': f'Ollama HTTP {e.code}',
                    'message': str(e)
                }).encode('utf-8')
                self.wfile.write(error_response)
                
        except URLError as e:
            # Connection error to Ollama
            self.send_response(503)
            self.send_header('Content-Type', 'application/json')
            self.end_headers()
            error_response = json.dumps({
                'error': 'Service Unavailable',
                'message': f'Cannot connect to Ollama: {str(e)}'
            }).encode('utf-8')
            self.wfile.write(error_response)
            
        except Exception as e:
            # Other errors
            self.send_response(500)
            self.send_header('Content-Type', 'application/json')
            self.end_headers()
            error_response = json.dumps({
                'error': 'Internal Server Error',
                'message': str(e)
            }).encode('utf-8')
            self.wfile.write(error_response)

def main():
    port = 8888
    
    # Change to the directory containing the HTML file
    os.chdir(os.path.dirname(os.path.abspath(__file__)))
    
    # Create server
    with socketserver.TCPServer(("", port), CORSHTTPRequestHandler) as httpd:
        print(f"""
🦙 Ollama Chat Frontend Server Started!

📍 Access the chat interface at: http://localhost:{port}
🔗 Ollama API proxy: http://localhost:{port}/api/*
📂 Serving files from: {os.getcwd()}

💡 Features:
   ✅ CORS handling for browser requests
   ✅ Ollama API proxy to avoid connection issues  
   ✅ Clean, responsive chat interface
   ✅ Model selection and status monitoring

🛑 Press Ctrl+C to stop the server
        """)
        
        try:
            httpd.serve_forever()
        except KeyboardInterrupt:
            print("\n\n👋 Server stopped. Thanks for using Ollama Chat!")

if __name__ == "__main__":
    main()