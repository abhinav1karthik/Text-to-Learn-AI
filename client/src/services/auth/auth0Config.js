export const auth0Config = {
  domain: import.meta.env.VITE_AUTH0_DOMAIN ?? '',
  clientId: import.meta.env.VITE_AUTH0_CLIENT_ID ?? '',
  audience: import.meta.env.VITE_AUTH0_AUDIENCE ?? '',
  redirectUri: window.location.origin,
};

export function isAuth0Configured() {
  return Boolean(auth0Config.domain && auth0Config.clientId && auth0Config.audience);
}
