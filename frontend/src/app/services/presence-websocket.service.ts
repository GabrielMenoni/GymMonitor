import { Injectable, OnDestroy, signal } from '@angular/core';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class PresenceWebsocketService implements OnDestroy {
  readonly presenceCount = signal<number | null>(null);
  readonly connectionStatus = signal<'connected' | 'disconnected' | 'error'>('disconnected');

  private client: Client;

  constructor() {
    this.client = new Client({
      webSocketFactory: () => new SockJS(environment.presenceServiceUrl + '/ws'),
      connectHeaders: {
        Authorization: `Bearer ${environment.adminToken}`,
      },
      reconnectDelay: 5000,
      onConnect: () => {
        this.connectionStatus.set('connected');
        this.client.subscribe('/topic/presence/count', (message) => {
          try {
            const body = JSON.parse(message.body);
            this.presenceCount.set(body.count);
          } catch (e) {
            console.error('Erro ao processar mensagem de contagem:', e);
          }
        });
      },
      onDisconnect: () => {
        this.connectionStatus.set('disconnected');
      },
      onStompError: () => {
        this.connectionStatus.set('error');
      },
    });

    this.client.activate();
  }

  ngOnDestroy(): void {
    this.client.deactivate();
  }
}
