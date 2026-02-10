import { Injectable } from '@angular/core';
import {
  HttpRequest,
  HttpHandler,
  HttpEvent,
  HttpInterceptor
} from '@angular/common/http';
import { Observable } from 'rxjs';
import { TokenService } from '../auth/token.service';

// Interceptor that attaches JWT token to Authorization header
@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  constructor(private tokenService: TokenService) {}

  intercept(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    const token = this.tokenService.getToken();

    console.log('🔐 Auth Interceptor:', {
      url: request.url,
      hasToken: !!token,
      token: token ? `${token.substring(0, 20)}...` : 'none'
    });

    if (token) {
      request = request.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`
        }
      });
      console.log('✅ Token attached to request');
    } else {
      console.log('⚠️ No token found - request will be sent without authorization');
    }

    return next.handle(request);
  }
}
