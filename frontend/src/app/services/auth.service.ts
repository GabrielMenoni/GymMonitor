import { Injectable, inject, PLATFORM_ID } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { isPlatformBrowser } from '@angular/common';
import { tap } from 'rxjs/operators';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

const TOKEN_KEY = 'gymmonitor_token';

export type UserRole = 'FUNCIONARIO' | 'ADMIN';

interface TokenResponse {
  token: string;
  role: string;
  email: string;
}

export interface JwtPayload {
  sub: string;
  role: UserRole;
  userId: string;
  exp: number;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly platformId = inject(PLATFORM_ID);

  login(role: UserRole, email: string, password: string): Observable<TokenResponse> {
    const path = role === 'ADMIN'
      ? '/auth/admin/login'
      : '/auth/funcionarios/login';

    return this.http
      .post<TokenResponse>(`${environment.apiUrl}${path}`, { email, password })
      .pipe(tap(res => this.saveToken(res.token)));
  }

  logout(): void {
    if (isPlatformBrowser(this.platformId)) {
      localStorage.removeItem(TOKEN_KEY);
    }
    this.router.navigate(['/login']);
  }

  getToken(): string | null {
    if (!isPlatformBrowser(this.platformId)) return null;
    return localStorage.getItem(TOKEN_KEY);
  }

  getPayload(): JwtPayload | null {
    const token = this.getToken();
    if (!token) return null;
    try {
      const base64 = token.split('.')[1];
      const json = atob(base64.replace(/-/g, '+').replace(/_/g, '/'));
      return JSON.parse(json) as JwtPayload;
    } catch {
      return null;
    }
  }

  isTokenValid(): boolean {
    const payload = this.getPayload();
    if (!payload) return false;
    return payload.exp * 1000 > Date.now();
  }

  getRole(): UserRole | null {
    return this.getPayload()?.role ?? null;
  }

  getEmail(): string | null {
    return this.getPayload()?.sub ?? null;
  }

  private saveToken(token: string): void {
    if (isPlatformBrowser(this.platformId)) {
      localStorage.setItem(TOKEN_KEY, token);
    }
  }
}
