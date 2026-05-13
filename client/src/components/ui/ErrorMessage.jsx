export default function ErrorMessage({ message, title = 'Something went wrong' }) {
  if (!message) {
    return null;
  }

  return (
    <div
      className="rounded-lg border border-red-200 bg-red-50 px-5 py-4 text-red-900 dark:border-red-900/70 dark:bg-red-950/40 dark:text-red-200"
      role="alert"
    >
      <strong className="block text-base font-semibold">{title}</strong>
      <p className="mt-1 text-sm leading-6">{message}</p>
    </div>
  );
}
