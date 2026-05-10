import LoginButton from '../components/auth/LoginButton.jsx';
import { useAuth } from '../hooks/useAuth.js';

export default function SignupPage() {
  const { isConfigured } = useAuth();

  return (
    <section className="page-stack">
      <div>
        <p className="eyebrow">Account</p>
        <h1>Create your account</h1>
        <p className="lead">
          Save generated courses, revisit lessons, and keep your learning workspace
          connected to your profile.
        </p>
      </div>

      <div className="prompt-panel">
        <LoginButton screenHint="signup" />
        {!isConfigured ? (
          <p>
            Auth0 settings are missing. Add the required values in
            <code>client/.env.local</code> to enable sign up.
          </p>
        ) : null}
      </div>
    </section>
  );
}
