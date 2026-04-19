import { AuthConfig } from 'angular-oauth2-oidc';
import { environment } from '../../../environments/environment';

export const authConfig: AuthConfig = {
  issuer: environment.keycloakIssuer,
  clientId: environment.keycloakClientId,
  responseType: 'code',
  redirectUri: window.location.origin + '/',
  scope: 'openid profile email',
  showDebugInformation: false,
  clearHashAfterLogin: true,
};
