import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react';
import toast from 'react-hot-toast';
import { TOKEN_KEY, USER_KEY, setUnauthorizedHandler } from '../api/client';
import { loginUser } from '../api/endpoints';
import { isExpired, millisUntilExpiry } from '../utils/jwt';

const AuthContext = createContext(null);

function readStoredUser() {
  const token = localStorage.getItem(TOKEN_KEY);
  if (!token) return null;

  // A token past its expiry is worthless: drop it rather than sending it.
  if (isExpired(token)) {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    return null;
  }

  try {
    const stored = JSON.parse(localStorage.getItem(USER_KEY) || 'null');
    if (!stored?.email) return null;
    return { ...stored, token };
  } catch {
    return null;
  }
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(readStoredUser);
  const [ready, setReady] = useState(false);
  const expiryTimer = useRef(null);

  const logout = useCallback((message) => {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    setUser(null);
    if (message) toast(message, { icon: '🔒' });
  }, []);

  const login = useCallback(async ({ email, password }) => {
    const data = await loginUser({ email, password });
    // Backend returns { token, email, role: [...] }
    const roles = Array.isArray(data.role) ? data.role : [data.role].filter(Boolean);
    const profile = { email: data.email, roles };

    localStorage.setItem(TOKEN_KEY, data.token);
    localStorage.setItem(USER_KEY, JSON.stringify(profile));
    setUser({ ...profile, token: data.token });
    return { ...profile, token: data.token };
  }, []);

  /**
   * Re-points the stored session after an account edit.
   *
   * The JWT's subject is the email, so changing the address invalidates the token
   * the app is holding: the API answers with a replacement, and this swaps both in
   * without a round trip through the login form.
   */
  const updateSession = useCallback(({ email, token } = {}) => {
    setUser((current) => {
      if (!current) return current;
      const next = {
        ...current,
        email: email || current.email,
        token: token || current.token,
      };
      localStorage.setItem(TOKEN_KEY, next.token);
      localStorage.setItem(USER_KEY, JSON.stringify({ email: next.email, roles: next.roles }));
      return next;
    });
  }, []);

  // A 401/403 on any authenticated call means this session is no longer usable.
  useEffect(() => {
    setUnauthorizedHandler(() => {
      if (localStorage.getItem(TOKEN_KEY)) {
        logout('Your session ended. Please sign in again.');
      }
    });
    setReady(true);
    return () => setUnauthorizedHandler(null);
  }, [logout]);

  // Sign out exactly when the token lapses, so the UI never shows a logged-in
  // state that the API will reject.
  useEffect(() => {
    clearTimeout(expiryTimer.current);
    if (!user?.token) return undefined;

    const remaining = millisUntilExpiry(user.token);
    if (remaining === null) return undefined;
    if (remaining <= 0) {
      logout('Your session expired. Please sign in again.');
      return undefined;
    }
    expiryTimer.current = setTimeout(
      () => logout('Your session expired. Please sign in again.'),
      remaining
    );
    return () => clearTimeout(expiryTimer.current);
  }, [user?.token, logout]);

  // Signing out in one tab signs out the others.
  useEffect(() => {
    const onStorage = (event) => {
      if (event.key === TOKEN_KEY) setUser(readStoredUser());
    };
    window.addEventListener('storage', onStorage);
    return () => window.removeEventListener('storage', onStorage);
  }, []);

  const value = useMemo(() => {
    const roles = user?.roles || [];
    const has = (role) => roles.includes(role);
    return {
      user,
      ready,
      roles,
      login,
      logout,
      updateSession,
      isAuthenticated: Boolean(user),
      isDeveloper: has('DEVELOPER'),
      isRecruiter: has('RECRUITER'),
      isAdmin: has('ADMIN'),
      homeFor: has('RECRUITER') ? '/recruiter/dashboard' : has('DEVELOPER') ? '/developer/dashboard' : '/jobs',
    };
  }, [user, ready, login, logout, updateSession]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used inside <AuthProvider>');
  return context;
}
