const browserHostname = typeof window !== 'undefined' ? window.location.hostname : 'localhost';
const isDockerDev = typeof window !== 'undefined' && window.location.port === '4200';
const apiPort = isDockerDev ? ':8080' : '';

export const environment = {
  production: false,
  apiUrl: `http://${browserHostname}${apiPort}/api`,
};
