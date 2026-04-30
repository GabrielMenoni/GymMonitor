import { Component, inject } from '@angular/core';
import { PresenceWebsocketService } from '../../services/presence-websocket.service';

@Component({
  selector: 'app-presence-counter',
  standalone: true,
  templateUrl: './presence-counter.html',
})
export class PresenceCounterComponent {
  protected readonly ws = inject(PresenceWebsocketService);
}
