import { useEffect, useState } from 'react';
import { applyTheme, getPreferredTheme } from '../../utils/theme.js';

export default function ThemeToggle() {
  const [theme, setTheme] = useState(getPreferredTheme);

  useEffect(() => {
    setTheme(applyTheme(theme));
  }, [theme]);

  const isDark = theme === 'dark';

  return (
    <button
      aria-label={isDark ? 'Switch to light mode' : 'Switch to dark mode'}
      className="inline-flex h-10 min-w-20 items-center justify-center rounded-full border border-slate-200 bg-slate-50 px-3 text-xs font-bold uppercase tracking-wide text-slate-600 transition hover:bg-slate-100 focus:outline-none focus:ring-2 focus:ring-blue-500 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-200 dark:hover:bg-slate-800"
      onClick={() => setTheme(isDark ? 'light' : 'dark')}
      type="button"
    >
      {isDark ? 'Light' : 'Dark'}
    </button>
  );
}
