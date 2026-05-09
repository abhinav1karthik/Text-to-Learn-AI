import { Link } from 'react-router-dom';

export default function NotFoundPage() {
  return (
    <section className="page-stack">
      <div>
        <p className="eyebrow">404</p>
        <h1>Page not found</h1>
        <p className="lead">The route you opened does not exist in the app shell yet.</p>
      </div>
      <Link className="text-link" to="/">
        Go home
      </Link>
    </section>
  );
}
