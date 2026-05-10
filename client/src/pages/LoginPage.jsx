import LoginButton from '../components/auth/LoginButton.jsx';
import { useAuth } from '../hooks/useAuth.js';

export default function LoginPage() {
  const { isConfigured } = useAuth();

  return (
    <section className="page-stack">
      <div>
        <p className="eyebrow">Account</p>
        <h1>Log in to Text To Learn</h1>
        <p className="lead">
          Access saved courses, lesson progress, and generated learning materials from your
          account.
        </p>
      </div>

      <div className="prompt-panel">
        <LoginButton />
        {!isConfigured ? (
          <p>
            Auth0 settings are missing. Add the required values in
            <code>client/.env.local</code> to enable login.
          </p>
        ) : null}
      </div>
    </section>
  );
}
