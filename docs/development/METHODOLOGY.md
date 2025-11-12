---
version: 1.0.0
last-updated: 2025-11-12
status: active
applies-to: 0.8.0-SNAPSHOT
category: development
---

# Development Methodology

## Story Point Anchoring Method

This project follows industry-standard story point anchoring to ensure consistent task sizing and realistic sprint planning.

### Task Sizing Philosophy

Following industry-standard story point anchoring:

- **Pebbles (1-3 points)**: Small, focused tasks (1-2 days)
- **Rocks (5-8 points)**: Medium anchor stories (3-5 days) 
- **Boulders (13+ points)**: Large epics requiring breakdown

### Story Point Distribution Guidelines

- **Pebbles (1-3 points)**: Small, focused work (1-2 days each)  
- **Rocks (5-8 points)**: Medium anchor stories (3-5 days each)
- **Boulders (13+ points)**: Large epics requiring careful breakdown

### Benefits of Anchoring Methodology

This anchoring approach transforms unwieldy "boulder" epics into manageable "pebble" tasks that follow industry best practices for agile story sizing and team productivity.

### TODO-Based Story Integration

The methodology supports automatic story generation from TODO comments in the codebase, ensuring development tasks are captured and sized appropriately in the project backlog.

### Implementation Standards

- All tasks should be broken down to avoid boulders where possible
- Epic-level work (13+ points) requires detailed breakdown into smaller stories
- Regular backlog grooming to maintain appropriate sizing distribution
- Story points reflect complexity, not time estimation

## Completed Stories Management Process

### File Structure Organization

The project maintains a clear separation between active and completed work through dedicated documentation files:

- **`docs/project-management/PROJECT_BACKLOG.md`** - Contains ONLY active/pending stories
- **`docs/project-management/COMPLETED_STORIES.md`** - Archives all completed stories with completion metadata
- **`docs/development/METHODOLOGY.md`** - Contains story point methodology and sizing philosophy
- **Main `README.md`** - Contains current project status and testing coverage information

### Completed Stories Workflow

⚠️ **CRITICAL**: Before making any backlog changes, follow the comprehensive safety procedures in [`BACKLOG_MANAGEMENT_PROCESS.md`](../project-management/BACKLOG_MANAGEMENT_PROCESS.md) to prevent story loss.

🤖 **FOR AI ASSISTANTS**: When any story is marked complete, ALWAYS reference this checklist and ask for confirmation before proceeding. Never add completed stories to PROJECT_BACKLOG.md under any circumstances.

🛠️ **TEST VERIFICATION TOOL**: Use `scripts/tests/story-completion-test-check.sh <service-name>` to verify all tests pass before marking stories complete. This script is MANDATORY for any story affecting code.

When a story is marked as completed, follow this standardized process:

#### 1. Story Completion Verification
- Verify all acceptance criteria have been met
- Confirm definition of done is satisfied
- Validate any testing requirements are complete
- Ensure documentation updates are included

#### 2. Story Migration Process
- **Move** the completed story from `PROJECT_BACKLOG.md` to `COMPLETED_STORIES.md`
- **Update** the story with completion date (`**Completed:** YYYY-MM-DD`)
- **Mark** all acceptance criteria as completed with checkmarks (✅)
- **Add** business impact summary to the completed story
- **Remove** the story entirely from the backlog file

#### 3. Documentation Updates
- **Update** the summary section in `COMPLETED_STORIES.md` with new totals:
  - Total completed story points
  - Completion date range
  - Story count updates
- **Update** the main `README.md` with any status changes
- **Keep** `PROJECT_BACKLOG.md` focused only on active work

#### 4. Content Organization Rules
- **PROJECT_BACKLOG.md** should contain ONLY active stories (no methodology, no project status)
- **Project status information** belongs in `README.md`
- **Methodology and best practices** belong in `docs/development/METHODOLOGY.md`
- **Testing coverage details** belong in `README.md` under development status
- **Completed story archive** belongs exclusively in `COMPLETED_STORIES.md`

### ⚠️ CRITICAL: Story Completion Checklist

**BEFORE marking any story as completed, verify:**

🔴 **MANDATORY TEST VERIFICATION (MUST BE FIRST):**
- [ ] **ALL TESTS PASSING**: Run `mvn test` for affected modules and verify 0 failures
- [ ] **COMPILATION SUCCESS**: Run `mvn compile` and verify no errors
- [ ] **TEST RESULTS DOCUMENTED**: Record actual test counts (X/Y tests passing)
- [ ] **NO FAILING TESTS EXCEPTION**: If ANY test fails, story CANNOT be marked complete

✅ **File Structure Rules:**
- [ ] PROJECT_BACKLOG.md contains ONLY active/pending stories
- [ ] COMPLETED_STORIES.md contains ALL completed stories
- [ ] NO "Recently Completed" sections in PROJECT_BACKLOG.md
- [ ] NO completed stories mixed with active stories

✅ **Story Migration Steps:**
- [ ] Story removed entirely from PROJECT_BACKLOG.md
- [ ] Story added to COMPLETED_STORIES.md with completion date
- [ ] All acceptance criteria marked with ✅
- [ ] Business impact summary added
- [ ] COMPLETED_STORIES.md summary updated with new totals

✅ **Documentation Updates:**
- [ ] Total story points recalculated correctly
- [ ] Completion date range updated
- [ ] CLAUDE.md updated with achievement details

### Story Completion Template

When moving a story to completed status, use this format:

```markdown
### **STORY-ID: Story Title** ⭐ **PRIORITY-LEVEL** 
**Epic:** Epic Name
**Story Points:** X  
**Priority:** Priority Level  
**Dependencies:** None/List dependencies  
**Completed:** YYYY-MM-DD

**Context:**
Original story context...

**Location:** Code locations if applicable

**Acceptance Criteria:**
- ✅ First acceptance criterion (completed)
- ✅ Second acceptance criterion (completed)
- ✅ Third acceptance criterion (completed)

**Definition of Done:**
- ✅ First done criterion (completed)
- ✅ Second done criterion (completed)
- ✅ Third done criterion (completed)

**Business Impact:** Summary of the business value delivered by completing this story.
```

### Project Manager Agent Guidelines

The project-manager agent should proactively:

1. **Monitor** active stories in PROJECT_BACKLOG.md for completion status
2. **Validate** completion criteria before moving stories
3. **Maintain** accurate story point totals and completion statistics
4. **Ensure** clean separation between active and completed work
5. **Update** all relevant documentation files during the completion process
6. **Preserve** story history and business impact information

### Quality Standards

- All completed stories must include business impact summaries
- Completion dates must be accurate and formatted consistently (YYYY-MM-DD)
- Story point totals must be recalculated accurately
- Active backlog must remain focused and actionable
- No incomplete or in-progress stories should exist in COMPLETED_STORIES.md

### Benefits of This Process

- **Clear Separation**: Active vs completed work is immediately apparent
- **Progress Tracking**: Easy calculation of velocity and completion metrics
- **Business Value**: Completed stories document value delivered
- **Backlog Focus**: Active backlog remains actionable and current
- **Historical Record**: Complete project history preserved in completed stories

## Documentation Synchronization Management System

### Overview

The project-manager agent must proactively maintain ALL documentation files in sync with project progress. This system ensures consistency, accuracy, and current status across all documentation.

### Core Documentation Files

#### **Project Documentation:**
- `README.md` - Main project overview, status, quick start
- `docs/README.md` - Documentation index
- `docs/development/CLAUDE.md` - Technical context
- `docs/development/METHODOLOGY.md` - Development processes
- `docs/development/TESTING_BEST_PRACTICES.md` - Testing standards

#### **Project Management Documentation:**
- `docs/project-management/PROJECT_BACKLOG.md` - Active stories
- `docs/project-management/COMPLETED_STORIES.md` - Completed stories
- `docs/project-management/DOCKER-001-SUMMARY.md` - Milestones

#### **Deployment Documentation:**
- `docs/deployment/DEPLOYMENT.md` - Deployment guide
- `docs/deployment/DOCKER.md` - Docker setup

### Automatic Synchronization Procedures

#### 1. Story Completion Synchronization

**Trigger:** When any story is marked as completed

**Required Updates:**
1. **Move story** from PROJECT_BACKLOG.md to COMPLETED_STORIES.md
2. **Update COMPLETED_STORIES.md summary** with new totals and date ranges
3. **Update README.md Development Status** section:
   - Update completed story point totals
   - Update completion percentages
   - Update "Recent Major Achievements" section
   - Update service implementation status if applicable
4. **Update docs/README.md** if system status changes significantly
5. **Update CLAUDE.md** with latest achievements and status
6. **Validate all cross-references** remain accurate

**Checklist:**
- [ ] Story moved with completion date and ✅ checkmarks
- [ ] COMPLETED_STORIES.md summary totals updated
- [ ] README.md development status reflects new completion
- [ ] All story point calculations accurate across files
- [ ] Cross-references to completed work updated

#### 2. Service Status Synchronization

**Trigger:** When service implementation status changes

**Required Updates:**
1. **README.md** - Update service status table and architecture diagram
2. **docs/README.md** - Update system status summary
3. **CLAUDE.md** - Update service implementation details
4. **Docker milestone files** - Update if Docker-related services change

**Checklist:**
- [ ] Service status consistent across all files
- [ ] Architecture diagrams reflect current status
- [ ] Health check URLs updated if changed
- [ ] Docker deployment status accurate

#### 3. Testing Coverage Synchronization  

**Trigger:** When testing coverage metrics change

**Required Updates:**
1. **README.md** - Update testing coverage percentages and badges
2. **CLAUDE.md** - Update testing achievement sections
3. **PROJECT_BACKLOG.md** - Update testing-related story priorities if needed

**Checklist:**
- [ ] Coverage percentages accurate across files
- [ ] Test success rates consistent
- [ ] Testing infrastructure status current
- [ ] Badge information reflects latest metrics

#### 4. Milestone Completion Synchronization

**Trigger:** When major milestones are completed

**Required Updates:**
1. **Create/update milestone summary** (e.g., DOCKER-001-SUMMARY.md)
2. **Update README.md** with milestone achievements
3. **Update docs/README.md** system status
4. **Update CLAUDE.md** achievements section
5. **Archive completed milestone stories** with business impact

**Checklist:**
- [ ] Milestone summary document created/updated
- [ ] Achievement reflected in main README
- [ ] System status updated across documentation
- [ ] Related stories properly archived

### Cross-Reference Integrity Maintenance

#### File Reference Validation

**Regular Checks Required:**
1. **Internal Links** - Verify all `[text](file.md)` links work
2. **Section References** - Check `[text](file.md#section)` anchors exist
3. **File Paths** - Validate all file path references are current
4. **Navigation Links** - Ensure documentation index links are accurate

#### Terminology Consistency

**Standardized Terms:**
- "BYO RAG System" (not "enterprise RAG" or "RAG system")
- "story points" (not "points" or "SP")
- "microservices" (not "services" when referring to architecture)
- "Docker deployment" (not "containerization" in user docs)

#### Status Indicators

**Consistent Status Symbols:**
- ✅ "Complete" / "Healthy" / "Working" / "Operational"  
- 🔄 "In Progress" / "Running" / "Partial"
- ❌ "Failed" / "Not Working" / "Issues"
- 🔧 "Needs Configuration" / "Setup Required"

### Proactive Maintenance Procedures

#### Weekly Documentation Audit

**Automated Checklist:**
1. **Story Point Totals** - Verify totals match across all files
2. **Service Status** - Check health endpoints and update status
3. **Cross-References** - Validate all internal documentation links
4. **Date Consistency** - Ensure completion dates are accurate
5. **Coverage Metrics** - Update testing and documentation coverage

#### Update Scenarios & Procedures

#### Scenario 1: Story Completion

```markdown
**When:** Story moves from active to completed
**Files to Update:** 
- PROJECT_BACKLOG.md (remove story)
- COMPLETED_STORIES.md (add story with metadata)  
- README.md (update development status)
- CLAUDE.md (update achievements)

**Procedure:**
1. Verify story completion criteria
2. Move story with completion template
3. Update summary totals
4. Update main project status
5. Validate cross-references
```

#### Scenario 2: Service Health Change

```markdown
**When:** Service status changes (healthy ↔ issues)
**Files to Update:**
- README.md (service status table)
- docs/README.md (system status)
- CLAUDE.md (current status)
- Relevant milestone files

**Procedure:**
1. Test service health endpoint
2. Update status indicators consistently
3. Update architecture diagrams if needed
4. Note any service dependencies affected
```

#### Scenario 3: Testing Coverage Update

```markdown
**When:** Test coverage metrics change significantly
**Files to Update:**
- README.md (coverage badges and metrics)
- CLAUDE.md (testing achievements)
- Testing milestone documentation

**Procedure:**
1. Calculate new coverage percentages
2. Update badges and status indicators
3. Update testing infrastructure status
4. Note coverage improvement/regression trends
```

#### Scenario 4: New Milestone Achievement

```markdown
**When:** Major milestone completed (e.g., DOCKER-001)
**Files to Update:**
- Create milestone summary document
- README.md (major achievements)
- docs/README.md (system status)
- CLAUDE.md (achievement history)

**Procedure:**
1. Create detailed milestone summary
2. Update all project status indicators
3. Archive related completed stories
4. Update system architecture if changed
5. Validate deployment documentation current
```

### Quality Assurance Standards

#### Documentation Consistency Rules

1. **Single Source of Truth** - Each piece of information has one primary location
2. **Consistent Formatting** - Use standardized templates and formatting
3. **Accurate Timestamps** - All completion dates must be YYYY-MM-DD format
4. **Complete Cross-References** - All links tested and functional
5. **Current Status** - All status indicators reflect actual system state

#### Error Prevention

1. **Automated Validation** - Use checklists for each update scenario  
2. **Double-Check Totals** - Verify all story point calculations
3. **Status Verification** - Test health endpoints before updating status
4. **Link Validation** - Check all documentation links before publishing
5. **Consistent Terminology** - Use standardized project terminology

### Implementation Standards for Project Manager Agent

#### Mandatory Behaviors

1. **Proactive Updates** - Monitor for completion triggers automatically
2. **Complete Updates** - Never partially update related documentation
3. **Validation Required** - Always verify information accuracy before updates
4. **Consistency First** - Maintain consistent formatting and terminology
5. **Cross-Reference Maintenance** - Keep all documentation links current

#### Update Verification

Before completing any documentation synchronization:
- [ ] All affected files identified and updated
- [ ] Story point totals accurate across all files  
- [ ] Status indicators consistent project-wide
- [ ] Cross-references tested and functional
- [ ] Completion dates and metadata accurate
- [ ] Business impact summaries included for completed stories

### Documentation Synchronization Checklist

#### For Each Story Completion:
- [ ] Story moved from backlog to completed with proper template
- [ ] COMPLETED_STORIES.md summary totals updated
- [ ] README.md development status section updated
- [ ] CLAUDE.md achievements section updated
- [ ] All story point calculations verified
- [ ] Cross-references validated

#### For Service Status Changes:
- [ ] Service status table updated in README.md
- [ ] Architecture diagrams reflect current status  
- [ ] docs/README.md system status updated
- [ ] Health check URLs verified current
- [ ] Related milestone documents updated if needed

#### For Major Milestones:
- [ ] Milestone summary document created/updated
- [ ] All related stories archived with business impact
- [ ] Main README.md achievements section updated
- [ ] System status updated across all documentation
- [ ] Deployment documentation verified current

This comprehensive documentation synchronization system ensures the project-manager agent maintains accurate, current, and consistent documentation across all project files without being explicitly asked to do so.