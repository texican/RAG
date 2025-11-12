# Agent System Triggers

**Purpose**: Runtime enforcement of agent-based workflows. This file defines triggers that should cause the AI to check for and follow agent-specific instructions.

---

## Meta-Rule: Always Check for Agent Instructions First

**Before ANY action, ask yourself:**
> "Does `.claude/agents/` have instructions for this domain?"

If **YES** → Read and follow those instructions  
If **NO** → Proceed with caution and document the approach  

---

## Backlog Management Triggers

**When you see ANY of these phrases or intentions:**

### Completion Triggers
- "Complete this story"
- "Mark story as done"
- "Story is finished"
- "Validate STORY-XXX"
- "Close this story"
- "Archive this story"

**ACTION**: 
1. **STOP** immediately
2. Read `.claude/agents/backlog-agent.md` 
3. Follow its completion workflow EXACTLY
4. Do NOT mark complete in BACKLOG.md

### Query Triggers
- "Where do completed stories go?"
- "Show me completed stories"
- "What's in the backlog?"
- "How many stories are complete?"
- "Story point totals"
- "Which stories are done?"

**ACTION**:
1. Read `.claude/agents/backlog-agent.md`
2. Understand file organization rules
3. Answer based on those rules
4. Never assume standard practices

### Estimation Triggers
- "Estimate this story"
- "How many points?"
- "Size this work"
- "Is this a pebble/rock/boulder?"
- "What's the effort?"

**ACTION**:
1. Read `.claude/agents/backlog-agent.md`
2. Use pebbles/rocks/boulders methodology
3. Compare to similar completed stories
4. Document reasoning

### File Modification Triggers
**When about to modify:**
- `BACKLOG.md`
- `docs/project-management/COMPLETED_STORIES.md`
- Story status anywhere
- Story point totals

**ACTION**:
1. **STOP** immediately
2. Read `.claude/agents/backlog-agent.md`
3. Create timestamped backup
4. Follow exact procedures
5. Never improvise

---

## Test Management Triggers

**When you see:**
- "Run tests"
- "Check test status"
- "Are tests passing?"
- "Verify tests"

**ACTION**:
1. Check for `.claude/agents/test-agent.md`
2. Follow test execution procedures
3. Report results accurately

---

## Git/Version Control Triggers

**When you see:**
- "Commit this"
- "Push changes"
- "Create branch"
- "Git status"

**ACTION**:
1. Check for `.claude/agents/git-agent.md`
2. Follow version control procedures
3. Use proper commit messages

---

## Deployment Triggers

**When you see:**
- "Deploy this"
- "Push to production"
- "Release"
- "Ship it"

**ACTION**:
1. Check for `.claude/agents/deploy-agent.md`
2. Follow deployment procedures
3. Verify prerequisites

---

## Universal Safety Triggers

**ALWAYS trigger on:**
- File deletion requests
- Bulk file modifications
- "Just do it" / "Skip the checks"
- "Trust me on this"
- User rushing or pressuring

**ACTION**:
1. **SLOW DOWN**
2. Check for relevant agent instructions
3. Create backups
4. Ask clarifying questions
5. Document the decision

---

## How to Use This File

### For AI Agents
1. Reference this file at the start of each conversation
2. Check triggers against user requests
3. Follow the ACTION steps when triggered
4. Never skip agent instructions when triggered

### For Humans
1. Update this file when adding new agents
2. Add new trigger phrases as patterns emerge
3. Review after any agent-related mistakes
4. Keep triggers specific and actionable

---

## Priority Order

When multiple triggers activate:
1. **Safety triggers** (deletion, bulk changes) - highest priority
2. **Backlog triggers** (story completion, file modification)
3. **Test triggers** (before marking stories complete)
4. **Git triggers** (after changes made)
5. **Deployment triggers** (final step)

---

**Last Updated**: 2025-11-12  
**Version**: 1.0.0  
**Maintained By**: Project team  
