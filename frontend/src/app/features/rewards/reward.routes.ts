import { Routes } from '@angular/router';
import { RewardHistoryComponent } from './pages/reward-history.component';
import { authGuard } from '../../core/auth/auth.guard';

export const REWARD_ROUTES: Routes = [
  {
    path: '',
    component: RewardHistoryComponent,
    canActivate: [authGuard]
  }
];
