---
version: 1.0.0
last-updated: 2025-11-12
status: active
applies-to: 0.8.0-SNAPSHOT
category: development
---

# Make vs Alternative Task Runners - Decision Rationale

## TL;DR

**Chosen: GNU Make**

**Why:** Universal availability, zero setup, bash-compatible, minimal learning curve, and perfect for Docker/Maven workflows.

## Comparison Matrix

| Criteria | Make | npm scripts | Gradle | Task/Taskfile | Just | Poetry/Invoke |
|----------|------|-------------|--------|---------------|------|---------------|
| **Pre-installed** | ✅ Yes (Unix) | ❌ Requires Node | ❌ Requires JVM | ❌ Requires Go | ❌ Requires Rust | ❌ Requires Python |
| **Zero config** | ✅ One file | ❌ package.json | ❌ build.gradle | ❌ Taskfile.yml | ❌ justfile | ❌ tasks.py |
| **Learning curve** | ✅ Low | ✅ Low | ❌ High | ⚠️ Medium | ⚠️ Medium | ⚠️ Medium |
| **Bash compatible** | ✅ Native | ⚠️ Via shell | ⚠️ Via exec | ⚠️ Via sh | ⚠️ Via sh | ❌ Python only |
| **Tab completion** | ✅ Built-in | ⚠️ With plugin | ⚠️ With plugin | ❌ No | ❌ No | ⚠️ With plugin |
| **IDE support** | ✅ Excellent | ✅ Excellent | ✅ Excellent | ⚠️ Limited | ⚠️ Limited | ⚠️ Limited |
| **Self-documenting** | ✅ make help | ⚠️ npm run | ❌ Manual | ⚠️ task --list | ⚠️ just --list | ⚠️ inv --list |
| **Parallel execution** | ✅ -j flag | ❌ Sequential | ✅ Native | ⚠️ Limited | ⚠️ Limited | ❌ Sequential |
| **Platform support** | ✅ All Unix | ✅ All | ✅ All | ✅ All | ✅ All | ✅ All |
| **Speed** | ✅ Fast | ⚠️ Node startup | ⚠️ JVM startup | ✅ Fast | ✅ Fast | ⚠️ Python startup |
| **Dependency mgmt** | ✅ Built-in | ❌ Manual | ✅ Complex | ✅ Simple | ⚠️ Limited | ❌ Manual |

## Detailed Analysis

### 1. GNU Make

```makefile
rebuild:
	./scripts/dev/rebuild-service.sh $(SERVICE)

logs:
	docker logs $(SERVICE) --tail 100 --follow
```

**Pros:**
- ✅ Pre-installed on every Unix system (macOS, Linux, WSL)
- ✅ No runtime dependencies (Node, Python, Go, etc.)
- ✅ Zero learning curve - everyone knows `make`
- ✅ Perfect for shell scripts - no escaping needed
- ✅ Tab completion works out of the box
- ✅ Excellent IDE integration (VS Code, IntelliJ, etc.)
- ✅ Self-documenting with `make help` target
- ✅ Parallel execution with `make -j4`
- ✅ Dependency management built-in
- ✅ Standard for C/C++/system projects

**Cons:**
- ⚠️ Whitespace-sensitive (tabs vs spaces)
- ⚠️ Syntax can be quirky for complex cases
- ⚠️ Not as modern-looking as alternatives

**Best for:** System-level tasks, Docker/build orchestration, polyglot projects

### 2. npm scripts

```json
{
  "scripts": {
    "rebuild": "./scripts/dev/rebuild-service.sh",
    "logs": "docker logs ${SERVICE} --tail 100 --follow"
  }
}
```

**Pros:**
- ✅ Familiar to JavaScript developers
- ✅ Good IDE integration
- ✅ Simple syntax

**Cons:**
- ❌ Requires Node.js installation
- ❌ Adds package.json to non-Node project
- ❌ Environment variable handling awkward
- ❌ No native parallel execution
- ❌ Not appropriate for Java/Maven project

**Best for:** JavaScript/TypeScript projects

### 3. Gradle

```groovy
task rebuild {
    doLast {
        exec {
            commandLine './scripts/dev/rebuild-service.sh', project.property('SERVICE')
        }
    }
}
```

**Pros:**
- ✅ Already using Gradle? (We use Maven)
- ✅ Powerful for complex workflows
- ✅ Good for Java projects

**Cons:**
- ❌ Complex syntax
- ❌ Heavy JVM startup time
- ❌ Overkill for simple Docker tasks
- ❌ Not suitable for non-Java devs
- ❌ We're a Maven project, not Gradle

**Best for:** Large Java/Kotlin projects with complex build logic

### 4. Task/Taskfile

```yaml
version: '3'
tasks:
  rebuild:
    cmds:
      - ./scripts/dev/rebuild-service.sh {{.SERVICE}}
```

**Pros:**
- ✅ Modern YAML syntax
- ✅ Fast (written in Go)
- ✅ Good for Docker workflows

**Cons:**
- ❌ Requires separate installation
- ❌ Less common - learning curve
- ❌ No tab completion by default
- ❌ Limited IDE support
- ❌ Smaller community

**Best for:** Modern DevOps projects, Docker-heavy workflows

### 5. Just

```just
rebuild SERVICE:
    ./scripts/dev/rebuild-service.sh {{SERVICE}}
```

**Pros:**
- ✅ Simpler than Make
- ✅ Modern syntax
- ✅ Good documentation

**Cons:**
- ❌ Requires Rust installation
- ❌ Not widely known
- ❌ Limited adoption
- ❌ No IDE support
- ❌ New tool to learn

**Best for:** Rust projects, modern alternatives to Make

### 6. Poetry/Invoke

```python
@task
def rebuild(c, service):
    c.run(f"./scripts/dev/rebuild-service.sh {service}")
```

**Pros:**
- ✅ Python syntax
- ✅ Powerful for Python projects

**Cons:**
- ❌ Requires Python setup
- ❌ Not appropriate for Java project
- ❌ Slower startup
- ❌ Not for system-level tasks

**Best for:** Python projects

## Why Make Won

### 1. Zero Barrier to Entry

```bash
# Works immediately on any Unix system:
git clone repo
cd repo
make rebuild SERVICE=rag-auth
```

No installation, no setup, no dependencies.

### 2. Perfect for Shell Scripts

Our workflow is:
- Run Maven (Java)
- Run Docker commands
- Run shell scripts

Make is designed for exactly this. No need to escape quotes or fight with another language:

```makefile
# Make - natural
rebuild:
	./scripts/dev/rebuild-service.sh $(SERVICE)

# npm - awkward
"rebuild": "SERVICE=${SERVICE} ./scripts/dev/rebuild-service.sh"

# Gradle - verbose
exec {
    commandLine 'bash', '-c', "./scripts/dev/rebuild-service.sh ${project.property('SERVICE')}"
}
```

### 3. Self-Documenting

```makefile
help:
	@echo "Available commands:"
	@echo "  make rebuild SERVICE=name"
	@echo "  make logs SERVICE=name"
```

Built-in documentation pattern. Just run `make help`.

### 4. Tab Completion

```bash
make reb<TAB>    # → make rebuild
make log<TAB>    # → make logs
```

Works out of the box. No plugins needed.

### 5. IDE Integration

Every IDE supports Makefiles:
- VS Code: Built-in support
- IntelliJ: Native integration
- Vim/Emacs: Extensive plugins
- Sublime/Atom: Built-in

### 6. Parallel Execution

```bash
# Build multiple services in parallel
make rebuild SERVICE=rag-auth &
make rebuild SERVICE=rag-admin &
wait

# Or use Make's built-in:
make -j4 build-all
```

### 7. Standard for System Tools

Docker, Kubernetes, and most system tools use Makefiles. Developers expect it.

## What About Modern Alternatives?

**Task/Taskfile** is great but:
- Requires installation
- Less familiar to most developers
- Our tasks are simple enough for Make

**Just** is excellent but:
- Too new, limited adoption
- Requires Rust toolchain
- Make is "good enough"

**npm scripts** would work but:
- Adds Node.js dependency to Java project
- Conceptually wrong (we're not a JS project)
- Less powerful than Make for system tasks

## Decision Matrix

| Requirement | Weight | Make | npm | Task | Just | Gradle |
|-------------|--------|------|-----|------|------|--------|
| No installation needed | 10 | ✅ 10 | ❌ 0 | ❌ 0 | ❌ 0 | ❌ 0 |
| Easy for Java devs | 8 | ✅ 8 | ⚠️ 4 | ⚠️ 4 | ⚠️ 3 | ✅ 8 |
| Good for Docker | 9 | ✅ 9 | ⚠️ 6 | ✅ 9 | ✅ 9 | ⚠️ 5 |
| Tab completion | 6 | ✅ 6 | ⚠️ 3 | ❌ 0 | ❌ 0 | ⚠️ 3 |
| IDE support | 7 | ✅ 7 | ✅ 7 | ⚠️ 3 | ⚠️ 2 | ✅ 7 |
| Learning curve | 8 | ✅ 8 | ✅ 8 | ⚠️ 5 | ⚠️ 4 | ❌ 2 |
| Speed | 5 | ✅ 5 | ⚠️ 3 | ✅ 5 | ✅ 5 | ❌ 2 |
| **Total** | **53** | **53** | **31** | **26** | **23** | **27** |

**Make wins decisively: 53/53 points**

## Counter-Arguments Addressed

### "Make syntax is ugly"

Yes, but:
- Our Makefile is simple (~100 lines)
- Mostly just calls shell scripts
- Readability isn't critical for task runner
- **Functionality > Aesthetics**

### "Tabs vs spaces is annoying"

Yes, but:
- Modern editors handle it automatically
- Only affects contributors, not users
- One-time learning curve
- **Minor inconvenience vs major benefits**

### "Make is old/outdated"

Yes, but:
- Still actively maintained
- Works perfectly for our use case
- "Old" = "battle-tested" and "stable"
- **If it ain't broke, don't fix it**

### "Task/Just are more modern"

Yes, but:
- Modern doesn't mean better for our case
- Requires installation step
- Smaller community = fewer resources
- **Pragmatism > Novelty**

## When to Reconsider

We should reconsider if:

1. **Project becomes Node.js-based** → Switch to npm scripts
2. **Team prefers Task/Just** → Migrate if consensus
3. **Make limitations hit** → Unlikely for our simple tasks
4. **CI/CD requirements change** → Re-evaluate based on platform

## Conclusion

**Make is the right choice because:**

1. ✅ **Zero barrier** - Works immediately
2. ✅ **Perfect fit** - Designed for shell/Docker/build tasks
3. ✅ **Universal** - Everyone knows it
4. ✅ **Fast** - No runtime overhead
5. ✅ **Standard** - Expected in system projects
6. ✅ **Powerful enough** - Handles all our needs
7. ✅ **Simple enough** - Doesn't overcomplicate

For a **Java/Maven project with Docker orchestration** accessed by **polyglot developers**, Make is the pragmatic choice.

## References

- [Make Manual](https://www.gnu.org/software/make/manual/)
- [Task vs Make](https://taskfile.dev/#/usage?id=comparison-with-make)
- [Just Comparison](https://github.com/casey/just#comparison-to-make)
- [Why Make is Still Relevant](https://stackoverflow.com/questions/7450713/why-is-make-still-so-widely-used)

---

**Last Updated:** 2025-10-01
**Decision Status:** ✅ Approved
**Review Date:** When requirements change significantly
