import { useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import { useAuth } from '../context/AuthContext';

const ERRORS = {
  role_required: 'No DevConnect account uses that email yet. Choose Developer or Recruiter, then continue with the provider again.',
  email_unverified: 'That account has no verified email address. Verify one with the provider, or register with a password.',
  oauth_failed: 'Sign-in was cancelled or could not be completed. Please try again.',
};

/**
 * Landing page after a Google/GitHub sign-in.
 *
 * The backend redirects here with #token=...&email=...&role=... on success, or
 * ?error=<code> on failure. The token sits in the fragment so it never reaches a
 * server log; it is cleared from the address bar and history straight away.
 */
export default function OAuthCallback() {
  const { completeOAuthLogin } = useAuth();
  const navigate = useNavigate();
  const handled = useRef(false);

  useEffect(() => {
    // StrictMode runs effects twice in development; the fragment is gone after the first.
    if (handled.current) return;
    handled.current = true;

    const fragment = new URLSearchParams(window.location.hash.slice(1));
    const error = new URLSearchParams(window.location.search).get('error');
    window.history.replaceState(null, '', window.location.pathname);

    const token = fragment.get('token');
    const email = fragment.get('email');

    if (error || !token || !email) {
      toast.error(ERRORS[error] || ERRORS.oauth_failed, { duration: 7000 });
      navigate(error === 'role_required' ? '/register' : '/login', { replace: true });
      return;
    }

    const roles = (fragment.get('role') || '').split(',').filter(Boolean);
    completeOAuthLogin({ token, email, roles });
    toast.success('Signed in.');

    navigate(
      roles.includes('RECRUITER') ? '/recruiter/dashboard' : roles.includes('DEVELOPER') ? '/developer/dashboard' : '/jobs',
      { replace: true }
    );
  }, [completeOAuthLogin, navigate]);

  return (
    <div className="wrap section" style={{ textAlign: 'center' }} aria-busy="true">
      <span className="spinner" /> <span className="muted">Signing you in…</span>
    </div>
  );
}
