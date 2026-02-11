# Documentation Index

Welcome to the Money Transfer System documentation! Find what you need quickly.

## 🚀 Getting Started

| Document | Description |
|----------|-------------|
| [README.md](../README.md) | Project overview and quick start |
| [CONTRIBUTING.md](../CONTRIBUTING.md) | **Start here** for new developers - complete setup guide |
| [SETUP_CHECKLIST.md](../SETUP_CHECKLIST.md) | Step-by-step checklist for first-time setup |
| [ARCHITECTURE.md](../ARCHITECTURE.md) | System architecture diagrams and design patterns |

## 📚 API Documentation

| Document | Use When |
|----------|----------|
| [API_ENDPOINTS.md](API_ENDPOINTS.md) | You need detailed API endpoint reference with examples |
| [API_QUICK_REFERENCE.md](API_QUICK_REFERENCE.md) | You need a quick lookup of available endpoints |
| [SWAGGER_UI_GUIDE.md](SWAGGER_UI_GUIDE.md) | You're using Swagger UI for the first time |
| [SWAGGER_QUICK_REFERENCE.md](SWAGGER_QUICK_REFERENCE.md) | Quick Swagger tips and shortcuts |
| [SWAGGER_IMPLEMENTATION.md](SWAGGER_IMPLEMENTATION.md) | You want to understand how Swagger is implemented |

## 🔐 Security & Access Control

| Document | Use When |
|----------|----------|
| [RBAC_IMPLEMENTATION.md](RBAC_IMPLEMENTATION.md) | Understanding roles (USER vs ADMIN) and permissions |
| [USER_MANAGEMENT_IMPLEMENTATION.md](USER_MANAGEMENT_IMPLEMENTATION.md) | Working with user accounts and authentication |
| [USER_MANAGEMENT_QUICK_REFERENCE.md](USER_MANAGEMENT_QUICK_REFERENCE.md) | Quick user management commands |

## ⚡ Rate Limiting

| Document | Use When |
|----------|----------|
| [RATE_LIMITING_QUICK_REFERENCE.md](RATE_LIMITING_QUICK_REFERENCE.md) | Quick lookup of rate limits |
| [RATE_LIMITING_IMPLEMENTATION.md](RATE_LIMITING_IMPLEMENTATION.md) | Understanding how rate limiting works |
| [RATE_LIMITING_INTEGRATION_TESTS.md](RATE_LIMITING_INTEGRATION_TESTS.md) | Writing rate limiting tests |
| [RATE_LIMITING_TEST_REPORT.md](RATE_LIMITING_TEST_REPORT.md) | Test results and coverage |

## 🗄️ Database

| Document | Use When |
|----------|----------|
| [FLYWAY_SETUP.md](FLYWAY_SETUP.md) | Working with database migrations |
| [../database/schema.sql](../database/schema.sql) | Need complete database schema |
| [../database/seed-data.sql](../database/seed-data.sql) | Need test data |

## 🔧 Implementation & Changes

| Document | Use When |
|----------|----------|
| [CODE_CHANGES.md](CODE_CHANGES.md) | Reviewing what has been implemented |
| [FINAL_STATUS.md](FINAL_STATUS.md) | Project status and completion report |
| [STEP_6_INTEGRATION_TESTS_COMPLETE.md](STEP_6_INTEGRATION_TESTS_COMPLETE.md) | Integration test details |

## 💡 Quick Reference by Task

### I want to

#### Set up the project for the first time

1. Read [CONTRIBUTING.md](../CONTRIBUTING.md)
2. Run `./setup.sh` from project root
3. Follow [SETUP_CHECKLIST.md](../SETUP_CHECKLIST.md)

#### Understand how the system works

1. Read [ARCHITECTURE.md](../ARCHITECTURE.md) for visual overview
2. Check [RBAC_IMPLEMENTATION.md](RBAC_IMPLEMENTATION.md) for security
3. Review [API_ENDPOINTS.md](API_ENDPOINTS.md) for available operations

#### Make API calls

1. Start with [SWAGGER_UI_GUIDE.md](SWAGGER_UI_GUIDE.md)
2. Use Swagger UI at <http://localhost:8080/api/v1/swagger-ui.html>
3. Refer to [API_QUICK_REFERENCE.md](API_QUICK_REFERENCE.md) for endpoints

#### Add a new feature

1. Read [CONTRIBUTING.md](../CONTRIBUTING.md) workflow section
2. Understand [ARCHITECTURE.md](../ARCHITECTURE.md) patterns
3. Follow existing code in [CODE_CHANGES.md](CODE_CHANGES.md)

#### Work with database

1. Read [FLYWAY_SETUP.md](FLYWAY_SETUP.md) for migrations
2. Check `backend/src/main/resources/db/migration/` for examples
3. Never modify existing migrations!

#### Test the API

1. Use Swagger UI (easiest): [SWAGGER_UI_GUIDE.md](SWAGGER_UI_GUIDE.md)
2. Test credentials:
   - User: `testuser` / `password`
   - Admin: `admin` / `admin123`
3. Check rate limits: [RATE_LIMITING_QUICK_REFERENCE.md](RATE_LIMITING_QUICK_REFERENCE.md)

#### Debug issues

1. Check [CONTRIBUTING.md](../CONTRIBUTING.md) troubleshooting section
2. Review logs in `backend/logs/`
3. Verify setup with [SETUP_CHECKLIST.md](../SETUP_CHECKLIST.md)

## 📝 Documentation Standards

When adding new documentation:

- Place API-related docs in `/docs/`
- Place setup/contributing docs in project root
- Use clear, descriptive filenames
- Include a table of contents for long docs
- Add code examples where helpful
- Keep quick reference docs concise
- Link related documents
- Update this index!

## 🔗 External Resources

- **Swagger UI**: <http://localhost:8080/api/v1/swagger-ui.html> (when running)
- **Frontend**: <http://localhost:4200> (when running)
- **Spring Boot Docs**: <https://docs.spring.io/spring-boot/docs/current/reference/html/>
- **Angular Docs**: <https://angular.io/docs>
- **Flyway Docs**: <https://flywaydb.org/documentation/>

---

**Can't find what you're looking for?**

1. Search the repository: `git grep "search term"`
2. Check [CONTRIBUTING.md](../CONTRIBUTING.md) FAQ section
3. Review [README.md](../README.md) for overview
4. Look at code examples in `/backend/src/` and `/frontend/src/`
