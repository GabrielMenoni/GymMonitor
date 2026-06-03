import { isPlatformBrowser } from '@angular/common';
import { Injectable, OnDestroy, PLATFORM_ID, inject, signal } from '@angular/core';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { environment } from '../../environments/environment';
import { AuthService } from './auth.service';

@Injectable({ providedIn: 'root' })
export class PresenceWebsocketService implements OnDestroy {
  readonly presenceCount = signal<number | null>(null);
  readonly connectionStatus = signal<'connected' | 'disconnected' | 'error'>('disconnected');

  private readonly platformId = inject(PLATFORM_ID);
  private readonly auth = inject(AuthService);
  private client?: Client;

  constructor() {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }

    const client = new Client({
      webSocketFactory: () => new SockJS(environment.apiUrl + '/presence/ws'),
      connectHeaders: {
        Authorization: `Bearer ${this.auth.getToken() ?? ''}`,
      },
      reconnectDelay: 5000,
      onConnect: () => {
        this.connectionStatus.set('connected');
        client.subscribe('/topic/presence/count', (message) => {
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

    this.client = client;
    client.activate();
  }

  ngOnDestroy(): void {
    this.client?.deactivate();
  }
}
