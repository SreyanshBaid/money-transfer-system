# Unit Testing Guide

## Overview

This document provides comprehensive guidance for writing and running unit tests in the Money Transfer System. Unit tests focus on testing individual components (services, controllers, utilities) in isolation using mocks and stubs.

**Testing Framework**: JUnit 5 (Jupiter)  
**Mocking Framework**: Mockito  
**Assertion Library**: AssertJ  
**Java Version**: Java 17  
**Build Tool**: Maven

---

## Table of Contents

1. [Test Structure](#test-structure)
2. [Running Unit Tests](#running-unit-tests)
3. [Writing Unit Tests](#writing-unit-tests)
4. [Test Categories](#test-categories)
5. [Best Practices](#best-practices)
6. [Code Coverage](#code-coverage)
7. [Troubleshooting](#troubleshooting)

---

## Test Structure

### Directory Layout

```
backend/src/test/java/com/moneytransfer/
├── controller/              # Controller layer tests
│   ├── AuthControllerTest.java
│   └── TransferControllerTest.java
├── service/                 # Service layer tests
│   ├── TransferServiceTest.java
│   ├── TokenBlacklistServiceTest.java
│   └── OwnershipServiceTest.java
├── analytics/               # Analytics component tests
│   └── exporter/
│       └── TransactionExportServiceTest.java
└── integration/             # Integration tests (separate from unit tests)
```

### Test File Naming Convention

- **Pattern**: `{ClassName}Test.java`
- **Examples**: 
  - `TransferService.java` → `TransferServiceTest.java`
  - `AuthController.java` → `AuthControllerTest.java`

---

## Running Unit Tests

### Run All Unit Tests

```bash
cd backend
mvn test
```

### Run Specific Test Class

```bash
mvn test -Dtest=TransferServiceTest
```

### Run Specific Test Method

```bash
mvn test -Dtest=TransferServiceTest#testSuccessfulTransfer
```

### Run with Coverage Report

```bash
mvn clean test jacoco:report
# View report at: target/site/jacoco/index.html
```

### Run Tests in IDE

**IntelliJ IDEA**:
- Right-click on test class → "Run 'ClassNameTest'"
- Right-click on test method → "Run 'testMethodName()'"
- Use `Ctrl+Shift+F10` (Windows/Linux) to run test at cursor

**VS Code**:
- Install "Test Runner for Java" extension
- Click the "Run Test" button above test methods
- View results in "Test" sidebar

---

## Writing Unit Tests

### Basic Test Structure

```java
@ExtendWith(MockitoExtension.class)
class ServiceNameTest {

    @Mock
    private DependencyRepository repository;
    
    @InjectMocks
    private ServiceName service;
    
    @Captor
    private ArgumentCaptor<EntityType> captor;
    
    @BeforeEach
    void setUp() {
        // Test setup code
    }
    
    @Test
    @DisplayName("✅ Description of what this test verifies")
    void testMethodName() {
        // Arrange
        // ... setup test data
        
        // Act
        // ... call method under test
        
        // Assert
        // ... verify results
    }
}
```

### Key Annotations

| Annotation | Purpose |
|------------|---------|
| `@ExtendWith(MockitoExtension.class)` | Enables Mockito integration with JUnit 5 |
| `@Mock` | Creates a mock instance of a dependency |
| `@InjectMocks` | Creates an instance and injects mocks into it |
| `@Captor` | Captures arguments passed to mocked methods |
| `@BeforeEach` | Runs before each test method |
| `@AfterEach` | Runs after each test method |
| `@Test` | Marks a method as a test |
| `@DisplayName` | Provides a readable test name |
| `@Disabled` | Temporarily disables a test |

---

## Test Categories

### 1. Service Layer Tests

**Example**: `TransferServiceTest.java`

Service tests verify business logic in isolation by mocking repository and utility dependencies.

**What to Test**:
- ✅ Successful business operations
- ✅ Validation logic (null checks, business rules)
- ✅ Error handling and exception throwing
- ✅ State changes and side effects
- ✅ Interaction with dependencies (using Mockito.verify)

**Example Test**:

```java
@Test
@DisplayName("✅ Transfer succeeds with valid accounts and sufficient balance")
void testSuccessfulTransfer() {
    // Arrange
    Account sourceAccount = Account.builder()
            .id(1L)
            .balance(new BigDecimal("1000.00"))
            .status(AccountStatus.ACTIVE.name())
            .build();
    
    Account destAccount = Account.builder()
            .id(2L)
            .balance(new BigDecimal("500.00"))
            .status(AccountStatus.ACTIVE.name())
            .build();
    
    when(accountRepository.findById(1L)).thenReturn(Optional.of(sourceAccount));
    when(accountRepository.findById(2L)).thenReturn(Optional.of(destAccount));
    
    TransferRequest request = TransferRequest.builder()
            .sourceAccountId(1L)
            .destinationAccountId(2L)
            .amount(new BigDecimal("100.00"))
            .idempotencyKey("550e8400-e29b-41d4-a716-446655440000")
            .build();
    
    // Act
    TransferResponse response = transferService.transfer(request);
    
    // Assert
    assertThat(response.getStatus()).isEqualTo("SUCCESS");
    assertThat(response.getAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
    verify(transactionLogRepository, times(2)).save(any(TransactionLog.class));
    verify(accountRepository, times(1)).save(sourceAccount);
    verify(accountRepository, times(1)).save(destAccount);
}
```

**Key Patterns**:
- Mock repository responses with `when().thenReturn()`
- Verify method calls with `verify()`
- Use `ArgumentCaptor` to inspect saved entities
- Test exceptions with `assertThatThrownBy()`

### 2. Controller Layer Tests

**Example**: `AuthControllerTest.java`, `TransferControllerTest.java`

Controller tests verify HTTP request/response handling, validation, and error responses.

**What to Test**:
- ✅ Request mapping and routing
- ✅ Input validation (invalid JSON, missing fields)
- ✅ Response status codes (200, 400, 401, 404, etc.)
- ✅ Response body structure and content
- ✅ Error handling and exception mapping

**Example Test**:

```java
@Test
@DisplayName("✅ Login returns token on valid credentials")
void testLoginSuccess() throws Exception {
    // Arrange
    LoginRequest request = new LoginRequest("testuser", "password");
    LoginResponse expectedResponse = new LoginResponse("jwt-token", "Bearer", 3600L);
    
    when(authService.login(any(LoginRequest.class))).thenReturn(expectedResponse);
    
    // Act & Assert
    mockMvc.perform(post("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").value("jwt-token"))
            .andExpect(jsonPath("$.tokenType").value("Bearer"));
}
```

### 3. Entity Tests

Tests for entity business logic methods (e.g., `Account.debit()`, `Account.credit()`).

**What to Test**:
- ✅ State changes in entity methods
- ✅ Validation logic within entities
- ✅ Business rules enforcement
- ✅ Edge cases and boundary conditions

**Example Test**:

```java
@Test
@DisplayName("✅ Entity: Account.debit() reduces balance correctly")
void testAccountDebitReducesBalance() {
    Account account = Account.builder()
            .balance(new BigDecimal("500.00"))
            .status(AccountStatus.ACTIVE.name())
            .build();
    
    account.debit(new BigDecimal("100.00"));
    
    assertThat(account.getBalance()).isEqualTo(new BigDecimal("400.00"));
}

@Test
@DisplayName("✅ Entity: Account.debit() fails if insufficient balance")
void testAccountDebitFailsWithInsufficientBalance() {
    Account account = Account.builder()
            .balance(new BigDecimal("50.00"))
            .status(AccountStatus.ACTIVE.name())
            .build();
    
    assertThatThrownBy(() -> account.debit(new BigDecimal("100.00")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Insufficient balance");
}
```

### 4. Utility/Helper Tests

**Example**: `TokenBlacklistServiceTest.java`, `OwnershipServiceTest.java`

Tests for utility classes, helpers, and supporting services.

**What to Test**:
- ✅ Algorithm correctness
- ✅ Data transformations
- ✅ Caching behavior
- ✅ Cleanup operations

---

## Best Practices

### 1. Follow AAA Pattern

Structure tests with **Arrange**, **Act**, **Assert**:

```java
@Test
void testExample() {
    // Arrange: Set up test data and mocks
    Account account = createTestAccount();
    when(repository.findById(1L)).thenReturn(Optional.of(account));
    
    // Act: Execute the method under test
    AccountResponse response = service.getAccount(1L);
    
    // Assert: Verify the results
    assertThat(response.getId()).isEqualTo(1L);
    assertThat(response.getBalance()).isEqualTo(new BigDecimal("1000.00"));
}
```

### 2. Use Descriptive Test Names

✅ **Good**:
```java
@DisplayName("✅ Transfer fails when source account has insufficient balance")
void testTransferFailsWithInsufficientBalance() { }
```

❌ **Bad**:
```java
@Test
void test1() { }
```

### 3. Test One Thing Per Test

Each test should verify a single behavior or scenario.

✅ **Good**: Separate tests for different error cases
```java
@Test void testTransferFailsWithInsufficientBalance() { }
@Test void testTransferFailsWithInactiveAccount() { }
@Test void testTransferFailsWithNonExistentAccount() { }
```

❌ **Bad**: One test for multiple scenarios
```java
@Test void testAllTransferFailures() { } // Tests multiple unrelated failures
```

### 4. Mock External Dependencies

Unit tests should isolate the component under test:

```java
@Mock
private AccountRepository accountRepository;

@Mock
private TransactionLogRepository transactionLogRepository;

@InjectMocks
private TransferService transferService;
```

### 5. Verify Interactions

Use `verify()` to ensure methods are called correctly:

```java
verify(repository).save(any(Account.class));
verify(repository, times(2)).findById(anyLong());
verify(repository, never()).delete(any());
```

### 6. Use AssertJ for Assertions

AssertJ provides fluent, readable assertions:

```java
// AssertJ (preferred)
assertThat(account.getBalance())
    .isEqualByComparingTo(new BigDecimal("1000.00"));

// JUnit assertions (less readable)
assertEquals(new BigDecimal("1000.00"), account.getBalance());
```

### 7. Test Edge Cases

- Null values
- Empty collections
- Boundary values (0, negative numbers, max values)
- Invalid states

```java
@Test
void testDebitFailsWithNegativeAmount() {
    assertThatThrownBy(() -> account.debit(new BigDecimal("-10")))
            .isInstanceOf(IllegalArgumentException.class);
}
```

### 8. Keep Tests Fast

- Avoid Thread.sleep()
- Don't connect to real databases
- Mock time-dependent operations
- Use in-memory implementations when needed

---

## Code Coverage

### Current Coverage Status

| Component | Tests | Coverage |
|-----------|-------|----------|
| Service Layer | 19 unit tests | High |
| Controller Layer | Covered via integration | Medium |
| Entities | Embedded in service tests | High |
| Utilities | 100% | High |

### Viewing Coverage Reports

```bash
mvn clean test jacoco:report
open target/site/jacoco/index.html
```

### Coverage Goals

- **Service Layer**: 80%+ line coverage
- **Critical Business Logic**: 90%+ line coverage
- **Controller Layer**: Primarily integration tested
- **Edge Cases**: All error paths covered

---

## Troubleshooting

### Common Issues

#### 1. Mock Not Returning Expected Value

**Problem**: Mock returns null instead of expected object

**Solution**: Ensure `when()` setup matches actual call:
```java
// Setup
when(repository.findById(1L)).thenReturn(Optional.of(account));

// Verify the call signature matches
AccountResponse response = service.getAccount(1L); // Must use 1L, not 1
```

#### 2. Verification Fails

**Problem**: `verify()` fails even though method should be called

**Solution**: Check argument matchers:
```java
// Use exact values or matchers consistently
verify(repository).save(eq(account)); // Exact match
verify(repository).save(any(Account.class)); // Any instance
```

#### 3. Tests Pass Individually But Fail Together

**Problem**: Test state leaks between tests

**Solution**: Use `@BeforeEach` to reset state:
```java
@BeforeEach
void setUp() {
    // Reset mocks or recreate instances
    Mockito.reset(repository);
}
```

#### 4. NullPointerException in Tests

**Problem**: Dependency not injected properly

**Solution**: Ensure proper annotations:
```java
@Mock
private DependencyClass dependency; // Needs @Mock

@InjectMocks
private ServiceClass service; // Needs @InjectMocks
```

---

## Example Test Classes

### Service Test Template

```java
package com.moneytransfer.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ServiceName Unit Tests")
class ServiceNameTest {

    @Mock
    private DependencyRepository repository;
    
    @InjectMocks
    private ServiceName service;
    
    private TestEntity testEntity;
    
    @BeforeEach
    void setUp() {
        testEntity = createTestEntity();
    }
    
    @Test
    @DisplayName("✅ Should perform operation successfully")
    void testSuccessfulOperation() {
        // Arrange
        when(repository.findById(anyLong())).thenReturn(Optional.of(testEntity));
        
        // Act
        Result result = service.performOperation(1L);
        
        // Assert
        assertThat(result).isNotNull();
        verify(repository).findById(1L);
    }
    
    @Test
    @DisplayName("❌ Should throw exception when entity not found")
    void testOperationFailsWhenNotFound() {
        // Arrange
        when(repository.findById(anyLong())).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThatThrownBy(() -> service.performOperation(1L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("not found");
    }
    
    private TestEntity createTestEntity() {
        return TestEntity.builder()
                .id(1L)
                .name("Test")
                .build();
    }
}
```

---

## Additional Resources

- **JUnit 5 Documentation**: https://junit.org/junit5/docs/current/user-guide/
- **Mockito Documentation**: https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html
- **AssertJ Documentation**: https://assertj.github.io/doc/
- **Integration Testing**: See [INTEGRATION_TESTING.md](INTEGRATION_TESTING.md)
- **Testing Strategy**: See [TESTING_STRATEGY.md](TESTING_STRATEGY.md)

---

**Last Updated**: February 13, 2026  
**Version**: 1.0.0