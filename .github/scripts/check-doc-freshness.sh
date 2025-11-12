#!/bin/bash
#
# Documentation Freshness Check
# Identifies documentation files that haven't been updated recently
#

set -e

FRESHNESS_THRESHOLD=90  # days
CURRENT_DATE=$(date +%s)

echo "Checking documentation freshness..."
echo "Threshold: ${FRESHNESS_THRESHOLD} days"
echo ""

stale_count=0

# Function to check file age
check_file_freshness() {
    local file=$1
    local last_modified=$(git log -1 --format=%ct "$file" 2>/dev/null)
    
    if [ -z "$last_modified" ]; then
        # File not in git, use file system timestamp
        last_modified=$(stat -f %m "$file" 2>/dev/null || stat -c %Y "$file" 2>/dev/null)
    fi
    
    if [ -n "$last_modified" ]; then
        local days_old=$(( (CURRENT_DATE - last_modified) / 86400 ))
        
        if [ $days_old -gt $FRESHNESS_THRESHOLD ]; then
            echo "⚠️  STALE: $file ($days_old days old)"
            ((stale_count++))
        fi
    fi
}

# Check documentation directories
for file in $(find docs -name "*.md" -not -path "*/archive/*" -not -path "*/node_modules/*" -not -path "*/target/*"); do
    check_file_freshness "$file"
done

# Check root docs
for file in README.md CONTRIBUTING.md CLAUDE.md QUALITY_STANDARDS.md; do
    if [ -f "$file" ]; then
        check_file_freshness "$file"
    fi
done

echo ""
echo "Total stale documents: $stale_count"

if [ $stale_count -gt 0 ]; then
    echo "⚠️  Found $stale_count stale documents (>$FRESHNESS_THRESHOLD days old)"
    echo "Consider reviewing and updating these documents"
fi

exit 0
