import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule, NgIf } from '@angular/common';
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
  imports: [CommonModule, NgIf, CombineBalancePipe],
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

  // Profile dropdown state
  showProfileDropdown = false;
  selectedAccount: AccountCardViewModel | null = null;

  private destroy$ = new Subject<void>();

  constructor(
    private accountService: AccountService,
    private authService: AuthService,
    private router: Router
  ) {
    console.log('🔍 OverviewComponent: Constructor - Initializing...');
    
    // Initialize user observable from auth service
    this.user$ = this.authService.getCurrentUser();
    
    // Debug: Log user data
    this.user$.subscribe(user => {
      console.log('👤 Greeting: User data received:', user);
      if (!user) {
        console.warn('⚠️ User is NULL - greeting will show "Hi Guest!"');
      } else {
        console.log('✅ User found:', { username: user.username, email: user.email });
      }
    });
    
    // Initialize accounts observable - fetches accounts and their balances
    // shareReplay ensures observable executes only once even with multiple subscribers
    this.accounts$ = this.authService.getAuthState().pipe(
      tap(state => console.log('📊 Auth state:', state)),
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

    // Close profile dropdown when clicking outside
    if (typeof document !== 'undefined') {
      document.addEventListener('click', () => {
        if (this.showProfileDropdown) {
          this.showProfileDropdown = false;
        }
      });
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  // TrackBy function for *ngFor optimization
  trackByAccountId(index: number, account: AccountCardViewModel): string {
    return String(account.id);
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

  // Toggle profile dropdown
  toggleProfileDropdown(): void {
    this.showProfileDropdown = !this.showProfileDropdown;
  }

  // Get user initials for avatar
  getUserInitials(user: User): string {
    if (!user || !user.username) return '?';
    const names = user.username.split(' ');
    console.log('User:', user);
    if (names.length >= 2) {
      return (names[0][0] + names[1][0]).toUpperCase();
    }
    console.log('User with no full name:', user);
    return user.username.substring(0, 2).toUpperCase();
  }

  // View account details
  viewAccountDetails(account: AccountCardViewModel): void {
    this.selectedAccount = account;
  }

  // Close account details modal
  closeAccountDetails(): void {
    this.selectedAccount = null;
  }
}
