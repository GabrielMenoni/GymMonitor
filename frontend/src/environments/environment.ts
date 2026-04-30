const browserHostname = typeof window !== 'undefined' ? window.location.hostname : 'localhost';

export const environment = {
  production: false,
  presenceServiceUrl: `http://${browserHostname}:8083`,
  adminToken: 'REPLACE_WITH_ADMIN_JWT_TOKEN',
};
