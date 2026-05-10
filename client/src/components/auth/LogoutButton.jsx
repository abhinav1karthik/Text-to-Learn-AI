import Button from '../ui/Button.jsx';
import { useAuth } from '../../hooks/useAuth.js';

export default function LogoutButton() {
  const { logout } = useAuth();

  function handleLogout() {
    logout({
      logoutParams: {
        returnTo: window.location.origin,
      },
    });
  }

  return (
    <Button type="button" className="button-secondary" onClick={handleLogout}>
      Log out
    </Button>
  );
}
