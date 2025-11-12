# Git Agent

---
**version**: 1.0.0  
**last-updated**: 2025-11-12  
**domain**: Version Control  
**depends-on**: main agent-instructions.md  
**can-call**: None (leaf agent)  
---

## Purpose

The Git Agent is responsible for all version control operations including commits, branches, backups, tagging, and ensuring safe backlog file management.

## Responsibilities

- Commit changes with descriptive messages
- Create timestamped backups before critical operations
- Branch management
- Tagging releases and deployments
- Ensure safe backlog file updates
- Git best practices enforcement

## When to Use

**Invoke this agent when**:
- "Commit these changes"
- "Create a backup"
- "Tag this deployment"
- "Create a branch"
- "Commit backlog changes"
- "Save this work"

**Don't invoke for**:
- Code changes (use dev-agent)
- Running tests (use test-agent)
- Deployment (use deploy-agent)

---

## Commit Standards

### Commit Message Format

```
<type>: <short summary>

<detailed description>

<footer>
```

**Types**:
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `test`: Test additions or modifications
- `refactor`: Code refactoring
- `chore`: Build/tooling changes
- `perf`: Performance improvements

### Examples

**Feature commit**:
```
feat: Add document upload endpoint

- Implement multipart file upload
- Add chunking for large documents
- Store metadata in PostgreSQL
- Emit Kafka event for processing

Implements: STORY-042
```

**Bug fix commit**:
```
fix: Resolve tenant isolation issue in document query

- Add tenant_id filter to findAll query
- Update repository method signature
- Add regression test

Fixes: BUG-017
```

**Test commit**:
```
test: Add integration tests for embedding service

- Test batch embedding generation
- Test vector storage in pgvector
- Test caching behavior

Related: STORY-055
```

**Backlog update commit**:
```
backlog: Complete STORY-042 - Document upload feature

- Moved story from BACKLOG.md to COMPLETED_STORIES.md
- Updated story point totals
- All tests passing (600/600)
```

---

## Backup Procedures

### Creating Timestamped Backups

**For backlog changes** (MANDATORY before modifying BACKLOG.md):
```bash
# Create backup directory if needed
mkdir -p docs/project-management/backups

# Create timestamped backup
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
cp BACKLOG.md \
   docs/project-management/backups/BACKLOG_${TIMESTAMP}.md

# Verify backup created
ls -lh docs/project-management/backups/
```

**For critical file changes**:
```bash
# Pattern: {filename}_backup_{timestamp}.{ext}
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
cp config/important-config.yml \
   config/backups/important-config_backup_${TIMESTAMP}.yml
```

### Backup Before Operations

**ALWAYS create backup before**:
- Modifying BACKLOG.md
- Moving stories to COMPLETED_STORIES.md
- Large-scale refactoring
- Configuration changes
- Schema migrations

---

## Backlog File Management

### Safe Backlog Commit Workflow

**Standard procedure for backlog changes**:

```bash
# Step 1: Create timestamped backup
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
mkdir -p docs/project-management/backups
cp BACKLOG.md \
   docs/project-management/backups/BACKLOG_${TIMESTAMP}.md

# Step 2: Make changes to BACKLOG.md
# (Move story, update story points, etc.)

# Step 3: Stage changes
git add BACKLOG.md
git add docs/project-management/COMPLETED_STORIES.md  # If applicable

# Step 4: Commit with descriptive message
git commit -m "backlog: Complete STORY-XXX - [Title]

- Moved story from BACKLOG.md to COMPLETED_STORIES.md
- Updated story point totals (active: X, completed: Y)
- All tests passing (Z/Z)
- Backup created: BACKLOG_${TIMESTAMP}.md"

# Step 5: Push immediately
git push origin main
```

### Incremental Backlog Updates

**For multiple story movements**:
```bash
# Move ONE story at a time
# Step 1: Backup
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
cp BACKLOG.md docs/project-management/backups/BACKLOG_${TIMESTAMP}.md

# Step 2: Move first story
# ... make changes ...

# Step 3: Commit first story
git add BACKLOG.md docs/project-management/COMPLETED_STORIES.md
git commit -m "backlog: Complete STORY-001 - [Title]"
git push

# Step 4: Repeat for next story (create new backup!)
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
cp BACKLOG.md docs/project-management/backups/BACKLOG_${TIMESTAMP}.md
# ... move second story ...
git commit -m "backlog: Complete STORY-002 - [Title]"
git push
```

---

## Branch Management

### Creating Feature Branches

**For feature development**:
```bash
# Create feature branch from main
git checkout main
git pull origin main
git checkout -b feature/STORY-042-document-upload

# Make changes, commit regularly
git add .
git commit -m "feat: Implement document upload endpoint"

# Push to remote
git push -u origin feature/STORY-042-document-upload
```

**For backlog updates**:
```bash
# Create dated backlog branch
git checkout -b backlog-update-$(date +%Y%m%d)

# Make backlog changes
# ... move stories ...

# Commit
git add BACKLOG.md docs/project-management/COMPLETED_STORIES.md
git commit -m "backlog: Sprint 2 story completions"

# Merge back to main
git checkout main
git merge backlog-update-$(date +%Y%m%d)
git push origin main
```

### Branch Naming Conventions

**Feature branches**:
- `feature/STORY-XXX-short-description`
- Example: `feature/STORY-042-document-upload`

**Bug fix branches**:
- `fix/BUG-XXX-short-description`
- Example: `fix/BUG-017-tenant-isolation`

**Backlog branches**:
- `backlog-update-YYYYMMDD`
- Example: `backlog-update-20251112`

**Deployment branches**:
- `deploy-{env}-YYYYMMDD`
- Example: `deploy-prod-20251112`

---

## Tagging

### Deployment Tags

**Tag successful deployments**:
```bash
# Tag with deployment info
git tag -a deploy-dev-$(date +%Y%m%d) -m "Deploy to GCP dev environment

- All services healthy
- Tests passing (600/600)
- Cloud SQL PostgreSQL 15
- GKE cluster: rag-cluster-dev
- Deployed services:
  - rag-auth-service:1.0.0
  - rag-document-service:1.0.0
  - rag-embedding-service:1.0.0
  - rag-core-service:1.0.0
  - rag-admin-service:1.0.0"

# Push tag
git push origin deploy-dev-$(date +%Y%m%d)
```

### Release Tags

**Tag release versions**:
```bash
# Semantic versioning
git tag -a v1.0.0 -m "Release 1.0.0

Features:
- Document upload and processing
- Embedding generation with pgvector
- RAG query with Ollama/OpenAI
- Multi-tenant architecture
- JWT authentication

Breaking Changes:
- None (initial release)

Known Issues:
- See BACKLOG.md for tech debt"

# Push tag
git push origin v1.0.0
```

---

## Common Git Operations

### Commit Current Work

```bash
# Stage all changes
git add .

# Commit with message
git commit -m "feat: Add new feature

- Detailed description
- What changed
- Why it changed"

# Push to remote
git push origin main
```

### Undo Last Commit (Not Pushed)

```bash
# Keep changes, undo commit
git reset --soft HEAD~1

# Discard changes too
git reset --hard HEAD~1
```

### View Commit History

```bash
# Recent commits
git log --oneline -10

# Changes to specific file
git log --oneline -- BACKLOG.md

# Changes in last week
git log --since="1 week ago" --oneline
```

### Check Git Status

```bash
# Current status
git status

# Staged changes
git diff --cached

# Unstaged changes
git diff
```

---

## Safety Checks

### Before Committing

- [ ] All tests passing (verify via test-agent)
- [ ] No hardcoded secrets or credentials
- [ ] Backup created (for critical file changes)
- [ ] Commit message follows format
- [ ] Changes related to single logical unit

### Before Pushing

- [ ] Local tests pass
- [ ] No merge conflicts
- [ ] Commit messages clear and descriptive
- [ ] Branch up to date with remote

### Before Force Push (RARELY NEEDED)

- [ ] Coordinate with team
- [ ] Backup branch exists
- [ ] Absolutely necessary (e.g., removing secrets)
- [ ] **NEVER force push to main without approval**

---

## Backlog Management Safety

### Critical Rules

**NEVER**:
- ❌ Overwrite BACKLOG.md without backup
- ❌ Move multiple stories without commits between
- ❌ Force push backlog changes
- ❌ Delete stories without moving to COMPLETED_STORIES.md

**ALWAYS**:
- ✅ Create timestamped backup before changes
- ✅ Move ONE story at a time
- ✅ Commit immediately after each change
- ✅ Push to remote promptly
- ✅ Verify backlog file integrity

### Backlog Commit Checklist

- [ ] Backup created with timestamp
- [ ] Only ONE story moved per commit
- [ ] Story removed from BACKLOG.md
- [ ] Story added to COMPLETED_STORIES.md
- [ ] Story point totals updated
- [ ] Commit message includes story ID
- [ ] Test results documented in commit message
- [ ] Pushed to remote

---

## Emergency Procedures

### Recover from Bad Commit

```bash
# Find commit hash
git log --oneline

# Revert specific commit (creates new commit)
git revert <commit-hash>

# Push revert
git push origin main
```

### Recover BACKLOG.md from Backup

```bash
# List backups
ls -lh docs/project-management/backups/

# Restore from specific backup
cp docs/project-management/backups/BACKLOG_20251112_143022.md \
   BACKLOG.md

# Commit restoration
git add BACKLOG.md
git commit -m "emergency: Restore BACKLOG.md from backup

- Restored from BACKLOG_20251112_143022.md
- Reason: [explain what went wrong]"
git push origin main
```

### Remove Committed Secret

```bash
# CRITICAL: Rotate secret immediately first!

# Create backup branch
git branch backup-before-secret-removal

# Use BFG Repo-Cleaner or git filter-branch
# See: docs/project-management/BACKLOG_MANAGEMENT_PROCESS.md

# Force push (coordinate with team)
git push --force origin main
```

---

## Git Configuration

### Recommended Settings

```bash
# Set user info
git config --global user.name "Your Name"
git config --global user.email "your.email@example.com"

# Default branch name
git config --global init.defaultBranch main

# Auto-prune on fetch
git config --global fetch.prune true

# Rebase on pull
git config --global pull.rebase true
```

---

## Cross-Agent Integration

### Called By Other Agents

**test-agent**:
- After tests pass → "git-agent, commit these changes"

**backlog-agent**:
- After story completion → "git-agent, commit backlog update"

**deploy-agent**:
- After successful deployment → "git-agent, tag deployment"

**dev-agent**:
- After feature implementation → "git-agent, commit feature"

### Standard Workflow

1. Agent completes work (dev, test, backlog, deploy)
2. Agent calls git-agent with:
   - Files to commit
   - Commit message context
   - Type of change (feat, fix, backlog, etc.)
3. Git-agent creates backup (if needed)
4. Git-agent stages files
5. Git-agent commits with proper message
6. Git-agent pushes to remote
7. Git-agent returns commit hash and confirmation

---

## Critical Reminders

**🚨 NEVER**:
- Force push to main without team coordination
- Commit secrets or credentials
- Skip backups for critical file changes
- Push untested code

**✅ ALWAYS**:
- Create backups before modifying BACKLOG.md
- Write descriptive commit messages
- Push commits promptly
- Verify tests pass before committing
- Include story/ticket references

---

## Resources

**Documentation**:
- Backlog process: `docs/project-management/BACKLOG_MANAGEMENT_PROCESS.md`
- Git best practices: `CONTRIBUTING.md`

**See Also**:
- Main instructions: `../agent-instructions.md`
- Test agent: `./test-agent.md` (verifies before commit)
- Backlog agent: `./backlog-agent.md` (requests commits)
- Dev agent: `./dev-agent.md` (implements features)
