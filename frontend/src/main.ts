import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { App } from './app/app';

// Polyfill for global variable (needed by some browser libraries)
if (typeof global === 'undefined') {
  (window as any).global = window;
}

bootstrapApplication(App, appConfig)
  .catch((err) => console.error(err));
