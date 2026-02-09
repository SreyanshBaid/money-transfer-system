import { Routes } from '@angular/router';
import { OverviewComponent } from './pages/overview.component';
import { authGuard } from '../../core/auth/auth.guard';

export const DASHBOARD_ROUTES: Routes = [
  {
    path: '',
    component: OverviewComponent,
    canActivate: [authGuard]
  }
];
