import { Routes } from '@angular/router';
import { AUTH_ROUTES } from './features/auth/auth.routes';
import { DASHBOARD_ROUTES } from './features/dashboard/dashboard.routes';
import { TRANSFER_ROUTES } from './features/transfer/transfer.routes';
import { TransactionHistoryComponent } from './features/dashboard/pages/transaction-history.component';
import { authGuard } from './core/auth/auth.guard';

export const routes: Routes = [
  {
    path: 'auth',
    children: AUTH_ROUTES
  },
  {
    path: 'dashboard',
    children: DASHBOARD_ROUTES
  },
  {
    path: 'transfer',
    children: TRANSFER_ROUTES
  },
  {
    path: 'history',
    component: TransactionHistoryComponent,
    canActivate: [authGuard]
  },
  {
    path: '',
    redirectTo: '/auth/login',
    pathMatch: 'full'
  },
  {
    path: '**',
    redirectTo: '/auth/login'
  }
];
