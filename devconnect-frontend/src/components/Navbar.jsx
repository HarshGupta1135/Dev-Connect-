import { useEffect, useState } from 'react';
import { Link, NavLink, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useTheme } from '../context/ThemeContext';

function ThemeButton() {
  const { resolved, toggle } = useTheme();
  return (
    <button
      type="button"
      className="btn btn--ghost btn--icon"
      onClick={toggle}
      aria-label={`Switch to ${resolved === 'dark' ? 'light' : 'dark'} theme`}
      title={`Switch to ${resolved === 'dark' ? 'light' : 'dark'} theme`}
    >
      {resolved === 'dark' ? (
        <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
          <circle cx="12" cy="12" r="4.2" />
          <path d="M12 2v2M12 20v2M2 12h2M20 12h2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M19.1 4.9l-1.4 1.4M6.3 17.7l-1.4 1.4" />
        </svg>
      ) : (
        <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
          <path d="M21 12.8A8.5 8.5 0 1 1 11.2 3a6.6 6.6 0 0 0 9.8 9.8z" />
        </svg>
      )}
    </button>
  );
}

export default function Navbar() {
  const { isAuthenticated, isDeveloper, isRecruiter, user, logout } = useAuth();
  const [stuck, setStuck] = useState(false);
  const [open, setOpen] = useState(false);
  const location = useLocation();
  const navigate = useNavigate();

  useEffect(() => {
    const onScroll = () => setStuck(window.scrollY > 6);
    onScroll();
    window.addEventListener('scroll', onScroll, { passive: true });
    return () => window.removeEventListener('scroll', onScroll);
  }, []);

  // A tapped link on mobile should close the menu it was tapped in.
  useEffect(() => setOpen(false), [location.pathname]);

  const handleSignOut = () => {
    logout('Signed out.');
    navigate('/');
  };

  return (
    <header className="nav" data-stuck={stuck}>
      <div className="wrap">
        <Link to="/" className="brand" aria-label="DevConnect home">
          <span className="brand-mark" aria-hidden="true">&lt;/&gt;</span>
          DevConnect
        </Link>

        <nav className="nav-links" data-open={open} aria-label="Main">
          <NavLink to="/jobs" className="nav-link">Browse jobs</NavLink>
          {isDeveloper && (
            <>
              <NavLink to="/developer/dashboard" className="nav-link">Dashboard</NavLink>
              <NavLink to="/developer/profile" className="nav-link">My profile</NavLink>
            </>
          )}
          {isRecruiter && (
            <>
              <NavLink to="/recruiter/dashboard" className="nav-link">Dashboard</NavLink>
              <NavLink to="/recruiter/jobs/new" className="nav-link">Post a job</NavLink>
            </>
          )}
        </nav>

        <div className="nav-actions">
          <ThemeButton />

          {isAuthenticated ? (
            <>
              <span className="chip hide-sm" title={user.email}>
                {user.email.split('@')[0]}
              </span>
              <button type="button" className="btn btn--outline btn--sm" onClick={handleSignOut}>
                Sign out
              </button>
            </>
          ) : (
            <>
              <Link to="/login" className="btn btn--ghost btn--sm hide-sm">Sign in</Link>
              <Link to="/register" className="btn btn--sm">Get started</Link>
            </>
          )}

          <button
            type="button"
            className="btn btn--ghost btn--icon nav-toggle"
            onClick={() => setOpen((value) => !value)}
            aria-expanded={open}
            aria-label="Toggle navigation"
          >
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              {open ? <path d="M6 6l12 12M18 6L6 18" /> : <path d="M3 6h18M3 12h18M3 18h18" />}
            </svg>
          </button>
        </div>
      </div>
    </header>
  );
}
