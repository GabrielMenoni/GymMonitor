import { TestBed } from '@angular/core/testing';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import { App } from './app';

vi.mock('@stomp/stompjs', () => ({
  Client: vi.fn().mockImplementation(function (this: any) {
    this.activate = vi.fn();
    this.deactivate = vi.fn();
    this.subscribe = vi.fn();
  }),
}));

vi.mock('sockjs-client', () => ({
  default: vi.fn(),
}));

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });
});
