---
version: 1.0.0
last-updated: 2025-11-12
status: archived
applies-to: 0.8.0-SNAPSHOT
category: project-management
---

---
version: 1.0.0
last-updated: 2025-11-12
status: archived
applies-to: 0.8.0-SNAPSHOT
category: documentation
---

# Documentation Organization Plan

## Current Issues

The docs directory has files in root that should be categorized:
- Docker-related docs in root (should be in deployment or development)
- Development workflow docs mixed with project management
- No clear structure for new developers

## Proposed Structure

```
docs/
├── README.md                          # Hub - links to all docs
│
├── getting-started/                   # NEW - For new developers
│   ├── QUICK_START.md                # 5-min setup guide
│   ├── QUICK_REFERENCE.md            # Cheat sheet (MOVE from root)
│   └── INSTALLATION.md               # Detailed setup
│
├── development/                       # Development guides
│   ├── DOCKER_DEVELOPMENT.md         # MOVE from root
│   ├── MAKE_VS_ALTERNATIVES.md       # MOVE from root
│   ├── ERROR_HANDLING_GUIDELINES.md  # Already here ✓
│   ├── TESTING_BEST_PRACTICES.md     # Already here ✓
│   ├── METHODOLOGY.md                # Already here ✓
│   ├── ADR-001-BYPASS-API-GATEWAY.md # Already here ✓
│   ├── SECURITY-001-DOCUMENTATION.md # Already here ✓
│   └── templates/                    # Already here ✓
│       └── DOCUMENTATION_TEMPLATES.md
│
├── deployment/                        # Deployment and operations
│   ├── DEPLOYMENT.md                 # Already here ✓
│   ├── DOCKER.md                     # Already here ✓
│   ├── SERVICE_CONNECTION_GUIDE.md   # Already here ✓
│   ├── SWAGGER_UI_ACCESS_GUIDE.md    # Already here ✓
│   ├── DOCKER_IMPROVEMENTS_SUMMARY.md # MOVE from root
│   └── ENFORCEMENT_MECHANISMS.md     # MOVE from root (ops concern)
│
├── architecture/                      # NEW - System design
│   ├── PROJECT_STRUCTURE.md          # MOVE from root
│   ├── ENFORCEMENT_DIAGRAM.md        # MOVE from root
│   └── SERVICE_ARCHITECTURE.md       # NEW - service diagram
│
├── api/                              # API documentation
│   └── API_DOCUMENTATION_PORTAL.md   # Already here ✓
│
├── testing/                          # Testing documentation
│   └── TEST_RESULTS_SUMMARY.md       # Already here ✓
│
├── project-management/               # Project tracking (keep as-is)
│   └── [all existing files]          # Already here ✓
│
└── archive/                          # OLD - Deprecated docs
    ├── CLAUDE.md                     # MOVE from development (now in root)
    ├── CONTEXT_ASSEMBLY_ERROR_ANALYSIS.md  # OLD
    ├── GATEWAY_TESTING_GUIDELINES.md       # OLD (gateway archived)
    ├── KAFKA_ERROR_HANDLING.md             # OLD (if not used)
    └── SCRIPT_SPECIFICATIONS.md            # OLD (outdated)
```

## Migration Actions

### 1. Create New Directories
- `docs/getting-started/`
- `docs/architecture/`
- `docs/archive/`

### 2. Move Files

#### To `docs/getting-started/`:
- `docs/QUICK_REFERENCE.md` → `docs/getting-started/QUICK_REFERENCE.md`

#### To `docs/development/`:
- `docs/DOCKER_DEVELOPMENT.md` → `docs/development/DOCKER_DEVELOPMENT.md`
- `docs/MAKE_VS_ALTERNATIVES.md` → `docs/development/MAKE_VS_ALTERNATIVES.md`

#### To `docs/deployment/`:
- `docs/DOCKER_IMPROVEMENTS_SUMMARY.md` → `docs/deployment/DOCKER_IMPROVEMENTS_SUMMARY.md`
- `docs/ENFORCEMENT_MECHANISMS.md` → `docs/deployment/ENFORCEMENT_MECHANISMS.md`

#### To `docs/architecture/`:
- `docs/PROJECT_STRUCTURE.md` → `docs/architecture/PROJECT_STRUCTURE.md`
- `docs/ENFORCEMENT_DIAGRAM.md` → `docs/architecture/ENFORCEMENT_DIAGRAM.md`

#### To `docs/archive/`:
- `docs/development/CLAUDE.md` → `docs/archive/CLAUDE.md` (superseded by root CLAUDE.md)
- `docs/development/CONTEXT_ASSEMBLY_ERROR_ANALYSIS.md` → `docs/archive/`
- `docs/development/GATEWAY_TESTING_GUIDELINES.md` → `docs/archive/`
- `docs/development/KAFKA_ERROR_HANDLING.md` → `docs/archive/` (if not actively used)
- `docs/SCRIPT_SPECIFICATIONS.md` → `docs/archive/`

### 3. Update Root docs/README.md

Create a comprehensive hub that links to all documentation.

### 4. Update References

Update all files that reference moved documents:
- Root README.md
- CONTRIBUTING.md
- Root CLAUDE.md
- Other docs with cross-references

## Benefits

1. **Clear Entry Point**: New developers start at `docs/getting-started/`
2. **Logical Grouping**: Related docs together
3. **Reduced Clutter**: Archive old/deprecated docs
4. **Scalability**: Easy to add new docs to appropriate section
5. **Discoverability**: Hub README makes everything findable

## Implementation Order

1. ✅ Create new directories
2. ✅ Move files
3. ✅ Update docs/README.md hub
4. ✅ Update cross-references
5. ✅ Update root documentation (README, CONTRIBUTING, CLAUDE.md)
6. ✅ Test all links
