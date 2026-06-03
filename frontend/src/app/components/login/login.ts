import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService, UserRole } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule],
  templateUrl: './login.html',
})
export class LoginComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

  readonly selectedRole = signal<UserRole>('FUNCIONARIO');
  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]],
  });

  selectRole(role: UserRole): void {
    this.selectedRole.set(role);
    this.clearError();
  }

  clearError(): void {
    this.errorMessage.set(null);
  }

  submit(): void {
    if (this.form.invalid || this.loading()) return;

    const { email, password } = this.form.value;
    this.loading.set(true);
    this.errorMessage.set(null);

    this.auth.login(this.selectedRole(), email!, password!).subscribe({
      next: () => this.router.navigate(['/']),
      error: (err: HttpErrorResponse) => {
        this.loading.set(false);
        this.errorMessage.set(this.mapLoginError(err));
      },
    });
  }

  private mapLoginError(err: HttpErrorResponse): string {
    if (err.status === 401) return 'Credenciais inválidas.';
    if (err.status === 0) return 'Não foi possível conectar ao servidor. Tente novamente.';
    return 'Erro inesperado. Tente novamente mais tarde.';
  }
}
