import { useAuth0 } from '@auth0/auth0-react';
import { isAuth0Configured } from '../services/auth/auth0Config.js';

export function useAuth() {
  const auth0 = useAuth0();

  return {
    ...auth0,
    isConfigured: isAuth0Configured(),
  };
}
