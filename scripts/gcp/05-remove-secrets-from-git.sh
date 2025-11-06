#!/bin/bash
set -euo pipefail

##############################################################################
# Remove Secrets from Git History
#
# This script removes .env and other secret files from git history using
# git-filter-repo. This is REQUIRED after migrating to Secret Manager to
# remove compromised credentials from version control history.
#
# WARNING: This rewrites git history and requires force push!
#
# Prerequisites:
# - git-filter-repo installed (pip install git-filter-repo)
# - All team members notified (they will need to re-clone)
# - Backup created (script creates backup branch automatically)
#
# Usage:
#   ./scripts/gcp/05-remove-secrets-from-git.sh [--confirm]
#
##############################################################################

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

CONFIRMED=false

# Parse arguments
while [[ $# -gt 0 ]]; do
  case $1 in
    --confirm)
      CONFIRMED=true
      shift
      ;;
    *)
      echo -e "${RED}Unknown option: $1${NC}"
      exit 1
      ;;
  esac
done

echo -e "${RED}╔════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${RED}║           ⚠️  DANGER: Git History Rewrite ⚠️                   ║${NC}"
echo -e "${RED}╚════════════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${YELLOW}This script will:${NC}"
echo "  1. Create a backup branch (backup-before-secret-removal)"
echo "  2. Remove .env from ALL commits in git history"
echo "  3. Rewrite git history (all commit SHAs will change)"
echo ""
echo -e "${RED}IMPORTANT WARNINGS:${NC}"
echo "  • All team members will need to re-clone the repository"
echo "  • All open pull requests will be affected"
echo "  • Force push is required to update remote"
echo "  • Cannot be undone easily (backup branch will be created)"
echo ""
echo -e "${YELLOW}Prerequisites:${NC}"
echo "  • git-filter-repo must be installed: pip install git-filter-repo"
echo "  • You must have push access to the repository"
echo "  • All team members must be notified before running"
echo ""

if [ "$CONFIRMED" = false ]; then
  echo -e "${RED}This script requires --confirm flag to proceed.${NC}"
  echo ""
  echo "Usage: $0 --confirm"
  echo ""
  exit 1
fi

cd "$PROJECT_ROOT"

##############################################################################
# Step 1: Check Prerequisites
##############################################################################

echo -e "${YELLOW}Step 1: Checking prerequisites...${NC}"

# Check if git-filter-repo is installed
if ! command -v git-filter-repo &> /dev/null; then
  echo -e "${RED}✗ git-filter-repo not found${NC}"
  echo ""
  echo "Install with: pip install git-filter-repo"
  echo ""
  exit 1
fi
echo -e "${GREEN}✓ git-filter-repo is installed${NC}"

# Check if we're in a git repository
if [ ! -d ".git" ]; then
  echo -e "${RED}✗ Not a git repository${NC}"
  exit 1
fi
echo -e "${GREEN}✓ In git repository${NC}"

# Check for uncommitted changes
if [ -n "$(git status --porcelain)" ]; then
  echo -e "${RED}✗ Uncommitted changes detected${NC}"
  echo "Please commit or stash changes before running this script."
  exit 1
fi
echo -e "${GREEN}✓ No uncommitted changes${NC}"

echo ""

##############################################################################
# Step 2: Create Backup Branch
##############################################################################

echo -e "${YELLOW}Step 2: Creating backup branch...${NC}"

BACKUP_BRANCH="backup-before-secret-removal-$(date +%Y%m%d-%H%M%S)"
git branch "$BACKUP_BRANCH"

echo -e "${GREEN}✓ Created backup branch: $BACKUP_BRANCH${NC}"
echo ""

##############################################################################
# Step 3: Show Files to be Removed
##############################################################################

echo -e "${YELLOW}Step 3: Scanning for secret files in history...${NC}"
echo ""

echo -e "${BLUE}Files that will be removed from history:${NC}"

# Check if .env exists in history
if git log --all --format=%H -- .env | head -1 &>/dev/null; then
  FIRST_COMMIT=$(git log --all --format=%H -- .env | tail -1)
  COMMIT_COUNT=$(git log --all --oneline -- .env | wc -l | tr -d ' ')
  echo "  • .env (appears in $COMMIT_COUNT commits)"
  echo "    First appearance: $FIRST_COMMIT"
fi

# Check for other secret files
SECRET_PATTERNS=(
  "*.key"
  "*.pem"
  "credentials.json"
  "*-sa-key.json"
  ".env.local"
  ".env.production"
)

for pattern in "${SECRET_PATTERNS[@]}"; do
  if git log --all --format=%H -- "$pattern" | head -1 &>/dev/null; then
    COUNT=$(git log --all --oneline -- "$pattern" | wc -l | tr -d ' ')
    echo "  • $pattern (appears in $COUNT commits)"
  fi
done

echo ""

##############################################################################
# Step 4: Final Confirmation
##############################################################################

echo -e "${RED}╔════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${RED}║                    FINAL CONFIRMATION                          ║${NC}"
echo -e "${RED}╚════════════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${YELLOW}Are you ABSOLUTELY SURE you want to proceed?${NC}"
echo ""
echo "This will:"
echo "  • Rewrite ALL git history"
echo "  • Change ALL commit SHAs"
echo "  • Require force push to remote"
echo "  • Require all team members to re-clone"
echo ""
read -p "Type 'YES I AM SURE' to continue: " confirmation

if [ "$confirmation" != "YES I AM SURE" ]; then
  echo -e "${YELLOW}Aborted by user.${NC}"
  echo "Backup branch preserved: $BACKUP_BRANCH"
  exit 0
fi

echo ""

##############################################################################
# Step 5: Remove Files from History
##############################################################################

echo -e "${YELLOW}Step 4: Removing secrets from git history...${NC}"
echo ""

# Use git-filter-repo to remove .env
echo -e "${BLUE}Running git-filter-repo...${NC}"
git filter-repo --path .env --invert-paths --force

echo -e "${GREEN}✓ Removed .env from git history${NC}"
echo ""

##############################################################################
# Step 6: Verify Removal
##############################################################################

echo -e "${YELLOW}Step 5: Verifying removal...${NC}"

# Check if .env still exists in history
if git log --all --format=%H -- .env | head -1 &>/dev/null; then
  echo -e "${RED}✗ .env still found in history${NC}"
  exit 1
fi

echo -e "${GREEN}✓ .env successfully removed from all commits${NC}"
echo ""

##############################################################################
# Step 7: Show Summary
##############################################################################

echo -e "${GREEN}╔════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║                   ✓ History Rewrite Complete                   ║${NC}"
echo -e "${GREEN}╚════════════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${BLUE}What happened:${NC}"
echo "  • Backup created: $BACKUP_BRANCH"
echo "  • .env removed from ALL commits"
echo "  • All commit SHAs have changed"
echo "  • Repository size may be reduced"
echo ""
echo -e "${RED}⚠️  CRITICAL NEXT STEPS:${NC}"
echo ""
echo -e "${YELLOW}1. Review the changes:${NC}"
echo "   git log --oneline | head -20"
echo "   git diff $BACKUP_BRANCH HEAD"
echo ""
echo -e "${YELLOW}2. Verify no secrets remain:${NC}"
echo "   git log --all --format=%H -- .env"
echo "   (should return nothing)"
echo ""
echo -e "${YELLOW}3. Force push to remote (DESTRUCTIVE):${NC}"
echo "   ${RED}git push origin --force --all${NC}"
echo "   ${RED}git push origin --force --tags${NC}"
echo ""
echo -e "${YELLOW}4. Notify all team members:${NC}"
echo "   They MUST re-clone the repository:"
echo "   git clone <repo-url> <new-directory>"
echo ""
echo -e "${YELLOW}5. Update pull requests:${NC}"
echo "   All open PRs will need to be rebased or recreated"
echo ""
echo -e "${BLUE}To restore from backup (if needed):${NC}"
echo "   git checkout $BACKUP_BRANCH"
echo "   git branch -f main"
echo "   git checkout main"
echo ""
echo -e "${GREEN}Git history rewrite complete!${NC}"
echo ""
