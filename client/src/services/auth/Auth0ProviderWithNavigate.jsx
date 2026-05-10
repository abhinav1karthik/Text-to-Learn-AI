import { Auth0Provider } from '@auth0/auth0-react';
import { useNavigate } from 'react-router-dom';
import { auth0Config } from './auth0Config.js';

export default function Auth0ProviderWithNavigate({ children }) {
  const navigate = useNavigate();

  function handleRedirectCallback(appState) {
    navigate(appState?.returnTo ?? '/', { replace: true });
  }

  return (
    <Auth0Provider
      domain={auth0Config.domain || 'auth0-domain-not-configured'}
      clientId={auth0Config.clientId || 'auth0-client-id-not-configured'}
      authorizationParams={{
        redirect_uri: auth0Config.redirectUri,
        audience: auth0Config.audience || 'auth0-audience-not-configured',
        scope: 'openid profile email',
      }}
      cacheLocation="localstorage"
      useRefreshTokens
      onRedirectCallback={handleRedirectCallback}
    >
      {children}
    </Auth0Provider>
  );
}
