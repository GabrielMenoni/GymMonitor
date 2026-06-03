import { Component, inject, input, output, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../services/auth.service';
import { environment } from '../../../environments/environment';

type CadastroTab = 'ALUNO' | 'FUNCIONARIO';

@Component({
  selector: 'app-cadastro-modal',
  imports: [ReactiveFormsModule],
  templateUrl: './cadastro-modal.html',
})
export class CadastroModalComponent {
  private readonly fb = inject(FormBuilder);
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);

  readonly isOpen = input.required<boolean>();
  readonly closed = output<void>();

  readonly isAdmin = this.auth.getRole() === 'ADMIN';
  readonly activeTab = signal<CadastroTab>('ALUNO');
  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);

  readonly alunoForm = this.fb.group({
    name:      ['', Validators.required],
    email:     ['', [Validators.required, Validators.email]],
    birthDate: ['', Validators.required],
    password:  ['', [Validators.required, Validators.minLength(6)]],
  });

  readonly funcionarioForm = this.fb.group({
    name:     ['', Validators.required],
    email:    ['', [Validators.required, Validators.email]],
    position: ['', Validators.required],
    salary:   [null as number | null, Validators.required],
    password: ['', [Validators.required, Validators.minLength(6)]],
  });

  selectTab(tab: CadastroTab): void {
    this.activeTab.set(tab);
    this.clearMessages();
  }

  clearMessages(): void {
    this.errorMessage.set(null);
    this.successMessage.set(null);
  }

  close(): void {
    this.alunoForm.reset();
    this.funcionarioForm.reset();
    this.clearMessages();
    this.closed.emit();
  }

  submit(): void {
    if (this.loading()) return;

    const tab = this.activeTab();
    const form = tab === 'ALUNO' ? this.alunoForm : this.funcionarioForm;

    if (form.invalid) {
      form.markAllAsTouched();
      return;
    }

    const url = tab === 'ALUNO'
      ? `${environment.apiUrl}/auth/alunos/cadastro`
      : `${environment.apiUrl}/auth/funcionarios/cadastro`;

    this.loading.set(true);
    this.clearMessages();

    this.http.post(url, form.value).subscribe({
      next: () => {
        this.loading.set(false);
        this.successMessage.set(
          tab === 'ALUNO' ? 'Aluno cadastrado com sucesso!' : 'Funcionário cadastrado com sucesso!'
        );
        form.reset();
      },
      error: (err: HttpErrorResponse) => {
        this.loading.set(false);
        this.errorMessage.set(this.mapError(err));
      },
    });
  }

  private mapError(err: HttpErrorResponse): string {
    if (err.status === 409) return 'Este e-mail já está em uso.';
    if (err.status === 403) return 'Você não tem permissão para esta ação.';
    if (err.status === 0)   return 'Não foi possível conectar ao servidor. Tente novamente.';
    if (err.status === 400) {
      const msg = err.error?.message ?? err.error?.error;
      return msg ?? 'Dados inválidos. Verifique os campos.';
    }
    return 'Erro inesperado. Tente novamente mais tarde.';
  }
}
