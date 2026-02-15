# Integration Testing Guide

## Overview

This document provides comprehensive guidance for writing and running integration tests in the Money Transfer System. Integration tests verify that multiple components work together correctly, including controllers, services, repositories, security, and the database.

**Testing Framework**: JUnit 5 (Jupiter) + Spring Boot Test  
**Test Configuration**: Spring Boot Test with MockMvc  
**Database**: In-memory H2 or test database  
**Security Testing**: Spring Security Test  
**Java Version**: Java 17  
**Build Tool**: Maven

---

## Table of Contents

1. [Integration Test Overview](#integration-test-overview)
2. [Running Integration Tests](#running-integration-tests)
3. [Test Categories](#test-categories)
4. [Writing Integration Tests](#writing-integration-tests)
5. [Database Management](#database-management)
6. [Security Testing](#security-testing)
7. [Best Practices](#best-practices)
8. [Test Coverage](#test-coverage)
9. [Troubleshooting](#troubleshooting)

---

## Integration Test Overview

### What Are Integration Tests?

Integration tests verify the interaction between multiple components:

- **Controllers** + **Services** + **Repositories** + **Database**
- **Security** + **Authentication** + **Authorization**
- **Rate Limiting** + **Validation** + **Error Handling**
- **Complete Request/Response Flow**

### Current Integration Test Suite

| Test Class | Purpose | Tests | Status |
|------------|---------|-------|--------|
| `RateLimitingIntegrationTest` | Rate limiting for auth, transfers, and reads | 12 | ✅ Passing |
| `JwtAuthenticationIntegrationTest` | JWT token generation and validation | 5+ | ✅ Passing |
| `SecurityRoleIntegrationTest` | RBAC (USER vs ADMIN roles) | 5+ | ✅ Passing |
| `UserOwnershipIntegrationTest` | User ownership checks for accounts | 5+ | ✅ Passing |
| `AccountReadIntegrationTest` | Account read operations with security | 5+ | ✅ Passing |

**Total Integration Tests**: 12+ test scenarios  
**Status**: 🟢 **ALL PASSING**

---

## Running Integration Tests

### Run All Integration Tests

```bash
cd backend
mvn verify
```

or

```bash
mvn test -Dtest="*IntegrationTest"
```

### Run Specific Integration Test Class

```bash
mvn test -Dtest=RateLimitingIntegrationTest
```

### Run Specific Test Method

```bash
mvn test -Dtest=RateLimitingIntegrationTest#testAuthRateLimitExceeded
```

### Run with Coverage

```bash
mvn clean verify jacoco:report
open target/site/jacoco/index.html
```

### Run in IDE

**IntelliJ IDEA**:
- Right-click on `*IntegrationTest.java` → "Run"
- Run all tests in `integration/` package

**VS Code**:
- Use "Test Runner for Java" extension
- Click "Run Test" on test class or method

---

## Test Categories

### 1. Rate Limiting Integration Tests

**File**: `RateLimitingIntegrationTest.java`  
**Purpose**: Verify rate limiting works correctly across all endpoints

**Test Scenarios**:

| Test | Scenario | Expected Result |
|------|----------|-----------------|
| `testAuthRateLimitExceeded` | 6 login attempts in 1 minute | 5 succeed, 6th returns 429 |
| `testAuthUnderRateLimit` | 5 login attempts | All succeed |
| `testAuthSeparateUserLimits` | Different users have separate limits | Each user gets their own bucket |
| `testTransferRateLimitExceeded` | 11 transfers in 1 minute | 10 succeed, 11th returns 429 |
| `testTransferUnderRateLimit` | 9 transfers in 1 minute | All succeed |
| `testAccountReadRateLimitExceeded` | 61 account reads | 60 succeed, 61st returns 429 |
| `testAccountBalanceUnderRateLimit` | 59 balance reads | All succeed |
| `testAccountTransactionsUnderRateLimit` | 3 transaction history reads | All succeed |
| `testUnauthenticatedReturns401` | Request without token | Returns 401, not 429 |
| `testInvalidTokenReturns401` | Request with invalid token | Returns 401, not 429 |
| `testSwaggerUiPublic` | Access Swagger UI | Public access allowed |
| `testOpenApiSpecPublic` | Access OpenAPI spec | Public access allowed |

**Key Features Tested**:
- ✅ Per-user rate limits (separate buckets)
- ✅ Per-username rate limits for auth
- ✅ Authentication checked before rate limiting
- ✅ Public endpoints not rate-limited
- ✅ Rate limit buckets reset between tests

**Example Test**:

```java
@Test
@DisplayName("✅ Auth rate limit exceeded (5 allowed, 6th blocked)")
void testAuthRateLimitExceeded() throws Exception {
    LoginRequest loginRequest = new LoginRequest("testuser", "wrongpassword");
    String requestBody = objectMapper.writeValueAsString(loginRequest);

    // First 5 attempts - all return 401 Unauthorized (wrong password)
    for (int i = 0; i < 5; i++) {
        mockMvc.perform(post(AUTH_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isUnauthorized()); // 401
    }

    // 6th attempt - rate limit exceeded, returns 429
    mockMvc.perform(post(AUTH_ENDPOINT)
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
            .andExpect(status().isTooManyRequests()); // 429
}
```

### 2. JWT Authentication Integration Tests

**File**: `JwtAuthenticationIntegrationTest.java`  
**Purpose**: Verify JWT token generation, validation, and authentication flow

**Test Scenarios**:
- ✅ Successful login returns valid JWT token
- ✅ Token contains correct claims (username, roles, expiration)
- ✅ Valid token allows access to protected endpoints
- ✅ Invalid token is rejected (401)
- ✅ Expired token is rejected (401)
- ✅ Missing token returns 401
- ✅ Token blacklist works after logout

**Example Test**:

```java
@Test
@DisplayName("✅ Valid JWT token allows access to protected endpoint")
void testValidTokenAllowsAccess() throws Exception {
    // Login and get token
    LoginRequest loginRequest = new LoginRequest("testuser", "password");
    MvcResult loginResult = mockMvc.perform(post("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isOk())
            .andReturn();
    
    String token = extractToken(loginResult);
    
    // Use token to access protected endpoint
    mockMvc.perform(get("/accounts")
            .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());
}
```

### 3. Security Role Integration Tests

**File**: `SecurityRoleIntegrationTest.java`  
**Purpose**: Verify RBAC (Role-Based Access Control) enforcement

**Test Scenarios**:
- ✅ USER can access USER endpoints
- ✅ USER cannot access ADMIN endpoints (403)
- ✅ ADMIN can access ADMIN endpoints
- ✅ ADMIN can access USER endpoints (superset)
- ✅ Unauthenticated requests return 401
- ✅ Role checks work for all endpoints

**Example Test**:

```java
@Test
@DisplayName("❌ USER cannot access ADMIN endpoints")
void testUserCannotAccessAdminEndpoints() throws Exception {
    String userToken = loginAsUser();
    
    // USER tries to access /api/v1/admin/accounts/{id}
    mockMvc.perform(get("/api/v1/admin/accounts/1001")
            .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isForbidden()); // 403
}

@Test
@DisplayName("✅ ADMIN can access ADMIN endpoints")
void testAdminCanAccessAdminEndpoints() throws Exception {
    String adminToken = loginAsAdmin();
    
    mockMvc.perform(get("/api/v1/admin/accounts/1001")
            .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk()); // 200
}
```

### 4. User Ownership Integration Tests

**File**: `UserOwnershipIntegrationTest.java`  
**Purpose**: Verify users can only access their own accounts

**Test Scenarios**:
- ✅ User can view their own account
- ✅ User cannot view another user's account (403)
- ✅ User can transfer from their own account
- ✅ User cannot transfer from another user's account (403)
- ✅ Admin can view any account

**Example Test**:

```java
@Test
@DisplayName("❌ User cannot access another user's account")
void testUserCannotAccessOtherAccount() throws Exception {
    String token = loginAsUser1();
    Long user2AccountId = user2Account.getId();
    
    mockMvc.perform(get("/accounts/" + user2AccountId)
            .header("Authorization", "Bearer " + token))
            .andExpect(status().isForbidden()); // 403
}
```

### 5. Account Read Integration Tests

**File**: `AccountReadIntegrationTest.java`  
**Purpose**: Verify account read operations work correctly

**Test Scenarios**:
- ✅ Get account by ID returns correct data
- ✅ Get account balance returns current balance
- ✅ Get transaction history returns correct transactions
- ✅ Account not found returns 404
- ✅ Rate limiting applies to reads
- ✅ Security checks apply to reads

---

## Writing Integration Tests

### Basic Test Structure

```java
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Feature Integration Tests")
class FeatureIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private String testUserToken;

    @BeforeEach
    void setUp() {
        // Clean database
        accountRepository.deleteAll();
        userRepository.deleteAll();
        
        // Setup test data
        setupTestData();
        
        // Generate test token
        testUserToken = generateTestToken("testuser", "USER");
    }

    @Test
    @DisplayName("✅ Test scenario description")
    void testFeature() throws Exception {
        // Given: Setup test data
        Account account = createTestAccount();
        
        // When: Make HTTP request
        mockMvc.perform(get("/accounts/" + account.getId())
                .header("Authorization", "Bearer " + testUserToken))
                
                // Then: Verify response
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(account.getId()))
                .andExpect(jsonPath("$.balance").value(1000.00));
    }
    
    private String generateTestToken(String username, String role) {
        return jwtUtil.generateToken(username, List.of(role));
    }
}
```

### Key Annotations

| Annotation | Purpose |
|------------|---------|
| `@SpringBootTest` | Loads full Spring application context |
| `@AutoConfigureMockMvc` | Configures MockMvc for HTTP testing |
| `@Transactional` | Rolls back database changes after each test |
| `@DirtiesContext` | Recreates context if state is modified |
| `@WithMockUser` | Mocks authenticated user (Spring Security) |
| `@BeforeEach` | Setup method run before each test |
| `@AfterEach` | Cleanup method run after each test |

---

## Database Management

### Test Database Configuration

```yaml
# application-test.yml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
    username: sa
    password: 
  jpa:
    hibernate:
      ddl-auto: create-drop
    properties:
      hibernate:
        dialect: org.hibernate.dialect.H2Dialect
```

### Database Cleanup

```java
@BeforeEach
void setUp() {
    // Delete in correct order (child tables first)
    transactionLogRepository.deleteAll();
    accountRepository.deleteAll();
    userRepository.deleteAll();
    
    // Clear caches
    rateLimitBuckets.clear();
    tokenBlacklist.clear();
}
```

### Test Data Setup

```java
private Account createTestAccount() {
    Account account = Account.builder()
            .accountNumber("ACC-" + System.currentTimeMillis())
            .accountHolder("Test User")
            .balance(new BigDecimal("1000.00"))
            .accountType("CHECKING")
            .status(AccountStatus.ACTIVE.name())
            .build();
    return accountRepository.save(account);
}

private User createTestUser(String username, String role) {
    User user = User.builder()
            .username(username)
            .password(passwordEncoder.encode("password"))
            .email(username + "@example.com")
            .role(UserRole.valueOf(role))
            .build();
    return userRepository.save(user);
}
```

---

## Security Testing

### Testing with JWT Tokens

```java
@Test
void testWithJwtToken() throws Exception {
    // Generate token
    String token = jwtUtil.generateToken("testuser", List.of("USER"));
    
    // Use token in request
    mockMvc.perform(get("/accounts")
            .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());
}
```

### Testing with Spring Security Mock

```java
@Test
@WithMockUser(username = "testuser", roles = {"USER"})
void testWithMockUser() throws Exception {
    mockMvc.perform(get("/accounts"))
            .andExpect(status().isOk());
}
```

### Testing Unauthorized Access

```java
@Test
void testUnauthorizedAccess() throws Exception {
    // No token provided
    mockMvc.perform(get("/accounts"))
            .andExpect(status().isUnauthorized()); // 401
}

@Test
void testForbiddenAccess() throws Exception {
    String userToken = generateUserToken();
    
    // USER tries to access ADMIN endpoint
    mockMvc.perform(get("/api/v1/admin/accounts/1")
            .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isForbidden()); // 403
}
```

---

## Best Practices

### 1. Test Real Interactions

Integration tests should test actual component interactions:

✅ **Good**: Test with real database, real security
```java
@SpringBootTest
@AutoConfigureMockMvc
class IntegrationTest { }
```

❌ **Bad**: Mock everything (this is a unit test)
```java
@Mock private Service service;
@Mock private Repository repository;
```

### 2. Clean State Between Tests

Always reset state in `@BeforeEach`:

```java
@BeforeEach
void setUp() {
    repository.deleteAll();
    rateLimitBuckets.clear();
    tokenBlacklist.clear();
}
```

### 3. Use Unique Test Data

Avoid hardcoded IDs and values:

✅ **Good**: Dynamic data
```java
String accountNumber = "ACC-" + System.currentTimeMillis();
String idempotencyKey = UUID.randomUUID().toString();
```

❌ **Bad**: Hardcoded values (can cause conflicts)
```java
String accountNumber = "ACC-001"; // May already exist
```

### 4. Test Complete Flows

Test end-to-end user journeys:

```java
@Test
void testCompleteTransferFlow() throws Exception {
    // 1. Login
    String token = loginAsUser();
    
    // 2. Create accounts
    Account source = createAccount(token);
    Account dest = createAccount(token);
    
    // 3. Initiate transfer
    TransferRequest request = createTransferRequest(source, dest, "100.00");
    mockMvc.perform(post("/transfers")
            .header("Authorization", "Bearer " + token)
            .content(toJson(request)))
            .andExpect(status().isCreated());
    
    // 4. Verify balances updated
    verifyBalance(source.getId(), "900.00");
    verifyBalance(dest.getId(), "1100.00");
    
    // 5. Verify transaction history
    verifyTransactionHistory(source.getId(), 1);
}
```

### 5. Test Error Scenarios

Don't just test happy paths:

```java
@Test
void testTransferWithInsufficientFunds() throws Exception {
    TransferRequest request = createTransferRequest("9999.00");
    
    mockMvc.perform(post("/transfers")
            .header("Authorization", "Bearer " + token)
            .content(toJson(request)))
            .andExpect(status().isConflict()) // 409
            .andExpect(jsonPath("$.message").value(containsString("Insufficient")));
}
```

### 6. Verify Side Effects

Check database changes, not just HTTP responses:

```java
@Test
void testAccountCreation() throws Exception {
    // Create account via API
    mockMvc.perform(post("/accounts")
            .header("Authorization", "Bearer " + token)
            .content(toJson(createAccountRequest)))
            .andExpect(status().isCreated());
    
    // Verify account exists in database
    List<Account> accounts = accountRepository.findAll();
    assertThat(accounts).hasSize(1);
    assertThat(accounts.get(0).getAccountHolder()).isEqualTo("Test User");
}
```

### 7. Keep Tests Independent

Each test should run successfully in isolation:

- Don't rely on test execution order
- Don't share mutable state between tests
- Clean up after each test

---

## Test Coverage

### Current Coverage

| Component | Integration Test Coverage |
|-----------|---------------------------|
| Authentication | ✅ JWT, Login, Logout tested |
| Authorization | ✅ USER/ADMIN roles tested |
| Rate Limiting | ✅ All endpoints tested |
| Transfers | ✅ Success and error cases |
| Account Reads | ✅ All read operations |
| Ownership | ✅ Cross-user access blocked |

**Total Integration Tests**: 12+ scenarios  
**Status**: 🟢 All Passing

---

## Troubleshooting

### Common Issues

#### 1. Database Constraint Violations

**Problem**: `ConstraintViolationException` due to duplicate data

**Solution**: Use unique test data
```java
String accountNumber = "ACC-" + System.currentTimeMillis();
```

#### 2. Rate Limit Bucket Persistence

**Problem**: Rate limits carry over between tests

**Solution**: Clear buckets in `@BeforeEach`
```java
@Autowired
private Map<String, Bucket> rateLimitBuckets;

@BeforeEach
void setUp() {
    rateLimitBuckets.clear();
}
```

#### 3. Authentication Fails in Tests

**Problem**: Token not accepted or user not found

**Solution**: Ensure user exists and token is valid
```java
// Create user first
User user = createTestUser("testuser", "USER");

// Then generate token
String token = jwtUtil.generateToken(user.getUsername(), List.of("USER"));
```

#### 4. Tests Pass Individually But Fail Together

**Problem**: Shared state or order dependency

**Solution**: Clean state in `@BeforeEach` and use `@DirtiesContext` if needed
```java
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
```

#### 5. 401 Instead of 429 (Rate Limit)

**Problem**: Authentication checked before rate limiting

**Solution**: This is correct behavior! Auth is checked first:
```java
// If token is invalid → 401 (not 429)
// If token is valid but rate limit exceeded → 429
```

---

## Running Tests with Maven

### Full Test Suite

```bash
# Unit tests only
mvn test

# Integration tests only
mvn test -Dtest="*IntegrationTest"

# All tests
mvn verify

# With coverage
mvn clean verify jacoco:report
```

### Test Results Location

```
backend/target/
├── surefire-reports/          # Test results (XML/TXT)
├── site/jacoco/               # Coverage report (HTML)
└── test-classes/              # Compiled test classes
```

---

## Additional Resources

- **Spring Boot Testing Guide**: https://spring.io/guides/gs/testing-web/
- **MockMvc Documentation**: https://docs.spring.io/spring-framework/reference/testing/spring-mvc-test-framework.html
- **Spring Security Testing**: https://docs.spring.io/spring-security/reference/servlet/test/index.html
- **Unit Testing Guide**: See [UNIT_TESTING.md](UNIT_TESTING.md)
- **Testing Strategy**: See [TESTING_STRATEGY.md](TESTING_STRATEGY.md)

---

**Last Updated**: February 13, 2026  
**Version**: 1.0.0