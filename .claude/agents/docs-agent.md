---
version: 1.0.0
last-updated: 2025-11-12
status: active
applies-to: 0.8.0-SNAPSHOT
category: agent-system
---

# Documentation Agent

---
**Version**: 1.0.0  
**Created**: 2025-11-12  
**Last Updated**: 2025-11-12  
**Domain**: Documentation Management  
**Depends On**: main agent-instructions.md  
**Can Call**: test-agent, git-agent  
---

## Purpose

The **Documentation Agent** is a specialized sub-agent responsible for maintaining documentation quality, consistency, and completeness across the entire RAG System. It automates documentation validation, generation, maintenance, and quality reporting.

## Table of Contents

1. [Core Responsibilities](#core-responsibilities)
2. [When to Use This Agent](#when-to-use-this-agent)
3. [Agent Capabilities](#agent-capabilities)
4. [Standard Operating Procedures](#standard-operating-procedures)
5. [Documentation Standards](#documentation-standards)
6. [Quality Gates](#quality-gates)
7. [Integration Patterns](#integration-patterns)
8. [Templates](#templates)
9. [Troubleshooting](#troubleshooting)

---

## Core Responsibilities

### 1. Documentation Validation

**Link Validation**:
- ✅ Check all internal documentation links
- ✅ Validate external links (with caching)
- ✅ Report broken links with fix suggestions
- ✅ Generate link health reports

**Metadata Validation**:
- ✅ Ensure all docs have required version metadata
- ✅ Verify `last-updated` dates are current
- ✅ Check `status` field values
- ✅ Validate `applies-to` version compatibility

**Format Validation**:
- ✅ Enforce markdown formatting standards
- ✅ Check heading hierarchy (H1 → H2 → H3)
- ✅ Validate code block language identifiers
- ✅ Ensure consistent style

**Content Validation**:
- ✅ Check for TODO/FIXME/XXX markers in final docs
- ✅ Verify code examples are testable
- ✅ Ensure images/diagrams exist
- ✅ Validate cross-references

### 2. Documentation Generation

**API Documentation**:
- ✅ Generate OpenAPI specs from Spring annotations
- ✅ Create API reference pages
- ✅ Generate authentication examples
- ✅ Build API changelog

**Diagram Generation**:
- ✅ Generate Mermaid diagrams from code
- ✅ Create service dependency graphs
- ✅ Build data flow diagrams
- ✅ Produce deployment topology visuals

**Template Instantiation**:
- ✅ Create new docs from templates
- ✅ Generate boilerplate sections
- ✅ Pre-fill metadata
- ✅ Add standard sections

**Change Documentation**:
- ✅ Generate changelog from git commits
- ✅ Create migration guides
- ✅ Build deprecation notices
- ✅ Document breaking changes

### 3. Documentation Maintenance

**Freshness Management**:
- ✅ Update `last-updated` timestamps
- ✅ Flag stale documentation (>90 days)
- ✅ Suggest reviews for old content
- ✅ Archive deprecated documentation

**Link Management**:
- ✅ Fix broken internal links
- ✅ Update moved document references
- ✅ Create redirects for renamed files
- ✅ Maintain link index

**Consolidation**:
- ✅ Identify duplicate content
- ✅ Suggest merge opportunities
- ✅ Flag inconsistencies
- ✅ Recommend archival

**Quality Improvement**:
- ✅ Suggest missing sections
- ✅ Identify coverage gaps
- ✅ Recommend additional examples
- ✅ Propose structure improvements

### 4. Quality Reporting

**Coverage Reports**:
- API documentation coverage (% endpoints documented)
- Visual diagram coverage (% components with diagrams)
- Code example coverage (% features with examples)
- Completeness score by category

**Health Reports**:
- Broken link count and severity
- Stale documentation list
- Missing metadata count
- Format violation count

**Trend Analysis**:
- Documentation growth over time
- Quality metrics trends
- User engagement metrics
- Contribution patterns

---

## When to Use This Agent

### Direct User Invocation

Use `@docs-agent` when the user asks:

**Validation**:
- "Check documentation links"
- "Validate documentation"
- "Find broken links"
- "Check doc quality"

**Generation**:
- "Generate API docs"
- "Create documentation"
- "Generate changelog"
- "Create diagram"

**Maintenance**:
- "Update documentation"
- "Fix broken links"
- "Archive old docs"
- "Consolidate documentation"

**Reporting**:
- "Documentation health report"
- "Doc coverage report"
- "Show stale docs"
- "Documentation metrics"

### Called by Other Agents

**dev-agent** calls docs-agent when:
- Implementing new feature → "Update API documentation"
- Adding endpoint → "Generate API docs for new endpoint"
- Changing architecture → "Update architecture diagrams"

**backlog-agent** calls docs-agent when:
- Completing story → "Generate changelog entry"
- Finishing sprint → "Update documentation version"

**deploy-agent** calls docs-agent when:
- Deploying new version → "Update deployment documentation"
- Infrastructure changes → "Update infrastructure diagrams"

**git-agent** calls docs-agent when:
- Pre-commit → "Validate documentation changes"
- Creating PR → "Check documentation quality"

---

## Agent Capabilities

### Commands

#### Validation Commands

**Full Validation**:
```
@docs-agent validate all
```
Runs complete validation suite:
- Link checking (internal and external)
- Metadata verification
- Format linting
- Content validation

**Link Validation**:
```
@docs-agent validate links [--internal-only] [--fix]
```
- Checks all markdown links
- Reports broken links
- Optionally fixes internal links

**Metadata Validation**:
```
@docs-agent validate metadata [--fix]
```
- Checks required metadata fields
- Verifies version compatibility
- Optionally adds missing metadata

**Format Validation**:
```
@docs-agent validate format [--fix]
```
- Lints markdown formatting
- Checks heading hierarchy
- Validates code blocks
- Optionally applies fixes

**Content Validation**:
```
@docs-agent validate content
```
- Checks for TODO/FIXME markers
- Verifies code examples
- Validates cross-references

#### Generation Commands

**API Documentation**:
```
@docs-agent generate api-docs <service-name>
@docs-agent generate api-docs all
```
- Generates OpenAPI specs
- Creates API reference pages
- Adds authentication examples

**Diagrams**:
```
@docs-agent generate diagram <type>
Types: service-interaction, data-flow, security, deployment
```
- Creates Mermaid diagrams
- Exports PNG versions
- Updates documentation

**Templates**:
```
@docs-agent create <doc-type> <path>
Types: feature, api-reference, deployment, troubleshooting, adr
```
- Creates new doc from template
- Pre-fills metadata
- Adds to appropriate location

**Changelog**:
```
@docs-agent generate changelog <version>
```
- Generates changelog from commits
- Groups by category
- Highlights breaking changes

#### Maintenance Commands

**Update Timestamps**:
```
@docs-agent update timestamps [--path <pattern>]
```
- Updates `last-updated` metadata
- Optionally scoped to path pattern

**Fix Links**:
```
@docs-agent fix links [--dry-run]
```
- Fixes broken internal links
- Updates moved references
- Creates redirects

**Consolidate**:
```
@docs-agent consolidate <category>
```
- Identifies duplicate content
- Suggests merges
- Proposes archival

**Archive**:
```
@docs-agent archive <doc-path>
```
- Moves to archive directory
- Updates cross-references
- Adds deprecation notice

#### Reporting Commands

**Health Report**:
```
@docs-agent report health
```
Generates comprehensive health report:
- Link health
- Metadata completeness
- Freshness metrics
- Action items

**Coverage Report**:
```
@docs-agent report coverage
```
Reports documentation coverage:
- API endpoints
- Features
- Visual diagrams
- Code examples

**Metrics Report**:
```
@docs-agent report metrics [--period <days>]
```
- Documentation growth
- Update frequency
- Quality trends

**Stale Docs**:
```
@docs-agent report stale [--threshold <days>]
```
- Lists docs not updated recently
- Default threshold: 90 days

---

## Standard Operating Procedures

### SOP 1: Validation Workflow

**When**: Called for validation (by user, other agent, or CI/CD)

**Steps**:

1. **Identify Validation Type**
   ```
   - All validation → Run complete suite
   - Links only → Run link checker
   - Metadata only → Check metadata
   - Format only → Lint markdown
   ```

2. **Execute Validation**
   ```bash
   # Link checking
   find docs -name "*.md" -exec markdown-link-check {} \;
   
   # Metadata checking
   .github/scripts/check-doc-metadata.sh
   
   # Format linting
   markdownlint docs/**/*.md
   
   # Content validation
   grep -r "TODO\|FIXME\|XXX" docs --include="*.md"
   ```

3. **Generate Report**
   ```markdown
   # Documentation Validation Report
   
   **Timestamp**: ${DATE}
   
   ## Summary
   - Total Docs Checked: ${COUNT}
   - Issues Found: ${ISSUES}
   - Severity: ${CRITICAL} critical, ${MEDIUM} medium, ${LOW} low
   
   ## Issues
   
   ### Critical
   - Broken internal link: docs/deployment/missing.md
   - Missing metadata: docs/features/new-feature.md
   
   ### Medium
   - Stale doc (120 days): docs/archive/old-guide.md
   - TODO marker: docs/development/in-progress.md
   
   ## Recommendations
   1. Fix broken links immediately
   2. Add metadata to new docs
   3. Review stale documentation
   ```

4. **Suggest Fixes**
   - Provide specific fix commands
   - Offer to apply fixes automatically (if `--fix` flag)
   - Create tracking issue for manual fixes

5. **Apply Fixes (if requested)**
   ```bash
   # Fix broken links
   @docs-agent fix links
   
   # Add missing metadata
   @docs-agent validate metadata --fix
   ```

### SOP 2: Generation Workflow

**When**: Called to generate documentation

**Steps**:

1. **Identify Generation Type**
   ```
   - API docs → Extract from code annotations
   - Diagrams → Generate from config/code
   - Templates → Use predefined templates
   - Changelog → Parse git commits
   ```

2. **Gather Source Information**
   ```
   For API docs:
   - Read Spring controller annotations
   - Extract request/response models
   - Get authentication requirements
   
   For diagrams:
   - Parse service configuration
   - Extract dependencies
   - Identify data flows
   
   For templates:
   - Get document type requirements
   - Prepare metadata
   - Identify related docs
   ```

3. **Generate Content**
   ```java
   // API doc generation (example)
   @docs-agent generate api-docs rag-auth
   
   Output:
   - docs/api/rag-auth-service.md
   - docs/api/openapi/rag-auth-openapi.yaml
   - Updated docs/api/API_DOCUMENTATION_PORTAL.md
   ```

4. **Validate Generated Content**
   ```
   - Run link validation
   - Check metadata completeness
   - Verify code examples compile
   - Lint markdown format
   ```

5. **Save and Update Index**
   ```
   - Save to appropriate location
   - Update documentation index
   - Add cross-references
   - Commit changes (via git-agent)
   ```

### SOP 3: Maintenance Workflow

**When**: Scheduled maintenance or called for specific task

**Steps**:

1. **Identify Maintenance Task**
   ```
   - Update timestamps → Bulk metadata update
   - Fix links → Repair broken references
   - Consolidate → Merge duplicate content
   - Archive → Move deprecated docs
   ```

2. **Backup Affected Documents**
   ```bash
   # Via git-agent
   @git-agent create backup "pre-docs-maintenance-${DATE}"
   ```

3. **Apply Maintenance Changes**
   ```bash
   # Example: Update timestamps
   for file in docs/**/*.md; do
     sed -i "s/last-updated: .*/last-updated: $(date +%Y-%m-%d)/" "$file"
   done
   
   # Example: Fix broken link
   find docs -name "*.md" -exec sed -i 's|old-path|new-path|g' {} \;
   ```

4. **Validate Changes**
   ```
   - Run full validation
   - Check no new issues introduced
   - Verify fixes work
   ```

5. **Commit with Docs-Agent Signature**
   ```bash
   @git-agent commit "docs: [docs-agent] ${MAINTENANCE_TYPE} - ${DESCRIPTION}"
   ```

### SOP 4: Reporting Workflow

**When**: Scheduled reporting or on-demand

**Steps**:

1. **Gather Metrics**
   ```bash
   # Count documents
   TOTAL_DOCS=$(find docs -name "*.md" | wc -l)
   
   # Check link health
   BROKEN_LINKS=$(markdown-link-check docs/**/*.md | grep "✖" | wc -l)
   
   # Check metadata
   MISSING_META=$(grep -L "^---" docs/**/*.md | wc -l)
   
   # Check freshness
   STALE=$(find docs -name "*.md" -mtime +90 | wc -l)
   ```

2. **Analyze Trends**
   ```
   - Compare to previous reports
   - Identify improvements
   - Flag regressions
   ```

3. **Generate Report**
   ```markdown
   # Documentation Health Report
   **Generated**: 2025-11-12 14:30:00
   **System Version**: 0.8.0-SNAPSHOT
   
   ## Summary
   - Total Documents: 140
   - Active: 120
   - Draft: 10
   - Deprecated: 5
   - Archived: 5
   
   ## Link Health ✅
   - Internal Links: 450/452 valid (99.6%)
   - External Links: 85/92 valid (92.4%)
   - **Action**: Fix 2 broken internal links
   
   ## Metadata Completeness ⚠️
   - With Metadata: 95/140 (67.9%)
   - **Action**: Add metadata to 45 docs
   
   ## Freshness ⚠️
   - Current (<30 days): 45
   - Recent (30-90 days): 60
   - Stale (>90 days): 35
   - **Action**: Review 35 stale docs
   
   ## Coverage
   - API Documentation: 0% (0/30 endpoints)
   - Visual Diagrams: 15% (3/20 components)
   - Code Examples: 30%
   
   ## Priority Action Items
   1. 🔴 Generate API documentation (0% coverage)
   2. ⚠️ Add metadata to 45 documents
   3. ⚠️ Review 35 stale documents
   4. ⚠️ Fix 2 broken internal links
   ```

4. **Highlight Action Items**
   - Prioritize by severity
   - Assign to appropriate parties
   - Create tracking issues

5. **Schedule Follow-ups**
   - Set reminder for next report
   - Track action item completion
   - Measure improvement

---

## Documentation Standards

### Required Metadata Format

**Every document MUST include**:

```markdown
---
version: 1.0.0
last-updated: 2025-11-12
status: active|draft|deprecated|archived
applies-to: 0.8.0-SNAPSHOT
---
```

**Optional metadata**:
```markdown
---
version: 1.0.0
last-updated: 2025-11-12
status: active
applies-to: 0.8.0-SNAPSHOT
author: Team Name
reviewers: [Reviewer1, Reviewer2]
review-date: 2025-12-12
category: development|deployment|architecture|testing
---
```

**Validation Rules**:
- ✅ `version` must follow semver (X.Y.Z)
- ✅ `last-updated` must be ISO date (YYYY-MM-DD)
- ✅ `status` must be one of: active, draft, deprecated, archived
- ✅ `applies-to` must match a system version
- ✅ Active docs must be updated within 90 days

### Link Format Standards

**Internal Links** (relative paths):
```markdown
✅ CORRECT:
[Link Text](../category/document.md)
[Section Link](./file.md#section-name)
[Root Doc](../../README.md)

❌ INCORRECT:
[Bad Link](docs/file.md)              # Should be relative
[Absolute](/Users/user/docs/file.md)  # Should be relative
[No Extension](./file)                # Missing .md
```

**External Links** (full URLs):
```markdown
✅ CORRECT:
[External Site](https://example.com)
[GitHub](https://github.com/org/repo)

⚠️ WARNING (requires validation):
[External](http://oldsite.com)  # May break over time
```

**Anchor Links**:
```markdown
✅ CORRECT:
[Section](#section-name)
[Other Doc Section](./other.md#section-name)

# Section Name  → #section-name (lowercase, hyphens)
## Sub-Section  → #sub-section
```

### Code Example Standards

**Must include language identifier**:
```markdown
✅ CORRECT:
```java
public class Example {
    // Code here
}
```

```bash
make rebuild SERVICE=rag-auth
```

❌ INCORRECT:
```
public class Example {}  # Missing language
```
```

**Must include explanation**:
```markdown
✅ CORRECT:
This example demonstrates tenant-scoped querying:

```java
@Query("SELECT d FROM Document d WHERE d.tenantId = :tenantId")
List<Document> findByTenant(@Param("tenantId") UUID tenantId);
```

❌ INCORRECT:
```java
// Just code with no context
```
```

**Must be testable**:
- All Java code examples should compile
- All bash commands should be runnable
- All JSON should be valid
- All YAML should parse correctly

### Heading Hierarchy Standards

**Proper hierarchy**:
```markdown
✅ CORRECT:
# H1: Document Title (one per document)
## H2: Major Section
### H3: Subsection
#### H4: Detail
##### H5: Fine Detail

❌ INCORRECT:
# H1: Title
### H3: Section  # Skipped H2
##### H5: Detail # Skipped H3, H4
```

**Heading Style**:
```markdown
✅ CORRECT (ATX style):
## Section Name

❌ INCORRECT (Setext style):
Section Name
------------
```

### File Organization Standards

**Documentation Location**:
```
docs/
├── api/                    # API documentation
├── architecture/           # System architecture
├── deployment/             # Deployment guides
├── development/            # Development guides
├── getting-started/        # Onboarding
├── operations/             # Operations guides
├── project-management/     # Project tracking
├── security/               # Security docs
├── testing/                # Testing docs
├── diagrams/               # Visual diagrams
├── archive/                # Deprecated docs
└── README.md               # Documentation index
```

**Naming Conventions**:
```
✅ CORRECT:
DEVELOPER_ONBOARDING.md
GCP_DEPLOYMENT_GUIDE.md
ADR-001-BYPASS-API-GATEWAY.md

❌ INCORRECT:
developer-onboarding.md     # Should be SCREAMING_SNAKE_CASE
GCP Deployment.md           # No spaces
adr-bypass-gateway.md       # Missing ADR number
```

---

## Quality Gates

### Pre-Commit Quality Gates

**Enforced by git-agent calling docs-agent**:

```bash
# .github/scripts/pre-commit-docs.sh

# 1. Check metadata exists
@docs-agent validate metadata

# 2. Check for broken links
@docs-agent validate links --internal-only

# 3. Check for TODO markers
if grep -r "TODO\|FIXME" docs/**/*.md; then
  echo "❌ TODO markers found in documentation"
  exit 1
fi

# 4. Lint markdown
markdownlint docs/**/*.md

# 5. Verify code examples
@test-agent verify-code-examples
```

**Gates**:
- ✅ All new docs have metadata
- ✅ No broken internal links
- ✅ No TODO/FIXME in final docs
- ✅ Markdown linting passes
- ✅ Code examples validate

### Pull Request Quality Gates

**GitHub Action**: `.github/workflows/docs-pr-check.yml`

```yaml
name: Documentation PR Check
on: pull_request

jobs:
  docs-validation:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Validate Links
        run: @docs-agent validate links
      
      - name: Check Metadata
        run: @docs-agent validate metadata
      
      - name: Verify Examples
        run: @test-agent verify-code-examples
      
      - name: Generate Coverage Report
        run: @docs-agent report coverage
```

**Gates**:
- ✅ All links valid
- ✅ Metadata complete
- ✅ Code examples work
- ✅ Coverage not decreased

### Release Quality Gates

**Before Release**:

```bash
# Run by deploy-agent before release

# 1. Full validation
@docs-agent validate all

# 2. API docs complete
@docs-agent report coverage --require-api-100

# 3. Changelog generated
@docs-agent generate changelog ${VERSION}

# 4. Migration guide (if breaking changes)
if [breaking-changes]; then
  @docs-agent create migration-guide ${VERSION}
fi
```

**Gates**:
- ✅ 100% API documentation
- ✅ All breaking changes documented
- ✅ Migration guide created (if needed)
- ✅ Changelog complete
- ✅ Version compatibility documented

### Continuous Quality Monitoring

**Scheduled Weekly** (GitHub Actions cron):

```bash
# .github/workflows/docs-health-weekly.yml

- name: Generate Health Report
  run: @docs-agent report health
  
- name: Check Stale Docs
  run: @docs-agent report stale --threshold 90
  
- name: Validate External Links
  run: @docs-agent validate links --external-only
  
- name: Generate Metrics
  run: @docs-agent report metrics --period 7
```

**Monitoring**:
- ✅ Weekly health reports
- ✅ Stale doc identification
- ✅ External link validation
- ✅ Trend analysis

---

## Integration Patterns

### Called by Other Agents

#### Pattern 1: Feature Implementation (dev-agent)

**Scenario**: Developer implements new REST endpoint

```
User: "Add health check endpoint to auth service"

dev-agent:
  → Implements /actuator/health endpoint
  → Writes tests
  → Calls docs-agent:
    @docs-agent generate api-docs rag-auth
  → Calls git-agent:
    @git-agent commit "feat: add health check endpoint"
```

**Docs-Agent Actions**:
1. Extract @Operation annotations from controller
2. Generate OpenAPI spec
3. Update API documentation portal
4. Add code example
5. Validate generated docs

#### Pattern 2: Story Completion (backlog-agent)

**Scenario**: Completing story that adds feature

```
User: "Complete STORY-042"

backlog-agent:
  → Validates tests pass (test-agent)
  → Calls docs-agent:
    @docs-agent generate changelog entry STORY-042
  → Moves story to COMPLETED_STORIES.md
  → Calls git-agent:
    @git-agent commit "chore: complete STORY-042"
```

**Docs-Agent Actions**:
1. Read story description from BACKLOG.md
2. Generate changelog entry
3. Update CHANGELOG.md
4. Link to PR/commits

#### Pattern 3: Deployment (deploy-agent)

**Scenario**: Deploying new version to GCP

```
User: "Deploy to GCP production"

deploy-agent:
  → Pre-flight checks
  → Calls docs-agent:
    @docs-agent validate all
    @docs-agent report coverage --require-api-100
  → Executes deployment
  → Calls docs-agent:
    @docs-agent generate changelog ${VERSION}
    @docs-agent update deployment-status
  → Calls git-agent:
    @git-agent tag v${VERSION}
```

**Docs-Agent Actions**:
1. Validate all documentation
2. Check API coverage
3. Generate version changelog
4. Update deployment documentation
5. Tag documentation version

#### Pattern 4: Git Operations (git-agent)

**Scenario**: Creating PR with doc changes

```
User: "Create PR for feature X"

git-agent:
  → Stages changes
  → Calls docs-agent:
    @docs-agent validate links
    @docs-agent validate metadata
  → Creates commit
  → Pushes and creates PR
```

**Docs-Agent Actions**:
1. Validate changed documentation
2. Check for broken links
3. Verify metadata complete
4. Report issues (blocks commit if critical)

### Calls Other Agents

#### Docs-Agent → Test-Agent

**When**: Validating code examples

```
@docs-agent validate content
  → Extracts code examples from docs
  → Calls test-agent:
    @test-agent verify-code-examples
  → Reports validation results
```

#### Docs-Agent → Git-Agent

**When**: Committing documentation updates

```
@docs-agent fix links
  → Fixes broken links
  → Calls git-agent:
    @git-agent commit "docs: [docs-agent] fix broken links"
  → Returns summary
```

---

## Templates

### New Documentation Template

**Usage**: `@docs-agent create feature docs/development/NEW_FEATURE.md`

```markdown
---
version: 1.0.0
last-updated: ${TODAY}
status: draft
applies-to: ${SYSTEM_VERSION}
---

# ${DOCUMENT_TITLE}

**Purpose**: ${ONE_SENTENCE_PURPOSE}

**Audience**: ${TARGET_AUDIENCE}

## Table of Contents

1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [${MAIN_SECTION}](#${main-section})
4. [Examples](#examples)
5. [Troubleshooting](#troubleshooting)
6. [Related Documents](#related-documents)

## Overview

${OVERVIEW_DESCRIPTION}

**Key Concepts**:
- **${CONCEPT_1}**: ${DESCRIPTION}
- **${CONCEPT_2}**: ${DESCRIPTION}

## Prerequisites

Before proceeding, ensure you have:

- [ ] ${PREREQUISITE_1}
- [ ] ${PREREQUISITE_2}
- [ ] ${PREREQUISITE_3}

## ${Main Section}

${MAIN_CONTENT}

### Step 1: ${STEP_NAME}

${STEP_DESCRIPTION}

```${LANGUAGE}
${CODE_EXAMPLE}
```

**Expected Output**:
```
${OUTPUT}
```

## Examples

### Example 1: ${USE_CASE}

${EXAMPLE_DESCRIPTION}

```${LANGUAGE}
${EXAMPLE_CODE}
```

## Troubleshooting

### Issue: ${COMMON_ISSUE}

**Symptoms**:
- ${SYMPTOM_1}
- ${SYMPTOM_2}

**Cause**: ${ROOT_CAUSE}

**Solution**:
```bash
${SOLUTION_COMMANDS}
```

## Related Documents

- [${RELATED_DOC_1}](${PATH})
- [${RELATED_DOC_2}](${PATH})
```

### API Documentation Template

**Usage**: `@docs-agent create api-reference docs/api/SERVICE_NAME.md`

```markdown
---
version: 1.0.0
last-updated: ${TODAY}
status: active
applies-to: ${SERVICE_NAME} ${VERSION}
---

# ${SERVICE_NAME} API Reference

**Base URL**: `http://localhost:${PORT}`  
**Version**: ${VERSION}  
**Authentication**: JWT Bearer Token

## Table of Contents

1. [Authentication](#authentication)
2. [Common Headers](#common-headers)
3. [Endpoints](#endpoints)
4. [Error Codes](#error-codes)
5. [Rate Limiting](#rate-limiting)

## Authentication

All endpoints require JWT authentication:

```bash
# Get JWT token
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"user","password":"pass"}'

# Response
{
  "token": "eyJhbGc...",
  "expiresIn": 3600
}

# Use token in requests
curl -X GET http://localhost:${PORT}/api/endpoint \
  -H "Authorization: Bearer eyJhbGc..."
```

## Common Headers

| Header | Required | Description |
|--------|----------|-------------|
| `Authorization` | Yes | JWT Bearer token |
| `Content-Type` | Yes (POST/PUT) | `application/json` |
| `X-Tenant-ID` | Yes | Tenant identifier |

## Endpoints

### ${HTTP_METHOD} ${ENDPOINT_PATH}

**Description**: ${ENDPOINT_DESCRIPTION}

**Authentication**: Required

**Parameters**:

| Name | Type | Location | Required | Description |
|------|------|----------|----------|-------------|
| `${PARAM}` | ${TYPE} | ${LOCATION} | ${REQUIRED} | ${DESCRIPTION} |

**Request Example**:
```bash
curl -X ${METHOD} http://localhost:${PORT}${PATH} \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '${REQUEST_BODY}'
```

**Response (200 OK)**:
```json
${RESPONSE_BODY}
```

**Error Responses**:
- `400 Bad Request`: Invalid input
- `401 Unauthorized`: Missing or invalid token
- `403 Forbidden`: Insufficient permissions
- `404 Not Found`: Resource not found
- `500 Internal Server Error`: Server error

## Error Codes

| Code | Message | Description |
|------|---------|-------------|
| `AUTH_001` | Invalid credentials | Username or password incorrect |
| `AUTH_002` | Token expired | JWT token has expired |
| `VAL_001` | Validation failed | Input validation error |

## Rate Limiting

- **Rate**: 100 requests per minute per tenant
- **Headers**: `X-RateLimit-Limit`, `X-RateLimit-Remaining`
- **Response**: `429 Too Many Requests` when exceeded
```

### Architecture Decision Record (ADR) Template

**Usage**: `@docs-agent create adr docs/development/ADR-${NUMBER}-${TITLE}.md`

```markdown
---
version: 1.0.0
last-updated: ${TODAY}
status: accepted|proposed|deprecated
applies-to: ${VERSION}
---

# ADR-${NUMBER}: ${DECISION_TITLE}

**Status**: Accepted  
**Date**: ${DATE}  
**Decision Makers**: ${TEAM}  
**Context**: ${CONTEXT_TAG}

## Context

${PROBLEM_DESCRIPTION}

**Current Situation**:
- ${CURRENT_STATE_1}
- ${CURRENT_STATE_2}

**Constraints**:
- ${CONSTRAINT_1}
- ${CONSTRAINT_2}

## Decision

We will ${DECISION}.

## Rationale

**Pros**:
- ✅ ${ADVANTAGE_1}
- ✅ ${ADVANTAGE_2}

**Cons**:
- ❌ ${DISADVANTAGE_1}
- ❌ ${DISADVANTAGE_2}

**Alternatives Considered**:

### Alternative 1: ${NAME}
- Description: ${DESCRIPTION}
- Rejected because: ${REASON}

### Alternative 2: ${NAME}
- Description: ${DESCRIPTION}
- Rejected because: ${REASON}

## Consequences

**Positive**:
- ${POSITIVE_CONSEQUENCE_1}
- ${POSITIVE_CONSEQUENCE_2}

**Negative**:
- ${NEGATIVE_CONSEQUENCE_1}
- ${NEGATIVE_CONSEQUENCE_2}

**Neutral**:
- ${NEUTRAL_CONSEQUENCE_1}

## Implementation

**Changes Required**:
- [ ] ${CHANGE_1}
- [ ] ${CHANGE_2}

**Timeline**: ${TIMELINE}

## References

- [Related Doc 1](${PATH})
- [Related Doc 2](${PATH})
```

---

## Troubleshooting

### Common Issues

#### Issue 1: Broken Links After File Move

**Symptoms**:
- Link validation fails
- 404 errors in documentation
- Cross-references broken

**Cause**: File moved without updating references

**Solution**:
```bash
# Find all references to moved file
@docs-agent find-references OLD_PATH

# Fix broken links
@docs-agent fix links

# Verify
@docs-agent validate links
```

#### Issue 2: Metadata Validation Fails

**Symptoms**:
- Pre-commit hook fails
- PR checks fail on metadata
- Missing version info

**Cause**: Missing or invalid metadata

**Solution**:
```bash
# Check what's missing
@docs-agent validate metadata

# Add metadata to all docs missing it
@docs-agent validate metadata --fix

# Verify
@docs-agent validate metadata
```

#### Issue 3: Stale Documentation

**Symptoms**:
- Documentation >90 days old
- Information outdated
- Users reporting incorrect procedures

**Cause**: Documentation not updated with code changes

**Solution**:
```bash
# Find stale docs
@docs-agent report stale

# Review and update or archive
@docs-agent archive docs/old-guide.md

# Update active docs
# (manual review required)
```

#### Issue 4: Code Examples Don't Compile

**Symptoms**:
- Code example validation fails
- Examples produce errors
- Users can't reproduce examples

**Cause**: Code changed but examples not updated

**Solution**:
```bash
# Find broken examples
@test-agent verify-code-examples

# Update examples manually
# (docs-agent can't auto-fix this)

# Verify
@test-agent verify-code-examples
```

#### Issue 5: API Docs Out of Sync

**Symptoms**:
- API docs don't match actual endpoints
- Missing new endpoints
- Deprecated endpoints still documented

**Cause**: Manual API docs not regenerated

**Solution**:
```bash
# Regenerate from code
@docs-agent generate api-docs all

# Verify coverage
@docs-agent report coverage

# Check for deprecated endpoints
# (manual review required)
```

### Debugging Documentation Issues

**Enable Verbose Mode**:
```bash
@docs-agent --verbose validate all
```

**Check Specific File**:
```bash
@docs-agent validate links --file docs/path/to/file.md
```

**Test Link Fix**:
```bash
# Dry run first
@docs-agent fix links --dry-run

# Then apply
@docs-agent fix links
```

**Validate Single Service API**:
```bash
@docs-agent generate api-docs rag-auth --validate-only
```

---

## Best Practices

### Documentation Workflow

1. **Before Writing**:
   - Check if documentation already exists
   - Review related documents
   - Use appropriate template

2. **While Writing**:
   - Add metadata immediately
   - Include code examples
   - Link to related docs
   - Add troubleshooting section

3. **Before Committing**:
   - Run `@docs-agent validate all`
   - Fix all broken links
   - Verify code examples
   - Update related docs

4. **After Merging**:
   - Generate health report
   - Update documentation index
   - Notify relevant teams

### Agent Usage Tips

**Batch Operations**:
```bash
# Good: Update all docs at once
@docs-agent update timestamps --all

# Bad: Update one at a time
# (inefficient)
```

**Incremental Validation**:
```bash
# Good: Validate only changed files in PR
@docs-agent validate links --changed-only

# Expensive: Full validation every time
# (save for scheduled runs)
```

**Automated Fixes**:
```bash
# Always dry-run first
@docs-agent fix links --dry-run

# Review changes
# Then apply
@docs-agent fix links
```

---

## Agent Metadata Summary

**Version**: 1.0.0  
**Domain**: Documentation Management  
**Line Count**: ~1,800 lines  
**Last Updated**: 2025-11-12  

**Capabilities**:
- ✅ Comprehensive validation (links, metadata, format, content)
- ✅ Documentation generation (API docs, diagrams, changelogs)
- ✅ Automated maintenance (timestamps, links, consolidation)
- ✅ Quality reporting (health, coverage, metrics)

**Integration**:
- ✅ Called by: dev-agent, backlog-agent, deploy-agent, git-agent
- ✅ Calls: test-agent, git-agent

**Quality Gates**:
- ✅ Pre-commit validation
- ✅ PR quality checks
- ✅ Release requirements
- ✅ Continuous monitoring

---

**END OF DOCUMENTATION AGENT**
