# System Architecture

Visual overview of the Money Transfer System architecture.

## High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         CLIENT LAYER                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Angular 21 Frontend (Port 4200)                                │
│  ├── Auth Module (Login/Register)                               │
│  ├── Dashboard Module (Overview, Accounts)                      │
│  ├── Transfer Module (Money Transfers)                          │
│  └── HTTP Interceptors (JWT Token Management)                   │
│                                                                  │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             │ HTTP/REST + JWT
                             │
┌────────────────────────────▼────────────────────────────────────┐
│                      API GATEWAY LAYER                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Spring Boot Backend (Port 8080)                                │
│  ├── Security Filter Chain                                      │
│  │   ├── JWT Authentication Filter                              │
│  │   ├── CORS Configuration                                     │
│  │   └── Rate Limiting Filter                                   │
│  │                                                               │
│  ├── Controllers (REST Endpoints)                               │
│  │   ├── /api/v1/auth/** (Public)                               │
│  │   ├── /api/v1/accounts/** (USER, ADMIN)                      │
│  │   ├── /api/v1/transfers/** (USER)                            │
│  │   └── /api/v1/admin/** (ADMIN only)                          │
│  │                                                               │
│  └── Swagger/OpenAPI Documentation                              │
│                                                                  │
└────────────────────────────┬────────────────────────────────────┘
                             │
┌────────────────────────────▼────────────────────────────────────┐
│                      BUSINESS LOGIC LAYER                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Service Layer                                                   │
│  ├── AccountService (Balance, History)                          │
│  ├── TransferService (Transfer Logic)                           │
│  ├── AuthService (Login, JWT Generation)                        │
│  └── UserService (User Management)                              │
│                                                                  │
│  Business Rules:                                                 │
│  ├── Transaction Validation                                     │
│  ├── Sufficient Balance Check                                   │
│  ├── Idempotency Key Enforcement                                │
│  └── Audit Logging                                              │
│                                                                  │
└────────────────────────────┬────────────────────────────────────┘
                             │
┌────────────────────────────▼────────────────────────────────────┐
│                       DATA ACCESS LAYER                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  JPA Repositories                                                │
│  ├── AccountRepository                                           │
│  ├── TransactionLogRepository                                   │
│  └── UserRepository                                              │
│                                                                  │
│  Domain Entities:                                                │
│  ├── Account (id, accountType, balance, ownerId)                │
│  ├── TransactionLog (id, fromAccount, toAccount, amount)        │
│  └── User (id, username, password, roles)                       │
│                                                                  │
└────────────────────────────┬────────────────────────────────────┘
                             │
┌────────────────────────────▼────────────────────────────────────┐
│                        DATABASE LAYER                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  MySQL 8.0 (money_transfer_db)                                  │
│  ├── accounts                                                    │
│  ├── transaction_logs                                            │
│  ├── users                                                       │
│  └── flyway_schema_history (migrations)                         │
│                                                                  │
│  Managed by Flyway Migrations                                    │
│  Location: backend/src/main/resources/db/migration/             │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

## Authentication Flow

```
┌──────────┐                                    ┌──────────┐
│          │  1. POST /api/v1/auth/login        │          │
│  Client  │──────────────────────────────────▶ │ Backend  │
│          │     {username, password}            │          │
└──────────┘                                    └────┬─────┘
                                                     │
                                                     │ 2. Validate
                                                     │    credentials
                                                     │
                                                ┌────▼─────┐
                                                │ Database │
                                                └────┬─────┘
                                                     │
┌──────────┐                                    ┌────▼─────┐
│          │  3. JWT Token + User Info          │          │
│  Client  │◀────────────────────────────────── │ Backend  │
│          │     {token, username, roles}        │          │
└────┬─────┘                                    └──────────┘
     │
     │ 4. Store token
     │    in memory/storage
     │
┌────▼─────┐
│          │  5. Subsequent requests            ┌──────────┐
│  Client  │──────────────────────────────────▶ │ Backend  │
│          │  Header: Authorization: Bearer JWT │          │
└──────────┘                                    └──────────┘
```

## Transfer Flow

```
┌──────────┐                                    ┌──────────┐
│          │  1. POST /api/v1/transfers         │          │
│  Client  │──────────────────────────────────▶ │ Backend  │
│          │  + JWT Token                        │          │
└──────────┘  + Transfer details                └────┬─────┘
              + Idempotency key                      │
                                                     │
                                                     │ 2. Validate JWT
                                                     │    Check USER role
                                                     │
                                                ┌────▼─────────┐
                                                │ Service      │
                                                │ Layer        │
                                                └────┬─────────┘
                                                     │
                                                     │ 3. Business
                                                     │    Validation:
                                                     │    - Balance check
                                                     │    - Idempotency
                                                     │
                                                ┌────▼─────────┐
                                                │ @Transactional
                                                │ BEGIN         │
                                                └────┬─────────┘
                                                     │
                                                     │ 4. SQL:
                                                     │    - Debit from
                                                     │    - Credit to
                                                     │    - Log transaction
                                                     │
                                                ┌────▼─────────┐
                                                │ Database     │
                                                │ (locked rows)│
                                                └────┬─────────┘
                                                     │
                                                ┌────▼─────────┐
                                                │ COMMIT       │
                                                └────┬─────────┘
                                                     │
┌──────────┐                                    ┌────▼─────┐
│          │  5. Success Response               │          │
│  Client  │◀────────────────────────────────── │ Backend  │
│          │  {transactionId, status}            │          │
└──────────┘                                    └──────────┘
```

## Role-Based Access Control (RBAC)

```
                        ┌──────────────────┐
                        │   JwtAuthFilter  │
                        │  (Validates JWT) │
                        └────────┬─────────┘
                                 │
                                 │ Extract roles
                                 │
                        ┌────────▼─────────┐
                        │ Security Context │
                        │  UserDetails     │
                        │  + Authorities   │
                        └────────┬─────────┘
                                 │
                  ┌──────────────┴──────────────┐
                  │                             │
         ┌────────▼────────┐         ┌─────────▼────────┐
         │   ROLE_USER     │         │   ROLE_ADMIN     │
         └────────┬────────┘         └─────────┬────────┘
                  │                             │
    ┌─────────────┼─────────────┐   ┌──────────┼──────────────┐
    │             │             │   │          │              │
    ▼             ▼             ▼   ▼          ▼              ▼
/transfers    /accounts/me  View own  /admin/**  View any   Admin
 (POST)        (GET)        history    (GET)    account    operations
```

## Data Flow Example: Get Balance

```
Frontend Component
    │
    │ 1. accountService.getBalance(accountId)
    │
    ▼
HTTP Interceptor
    │
    │ 2. Add: Authorization: Bearer {jwt}
    │
    ▼
Backend Security Filter
    │
    │ 3. Validate JWT
    │ 4. Extract user + roles
    │
    ▼
AccountController
    │
    │ 5. @PreAuthorize check
    │ 6. Verify ownership OR admin
    │
    ▼
AccountService
    │
    │ 7. Business logic
    │
    ▼
AccountRepository (JPA)
    │
    │ 8. SELECT balance FROM accounts WHERE id = ?
    │
    ▼
MySQL Database
    │
    │ 9. Return balance
    │
    ▼
← Response flows back through layers
```

## File Organization

```
backend/
├── config/         # Security, CORS, Swagger config
├── controller/     # REST endpoints
├── service/        # Business logic
├── repository/     # Data access
├── domain/         # JPA entities
├── dto/            # Request/Response objects
├── security/       # JWT utilities
└── util/           # Helper classes

frontend/
├── core/
│   ├── auth/       # Auth service, guards
│   └── interceptors/ # HTTP interceptors
├── features/
│   ├── auth/       # Login/register pages
│   ├── dashboard/  # Dashboard pages
│   └── transfer/   # Transfer pages
└── shared/
    ├── components/ # Reusable UI components
    └── pipes/      # Data transformation
```

## Technology Stack

### Backend

- **Framework**: Spring Boot 3.2.2
- **Security**: Spring Security + JWT
- **Database**: MySQL 8.0
- **ORM**: JPA/Hibernate
- **Migrations**: Flyway
- **API Docs**: SpringDoc OpenAPI 3 (Swagger)
- **Testing**: JUnit 5, Testcontainers

### Frontend

- **Framework**: Angular 21
- **Architecture**: Standalone components
- **State**: Signals (reactive)
- **Styling**: Tailwind CSS
- **Icons**: Lucide Icons
- **HTTP**: HttpClient with interceptors
- **Forms**: Reactive Forms

### Infrastructure

- **Build**: Maven (backend), npm (frontend)
- **Java**: 17+
- **Node.js**: 20+
- **Database**: MySQL 8.0+

---

## Key Design Patterns

1. **Repository Pattern**: Data access abstraction (JPA repositories)
2. **Service Layer Pattern**: Business logic separation
3. **DTO Pattern**: Request/response transformation
4. **Filter Chain Pattern**: Security & request processing
5. **Interceptor Pattern**: HTTP request/response handling (frontend)
6. **Guard Pattern**: Route protection (frontend)
7. **Singleton Services**: Angular services (dependency injection)

## Security Layers

1. **Transport**: CORS, HTTPS-ready
2. **Authentication**: JWT tokens
3. **Authorization**: Role-based (@PreAuthorize)
4. **Rate Limiting**: Bucket4j per-user limits
5. **Input Validation**: @Valid, JSR-303
6. **SQL Injection Prevention**: JPA/Hibernate parameterized queries
7. **Password Security**: BCrypt hashing

---

For implementation details, see:

- [CONTRIBUTING.md](CONTRIBUTING.md)
- [docs/RBAC_IMPLEMENTATION.md](docs/RBAC_IMPLEMENTATION.md)
- [docs/API_ENDPOINTS.md](docs/API_ENDPOINTS.md)
