# API Quick Reference

## Endpoint Summary

```
┌──────────────────────────── PUBLIC ────────────────────────────┐
│ POST   /auth/login                    - Login (no auth needed)  │
│ POST   /users/forgot-password         - Password reset request │
│ POST   /users/reset-password          - Reset with token       │
│ GET    /swagger-ui.html               - API Documentation      │
│ GET    /v3/api-docs                   - OpenAPI Spec           │
└────────────────────────────────────────────────────────────────┘

┌──────────────────── AUTHENTICATED ENDPOINTS (USER/ADMIN) ────────────────┐
│ POST   /auth/logout                   - Logout and blacklist token       │
│ GET    /accounts                      - List my accounts                 │
│ POST   /accounts                      - Create new account               │
│ GET    /accounts/{id}                 - Account details                  │
│ GET    /accounts/{id}/balance         - Account balance                 │
│ GET    /accounts/{id}/transactions    - Transaction history             │
│ POST   /transfers                     - Initiate transfer               │
│ GET    /transfers/health              - Health check                    │
│ GET    /users/{username}              - Get user profile                │
└──────────────────────────────────────────────────────────────────────────┘

┌──────────────────────── ADMIN ENDPOINTS (ADMIN ONLY) ──────────────────┐
│ POST   /users/register                             - Register new user  │
│ GET    /users                                      - List all users     │
│ GET    /admin/accounts/{id}                       - View any account   │
│ GET    /admin/accounts/{id}/balance              - View any balance   │
│ GET    /admin/accounts/{id}/transactions         - View any history   │
│ POST   /admin/users/{userId}/accounts            - Create user account │
│ GET    /admin/health                              - Admin health check │
└─────────────────────────────────────────────────────────────────────────┘
```

**Total**: 21 endpoints (5 public + 9 authenticated + 7 admin)

---

## Authentication

### Login
```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password"
  }'
```

**Response**:
```json
{
  "token": "eyJhbGc...",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

### Using Token
```bash
curl -X GET http://localhost:8080/accounts \
  -H "Authorization: Bearer {TOKEN}"
```

### Logout
```bash
curl -X POST http://localhost:8080/auth/logout \
  -H "Authorization: Bearer {TOKEN}"
```

---

## User Management

### Register User (ADMIN Only)
```bash
curl -X POST http://localhost:8080/users/register \
  -H "Authorization: Bearer {ADMIN_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "newuser",
    "password": "password123",
    "email": "newuser@example.com"
  }'
```

### Get User Profile
```bash
curl -X GET http://localhost:8080/users/testuser \
  -H "Authorization: Bearer {TOKEN}"
```

### List All Users (ADMIN Only)
```bash
curl -X GET http://localhost:8080/users \
  -H "Authorization: Bearer {ADMIN_TOKEN}"
```

### Forgot Password
```bash
curl -X POST http://localhost:8080/users/forgot-password \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com"
  }'
```

### Reset Password
```bash
curl -X POST http://localhost:8080/users/reset-password \
  -H "Content-Type: application/json" \
  -d '{
    "token": "reset-token-from-email",
    "newPassword": "newPassword123"
  }'
```

---

## Test Credentials

| Username | Password | Role |
|----------|----------|------|
| testuser | password | USER |
| admin    | admin123 | ADMIN |

---

## Account Endpoints

### List My Accounts
```
GET /accounts
Authorization: Bearer {TOKEN}
```

### Create Account
```bash
curl -X POST http://localhost:8080/accounts \
  -H "Authorization: Bearer {TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "accountNumber": "ACC-003",
    "accountHolder": "John Doe",
    "initialBalance": 1000.00,
    "accountType": "CHECKING"
  }'
```

### Account Details
```
GET /accounts/{accountId}
Authorization: Bearer {TOKEN}
```

### Account Balance
```
GET /accounts/{accountId}/balance
Authorization: Bearer {TOKEN}
```

### Transaction History
```
GET /accounts/{accountId}/transactions
Authorization: Bearer {TOKEN}
```

---

## Money Transfers

### Initiate Transfer
```
POST /transfers
Authorization: Bearer {TOKEN}
Content-Type: application/json

{
  "sourceAccountId": 1001,
  "destinationAccountId": 1002,
  "amount": 100.00,
  "idempotencyKey": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Response (201)**:
```json
{
  "transactionId": 245,
  "status": "SUCCESS",
  "sourceAccountId": 1001,
  "destinationAccountId": 1002,
  "amount": 100.00,
  "timestamp": "2026-02-05T15:31:45Z"
}
```

---

## Admin Endpoints

### Get Any Account (ADMIN Only)
```
GET /api/v1/admin/accounts/{accountId}
Authorization: Bearer {ADMIN_TOKEN}
```

### Get Any Account Balance (ADMIN Only)
```
GET /api/v1/admin/accounts/{accountId}/balance
Authorization: Bearer {ADMIN_TOKEN}
```

### Get Any Account Transactions (ADMIN Only)
```
GET /api/v1/admin/accounts/{accountId}/transactions
Authorization: Bearer {ADMIN_TOKEN}
```

### Create Account for User (ADMIN Only)
```bash
curl -X POST http://localhost:8080/admin/users/5/accounts \
  -H "Authorization: Bearer {ADMIN_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "accountNumber": "ACC-005",
    "accountHolder": "Jane Smith",
    "initialBalance": 2000.00,
    "accountType": "SAVINGS"
  }'
```

---

## Rate Limits

| Operation | Limit | Per |
|-----------|-------|-----|
| Login | 5 | minute |
| Read (accounts, transactions, balance) | 60 | minute |
| Transfer | 10 | minute |

**Response (429)**:
```json
{
  "status": "RATE_LIMITED",
  "description": "Transfer rate limit exceeded"
}
```

---

## Common Status Codes

| Code | Meaning |
|------|---------|
| 200 | Success (GET) |
| 201 | Created (POST transfer) |
| 400 | Invalid input |
| 401 | Unauthorized (no token) |
| 403 | Forbidden (wrong role) |
| 404 | Not found |
| 409 | Conflict (insufficient funds) |
| 429 | Rate limit exceeded |
| 500 | Server error |

---

## Error Example

```json
{
  "status": 400,
  "message": "Validation failed: idempotencyKey - Idempotency key must be a valid UUID",
  "error": "Validation Error",
  "timestamp": "2026-02-05T15:31:45Z",
  "path": "/transfers"
}
```

---

## Roles

### USER
- ✅ View own accounts
- ✅ Create own accounts
- ✅ Initiate transfers
- ✅ View own profile
- ❌ Cannot access admin endpoints
- ❌ Cannot view other users

### ADMIN
- ✅ View any account
- ✅ Create accounts for users
- ✅ View all users
- ✅ Register new users
- ❌ Cannot transfer money (no user impersonation)

---

## Token Details

```json
{
  "sub": "username",
  "roles": ["USER"],
  "iss": "money-transfer-system",
  "exp": 1675535445
}
```

- Token expires in **1 hour**
- Include in all requests: `Authorization: Bearer {TOKEN}`

---

## Useful Links../SECURITY/RBAC_IMPLEMENTATION.md](../SECURITY/RBAC_IMPLEMENTATION.md)
- **User Management**: [../SECURITY/USER_MANAGEMENT_QUICK_REFERENCE.md](../SECURITY/USER_MANAGEMENT_QUICK_REFERENCE.md)

---

*Last Updated: February 13//localhost:8080/v3/api-docs
- **RBAC Guide**: [RBAC_IMPLEMENTATION.md](RBAC_IMPLEMENTATION.md)

---

*Last Updated: February 5, 2026*
