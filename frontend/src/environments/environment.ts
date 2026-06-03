const browserHostname = typeof window !== 'undefined' ? window.location.hostname : 'localhost';

export const environment = {
  production: false,
  presenceServiceUrl: `http://${browserHostname}:8083`,
  userServiceUrl: `http://${browserHostname}:8082`,
  // WARNING: Never commit real credentials here. Inject via CI/secrets manager for production.
  adminToken: 'REPLACE_WITH_ADMIN_JWT_TOKEN',
};
