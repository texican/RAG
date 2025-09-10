#!/bin/bash

# Backlog Validation Script
# Prevents story loss by validating backlog consistency before changes

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

BACKLOG_FILE="/Users/stryfe/Projects/RAG/docs/project-management/PROJECT_BACKLOG.md"
COMPLETED_FILE="/Users/stryfe/Projects/RAG/docs/project-management/COMPLETED_STORIES.md"
BACKUP_DIR="/Users/stryfe/Projects/RAG/backups/$(date +%Y%m%d_%H%M%S)"

echo -e "${GREEN}🔍 BYO RAG Backlog Validation${NC}"
echo "=================================================="

# Function to count story points
count_story_points() {
    local file="$1"
    local label="$2"
    
    if [[ -f "$file" ]]; then
        local points=$(grep -o "Story Points:\*\* [0-9]\+" "$file" | grep -o "[0-9]\+" | awk '{sum += $1} END {print sum+0}')
        echo -e "${GREEN}✅ $label: $points story points${NC}"
        echo "$points"
    else
        echo -e "${RED}❌ $label: File not found${NC}"
        echo "0"
    fi
}

# Function to count stories
count_stories() {
    local file="$1"
    local label="$2"
    
    if [[ -f "$file" ]]; then
        local count=$(grep -c "^### \*\*.*:" "$file" || echo "0")
        echo -e "${GREEN}✅ $label: $count stories${NC}"
        echo "$count"
    else
        echo -e "${RED}❌ $label: File not found${NC}"
        echo "0"
    fi
}

# Create backup directory
mkdir -p "$BACKUP_DIR"

# Backup current files
echo -e "${YELLOW}📦 Creating backups...${NC}"
if [[ -f "$BACKLOG_FILE" ]]; then
    cp "$BACKLOG_FILE" "$BACKUP_DIR/PROJECT_BACKLOG_backup.md"
    echo "✅ Backed up PROJECT_BACKLOG.md"
fi

if [[ -f "$COMPLETED_FILE" ]]; then
    cp "$COMPLETED_FILE" "$BACKUP_DIR/COMPLETED_STORIES_backup.md"
    echo "✅ Backed up COMPLETED_STORIES.md"
fi

echo ""

# Validate current state
echo -e "${YELLOW}📊 Current Backlog Status:${NC}"
active_points=$(count_story_points "$BACKLOG_FILE" "Active Stories")
active_count=$(count_stories "$BACKLOG_FILE" "Active Stories")

echo ""
echo -e "${YELLOW}📊 Current Completed Status:${NC}"
completed_points=$(count_story_points "$COMPLETED_FILE" "Completed Stories")
completed_count=$(count_stories "$COMPLETED_FILE" "Completed Stories")

echo ""
echo -e "${YELLOW}📊 Summary:${NC}"
total_points=$((active_points + completed_points))
total_count=$((active_count + completed_count))

echo -e "${GREEN}📈 Total Project Scope: $total_count stories, $total_points story points${NC}"
echo -e "${GREEN}🎯 Progress: $completed_points/$total_points points completed ($(( completed_points * 100 / total_points ))%)${NC}"

# Validation checks
echo ""
echo -e "${YELLOW}🔧 Validation Checks:${NC}"

# Check for critical stories
critical_stories=$(grep -c "⭐ \*\*CRITICAL\*\*" "$BACKLOG_FILE" || echo "0")
echo -e "${GREEN}✅ Critical stories in backlog: $critical_stories${NC}"

# Check for story ID patterns
story_ids=$(grep -o "[A-Z]\+-[0-9]\+:" "$BACKLOG_FILE" | wc -l || echo "0")
echo -e "${GREEN}✅ Story IDs found: $story_ids${NC}"

# Check for minimum expected content
if [[ $active_count -lt 5 ]]; then
    echo -e "${RED}⚠️  WARNING: Only $active_count active stories (expected 8-12)${NC}"
fi

if [[ $active_points -lt 50 ]]; then
    echo -e "${RED}⚠️  WARNING: Only $active_points active story points (expected 70-80)${NC}"
fi

echo ""
echo -e "${GREEN}✅ Validation complete. Backups saved to: $BACKUP_DIR${NC}"
echo -e "${GREEN}📁 Use backups to restore if needed.${NC}"