import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { Observable, Subject, BehaviorSubject } from 'rxjs';
import { switchMap, takeUntil, tap, finalize, shareReplay } from 'rxjs/operators';
import { AccountService, AccountCardViewModel } from '../services/account.service';
import { AuthService } from '../../../core/auth/auth.service';
import { User } from '../../../core/auth/auth.models';
import { CombineBalancePipe } from '../../../shared/pipes';

// OverviewComponent fetches user profile and all accounts with balances
// Displays accounts as scrollable cards with proper loading/error handling
@Component({
  selector: 'app-overview',
  standalone: true,
  imports: [CommonModule, CombineBalancePipe],
  templateUrl: './overview.component.html',
  styleUrls: ['./overview.component.css']
})
export class OverviewComponent implements OnInit, OnDestroy {
  // Observable streams for data
  accounts$: Observable<AccountCardViewModel[]>;
  user$: Observable<User | null>;
  
  // State management
  isLoadingSubject$ = new BehaviorSubject<boolean>(true);
  isLoading$ = this.isLoadingSubject$.asObservable();
  
  errorSubject$ = new BehaviorSubject<string | null>(null);
  error$ = this.errorSubject$.asObservable();

  private destroy$ = new Subject<void>();

  constructor(
    private accountService: AccountService,
    private authService: AuthService,
    private router: Router
  ) {
    // Initialize user observable from auth service
    this.user$ = this.authService.getCurrentUser();
    
    // Initialize accounts observable - fetches accounts and their balances
    // shareReplay ensures observable executes only once even with multiple subscribers
    this.accounts$ = this.authService.getAuthState().pipe(
      switchMap(authState => {
        if (authState.isAuthenticated) {
          this.isLoadingSubject$.next(true);
          this.errorSubject$.next(null);
          return this.accountService.getAccountsWithDetails().pipe(
            tap(() => {
              // Data arrived successfully
              this.isLoadingSubject$.next(false);
              this.errorSubject$.next(null);
            }),
            finalize(() => {
              // Ensure loading is false when observable completes
              this.isLoadingSubject$.next(false);
            })
          );
        }
        this.isLoadingSubject$.next(false);
        return new Observable<AccountCardViewModel[]>(obs => {
          obs.next([]);
          obs.complete();
        });
      }),
      shareReplay(1), // Cache the result and share across subscribers
      takeUntil(this.destroy$)
    );
  }

  ngOnInit(): void {
    // Subscribe to accounts to trigger the data fetch
    this.accounts$.pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      error: (err) => {
        this.isLoadingSubject$.next(false);
        this.errorSubject$.next('Failed to load accounts. Please try again.');
        console.error('Error loading accounts:', err);
      }
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  // TrackBy function for *ngFor optimization
  trackByAccountId(index: number, account: AccountCardViewModel): string {
    return account.id;
  }

  navigateToTransfer(accountId: string): void {
    this.router.navigate(['/transfer'], { queryParams: { accountId } });
  }

  navigateToHistory(accountId: string): void {
    this.router.navigate(['/history'], { queryParams: { accountId } });
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
