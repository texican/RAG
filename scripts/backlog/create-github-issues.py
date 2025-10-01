#!/usr/bin/env python3
"""
Generate GitHub Issues from Backlog
Creates GitHub issues for all backlog items with proper labels and priorities
"""

import json
import sys
from typing import Dict, List

def create_issue_template(story_id: str, title: str, description: str, 
                         acceptance_criteria: List[str], effort: int, 
                         priority: str, dependencies: List[str]) -> Dict:
    """Create a GitHub issue template"""
    
    # Map priorities to GitHub labels
    priority_labels = {
        "CRITICAL": "priority: critical",
        "HIGH": "priority: high", 
        "MEDIUM": "priority: medium",
        "LOW": "priority: low"
    }
    
    # Create issue body
    body = f"""## Description
{description}

## Acceptance Criteria
"""
    for criteria in acceptance_criteria:
        body += f"- [ ] {criteria}\n"
    
    body += f"""
## Technical Details
- **Story Points**: {effort}
- **Priority**: {priority}
- **Dependencies**: {', '.join(dependencies) if dependencies else 'None'}

## Definition of Done
- [ ] All acceptance criteria met
- [ ] Unit tests passing (100%)
- [ ] Integration tests passing (100%)
- [ ] Code review completed
- [ ] Quality gate validation passed
- [ ] Documentation updated

## Quality Gate
Before marking this issue as complete, run:
```bash
./scripts/quality/validate-system.sh
```
All quality checks must pass.
"""
    
    return {
        "title": f"{story_id}: {title}",
        "body": body,
        "labels": [
            priority_labels.get(priority, "priority: medium"),
            "type: story",
            "status: backlog"
        ]
    }

def generate_critical_issues():
    """Generate CRITICAL priority issues"""
    return [
        create_issue_template(
            "CRIT-001",
            "Fix Auth Service Integration Test Failures",
            "43 integration tests failing due to Spring Boot context loading issues",
            [
                "All 43 failing integration tests pass",
                "Spring Boot application context loads successfully in test environment", 
                "H2 test database configuration working properly",
                "JWT configuration properly loaded in test context",
                "Security configuration compatible with test environment"
            ],
            8,
            "CRITICAL",
            []
        ),
        create_issue_template(
            "CRIT-002", 
            "Fix Embedding Service Integration Test Failures",
            "8 EmbeddingIntegrationTest failures preventing proper testing",
            [
                "All 8 EmbeddingIntegrationTest tests pass",
                "Spring Boot application context loads with Redis test configuration",
                "Embedded Redis properly configured for tests", 
                "Test containers working with embedding service dependencies"
            ],
            5,
            "CRITICAL",
            []
        ),
        create_issue_template(
            "CRIT-003",
            "Fix Document Upload Functionality", 
            "Document upload endpoint returns HTTP 500 errors, breaking core workflow",
            [
                "Document upload endpoint returns HTTP 200/201 for valid uploads",
                "File storage operations work without persistence errors",
                "Tenant/user database relationships properly configured",
                "Multipart file uploads processed successfully",
                "Document metadata persisted correctly"
            ],
            8,
            "CRITICAL",
            ["CRIT-004"]
        ),
        create_issue_template(
            "CRIT-004",
            "Fix Database Relationship and Persistence Issues",
            "Tenant/user relationship constraints causing persistence failures",
            [
                "Tenant entities properly created with all required fields",
                "User entities properly linked to tenants with foreign key constraints",
                "Document entities properly linked to tenants and users", 
                "Version fields properly managed for optimistic locking",
                "Database schema consistent across all environments"
            ],
            5,
            "CRITICAL",
            []
        )
    ]

def generate_high_priority_issues():
    """Generate HIGH priority issues"""
    return [
        create_issue_template(
            "HIGH-001",
            "Implement Core Service Unit Tests",
            "Core service has no unit test coverage",
            [
                "Comprehensive unit tests for all core service components",
                "Service layer tests with mocked dependencies",
                "Controller tests with MockMvc",
                "Repository tests with test containers",
                "100% unit test pass rate"
            ],
            13,
            "HIGH",
            []
        ),
        create_issue_template(
            "HIGH-002",
            "Implement Admin Service Unit Tests", 
            "Admin service has no unit test coverage",
            [
                "Comprehensive unit tests for all admin service components",
                "Authentication and authorization tests",
                "Admin workflow tests (user management, system config)",
                "API endpoint tests with proper security validation",
                "100% unit test pass rate"
            ],
            10,
            "HIGH",
            []
        ),
        create_issue_template(
            "HIGH-003",
            "Implement End-to-End Integration Tests",
            "No comprehensive end-to-end workflow testing",
            [
                "Complete user journey tests (register → login → upload → search → retrieve)",
                "Cross-service integration testing",
                "Docker Compose environment testing",
                "Authentication flow integration tests",
                "Document processing pipeline integration tests"
            ],
            21,
            "HIGH", 
            ["CRIT-001", "CRIT-002", "CRIT-003", "CRIT-004"]
        ),
        create_issue_template(
            "HIGH-004",
            "Fix Spring Boot Test Context Configuration",
            "Test environments not properly configured for Spring Boot context loading",
            [
                "All Spring Boot @SpringBootTest contexts load successfully",
                "Test database configurations working (H2, TestContainers)",
                "Test Redis configurations working (embedded Redis)",
                "Test Kafka configurations working (TestContainers)",
                "Test security configurations compatible with test environment"
            ],
            8,
            "HIGH",
            []
        )
    ]

def main():
    """Generate all GitHub issues"""
    print("# GitHub Issues for RAG Enterprise System Backlog")
    print()
    print("Copy and paste each issue into GitHub Issues with the specified labels.")
    print()
    
    critical_issues = generate_critical_issues()
    high_issues = generate_high_priority_issues()
    
    print("## 🚨 CRITICAL PRIORITY ISSUES (Production Blockers)")
    print()
    for i, issue in enumerate(critical_issues, 1):
        print(f"### Issue {i}: {issue['title']}")
        print()
        print("**Labels:**", ", ".join(issue['labels']))
        print()
        print("**Body:**")
        print("```")
        print(issue['body'])
        print("```")
        print()
        print("---")
        print()
    
    print("## 🔧 HIGH PRIORITY ISSUES")
    print()
    for i, issue in enumerate(high_issues, 1):
        print(f"### Issue {i}: {issue['title']}")
        print()
        print("**Labels:**", ", ".join(issue['labels']))
        print()
        print("**Body:**")
        print("```")
        print(issue['body'])
        print("```")
        print()
        print("---")
        print()
    
    print("## Summary")
    print(f"- **Critical Issues**: {len(critical_issues)}")
    print(f"- **High Priority Issues**: {len(high_issues)}")
    print(f"- **Total Issues Generated**: {len(critical_issues) + len(high_issues)}")
    print()
    print("**Next Steps:**")
    print("1. Create these issues in GitHub")
    print("2. Assign CRITICAL issues to current sprint")
    print("3. Ensure all CRITICAL blockers are resolved before production")

if __name__ == "__main__":
    main()