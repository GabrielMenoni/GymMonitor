import { Component } from '@angular/core';
import { PresenceCounterComponent } from './components/presence-counter/presence-counter';

@Component({
  selector: 'app-root',
  imports: [PresenceCounterComponent],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {}
