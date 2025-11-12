---
version: 2.0.0
last-updated: 2025-11-12
status: active
applies-to: 0.8.0-SNAPSHOT
category: agent-system
---

# Sub-Agent Architecture

**Version**: 2.0.0  
**Last Updated**: 2025-11-12  
**Purpose**: Specialized agents for domain-specific tasks

## 🎉 NEW: VS Code Custom Agents Integration

**As of 2025-11-12**, all agents have been migrated to VS Code's native custom agent system:

- **Location**: `.github/agents/` (VS Code standard location)
- **Format**: `.agent.md` files with YAML frontmatter
- **Features**:
  - ✅ Native VS Code integration
  - ✅ Agent dropdown in Copilot Chat
  - ✅ Handoffs between agents with button UI
  - ✅ Tool restrictions per agent
  - ✅ Model selection per agent

**New Files Created**:
- `.github/agents/test.agent.md` - Testing expert
- `.github/agents/git.agent.md` - Version control expert
- `.github/agents/backlog.agent.md` - Backlog management expert
- `.github/agents/deploy.agent.md` - Deployment expert
- `.github/agents/dev.agent.md` - Development expert
- `.github/agents/docs.agent.md` - Documentation expert

**How to Use**:
1. Open VS Code Copilot Chat
2. Click the agents dropdown (@ icon)
3. Select the agent you need (e.g., @test, @git, @backlog)
4. Agent-specific instructions and tools will be applied
5. Use handoff buttons to transition between agents

**Legacy Files**:
- Files in `.claude/agents/*.md` remain as reference documentation
- They contain detailed procedures and examples
- VS Code agents in `.github/agents/` are the active system

---

## Overview

The RAG project uses a **specialized sub-agent architecture** to improve performance, maintainability, and clarity. Instead of a single monolithic agent handling all tasks, work is delegated to focused sub-agents based on domain expertise.

## Architecture Principles

1. **Single Responsibility**: Each agent focuses on one domain
2. **Clear Delegation**: Routing logic determines which agent handles each task
3. **Context Optimization**: Agents load only relevant context
4. **Independent Maintenance**: Agents can be updated without affecting others
5. **Cross-Agent Communication**: Agents can call other agents when needed

## Available Sub-Agents

### 1. Test Agent (`test-agent.md`)
**Domain**: Testing standards, execution, and validation  
**Responsibilities**:
- Execute test suites (unit, integration, E2E)
- Enforce test-first protocol
- Validate 100% pass rate requirement
- Analyze test failures
- Generate test coverage reports

**When to Use**:
- "Run tests"
- "Check test coverage"
- "Analyze test failure"
- "Validate tests before story completion"

**Key Capabilities**:
- Execute `mvn test` for specific services
- Parse test results and identify failures
- Enforce definition of done (100% pass rate)
- Generate test reports

---

### 2. Backlog Agent (`backlog-agent.md`)
**Domain**: Backlog management and project planning  
**Responsibilities**:
- Story creation and estimation (pebbles/rocks/boulders)
- Story completion workflow
- Backlog safety procedures
- Sprint planning
- Story point reconciliation

**When to Use**:
- "Create new story"
- "Estimate this work"
- "Complete story STORY-XXX"
- "Sprint planning"
- "Reconcile story points"

**Key Capabilities**:
- Enforce safety procedures (backup, incremental changes)
- Validate definition of done before completion
- Move stories from BACKLOG.md to COMPLETED_STORIES.md
- Calculate story point totals

**Critical**: MUST enforce backup procedures before any backlog changes

---

### 3. Deploy Agent (`deploy-agent.md`)
**Domain**: Deployment and infrastructure  
**Responsibilities**:
- Local deployment (Colima/Docker Compose)
- GCP deployment (7-phase process)
- Kubernetes operations
- Health check validation
- Service monitoring

**When to Use**:
- "Deploy locally"
- "Deploy to GCP"
- "Setup local environment"
- "Check service health"
- "Kubernetes troubleshooting"

**Key Capabilities**:
- Execute deployment scripts
- Validate health checks
- Monitor pod status
- Troubleshoot deployment issues

**Sub-Commands**:
- `@deploy-agent local` - Local Docker Compose deployment
- `@deploy-agent gcp` - GCP/GKE deployment
- `@deploy-agent k8s` - Kubernetes operations

---

### 4. Dev Agent (`dev-agent.md`)
**Domain**: Development workflows and code standards  
**Responsibilities**:
- Code implementation guidance
- Service structure patterns
- REST endpoint creation
- Configuration management
- Tenant isolation enforcement

**When to Use**:
- "Add new endpoint"
- "Create new service"
- "How do I implement X?"
- "Code pattern for Y"

**Key Capabilities**:
- Provide code templates
- Enforce tenant isolation
- Guide error handling patterns
- Service rebuild workflows

---

### 5. Git Agent (`git-agent.md`)
**Domain**: Version control operations  
**Responsibilities**:
- Branch management
- Commit message standards
- Backup creation
- Change validation
- Merge operations

**When to Use**:
- "Create branch"
- "Commit changes"
- "Create backup"
- "Validate changes"

**Key Capabilities**:
- Create timestamped backups
- Enforce commit message standards
- Validate changes before commit
- Manage git workflow

---

### 6. Docs Agent (`docs-agent.md`)
**Domain**: Documentation management  
**Responsibilities**:
- Documentation validation (links, metadata, format)
- Documentation generation (API docs, diagrams, changelogs)
- Documentation maintenance (freshness, consolidation)
- Quality reporting (coverage, health metrics)

**When to Use**:
- "Validate documentation"
- "Check broken links"
- "Generate API docs"
- "Create documentation"
- "Documentation health report"

**Key Capabilities**:
- Automated link checking
- OpenAPI spec generation
- Metadata enforcement
- Diagram creation
- Documentation quality gates

---

## Delegation Framework

### Routing Decision Tree

```
User Request
    │
    ├─ Test-related? ────────────────→ test-agent.md
    │   (run tests, coverage, failures)
    │
    ├─ Backlog-related? ─────────────→ backlog-agent.md
    │   (story creation, completion, estimation)
    │
    ├─ Deployment-related? ──────────→ deploy-agent.md
    │   (local/GCP deploy, k8s, health checks)
    │
    ├─ Development-related? ─────────→ dev-agent.md
    │   (code patterns, endpoints, services)
    │
    ├─ Git-related? ─────────────────→ git-agent.md
    │   (branches, commits, backups)
    │
    ├─ Documentation-related? ───────→ docs-agent.md
    │   (validate, generate, fix docs)
    │
    └─ General/Unknown? ─────────────→ main agent-instructions.md
        (routing help, core concepts)
```

### Cross-Agent Communication

Agents can delegate to other agents:

**Example 1**: Story Completion Workflow
```
User: "Complete STORY-023"
  → backlog-agent.md
      → test-agent.md (validate all tests passing)
      → git-agent.md (commit changes)
      → backlog-agent.md (move story to COMPLETED_STORIES.md)
```

**Example 2**: Deployment Workflow
```
User: "Deploy to GCP"
  → deploy-agent.md
      → test-agent.md (pre-deployment test validation)
      → deploy-agent.md (execute deployment)
      → deploy-agent.md (validate health checks)
```

**Example 3**: Development Workflow
```
User: "Add new endpoint"
  → dev-agent.md (provide implementation guidance)
      → dev-agent.md (rebuild service)
      → test-agent.md (run tests)
      → docs-agent.md (generate API documentation)
      → git-agent.md (commit if tests pass)
```

## Agent Metadata Format

Each agent file includes metadata header:

```markdown
---
version: 1.0.0
last-updated: 2025-11-12
domain: [Testing/Backlog/Deployment/Development/Git]
depends-on: main agent-instructions.md
can-call: [list of other agents]
---
```

## Benefits of Sub-Agent Architecture

### Performance
- ✅ **60-80% reduction** in context loading for specialized tasks
- ✅ **Faster response times** - less content to process
- ✅ **Lower token usage** - load only relevant context

### Maintainability
- ✅ **Easier updates** - change only affected domain
- ✅ **Clear ownership** - each domain has single source of truth
- ✅ **Reduced conflicts** - independent agent updates

### Usability
- ✅ **Faster navigation** - find relevant info quickly
- ✅ **Clearer routing** - obvious which agent handles what
- ✅ **Better errors** - domain-specific error messages

### Scalability
- ✅ **Easy additions** - new agents don't affect existing
- ✅ **Independent versioning** - agents evolve separately
- ✅ **Parallel development** - multiple agents can be improved simultaneously

## File Size Comparison

**Before Modularization**:
- `agent-instructions.md`: 3,300 lines (everything)

**After Modularization**:
- `agent-instructions.md`: ~500 lines (index + core concepts)
- `test-agent.md`: ~850 lines
- `backlog-agent.md`: ~750 lines
- `deploy-agent.md`: ~1,150 lines
- `dev-agent.md`: ~650 lines
- `git-agent.md`: ~550 lines
- `docs-agent.md`: ~1,800 lines
- `references/`: ~800 lines (examples, patterns, troubleshooting)

**Result**: Instead of loading 3,300 lines for any task, load 500 + domain-specific content (550-1,800 lines) = **1,050-2,300 lines total** (30-70% reduction, depending on agent)

## Usage Examples

### Example 1: Running Tests
```
User: "Run all tests"
Agent: Routing to test-agent.md...
Test Agent: 
  - Executing: mvn clean test
  - Services: auth, document, embedding, core, admin
  - Results: 594/600 passing (99%)
  - Failures: 6 tests in TECH-DEBT-006, TECH-DEBT-007
  - Status: NOT production ready (failures present)
```

### Example 2: Completing a Story
```
User: "Complete STORY-023"
Agent: Routing to backlog-agent.md...
Backlog Agent:
  Step 1: Creating backup...
  Step 2: Calling test-agent to validate tests...
  Test Agent: All tests passing ✅
  Step 3: Calling git-agent to commit...
  Git Agent: Changes committed ✅
  Step 4: Moving story to COMPLETED_STORIES.md...
  Step 5: Updating story point totals...
  Status: Story STORY-023 completed ✅
```

### Example 3: Deploying to GCP
```
User: "Deploy to GCP dev environment"
Agent: Routing to deploy-agent.md...
Deploy Agent:
  Step 1: Calling test-agent for pre-deployment validation...
  Test Agent: 594/600 tests passing ✅
  Step 2: Executing GCP deployment (7 phases)...
  [Phase 1-7 execution details]
  Step 3: Validating health checks...
  Status: Deployment successful ✅
```

## Maintenance Guidelines

### Adding a New Sub-Agent

1. **Identify Domain**: Clear, focused responsibility
2. **Create File**: `.claude/agents/new-agent.md`
3. **Add Metadata**: Version, domain, dependencies
4. **Document Responsibilities**: What it handles
5. **Update Routing**: Add to delegation framework
6. **Test Independently**: Validate agent works alone
7. **Update README**: Add to this file

### Updating an Existing Agent

1. **Update Version**: Increment version number
2. **Update Last-Updated**: Current date
3. **Document Changes**: What changed and why
4. **Test Changes**: Ensure no regressions
5. **Update Cross-References**: If routing changed

### Deprecating an Agent

1. **Mark Deprecated**: Add deprecation notice
2. **Provide Alternative**: Which agent replaces it
3. **Migration Period**: Allow transition time
4. **Remove References**: Update routing logic
5. **Archive**: Move to `.claude/agents/archived/`

## Version History

### v1.1.0 (2025-11-12)
- Added Documentation Agent (docs-agent.md)
- Updated routing to include documentation tasks
- Enhanced agent delegation framework

### v1.0.0 (2025-11-12)
- Initial sub-agent architecture
- Created 5 specialized agents:
  - test-agent.md
  - backlog-agent.md
  - deploy-agent.md
  - dev-agent.md
  - git-agent.md
- Established delegation framework
- Reduced main agent-instructions.md to 500 lines

## Future Enhancements

### Planned Agents
- **security-agent.md**: Security scanning, vulnerability assessment
- **performance-agent.md**: Performance testing, optimization
- **monitoring-agent.md**: Observability, alerting, metrics

### Planned Features
- Automated agent capability registry
- Agent performance metrics
- Cross-agent orchestration patterns
- Agent version compatibility matrix

---

**For Questions**: See main agent-instructions.md or specific agent documentation
