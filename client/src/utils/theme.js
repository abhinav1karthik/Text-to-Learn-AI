export const THEME_STORAGE_KEY = 'text-to-learn-theme';

export function getPreferredTheme() {
  if (typeof window === 'undefined') {
    return 'light';
  }

  const storedTheme = window.localStorage.getItem(THEME_STORAGE_KEY);
  if (storedTheme === 'dark' || storedTheme === 'light') {
    return storedTheme;
  }

  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
}

export function applyTheme(theme) {
  const nextTheme = theme === 'dark' ? 'dark' : 'light';
  const root = document.documentElement;

  root.classList.remove('light', 'dark');
  root.classList.add(nextTheme);
  root.dataset.theme = nextTheme;
  root.style.colorScheme = nextTheme;
  window.localStorage.setItem(THEME_STORAGE_KEY, nextTheme);

  return nextTheme;
}
