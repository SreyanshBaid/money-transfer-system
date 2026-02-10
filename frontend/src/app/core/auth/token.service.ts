import { Injectable } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { PLATFORM_ID, inject } from '@angular/core';

// Handles storing and retrieving JWT token from localStorage
@Injectable({
  providedIn: 'root'
})
export class TokenService {
  private readonly TOKEN_KEY = 'auth_token';
  private readonly USER_KEY = 'auth_user';
  platformId = inject(PLATFORM_ID);

  constructor() {}

  // Stores JWT token in localStorage
  setToken(token: string): void {
    if (isPlatformBrowser(this.platformId)) {
      localStorage.setItem(this.TOKEN_KEY, token);
    }
  }

  // Retrieves JWT token from localStorage
  getToken(): string | null {
    if (isPlatformBrowser(this.platformId)) {
      return localStorage.getItem(this.TOKEN_KEY);
    }
    return null;
  }

  // Stores user data in localStorage
  setUser(user: any): void {
    if (isPlatformBrowser(this.platformId)) {
      localStorage.setItem(this.USER_KEY, JSON.stringify(user));
    }
  }

  // Retrieves user data from localStorage
  getUser(): any | null {
    if (isPlatformBrowser(this.platformId)) {
      const userStr = localStorage.getItem(this.USER_KEY);
      if (!userStr) {
        return null;
      }
      try {
        return JSON.parse(userStr);
      } catch {
        localStorage.removeItem(this.USER_KEY);
        return null;
      }
    }
    return null;
  }

  // Removes JWT token from localStorage
  clearToken(): void {
    if (isPlatformBrowser(this.platformId)) {
      localStorage.removeItem(this.TOKEN_KEY);
      localStorage.removeItem(this.USER_KEY);
    }
  }

  // Checks if token exists
  hasToken(): boolean {
    return !!this.getToken();
  }
}
