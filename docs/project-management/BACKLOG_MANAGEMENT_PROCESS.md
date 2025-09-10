# Backlog Management Safeguard Process

## Overview

This document establishes comprehensive safeguards and procedures for managing the project backlog to prevent story loss, file corruption, and data inconsistencies. This process was created in response to incidents where the PROJECT_BACKLOG.md file was overwritten, resulting in loss of critical project stories and story point accounting.

## Critical Principles

1. **NEVER overwrite entire files** - Always make incremental changes
2. **ALWAYS backup before changes** - Create safety nets before modifications
3. **VERIFY before moving** - Confirm story completion with evidence
4. **AUDIT everything** - Maintain clear trail of all changes
5. **VALIDATE totals** - Ensure story point accounting remains accurate

---

## 1. Story Transition Workflow

### 1.1 Pre-Transition Verification

Before moving any story from active to completed status:

1. **Read Current Backlog State**
   ```bash
   # Always read the current backlog first
   cat docs/project-management/PROJECT_BACKLOG.md
   ```

2. **Create Backup**
   ```bash
   # Create timestamped backup
   cp docs/project-management/PROJECT_BACKLOG.md \
      docs/project-management/backups/PROJECT_BACKLOG_$(date +%Y%m%d_%H%M%S).md
   ```

3. **Verify Story Completion Evidence**
   - Check that all acceptance criteria are met
   - Verify all definition of done items are satisfied
   - Confirm implementation exists in codebase
   - Validate tests are passing
   - Ensure documentation is updated

### 1.2 Safe Story Movement Process

```bash
# Step 1: Create working directory for backlog changes
mkdir -p docs/project-management/backups
mkdir -p docs/project-management/working

# Step 2: Create backup with timestamp
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
cp docs/project-management/PROJECT_BACKLOG.md \
   docs/project-management/backups/PROJECT_BACKLOG_${TIMESTAMP}.md

# Step 3: Create working copy
cp docs/project-management/PROJECT_BACKLOG.md \
   docs/project-management/working/PROJECT_BACKLOG_working.md

# Step 4: Make incremental changes to working copy
# (Edit the working copy, never the original directly)

# Step 5: Validate changes before applying
diff docs/project-management/PROJECT_BACKLOG.md \
     docs/project-management/working/PROJECT_BACKLOG_working.md

# Step 6: Apply changes only after validation
cp docs/project-management/working/PROJECT_BACKLOG_working.md \
   docs/project-management/PROJECT_BACKLOG.md

# Step 7: Commit changes immediately
git add docs/project-management/PROJECT_BACKLOG.md
git commit -m "Backlog update: Move story [STORY-ID] to completed - [timestamp]"
```

---

## 2. Pre-Change Verification Checklist

### 2.1 Mandatory Pre-Flight Checks

- [ ] **Backup Created**: Timestamped backup exists in backups/ directory
- [ ] **Current State Read**: Full content of current backlog reviewed
- [ ] **Story Completion Evidence**: All acceptance criteria verified
- [ ] **Implementation Confirmed**: Code changes exist and are functional
- [ ] **Tests Passing**: All related tests are green
- [ ] **Documentation Updated**: Any required docs have been updated
- [ ] **Dependencies Resolved**: No blocking dependencies remain
- [ ] **Story Points Calculated**: Points for moving story are noted

### 2.2 Story Completion Evidence Requirements

For each story being moved to completed:

1. **Code Evidence**
   ```bash
   # Verify implementation exists
   find . -name "*.java" -o -name "*.js" -o -name "*.py" -o -name "*.md" | \
   xargs grep -l "STORY-ID"
   ```

2. **Test Evidence**
   ```bash
   # Verify tests exist and pass
   ./gradlew test --tests "*StoryID*"
   # or
   npm test -- --grep "STORY-ID"
   ```

3. **Documentation Evidence**
   ```bash
   # Check documentation updates
   git log --oneline --since="1 week ago" -- docs/
   ```

### 2.3 Definition of Done Verification

- [ ] All acceptance criteria implemented
- [ ] Unit tests written and passing
- [ ] Integration tests passing
- [ ] Code reviewed and approved
- [ ] Documentation updated
- [ ] No breaking changes introduced
- [ ] Performance impact assessed
- [ ] Security considerations addressed

---

## 3. Safe File Management

### 3.1 File Modification Rules

1. **NEVER use direct overwrite operations**
   ```bash
   # FORBIDDEN:
   echo "new content" > PROJECT_BACKLOG.md
   
   # REQUIRED:
   # 1. Read current content
   # 2. Create backup
   # 3. Make incremental changes
   # 4. Validate changes
   # 5. Apply carefully
   ```

2. **Always use incremental changes**
   ```bash
   # Good: Modify specific sections
   sed '/^### \*\*STORY-001/,/^---/c\
   [updated story content]' PROJECT_BACKLOG.md
   
   # Better: Use dedicated editing tools that preserve structure
   ```

### 3.2 Git Workflow for Backlog Changes

```bash
# 1. Create feature branch for backlog changes
git checkout -b backlog-update-$(date +%Y%m%d)

# 2. Make incremental commits
git add docs/project-management/PROJECT_BACKLOG.md
git commit -m "Backlog: Update story STORY-001 status"

# 3. Validate changes
git diff HEAD~1 docs/project-management/PROJECT_BACKLOG.md

# 4. Merge back to main
git checkout main
git merge backlog-update-$(date +%Y%m%d)

# 5. Tag for easy recovery
git tag backlog-checkpoint-$(date +%Y%m%d)
```

### 3.3 Audit Trail Requirements

Every backlog change must include:

1. **Change Description**: What was modified
2. **Reason**: Why the change was made
3. **Evidence**: Links to supporting evidence
4. **Story Points**: Points moved and impact on totals
5. **Timestamp**: When the change was made
6. **Author**: Who made the change

---

## 4. Story Point Accounting

### 4.1 Point Tracking Process

1. **Before Changes**: Calculate current totals
   ```bash
   # Extract current story points
   grep -o "Story Points:** [0-9]*" PROJECT_BACKLOG.md | \
   awk -F: '{sum += $2} END {print "Total Active Points:", sum}'
   ```

2. **During Changes**: Track points being moved
   ```bash
   # Record points being transferred
   echo "Moving story STORY-001 (8 points) from active to completed" >> \
   docs/project-management/backlog-audit.log
   ```

3. **After Changes**: Verify totals
   ```bash
   # Verify new totals
   NEW_TOTAL=$(grep -o "Story Points:** [0-9]*" PROJECT_BACKLOG.md | \
              awk -F: '{sum += $2} END {print sum}')
   echo "New active total: $NEW_TOTAL points"
   ```

### 4.2 Point Reconciliation

```bash
# Create reconciliation script
cat > scripts/reconcile-points.sh << 'EOF'
#!/bin/bash
ACTIVE_POINTS=$(grep -o "Story Points:** [0-9]*" docs/project-management/PROJECT_BACKLOG.md | \
               awk -F: '{sum += $2} END {print sum}')

COMPLETED_POINTS=$(grep -o "Story Points:** [0-9]*" docs/project-management/COMPLETED_STORIES.md | \
                  awk -F: '{sum += $2} END {print sum}')

echo "Active Stories: $ACTIVE_POINTS points"
echo "Completed Stories: $COMPLETED_POINTS points"
echo "Total Project Points: $((ACTIVE_POINTS + COMPLETED_POINTS))"
EOF

chmod +x scripts/reconcile-points.sh
```

---

## 5. Quality Gates

### 5.1 Completion Evidence Requirements

Before marking any story as complete:

1. **Implementation Evidence**
   - Code files exist and contain required functionality
   - All acceptance criteria have corresponding implementation
   - No TODO comments remain in critical sections

2. **Testing Evidence**
   ```bash
   # Verify test coverage
   ./gradlew jacocoTestReport
   # Check that story-related code has adequate coverage
   ```

3. **Documentation Evidence**
   - README files updated if functionality affects user interface
   - API documentation updated for new endpoints
   - Architecture docs updated for significant changes

### 5.2 Business Impact Assessment

For each completed story, document:

- **Functional Impact**: What new capability was added
- **Performance Impact**: Any performance changes measured
- **User Impact**: How this affects end users
- **Technical Debt**: Any debt added or resolved
- **Dependencies**: What this unblocks or enables

### 5.3 Dependency Verification

```bash
# Check for unresolved dependencies
grep -r "Dependencies:" docs/project-management/PROJECT_BACKLOG.md | \
grep -v "None"

# Verify dependency chain
scripts/check-story-dependencies.sh STORY-001
```

---

## 6. Recovery Procedures

### 6.1 Emergency Backlog Recovery

If the backlog file is corrupted or overwritten:

1. **Immediate Recovery**
   ```bash
   # Check git history
   git log --oneline docs/project-management/PROJECT_BACKLOG.md
   
   # Restore from most recent commit
   git checkout HEAD~1 -- docs/project-management/PROJECT_BACKLOG.md
   
   # Or restore from backup
   cp docs/project-management/backups/PROJECT_BACKLOG_LATEST.md \
      docs/project-management/PROJECT_BACKLOG.md
   ```

2. **Recovery Validation**
   ```bash
   # Verify story count
   grep -c "^### \*\*" docs/project-management/PROJECT_BACKLOG.md
   
   # Verify story points
   scripts/reconcile-points.sh
   
   # Check for missing stories
   diff docs/project-management/backups/PROJECT_BACKLOG_LATEST.md \
        docs/project-management/PROJECT_BACKLOG.md
   ```

### 6.2 Story Point Reconciliation Process

```bash
# Create reconciliation report
cat > scripts/backlog-health-check.sh << 'EOF'
#!/bin/bash
echo "=== Backlog Health Check ==="
echo "Date: $(date)"
echo

# Count active stories
ACTIVE_COUNT=$(grep -c "^### \*\*" docs/project-management/PROJECT_BACKLOG.md)
echo "Active Stories: $ACTIVE_COUNT"

# Count completed stories
COMPLETED_COUNT=$(grep -c "^### \*\*" docs/project-management/COMPLETED_STORIES.md)
echo "Completed Stories: $COMPLETED_COUNT"

# Calculate points
./scripts/reconcile-points.sh

# Check for orphaned references
echo
echo "=== Potential Issues ==="
grep -n "TODO\|FIXME\|XXX" docs/project-management/*.md || echo "No issues found"
EOF

chmod +x scripts/backlog-health-check.sh
```

### 6.3 Communication Protocols

When backlog issues occur:

1. **Immediate Notification**
   ```bash
   # Log the incident
   echo "$(date): Backlog recovery performed - $REASON" >> \
   docs/project-management/incident-log.md
   ```

2. **Team Communication**
   - Notify all stakeholders of the issue
   - Explain what was lost and what was recovered
   - Provide new story point totals
   - Outline preventive measures taken

---

## 7. Tools and Automation

### 7.1 Validation Scripts

```bash
# Create validation script directory
mkdir -p scripts/backlog-tools

# Story validation script
cat > scripts/backlog-tools/validate-story.sh << 'EOF'
#!/bin/bash
STORY_ID=$1

if [ -z "$STORY_ID" ]; then
  echo "Usage: $0 STORY-ID"
  exit 1
fi

echo "Validating story: $STORY_ID"

# Check if story exists in backlog
if grep -q "$STORY_ID" docs/project-management/PROJECT_BACKLOG.md; then
  echo "✓ Story found in active backlog"
else
  echo "✗ Story not found in active backlog"
fi

# Check for implementation
IMPL_FILES=$(find . -name "*.java" -o -name "*.js" -o -name "*.py" | \
            xargs grep -l "$STORY_ID" 2>/dev/null)

if [ -n "$IMPL_FILES" ]; then
  echo "✓ Implementation found in:"
  echo "$IMPL_FILES"
else
  echo "✗ No implementation found"
fi

# Check for tests
TEST_FILES=$(find . -path "*/test*" -name "*test*" | \
            xargs grep -l "$STORY_ID" 2>/dev/null)

if [ -n "$TEST_FILES" ]; then
  echo "✓ Tests found in:"
  echo "$TEST_FILES"
else
  echo "✗ No tests found"
fi
EOF

chmod +x scripts/backlog-tools/validate-story.sh
```

### 7.2 Automated Consistency Checks

```bash
# Create pre-commit hook for backlog changes
cat > .git/hooks/pre-commit << 'EOF'
#!/bin/bash

# Check if backlog files are being modified
if git diff --cached --name-only | grep -q "PROJECT_BACKLOG.md\|COMPLETED_STORIES.md"; then
  echo "Backlog files detected in commit. Running validation..."
  
  # Run story point reconciliation
  if [ -f scripts/reconcile-points.sh ]; then
    scripts/reconcile-points.sh
  fi
  
  # Validate story format
  if [ -f scripts/backlog-tools/validate-format.sh ]; then
    scripts/backlog-tools/validate-format.sh
  fi
  
  echo "Validation complete. Proceeding with commit."
fi
EOF

chmod +x .git/hooks/pre-commit
```

### 7.3 Monitoring and Alerts

```bash
# Create monitoring script
cat > scripts/backlog-monitor.sh << 'EOF'
#!/bin/bash

# Monitor backlog health
HEALTH_CHECK=$(scripts/backlog-health-check.sh)
echo "$HEALTH_CHECK"

# Check for recent changes
RECENT_CHANGES=$(git log --since="24 hours ago" --oneline -- docs/project-management/)

if [ -n "$RECENT_CHANGES" ]; then
  echo "Recent backlog changes detected:"
  echo "$RECENT_CHANGES"
fi

# Verify file integrity
if [ ! -f docs/project-management/PROJECT_BACKLOG.md ]; then
  echo "ALERT: PROJECT_BACKLOG.md missing!"
  exit 1
fi

# Check for empty backlog
STORY_COUNT=$(grep -c "^### \*\*" docs/project-management/PROJECT_BACKLOG.md)
if [ "$STORY_COUNT" -eq 0 ]; then
  echo "ALERT: No active stories found in backlog!"
  exit 1
fi

echo "Backlog monitoring complete. Status: OK"
EOF

chmod +x scripts/backlog-monitor.sh
```

---

## 8. Roles and Responsibilities

### 8.1 Backlog Modification Authority

**Primary Backlog Managers:**
- Project Manager (read/write access)
- Technical Lead (read/write access)
- Product Owner (read/write access)

**Secondary Contributors:**
- Development Team (read access, can suggest changes)
- QA Team (read access, can verify completion)

### 8.2 Required Approvals

**Story Completion (Moving to COMPLETED_STORIES.md):**
- Technical verification by lead developer
- Business acceptance by product owner
- QA sign-off for user-facing features

**Story Modification (Changing active stories):**
- Agreement from project manager
- Technical feasibility review if scope changes
- Story point re-estimation if significant changes

**Backlog Restructuring:**
- Approval from all primary backlog managers
- Full backup and recovery plan
- Staged implementation with rollback capability

### 8.3 Review and Validation Process

```bash
# Create review checklist
cat > docs/project-management/REVIEW_CHECKLIST.md << 'EOF'
# Backlog Change Review Checklist

## For Story Completion:
- [ ] All acceptance criteria verified as complete
- [ ] Implementation code reviewed and approved
- [ ] Tests written and passing
- [ ] Documentation updated
- [ ] No breaking changes introduced
- [ ] Story points accurate
- [ ] Dependencies resolved

## For Story Modification:
- [ ] Change reason documented
- [ ] Impact on timeline assessed
- [ ] Dependencies checked
- [ ] Story points re-evaluated
- [ ] Team notified of changes

## For Bulk Changes:
- [ ] Backup created and verified
- [ ] Changes made incrementally
- [ ] Each change committed separately
- [ ] Story point totals reconciled
- [ ] Health check run and passed
EOF
```

### 8.4 Escalation Procedures

**Level 1: Process Violations**
- Document the issue
- Restore from backup if necessary
- Review process with violator
- Update training materials

**Level 2: Data Loss Incidents**
- Immediate backup restoration
- Full reconciliation process
- Team notification
- Post-incident review

**Level 3: Systemic Issues**
- Emergency response protocol
- Stakeholder notification
- External recovery assistance if needed
- Process redesign consideration

---

## Emergency Quick Reference

### Immediate Recovery Commands

```bash
# 1. Create emergency backup
cp docs/project-management/PROJECT_BACKLOG.md \
   docs/project-management/emergency-backup-$(date +%Y%m%d_%H%M%S).md

# 2. Restore from git
git checkout HEAD~1 -- docs/project-management/PROJECT_BACKLOG.md

# 3. Restore from backup
cp docs/project-management/backups/PROJECT_BACKLOG_LATEST.md \
   docs/project-management/PROJECT_BACKLOG.md

# 4. Validate recovery
scripts/backlog-health-check.sh

# 5. Document incident
echo "$(date): Emergency recovery - [REASON]" >> \
docs/project-management/incident-log.md
```

### Contact Information

- **Primary Contact**: Project Manager
- **Technical Contact**: Technical Lead
- **Emergency Contact**: Product Owner

---

*This process document should be reviewed monthly and updated based on lessons learned and process improvements. All changes to this process must be approved by the project management team and communicated to all stakeholders.*