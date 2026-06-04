import { Component, signal } from '@angular/core';
import { NavbarComponent } from '../navbar/navbar';
import { CadastroModalComponent } from '../cadastro-modal/cadastro-modal';
import { DashboardComponent } from '../dashboard/dashboard';

@Component({
  selector: 'app-main',
  imports: [NavbarComponent, CadastroModalComponent, DashboardComponent],
  templateUrl: './main.html',
})
export class MainComponent {
  readonly cadastroOpen = signal(false);
}
