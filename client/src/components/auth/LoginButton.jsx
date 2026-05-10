import Button from '../ui/Button.jsx';
import { useAuth } from '../../hooks/useAuth.js';

export default function LoginButton({ screenHint }) {
  const { isConfigured, loginWithRedirect } = useAuth();

  function handleLogin() {
    if (!isConfigured) {
      return;
    }

    loginWithRedirect({
      appState: { returnTo: '/' },
      authorizationParams: screenHint ? { screen_hint: screenHint } : undefined,
    });
  }

  return (
    <Button type="button" onClick={handleLogin} disabled={!isConfigured}>
      {screenHint === 'signup' ? 'Sign up' : 'Log in'}
    </Button>
  );
}
