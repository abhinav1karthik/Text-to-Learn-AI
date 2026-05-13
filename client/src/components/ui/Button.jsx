const VARIANTS = {
  primary:
    'border-transparent bg-blue-600 text-white shadow-sm hover:bg-blue-700 disabled:bg-blue-300',
  secondary:
    'border-slate-300 bg-white text-slate-700 shadow-sm hover:border-slate-400 hover:bg-slate-50 disabled:bg-slate-100 disabled:text-slate-400 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-200 dark:hover:bg-slate-800 dark:disabled:bg-slate-900 dark:disabled:text-slate-600',
  ghost:
    'border-transparent bg-transparent text-slate-600 hover:bg-slate-100 hover:text-slate-950 disabled:text-slate-400 dark:text-slate-300 dark:hover:bg-slate-900 dark:hover:text-white',
};

export default function Button({ children, className = '', variant = 'primary', ...props }) {
  const legacySecondary = className.includes('button-secondary');
  const legacyGhost = className.includes('button-link');
  const resolvedVariant = legacySecondary ? 'secondary' : legacyGhost ? 'ghost' : variant;
  const cleanedClassName = className
    .replace('button-secondary', '')
    .replace('button-link', '')
    .trim();

  const classes = [
    'inline-flex min-h-11 items-center justify-center rounded-lg border px-4 py-2.5 text-sm font-semibold transition focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 disabled:cursor-not-allowed',
    VARIANTS[resolvedVariant] ?? VARIANTS.primary,
    cleanedClassName,
  ]
    .filter(Boolean)
    .join(' ');

  return (
    <button className={classes} {...props}>
      {children}
    </button>
  );
}
