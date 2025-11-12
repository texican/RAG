# Agent Instructions for RAG System - Main Index

**Version**: 2.0.0  
**Last Updated**: 2025-11-12  
**Architecture**: Modular Sub-Agent System

---

## Table of Contents

1. [Project Overview](#project-overview)
2. [Sub-Agent Architecture](#sub-agent-architecture)
3. [Routing Decision Tree](#routing-decision-tree)
4. [Critical Project Rules](#critical-project-rules)
5. [Key Design Decisions](#key-design-decisions)
6. [Communication Standards](#communication-standards)
7. [Quick Reference](#quick-reference)

---

## Project Overview

This is the **BYO (Build Your Own) RAG System** - an enterprise-grade Retrieval Augmented Generation platform built with Java 21, Spring Boot 3.2, and Spring AI.

**Current Version**: 0.8.0-SNAPSHOT  
**Status**: Production-ready with GCP deployment infrastructure  
**Test Coverage**: 594/600 functional tests passing (99%)

### System Architecture
- **6 Microservices**: Auth, Document, Embedding, Core (RAG), Admin
- **Multi-tenant**: Complete data isolation via tenant_id filtering
- **Event-Driven**: Kafka optional (disabled by default)
- **Containerized**: Docker Compose (local), GKE (production)
- **AI-Powered**: Supports OpenAI and local Ollama models

### Technology Stack
```
Core:       Java 21, Spring Boot 3.2.11, Spring AI 1.0.0-M1
Data:       PostgreSQL 42.7.3, Redis Stack 5.0.2
AI/ML:      LangChain4j 0.33.0, Ollama
Cloud:      GCP (GKE, Cloud SQL, Memorystore, Artifact Registry)
Testing:    JUnit 5.10.2, Testcontainers 1.19.8
```

### Database Architecture
- **PostgreSQL**: Shared `byo_rag_{env}` database
- **Redis**: Single instance with key prefixes `byo_rag_{env}:{service}:*`
- **Kafka**: OPTIONAL (disabled by default, ~$250-450/month savings)

---

## Sub-Agent Architecture

This agent system uses **specialized sub-agents** for different domains. Each sub-agent is an expert in its area with focused responsibilities.

### 🚨 Critical Rule: Always Check Agent Instructions First

**Before ANY action related to these domains, you MUST:**
1. Check if `.claude/agents/{domain}-agent.md` exists
2. Read the agent instructions completely
3. Follow the documented procedures exactly
4. Never improvise or assume "standard" practices

**Trigger Reference**: See `.claude/AGENT_TRIGGERS.md` for comprehensive list of phrases and actions that require agent consultation.

### Available Sub-Agents

| Agent | Domain | File | Purpose |
|-------|--------|------|---------|
| **Test Agent** | Testing | `.claude/agents/test-agent.md` | Test execution, validation, quality gates |
| **Backlog Agent** | Backlog Mgmt | `.claude/agents/backlog-agent.md` | Story estimation, completion, sprint planning |
| **Deploy Agent** | Deployment | `.claude/agents/deploy-agent.md` | Local/GCP deployment, infrastructure |
| **Dev Agent** | Development | `.claude/agents/dev-agent.md` | Feature implementation, debugging |
| **Git Agent** | Version Control | `.claude/agents/git-agent.md` | Commits, branches, backups, tagging |
| **Docs Agent** | Documentation | `.claude/agents/docs-agent.md` | Documentation validation, generation, maintenance |

### How Sub-Agents Work

1. **Main Agent** receives user request
2. **Routing** - Determines which sub-agent(s) to invoke based on keywords/context
3. **Delegation** - Invokes specialized sub-agent(s)
4. **Coordination** - Sub-agents may call other sub-agents
5. **Response** - Results aggregated and returned to user

**Benefits**:
- 60-80% reduction in context loading per task
- Faster response times (less content to process)
- Better accuracy (focused domain knowledge)
- Easier maintenance (domain ownership)

**Architecture Details**: See `.claude/agents/README.md` for complete delegation framework

---

## Routing Decision Tree

### When to Use Which Agent

**Use Test Agent** when:
- "Run tests"
- "Check test coverage"
- "Analyze test failure"
- "Validate tests before story completion"
- "Run integration tests"
- "Test report"

**Use Backlog Agent** when:
- "Estimate this story"
- "Complete this story"
- "Move story to completed"
- "Plan next sprint"
- "Update backlog"
- "How many story points"

**Use Deploy Agent** when:
- "Deploy to GCP"
- "Setup local environment"
- "Deploy services"
- "Validate deployment"
- "Fix deployment issue"
- "Start local services"

**Use Dev Agent** when:
- "Implement this feature"
- "Add REST endpoint"
- "Fix this bug"
- "Debug this service"
- "Add configuration"
- "Make schema change"

**Use Git Agent** when:
- "Commit these changes"
- "Commit and push"
- "Push changes"
- "Create a backup"
- "Tag this deployment"
- "Create a branch"
- "Save this work"
- Any git/version control operations

**Use Docs Agent** when:
- "Validate documentation"
- "Check broken links"
- "Generate API docs"
- "Update documentation"
- "Documentation health report"
- "Fix documentation"
- Any documentation quality/maintenance tasks

### Agent Dependencies

```
Main Agent (this file)
  ├─ Test Agent (leaf agent)
  ├─ Backlog Agent (calls: test-agent, git-agent, docs-agent)
  ├─ Deploy Agent (calls: test-agent, git-agent, docs-agent)
  ├─ Dev Agent (calls: test-agent, git-agent, docs-agent)
  ├─ Git Agent (calls: docs-agent)
  └─ Docs Agent (calls: test-agent, git-agent)
```

**Delegation Examples**:

**Example 1: Story Completion**
```
User: "Complete STORY-042"
Main → Backlog Agent
  → Backlog Agent calls Test Agent (verify tests pass)
  → Backlog Agent moves story
  → Backlog Agent calls Git Agent (commit changes)
Main → Returns summary to user
```

**Example 2: Deploy to GCP**
```
User: "Deploy to GCP dev"
Main → Deploy Agent
  → Deploy Agent calls Test Agent (verify tests before deploy)
  → Deploy Agent executes GCP deployment phases
  → Deploy Agent calls Git Agent (tag successful deployment)
Main → Returns deployment status
```

**Example 3: Implement Feature**
```
User: "Add health check endpoint"
Main → Dev Agent
  → Dev Agent implements code
  → Dev Agent calls Test Agent (write and run tests)
  → Dev Agent calls Git Agent (commit feature)
Main → Returns implementation summary
```

---

## Critical Project Rules

### Development Workflow

**ALWAYS Use Makefile Commands for Docker**:
```bash
# ✅ CORRECT: Rebuild service after code changes
make rebuild SERVICE=rag-auth

# ✅ View logs
make logs SERVICE=rag-auth

# ❌ NEVER: docker restart rag-auth (doesn't reload code)
```

**Why**: The rebuild script:
1. Builds JAR with Maven
2. Builds Docker image with correct name
3. Stops and removes old container
4. Creates new container from new image
5. Waits for health check

### Service Names (Strict)

| Service | Container | Port |
|---------|-----------|------|
| Auth | `rag-auth` | 8081 |
| Document | `rag-document` | 8082 |
| Embedding | `rag-embedding` | 8083 |
| Core | `rag-core` | 8084 |
| Admin | `rag-admin` | 8085 |

### Configuration Profiles

```yaml
# Local development (direct connections)
spring.profiles.active=local
# Uses: localhost, direct JDBC URLs

# Docker Compose (service names)
spring.profiles.active=docker  
# Uses: postgres, redis, kafka (Docker service names)

# GCP/GKE (production)
spring.profiles.active=gcp
# Uses: Cloud SQL proxy, Memorystore, Secret Manager
```

### Tenant Isolation (Critical!)

**Every query MUST filter by tenant_id** - See dev-agent for patterns

---

## Key Design Decisions

### Kafka is Optional (Critical!)
- **Default State**: Disabled via `exclude = {KafkaAutoConfiguration.class}`
- **Why**: Cost savings (~$250-450/month), simpler deployment
- **All Kafka code**: Protected by `@ConditionalOnBean(KafkaTemplate.class)`
- **Fallback**: Services work synchronously via REST/Feign
- **Re-enable**: See `docs/architecture/KAFKA_OPTIONAL.md`

### API Gateway Bypassed
- **Decision**: Direct service access instead of gateway routing
- **Why**: Simplified architecture, lower latency
- **Status**: Gateway archived in `archive/rag-gateway/`
- **See**: `docs/development/ADR-001-BYPASS-API-GATEWAY.md`

### Shared Database Pattern
- **Implementation**: Shared `byo_rag_{env}` database for all services
- **Why**: Immediate tenant data consistency, simpler deployment
- **Trade-off**: Less microservice isolation, but practical for RAG use case

---

## Communication Standards

### Required Status Update Format

**ALWAYS include in status updates**:
1. **What was done** (specific actions taken)
2. **Current state** (where things stand now)
3. **Test results** (X/Y tests passing)
4. **Blockers** (any issues preventing progress)
5. **Next steps** (what comes next)

**Template**:
```markdown
## Status Update

**Completed**:
- [Specific action 1]
- [Specific action 2]

**Current State**:
- [Where things stand]
- Test Results: X/Y passing (Z%)

**Blockers**:
- [Any blockers, or "None"]

**Next Steps**:
- [Next action 1]
- [Next action 2]
```

**Examples**: See `.claude/references/communication-examples.md`

### Forbidden Phrases

**NEVER** use vague language:
- ❌ "Mostly working"
- ❌ "Almost done"
- ❌ "Just a few test failures"
- ❌ "Minor issues"

**ALWAYS** be specific:
- ✅ "594/600 tests passing (99%)"
- ✅ "6 tests failing in rag-core-service"
- ✅ "Deployment blocked by database connection issue"

### Honesty & Transparency

**Critical Rules**:
- ✅ Always lead with problems and blockers
- ✅ Quantify everything (exact numbers, not estimates)
- ✅ Admit when something doesn't work
- ✅ Provide context for failures
- ❌ Never minimize issues
- ❌ Never omit important information
- ❌ Never claim completion with failing tests

---

## Quick Reference

### Common Tasks → Agent Mapping

| Task | Agent to Use | File |
|------|-------------|------|
| Run tests | Test Agent | `.claude/agents/test-agent.md` |
| Complete story | Backlog Agent | `.claude/agents/backlog-agent.md` |
| Deploy locally | Deploy Agent | `.claude/agents/deploy-agent.md` |
| Deploy to GCP | Deploy Agent | `.claude/agents/deploy-agent.md` |
| Add feature | Dev Agent | `.claude/agents/dev-agent.md` |
| Fix bug | Dev Agent | `.claude/agents/dev-agent.md` |
| Commit changes | Git Agent | `.claude/agents/git-agent.md` |
| Debug service | Dev Agent | `.claude/agents/dev-agent.md` |
| Estimate story | Backlog Agent | `.claude/agents/backlog-agent.md` |
| Validate docs | Docs Agent | `.claude/agents/docs-agent.md` |
| Generate API docs | Docs Agent | `.claude/agents/docs-agent.md` |
| Fix broken links | Docs Agent | `.claude/agents/docs-agent.md` |

### Key Documentation Files

**Project Management**:
- `BACKLOG.md` - Active stories ONLY
- `docs/project-management/COMPLETED_STORIES.md` - Completed stories archive
- `docs/project-management/BACKLOG_MANAGEMENT_PROCESS.md` - Safety procedures
- `QUALITY_STANDARDS.md` - Quality gates

**Development**:
- `docs/development/DEVELOPMENT_GUIDE.md` - Development workflows
- `docs/development/TESTING_BEST_PRACTICES.md` - Testing standards
- `docs/architecture/KAFKA_OPTIONAL.md` - Kafka configuration

**Deployment**:
- `docs/deployment/GCP_DEPLOYMENT_GUIDE.md` - Complete GCP deployment
- `docs/deployment/INITIAL_DEPLOYMENT_GUIDE.md` - First-time deployment
- `docker-compose.yml` - Local development stack

### Current Project Status

**Infrastructure**:
- **Local**: Docker Compose with Colima (macOS)
- **GCP Project**: `byo-rag-dev`
- **GKE Cluster**: `rag-cluster-dev` (us-central1)
- **Cloud SQL**: `rag-postgres-dev` (PostgreSQL 15)
- **Memorystore**: `rag-redis-dev` (Redis 7.0)

**Test Status**: 594/600 passing (99%)
- Known issues: 3 auth config tests, 3 Ollama embedding tests
- See: `BACKLOG.md` for TECH-DEBT stories

**Current Sprint**: Sprint 2
- Active stories: See `BACKLOG.md`
- Completed stories: See `docs/project-management/COMPLETED_STORIES.md`

---

## Decision Matrix: Which Agent to Use

```
┌─────────────────────────────────────────────────────────────┐
│                    USER REQUEST                              │
└────────────┬────────────────────────────────────────────────┘
             │
             ├─ Contains "test", "tests", "testing"
             │  → TEST AGENT
             │
             ├─ Contains "deploy", "deployment", "local", "GCP"
             │  → DEPLOY AGENT
             │
             ├─ Contains "story", "backlog", "estimate", "complete"
             │  → BACKLOG AGENT
             │
             ├─ Contains "commit", "push", "git", "branch", "backup", "save"
             │  → GIT AGENT
             │
             ├─ Contains "documentation", "docs", "links", "api docs", "validate"
             │  → DOCS AGENT
             │
             ├─ Contains "implement", "add", "fix", "debug", "feature"
             │  → DEV AGENT
             │
             └─ General question / needs context
                → MAIN AGENT (this file)
```

---

## Version History

**v2.0.0** (2025-11-12):
- Modularized into sub-agent architecture
- Created 5 specialized agents (test, backlog, deploy, dev, git)
- Reduced main file from 3,300 lines to ~500 lines
- Added routing decision tree
- Improved cross-agent communication patterns

**v1.0.0** (2025-10-01):
- Initial monolithic agent instructions
- Comprehensive but inefficient (3,300 lines)
- All guidance in single file

---

## Additional Resources

**Sub-Agents**:
- **Agents README**: `.claude/agents/README.md` - Sub-agent architecture guide
- **Test Agent**: `.claude/agents/test-agent.md` - Testing expert (850 lines)
- **Backlog Agent**: `.claude/agents/backlog-agent.md` - Backlog management (750 lines)
- **Deploy Agent**: `.claude/agents/deploy-agent.md` - Deployment expert (1,150 lines)
- **Dev Agent**: `.claude/agents/dev-agent.md` - Development expert (650 lines)
- **Git Agent**: `.claude/agents/git-agent.md` - Version control expert (550 lines)
- **Docs Agent**: `.claude/agents/docs-agent.md` - Documentation expert (1,800 lines)

**References**:
- **Communication Examples**: `.claude/references/communication-examples.md` - Status update templates

**Total Context Available**: ~4,450 lines across all agents + references
**Context Used Per Task**: ~500-900 lines (60-80% reduction)

---

## Critical Reminders

**🚨 NEVER**:
- Use vague language in status updates
- Minimize test failures or blockers
- Claim completion with failing tests
- Skip tenant isolation checks
- Commit without running tests (call test-agent first)
- Deploy without validation (call deploy-agent)

**✅ ALWAYS**:
- Route tasks to appropriate sub-agent
- Quantify results (exact numbers)
- Lead with problems and blockers
- Verify tests pass before story completion
- Create backups before critical operations (git-agent)
- Use Makefile commands for Docker operations

---

**Need help?** Refer to the appropriate sub-agent for detailed guidance in that domain.
