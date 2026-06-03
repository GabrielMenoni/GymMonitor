import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-not-found',
  templateUrl: './not-found.html',
})
export class NotFoundComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  goHome(): void {
    if (this.auth.isTokenValid()) {
      this.router.navigate(['/']);
    } else {
      this.router.navigate(['/login']);
    }
  }
}
