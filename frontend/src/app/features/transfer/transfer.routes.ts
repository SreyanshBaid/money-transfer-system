import { Routes } from '@angular/router';
import { InitiateTransferComponent } from './pages/initiate-transfer.component';
import { authGuard } from '../../core/auth/auth.guard';

export const TRANSFER_ROUTES: Routes = [
  {
    path: '',
    component: InitiateTransferComponent,
    canActivate: [authGuard]
  }
];
