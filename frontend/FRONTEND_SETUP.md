# Money Transfer System - Frontend

Modern Angular 21 frontend for the Money Transfer System with JWT authentication, dashboard, and transfer management.

## 🚀 Quick Start

### Prerequisites
- Node.js 20+
- npm 11.6.2+
- Backend running on `http://localhost:8080`

### Installation

```bash
cd frontend
npm install
```

### Development Server

```bash
npm run start
```

Navigate to `http://localhost:4200/`. The application will automatically reload if you change any of the source files.

### Build

```bash
npm run build
```

The build artifacts will be stored in the `dist/` directory.

### Production Deployment

```bash
npm run build

# Start with SSR
npm run serve:ssr:frontend
```

---

## 📚 Architecture Overview

### Modern Angular Stack
- **Framework**: Angular 21.1.0 (Standalone Components)
- **Build Tool**: Angular CLI 21.1.3
- **State Management**: RxJS Observables
- **Forms**: Reactive Forms with Validation
- **Styling**: CSS with responsive design
- **SSR**: Server-Side Rendering support included

### Project Structure

```
src/app/
├── core/                    # Singleton services
│   ├── auth/               # Authentication services
│   └── interceptors/       # HTTP interceptors
├── shared/                 # Reusable components & pipes
│   ├── components/
│   └── pipes/
├── features/               # Feature modules
│   ├── auth/              # Login feature
│   ├── dashboard/         # Dashboard & history
│   └── transfer/          # Transfer feature
├── app.ts                 # Root component
├── app.routes.ts          # Main routing
└── app.config.ts          # Application config
```

---

## 🔐 Authentication

### Login Flow

1. User enters credentials (username/password)
2. Frontend calls `POST /api/auth/login`
3. Backend returns JWT token
4. Token stored in localStorage
5. Token automatically attached to all requests via interceptor

### Protected Routes

All feature routes are protected with `authGuard`:
- `/dashboard` - Account overview
- `/transfer` - Send money
- `/history` - Transaction history

Unauthorized access redirects to login.

### Demo Credentials

```
Username: testuser
Password: password123
```

---

## 🎯 Features

### 1. **Login Page**
- Username/password validation
- Reactive form with error messages
- Loading state feedback
- Auto-redirect to dashboard on success

### 2. **Dashboard**
- Welcome message with username
- Current account balance display
- Navigation to transfer and history
- Logout button
- Error handling

### 3. **Transfer Money**
- Recipient account ID input
- Amount input with currency symbol
- Optional description field
- Form validation
- Success/error feedback
- Auto-redirect after success

### 4. **Transaction History**
- Table with transaction details
- Date formatting (readable format)
- Currency formatting (USD)
- Transaction type badges (DEBIT/CREDIT)
- Status indicators (SUCCESS/PENDING/FAILED)
- Color-coded display

---

## 🔧 API Integration

The frontend expects the following backend endpoints:

### Authentication
```
POST /api/auth/login
Request:  { username: string, password: string }
Response: { token: string, user: User }
```

### Account Operations
```
GET /api/accounts/current
Response: { id, balance, currency, lastUpdated }

GET /api/accounts/current/balance
Response: number

GET /api/accounts/current/transactions?limit=20
Response: Transaction[]
```

### Transfers
```
POST /api/transfers
Request:  { toAccountId: string, amount: number, description?: string }
Response: { id, status, amount, timestamp, message? }
```

---

## 📦 Key Dependencies

- `@angular/core` - Core framework
- `@angular/common` - Common utilities
- `@angular/forms` - Reactive forms
- `@angular/platform-browser` - Browser platform
- `@angular/router` - Routing
- `rxjs` - Reactive programming
- `express` - SSR server

---

## 🧪 Development Commands

```bash
# Start dev server
npm run start

# Build for production
npm run build

# Run tests
npm run test

# Build with SSR
npm run build

# Start SSR server
npm run serve:ssr:frontend

# Run linting (if configured)
npm run lint
```

---

## 🎨 Styling

### Design System
- **Colors**: 
  - Primary: `#667eea` (Purple)
  - Secondary: `#764ba2` (Dark Purple)
  - Error: `#dc3545` (Red)
  - Success: `#155724` (Green)

- **Layout**: CSS Grid and Flexbox
- **Responsive**: Mobile-first breakpoints at 600px, 768px
- **Fonts**: System font stack for optimal performance

### Component Styles
Each component has scoped CSS with:
- Consistent spacing
- Responsive breakpoints
- Accessible color contrast
- Smooth transitions

---

## 🔒 Security Features

- ✅ JWT token-based authentication
- ✅ HTTP interceptor for automatic token injection
- ✅ Secure token storage with SSR guards
- ✅ Route protection with auth guards
- ✅ HTTPS recommended for production
- ✅ Secure localStorage access (SSR-aware)

---

## 📱 Responsive Design

The application is fully responsive:
- Mobile: < 600px
- Tablet: 600px - 1000px
- Desktop: > 1000px

All components adapt to screen size with proper spacing and layout adjustments.

---

## 🐛 Error Handling

- HTTP errors are caught and displayed to users
- Form validation errors shown inline
- Loading states prevent multiple submissions
- Network errors handled gracefully
- Proper error messages guide users

---

## 🚀 Performance

- Angular SSR support for fast initial load
- Lazy loading ready for feature modules
- Optimized CSS (no unused styles)
- Efficient change detection with OnPush
- Tree-shaking enabled in production
- Gzip compression on bundled assets

---

## 📋 Form Validation

### Login Form
- Username: Required, minimum 3 characters
- Password: Required, minimum 6 characters

### Transfer Form
- Recipient Account ID: Required
- Amount: Required, must be > 0
- Description: Optional, max 255 characters

Real-time validation with user feedback.

---

## 🔄 State Management

The application uses RxJS for reactive state:
- `AuthService` maintains auth state via BehaviorSubject
- Components subscribe to `auth$` Observable
- No external state library needed (lightweight)
- Type-safe state interface

---

## 🌐 Browser Support

- Chrome/Edge: Latest 2 versions
- Firefox: Latest 2 versions
- Safari: Latest 2 versions
- Mobile browsers: iOS Safari 12+, Chrome Android latest

---

## 📝 Environment Configuration

Update the API URL in services before deployment:

**token.service.ts, auth.service.ts, etc.**
```typescript
private apiUrl = 'http://localhost:8080/api';  // Change for production
```

For production, use environment-specific configurations.

---

## 🚢 Deployment

### Requirements
- Node.js 20+
- npm 11.6.2+
- Backend API running

### Steps
1. Build: `npm run build`
2. Deploy `dist/frontend` directory
3. Configure reverse proxy to forward API calls to backend
4. Enable HTTPS
5. Set backend URL in configuration

### Docker Deployment
The frontend is SSR-ready for containerization:
- Build output includes server bundles
- Run with `npm run serve:ssr:frontend`
- Expose port 4200

---

## 📚 Additional Resources

- [Angular Documentation](https://angular.dev)
- [RxJS Documentation](https://rxjs.dev)
- [Reactive Forms](https://angular.dev/guide/reactive-forms)
- [Routing Guide](https://angular.dev/guide/routing)

---

## 🤝 Contributing

When adding features:
1. Use standalone components
2. Add proper TypeScript typing
3. Include validation for forms
4. Handle loading and error states
5. Ensure responsive design
6. Add meaningful comments

---

## 📄 License

See main project LICENSE

---

## 👥 Support

For issues or questions, refer to the main project documentation or backend team.

---

**Last Updated**: February 9, 2026  
**Status**: ✅ **Production Ready**
