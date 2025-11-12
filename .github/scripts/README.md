# Documentation Quality Tools

This directory contains scripts and hooks for maintaining documentation quality.

## Scripts

### `check-doc-metadata.py`
Python script that validates documentation metadata across all markdown files.

**Usage**:
```bash
python3 .github/scripts/check-doc-metadata.py
```

**Checks**:
- Required metadata fields present
- Valid semver version format
- Valid ISO date format
- Valid status values (active/draft/deprecated/archived)
- Document freshness (<90 days for active docs)

### `check-doc-freshness.sh`
Bash script that identifies stale documentation (>90 days since last update).

**Usage**:
```bash
.github/scripts/check-doc-freshness.sh
```

## Hooks

### `pre-commit-docs`
Git pre-commit hook for documentation quality checks.

**Installation**:
```bash
# Copy to git hooks directory
cp .github/hooks/pre-commit-docs .git/hooks/pre-commit
chmod +x .git/hooks/pre-commit
```

Or add to existing pre-commit hook:
```bash
# At the end of .git/hooks/pre-commit
.github/hooks/pre-commit-docs
```

**Checks**:
- TODO/FIXME/XXX markers in non-archived docs
- Metadata presence warnings
- Markdown linting (if markdownlint installed)

## GitHub Actions

### `docs-validation.yml`
Automated documentation validation workflow.

**Triggers**:
- Pull requests modifying markdown files
- Pushes to main branch
- Weekly scheduled runs (Mondays 9 AM UTC)
- Manual workflow dispatch

**Jobs**:
1. **validate-links**: Check all markdown links
2. **validate-metadata**: Verify metadata completeness
3. **lint-markdown**: Lint markdown formatting
4. **check-quality**: Check for TODO markers and stale docs

## Configuration Files

### `.markdown-link-check.json`
Configuration for markdown-link-check tool.

**Features**:
- Ignore localhost and example.com links
- Retry on 429 (rate limiting)
- Custom timeouts and headers

### `.markdownlint.json`
Configuration for markdownlint tool.

**Rules**:
- ATX-style headings (#)
- 120 character line length (code/tables excepted)
- Allow certain HTML elements
- Fenced code blocks

## Required Tools

### For Local Development
```bash
# Python 3.x (for metadata validation)
python3 --version

# markdownlint (optional but recommended)
npm install -g markdownlint-cli

# markdown-link-check (for link validation)
npm install -g markdown-link-check
```

### For CI/CD
All tools are installed automatically in GitHub Actions.

## Quality Standards

### Required Metadata Format
```markdown
---
version: X.Y.Z
last-updated: YYYY-MM-DD
status: active|draft|deprecated|archived
applies-to: system-version
---
```

### Markdown Standards
- Use ATX-style headings (# Header)
- Maximum 120 characters per line (except code/tables)
- Include language identifiers in code blocks
- Validate all links

### Quality Gates
- ✅ No broken internal links
- ✅ All active docs have metadata
- ✅ No TODO/FIXME in production docs
- ✅ Markdown linting passes
- ✅ Active docs updated within 90 days

## Troubleshooting

### "TODO markers found"
Remove or resolve TODO/FIXME/XXX comments before committing documentation.

### "Missing metadata header"
Add required metadata to the top of the markdown file:
```markdown
---
version: 1.0.0
last-updated: 2025-11-12
status: active
applies-to: 0.8.0-SNAPSHOT
---
```

### "Markdown linting failed"
Run markdownlint with fix flag:
```bash
markdownlint --fix path/to/file.md
```

### "Stale document"
Review and update old documentation, or archive if no longer relevant.

## Contributing

When adding new quality checks:
1. Update relevant script
2. Add tests if applicable
3. Update this README
4. Update docs-agent.md with new capabilities
