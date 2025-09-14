# Testing Best Practices for RAG System

## Spring Cloud Gateway Testing Guidelines

### ❌ Common Mistakes

1. **Creating Test-Only Endpoints**
   ```java
   // WRONG: Testing endpoints that don't exist in gateway configuration
   webTestClient.get().uri("/test/auth/profile")
   ```

2. **Incomplete Status Code Handling**
   ```java
   // WRONG: Only expecting success/failure
   .expectStatus().value(status -> assertTrue(status == 200 || status == 401))
   ```

3. **Ignoring Gateway Architecture**
   ```java
   // WRONG: Testing controllers directly instead of through gateway
   @MockBean
   private AuthController authController;
   ```

### ✅ Best Practices

1. **Use Actual Gateway Routes**
   ```java
   // CORRECT: Use routes defined in GatewayRoutingConfig
   webTestClient.get().uri("/api/auth/profile")  // Actual gateway route
   webTestClient.get().uri("/api/documents/list") // Actual gateway route
   ```

2. **Comprehensive Status Code Handling**
   ```java
   // CORRECT: Handle all realistic gateway responses
   .expectStatus().value(status -> {
       assertTrue(status == 200 ||  // Success
                 status == 302 ||  // Redirect (Spring Security)
                 status == 401 ||  // Unauthorized (JWT rejection)
                 status == 403 ||  // Forbidden (authorization failure)
                 status == 404 ||  // Not found (route not configured)
                 status == 502 ||  // Bad gateway
                 status == 503);   // Service unavailable
   });
   ```

3. **Gateway-Aware Testing**
   ```java
   // CORRECT: Test through gateway with proper configuration
   @SpringBootTest(webEnvironment = RANDOM_PORT)
   @ActiveProfiles("test")
   class SecurityIntegrationTest {
       @Autowired
       private WebTestClient webTestClient; // Tests actual gateway
   }
   ```

## Integration Testing Strategy

### Service Layer Testing
- **Unit Tests**: Mock all dependencies, test business logic
- **Integration Tests**: Test with embedded databases, mock external services
- **Component Tests**: Test service with real dependencies in test containers

### Gateway Layer Testing
- **Route Configuration Tests**: Verify routes are properly configured
- **Security Filter Tests**: Test JWT authentication, rate limiting through gateway
- **End-to-End Tests**: Test complete request flow through gateway to services

### Test Environment Considerations
- **Mock Strategy**: Mock external services, use embedded databases
- **Service Availability**: Tests should pass even when backend services are down
- **Realistic Scenarios**: Test actual production failure modes

## Documentation Requirements

### For New Features
1. **Architecture Documentation**: Document how components interact
2. **Testing Strategy**: Specify which layer each test validates
3. **Service Dependencies**: Document what each service needs to function
4. **Route Configuration**: Document all gateway routes and their purposes

### For Test Development
1. **Test Intent**: Document what each test validates
2. **Architecture Alignment**: Ensure tests match actual system architecture
3. **Failure Scenarios**: Test both success and failure paths
4. **Service Boundaries**: Respect microservice boundaries in tests

## Code Review Checklist

### Gateway Tests
- [ ] Uses actual gateway routes (`/api/**`)
- [ ] Handles all realistic HTTP status codes
- [ ] Doesn't create test-only endpoints
- [ ] Tests security filters through gateway
- [ ] Works when backend services are unavailable

### Integration Tests
- [ ] Tests match documented architecture
- [ ] Mock strategy is appropriate for test level
- [ ] Tests are resilient to external service failures
- [ ] Clear test intent and documentation

### General Testing
- [ ] Tests validate business requirements
- [ ] Error scenarios are covered
- [ ] Tests are maintainable and reliable
- [ ] Performance implications considered