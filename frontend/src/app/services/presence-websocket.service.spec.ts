import { TestBed } from '@angular/core/testing';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import { PresenceWebsocketService } from './presence-websocket.service';

const mockSubscribe = vi.fn();
const mockActivate = vi.fn();
const mockDeactivate = vi.fn();

vi.mock('@stomp/stompjs', () => ({
  Client: vi.fn().mockImplementation(function (this: any) {
    this.activate = mockActivate;
    this.deactivate = mockDeactivate;
    this.subscribe = mockSubscribe;
  }),
}));

vi.mock('sockjs-client', () => ({
  default: vi.fn(),
}));

describe('PresenceWebsocketService', () => {
  let service: PresenceWebsocketService;

  beforeEach(() => {
    vi.clearAllMocks();
    TestBed.configureTestingModule({});
    service = TestBed.inject(PresenceWebsocketService);
  });

  it('deve ser criado', () => {
    expect(service).toBeTruthy();
  });

  it('deve iniciar com presenceCount null', () => {
    expect(service.presenceCount()).toBeNull();
  });

  it('deve iniciar com connectionStatus disconnected', () => {
    expect(service.connectionStatus()).toBe('disconnected');
  });

  it('deve chamar activate ao construir', () => {
    expect(mockActivate).toHaveBeenCalledTimes(1);
  });
});
