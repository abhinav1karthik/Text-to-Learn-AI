export default function ErrorMessage({ message, title = 'Something went wrong' }) {
  if (!message) {
    return null;
  }

  return (
    <div className="error-message" role="alert">
      <strong>{title}</strong>
      <p>{message}</p>
    </div>
  );
}
