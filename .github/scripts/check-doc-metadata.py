#!/usr/bin/env python3
"""
Documentation Metadata Validation Script

Validates that all markdown files have required metadata headers
and that the metadata is properly formatted.

Required metadata:
  - version: X.Y.Z (semver format)
  - last-updated: YYYY-MM-DD (ISO date)
  - status: active|draft|deprecated|archived
  - applies-to: version string

Optional metadata:
  - author: string
  - reviewers: list
  - category: string
"""

import os
import re
import sys
from pathlib import Path
from datetime import datetime, timedelta
from typing import Dict, List, Tuple, Optional

# Metadata requirements
REQUIRED_FIELDS = ['version', 'last-updated', 'status', 'applies-to']
OPTIONAL_FIELDS = ['author', 'reviewers', 'category', 'review-date']
VALID_STATUSES = ['active', 'draft', 'deprecated', 'archived']

# Directories to check
DOCS_DIRS = ['docs', 'specs', '.claude/agents']
ROOT_DOCS = ['README.md', 'CONTRIBUTING.md', 'CLAUDE.md', 'QUALITY_STANDARDS.md']

# Directories to skip
SKIP_DIRS = ['node_modules', 'target', '.git', 'archive']

# Files to skip
SKIP_FILES = ['BACKLOG.md', 'E2E-TEST-RESULTS.md']

# Freshness threshold (days)
FRESHNESS_THRESHOLD = 90


class MetadataValidator:
    def __init__(self):
        self.errors = []
        self.warnings = []
        self.files_checked = 0
        self.files_with_metadata = 0
        self.files_missing_metadata = []
        
    def extract_metadata(self, content: str) -> Optional[Dict[str, str]]:
        """Extract YAML front matter from markdown content."""
        # Match YAML front matter between --- delimiters
        pattern = r'^---\s*\n(.*?)\n---\s*\n'
        match = re.match(pattern, content, re.DOTALL)
        
        if not match:
            return None
        
        metadata = {}
        yaml_content = match.group(1)
        
        # Parse simple YAML key-value pairs
        for line in yaml_content.split('\n'):
            line = line.strip()
            if not line or line.startswith('#'):
                continue
            
            if ':' in line:
                key, value = line.split(':', 1)
                metadata[key.strip()] = value.strip()
        
        return metadata
    
    def validate_version(self, version: str) -> bool:
        """Validate semver format (X.Y.Z)."""
        pattern = r'^\d+\.\d+\.\d+$'
        return bool(re.match(pattern, version))
    
    def validate_date(self, date_str: str) -> bool:
        """Validate ISO date format (YYYY-MM-DD)."""
        pattern = r'^\d{4}-\d{2}-\d{2}$'
        if not re.match(pattern, date_str):
            return False
        
        try:
            datetime.strptime(date_str, '%Y-%m-%d')
            return True
        except ValueError:
            return False
    
    def check_freshness(self, date_str: str) -> Tuple[bool, int]:
        """Check if date is within freshness threshold."""
        try:
            doc_date = datetime.strptime(date_str, '%Y-%m-%d')
            days_old = (datetime.now() - doc_date).days
            is_fresh = days_old <= FRESHNESS_THRESHOLD
            return is_fresh, days_old
        except ValueError:
            return False, -1
    
    def validate_file(self, filepath: Path) -> bool:
        """Validate a single markdown file."""
        self.files_checked += 1
        
        try:
            with open(filepath, 'r', encoding='utf-8') as f:
                content = f.read()
        except Exception as e:
            self.errors.append(f"{filepath}: Error reading file - {e}")
            return False
        
        # Extract metadata
        metadata = self.extract_metadata(content)
        
        if metadata is None:
            self.files_missing_metadata.append(str(filepath))
            self.warnings.append(f"{filepath}: Missing metadata header")
            return False
        
        self.files_with_metadata += 1
        file_valid = True
        
        # Check required fields
        for field in REQUIRED_FIELDS:
            if field not in metadata:
                self.errors.append(f"{filepath}: Missing required field '{field}'")
                file_valid = False
        
        # Validate field formats
        if 'version' in metadata:
            if not self.validate_version(metadata['version']):
                self.errors.append(
                    f"{filepath}: Invalid version format '{metadata['version']}' "
                    "(expected X.Y.Z)"
                )
                file_valid = False
        
        if 'last-updated' in metadata:
            if not self.validate_date(metadata['last-updated']):
                self.errors.append(
                    f"{filepath}: Invalid date format '{metadata['last-updated']}' "
                    "(expected YYYY-MM-DD)"
                )
                file_valid = False
            else:
                # Check freshness for active documents
                if metadata.get('status') == 'active':
                    is_fresh, days_old = self.check_freshness(metadata['last-updated'])
                    if not is_fresh:
                        self.warnings.append(
                            f"{filepath}: Stale document ({days_old} days old, "
                            f"threshold is {FRESHNESS_THRESHOLD} days)"
                        )
        
        if 'status' in metadata:
            if metadata['status'] not in VALID_STATUSES:
                self.errors.append(
                    f"{filepath}: Invalid status '{metadata['status']}' "
                    f"(must be one of: {', '.join(VALID_STATUSES)})"
                )
                file_valid = False
        
        return file_valid
    
    def should_skip_file(self, filepath: Path) -> bool:
        """Check if file should be skipped."""
        # Skip files in skip directories
        for skip_dir in SKIP_DIRS:
            if skip_dir in filepath.parts:
                return True
        
        # Skip specific files
        if filepath.name in SKIP_FILES:
            return True
        
        # Skip archived docs
        if 'archive' in str(filepath).lower():
            return True
        
        return False
    
    def validate_directory(self, directory: str):
        """Validate all markdown files in a directory."""
        dir_path = Path(directory)
        
        if not dir_path.exists():
            return
        
        for md_file in dir_path.rglob('*.md'):
            if not self.should_skip_file(md_file):
                self.validate_file(md_file)
    
    def validate_all(self):
        """Validate all documentation files."""
        # Check documentation directories
        for doc_dir in DOCS_DIRS:
            self.validate_directory(doc_dir)
        
        # Check root documentation files
        for root_doc in ROOT_DOCS:
            root_path = Path(root_doc)
            if root_path.exists() and not self.should_skip_file(root_path):
                self.validate_file(root_path)
    
    def print_report(self):
        """Print validation report."""
        print("\n" + "=" * 80)
        print("DOCUMENTATION METADATA VALIDATION REPORT")
        print("=" * 80)
        
        print(f"\n📊 Summary:")
        print(f"  Files checked: {self.files_checked}")
        print(f"  Files with metadata: {self.files_with_metadata}")
        print(f"  Files missing metadata: {len(self.files_missing_metadata)}")
        print(f"  Errors: {len(self.errors)}")
        print(f"  Warnings: {len(self.warnings)}")
        
        coverage = (self.files_with_metadata / self.files_checked * 100) if self.files_checked > 0 else 0
        print(f"\n  Metadata Coverage: {coverage:.1f}%")
        
        if self.errors:
            print(f"\n❌ Errors ({len(self.errors)}):")
            for error in self.errors:
                print(f"  - {error}")
        
        if self.warnings:
            print(f"\n⚠️  Warnings ({len(self.warnings)}):")
            for warning in self.warnings[:20]:  # Limit to first 20 warnings
                print(f"  - {warning}")
            if len(self.warnings) > 20:
                print(f"  ... and {len(self.warnings) - 20} more warnings")
        
        if self.files_missing_metadata:
            print(f"\n📝 Files Missing Metadata ({len(self.files_missing_metadata)}):")
            for filepath in self.files_missing_metadata[:10]:  # Show first 10
                print(f"  - {filepath}")
            if len(self.files_missing_metadata) > 10:
                print(f"  ... and {len(self.files_missing_metadata) - 10} more files")
        
        if not self.errors and not self.warnings:
            print("\n✅ All documentation metadata is valid!")
        
        print("\n" + "=" * 80)
        
        # Save detailed report
        self.save_report()
    
    def save_report(self):
        """Save detailed report to file."""
        with open('metadata-validation-report.txt', 'w') as f:
            f.write("DOCUMENTATION METADATA VALIDATION REPORT\n")
            f.write(f"Generated: {datetime.now().isoformat()}\n")
            f.write("=" * 80 + "\n\n")
            
            f.write(f"Files checked: {self.files_checked}\n")
            f.write(f"Files with metadata: {self.files_with_metadata}\n")
            f.write(f"Files missing metadata: {len(self.files_missing_metadata)}\n")
            f.write(f"Errors: {len(self.errors)}\n")
            f.write(f"Warnings: {len(self.warnings)}\n\n")
            
            if self.errors:
                f.write("ERRORS:\n")
                for error in self.errors:
                    f.write(f"  {error}\n")
                f.write("\n")
            
            if self.warnings:
                f.write("WARNINGS:\n")
                for warning in self.warnings:
                    f.write(f"  {warning}\n")
                f.write("\n")
            
            if self.files_missing_metadata:
                f.write("FILES MISSING METADATA:\n")
                for filepath in self.files_missing_metadata:
                    f.write(f"  {filepath}\n")


def main():
    """Main entry point."""
    validator = MetadataValidator()
    validator.validate_all()
    validator.print_report()
    
    # Exit with error code if validation failed
    if validator.errors:
        sys.exit(1)
    
    sys.exit(0)


if __name__ == '__main__':
    main()
