import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../environments/environment';

export interface AuthResponse {
  token: string;
  role: string;
  email: string;
}

export interface RegisterPayload {
  email: string;
  password: string;
  fullName: string;
  role: 'PUBLIC' | 'PROFESSIONAL';
}

export interface LoginPayload {
  email: string;
  password: string;
}

const TOKEN_KEY = 'mh_token';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly api = environment.apiUrl;

  constructor(private http: HttpClient) {}

  login(payload: LoginPayload): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.api}/api/auth/login`, payload).pipe(
      tap(res => localStorage.setItem(TOKEN_KEY, res.token))
    );
  }

  register(payload: RegisterPayload): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.api}/api/auth/register`, payload).pipe(
      tap(res => localStorage.setItem(TOKEN_KEY, res.token))
    );
  }

  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
  }

  getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  isLoggedIn(): boolean {
    return !!this.getToken();
  }

  getRole(): string | null {
    const token = this.getToken();
    if (!token) return null;
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      return payload.role ?? null;
    } catch {
      return null;
    }
  }

  isProfessional(): boolean {
    return this.getRole() === 'PROFESSIONAL';
  }
}
