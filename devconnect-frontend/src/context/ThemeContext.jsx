import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';

const ThemeContext = createContext(null);
const THEME_KEY = 'devconnect.theme';

/** "system" leaves the choice to the OS; the other two pin it. */
export function ThemeProvider({ children }) {
  const [theme, setTheme] = useState(() => localStorage.getItem(THEME_KEY) || 'system');

  useEffect(() => {
    const root = document.documentElement;
    if (theme === 'system') {
      root.removeAttribute('data-theme');
      localStorage.removeItem(THEME_KEY);
    } else {
      root.setAttribute('data-theme', theme);
      localStorage.setItem(THEME_KEY, theme);
    }
  }, [theme]);

  // Tracks the OS preference so "system" keeps up when it changes mid-session —
  // toast colours and the toggle icon are derived from this.
  const [systemDark, setSystemDark] = useState(
    () => window.matchMedia('(prefers-color-scheme: dark)').matches
  );

  useEffect(() => {
    const query = window.matchMedia('(prefers-color-scheme: dark)');
    const onChange = (event) => setSystemDark(event.matches);
    query.addEventListener('change', onChange);
    return () => query.removeEventListener('change', onChange);
  }, []);

  const resolved = useMemo(() => {
    if (theme !== 'system') return theme;
    return systemDark ? 'dark' : 'light';
  }, [theme, systemDark]);

  const toggle = useCallback(() => {
    setTheme((current) => {
      const isDark = current === 'dark' || (current === 'system' && systemDark);
      return isDark ? 'light' : 'dark';
    });
  }, [systemDark]);

  const value = useMemo(() => ({ theme, resolved, setTheme, toggle }), [theme, resolved, toggle]);
  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>;
}

export function useTheme() {
  const context = useContext(ThemeContext);
  if (!context) throw new Error('useTheme must be used inside <ThemeProvider>');
  return context;
}
