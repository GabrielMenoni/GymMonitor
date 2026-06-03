import { Component, inject, output } from '@angular/core';
import { AuthService } from '../../services/auth.service';
import { ThemeService } from '../../services/theme.service';

@Component({
  selector: 'app-navbar',
  templateUrl: './navbar.html',
})
export class NavbarComponent {
  readonly auth = inject(AuthService);
  readonly theme = inject(ThemeService);

  readonly openCadastro = output<void>();

  get email(): string | null {
    return this.auth.getEmail();
  }

  get canCadastrar(): boolean {
    const role = this.auth.getRole();
    return role === 'FUNCIONARIO' || role === 'ADMIN';
  }

  logout(): void {
    this.auth.logout();
  }
}
