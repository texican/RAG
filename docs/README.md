# RAG System Documentation Hub

Welcome to the RAG System documentation! This page is your starting point for all project documentation.

## 🚀 Getting Started (New Developers Start Here!)

**New to the project?** Start here:

- **[Quick Reference](getting-started/QUICK_REFERENCE.md)** - Cheat sheet for common commands ⭐
- **[Installation Guide](../README.md#getting-started)** - Full setup instructions (in root README)
- **[Contributing Guide](../CONTRIBUTING.md)** - Development workflow and best practices

## 📚 Documentation Categories

### 🛠️ Development

Guides for daily development work:

- **[Docker Development Guide](development/DOCKER_DEVELOPMENT.md)** - Docker workflow, troubleshooting ⭐
- **[Make vs Alternatives](development/MAKE_VS_ALTERNATIVES.md)** - Why we chose Make
- **[Testing Best Practices](development/TESTING_BEST_PRACTICES.md)** - How to write tests
- **[Error Handling Guidelines](development/ERROR_HANDLING_GUIDELINES.md)** - Error handling patterns
- **[Development Methodology](development/METHODOLOGY.md)** - Our development approach
- **[ADR-001: Bypass API Gateway](development/ADR-001-BYPASS-API-GATEWAY.md)** - Architecture decision
- **[Security Documentation](development/SECURITY-001-DOCUMENTATION.md)** - Security guidelines
- **[Documentation Templates](development/templates/DOCUMENTATION_TEMPLATES.md)** - Doc templates

### 🚀 Deployment & Operations

Guides for deploying and running the system:

- **[Deployment Guide](deployment/DEPLOYMENT.md)** - How to deploy ⭐
- **[Docker Guide](deployment/DOCKER.md)** - Docker setup and configuration
- **[Service Connection Guide](deployment/SERVICE_CONNECTION_GUIDE.md)** - How services connect
- **[Swagger UI Access](deployment/SWAGGER_UI_ACCESS_GUIDE.md)** - Access Swagger documentation
- **[Docker Improvements Summary](deployment/DOCKER_IMPROVEMENTS_SUMMARY.md)** - Docker workflow improvements
- **[Enforcement Mechanisms](deployment/ENFORCEMENT_MECHANISMS.md)** - How we ensure correct Docker usage

### 🏗️ Architecture

System design and structure:

- **[Project Structure](architecture/PROJECT_STRUCTURE.md)** - Repository organization
- **[Enforcement Diagram](architecture/ENFORCEMENT_DIAGRAM.md)** - Visual workflow diagrams

### 🌐 API Documentation

- **[API Documentation Portal](api/API_DOCUMENTATION_PORTAL.md)** - Hub for all API docs

### 🧪 Testing

- **[Test Results Summary](testing/TEST_RESULTS_SUMMARY.md)** - Latest test results

### 📊 Project Management

Internal project tracking (for maintainers):

- **[Current Tasks](project-management/CURRENT_TASKS.md)** - Active work
- **[Project Backlog](project-management/PROJECT_BACKLOG.md)** - Planned work
- **[Completed Stories](project-management/COMPLETED_STORIES.md)** - Done work
- **[Sprint Plan](project-management/SPRINT_PLAN.md)** - Current sprint
- And more in [project-management/](project-management/)

### 📦 Archive

Older documentation kept for reference:

- **[Archive](archive/)** - Deprecated/historical docs

## 🎯 Quick Links by Role

### I'm a New Developer
1. Read [Quick Reference](getting-started/QUICK_REFERENCE.md) ⭐
2. Follow [Installation Guide](../README.md#getting-started)
3. Review [Contributing Guide](../CONTRIBUTING.md)
4. Check [Docker Development Guide](development/DOCKER_DEVELOPMENT.md)

### I'm Working on a Feature
1. Check [Current Tasks](project-management/CURRENT_TASKS.md)
2. Review [Testing Best Practices](development/TESTING_BEST_PRACTICES.md)
3. Follow [Contributing Guide](../CONTRIBUTING.md)
4. Use [Quick Reference](getting-started/QUICK_REFERENCE.md) for commands

### I'm Deploying the System
1. Read [Deployment Guide](deployment/DEPLOYMENT.md) ⭐
2. Check [Service Connection Guide](deployment/SERVICE_CONNECTION_GUIDE.md)
3. Review [Docker Guide](deployment/DOCKER.md)
4. Use [Swagger UI Access](deployment/SWAGGER_UI_ACCESS_GUIDE.md) for testing

### I'm Debugging an Issue
1. Check [Docker Development Guide](development/DOCKER_DEVELOPMENT.md) ⭐
2. Review [Quick Reference](getting-started/QUICK_REFERENCE.md) for commands
3. Check [Error Handling Guidelines](development/ERROR_HANDLING_GUIDELINES.md)
4. Look at [Test Results](testing/TEST_RESULTS_SUMMARY.md)

### I'm Understanding the Architecture
1. Read [Project Structure](architecture/PROJECT_STRUCTURE.md)
2. Check [ADR-001: Bypass API Gateway](development/ADR-001-BYPASS-API-GATEWAY.md)
3. Review [Service Connection Guide](deployment/SERVICE_CONNECTION_GUIDE.md)
4. Look at [Enforcement Diagram](architecture/ENFORCEMENT_DIAGRAM.md)

## 📖 Documentation Standards

When creating new documentation:

1. Use the [templates](development/templates/DOCUMENTATION_TEMPLATES.md)
2. Place in appropriate directory (see structure below)
3. Add link to this README
4. Follow [Methodology](development/METHODOLOGY.md)

## 🔍 Can't Find What You Need?

1. Use GitHub search: Press `/` and search keywords
2. Check the [Archive](archive/) for older docs
3. Ask in team chat
4. Create an issue if documentation is missing

## 📝 Contributing to Documentation

Documentation improvements are always welcome!

1. Follow [Contributing Guide](../CONTRIBUTING.md)
2. Use [Documentation Templates](development/templates/DOCUMENTATION_TEMPLATES.md)
3. Update this README if adding new docs
4. Keep docs in appropriate categories

## 🗂️ Directory Structure

```
docs/
├── README.md (this file)           # Documentation hub
│
├── getting-started/                # New developer guides
│   └── QUICK_REFERENCE.md         # Cheat sheet ⭐
│
├── development/                    # Development guides
│   ├── DOCKER_DEVELOPMENT.md      # Docker workflow ⭐
│   ├── MAKE_VS_ALTERNATIVES.md
│   ├── TESTING_BEST_PRACTICES.md
│   └── ...
│
├── deployment/                     # Deployment/ops guides
│   ├── DEPLOYMENT.md              # Deployment guide ⭐
│   ├── DOCKER.md
│   └── ...
│
├── architecture/                   # System design docs
│   ├── PROJECT_STRUCTURE.md
│   └── ...
│
├── api/                           # API documentation
│   └── API_DOCUMENTATION_PORTAL.md
│
├── testing/                       # Testing docs
│   └── TEST_RESULTS_SUMMARY.md
│
├── project-management/            # Project tracking
│   ├── CURRENT_TASKS.md
│   ├── PROJECT_BACKLOG.md
│   └── ...
│
└── archive/                       # Old/deprecated docs
    └── ...
```

---

**Last Updated:** 2025-10-01

**Quick Navigation:** [Root README](../README.md) | [Contributing](../CONTRIBUTING.md) | [Quick Reference](getting-started/QUICK_REFERENCE.md)
