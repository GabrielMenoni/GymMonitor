import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface UserPresenceInfo {
  userId: string;
  userType: 'ALUNO' | 'FUNCIONARIO';
  entradaEm: string;
  sessaoId: string;
}

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly http = inject(HttpClient);

  getAlunosCount(): Observable<{ count: number }> {
    return this.http.get<{ count: number }>(
      `${environment.apiUrl}/user-service/alunos/count`
    );
  }

  getFuncionariosCount(): Observable<{ count: number }> {
    return this.http.get<{ count: number }>(
      `${environment.apiUrl}/user-service/funcionarios/count`
    );
  }

  getPresenceUsers(): Observable<{ users: UserPresenceInfo[] }> {
    return this.http.get<{ users: UserPresenceInfo[] }>(
      `${environment.apiUrl}/presence/users`
    );
  }

  getPresenceHistory(from: number, to: number): Observable<{ points: { timestamp: number; count: number }[] }> {
    return this.http.get<{ points: { timestamp: number; count: number }[] }>(
      `${environment.apiUrl}/presence/history?from=${from}&to=${to}`
    );
  }
}
