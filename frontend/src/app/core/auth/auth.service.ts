import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { BehaviorSubject, Observable, throwError } from 'rxjs';
import { catchError, tap, switchMap, map, shareReplay } from 'rxjs/operators';
import { TokenService } from './token.service';
import { LoginRequest, LoginResponse, User, AuthState } from './auth.models';

// AuthService handles login, logout, authentication status
@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = 'http://localhost:8080/api/v1/auth';
  
  private authState = new BehaviorSubject<AuthState>({
    isAuthenticated: false,
    user: null,
    token: null,
    loading: false,
    error: null
  });

  public auth$ = this.authState.asObservable();

  constructor(
    private http: HttpClient,
    private tokenService: TokenService
  ) {
    this.loadStoredUser();
  }

  // Performs login with username and password
  login(credentials: LoginRequest): Observable<LoginResponse> {
    console.log('AuthService: Starting login request');
    this.updateState({ loading: true, error: null });
    
    return this.http.post<LoginResponse>(`${this.apiUrl}/login`, credentials).pipe(
      tap(response => {
        console.log('AuthService: Login successful');
        this.tokenService.setToken(response.token);
        this.tokenService.setUser(response.user);
        this.updateState({
          isAuthenticated: true,
          user: response.user,
          token: response.token,
          loading: false,
          error: null
        });
      }),
      catchError(error => {
        console.error('AuthService: Login error', error);
        return this.handleError(error);
      })
    );
  }

  // Clears authentication and logs out user
  logout(): void {
    this.tokenService.clearToken();
    this.updateState({
      isAuthenticated: false,
      user: null,
      token: null,
      loading: false,
      error: null
    });
  }

  // Checks if user is currently authenticated
  isAuthenticated(): Observable<boolean> {
    return this.auth$.pipe(
      map(state => state.isAuthenticated)
    );
  }

  // Gets current authentication token
  getToken(): string | null {
    return this.tokenService.getToken();
  }

  // Gets current user
  getCurrentUser(): Observable<User | null> {
    return this.auth$.pipe(
      map(state => state.user)
    );
  }

  // Gets current auth state
  getAuthState(): Observable<AuthState> {
    return this.auth$;
  }

  // Private helper methods
  private updateState(partial: Partial<AuthState>): void {
    const current = this.authState.getValue();
    this.authState.next({ ...current, ...partial });
  }

  private handleError(error: HttpErrorResponse) {
    console.log('AuthService handleError:', { status: error.status, error });
    let errorMessage = 'An error occurred';
    
    if (error.error instanceof ErrorEvent) {
      errorMessage = error.error.message;
    } else {
      errorMessage = error.error?.message || error.statusText;
    }

    this.updateState({ 
      loading: false, 
      error: errorMessage,
      isAuthenticated: false 
    });

    const errorObj = {
      message: errorMessage,
      status: error.status
    };
    console.log('AuthService throwing error:', errorObj);

    return throwError(() => errorObj);
  }

  private loadStoredUser(): void {
    const token = this.tokenService.getToken();
    const user = this.tokenService.getUser();
    if (token && user) {
      // Restore both token and user from localStorage
      this.updateState({ isAuthenticated: true, token, user });
    }
  }
}
