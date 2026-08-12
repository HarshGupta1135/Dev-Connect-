import { useEffect, useState } from 'react';
import { Link, NavLink, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import ThemeControl from './ThemeControl';
import { openCommandPalette } from './CommandPalette';
import { useSavedJobs } from '../hooks/useSavedJobs';

/** Looks like a search field because that is what it opens. */
function PaletteTrigger() {
  const isMac = typeof navigator !== 'undefined' && /Mac|iPhone|iPad/.test(navigator.platform || navigator.userAgent);

  return (
    <button type="button" className="cmdk-trigger" onClick={openCommandPalette} aria-label="Open command palette">
      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" style={{ flex: 'none' }}>
        <circle cx="11" cy="11" r="7" />
        <path d="M20 20l-3.5-3.5" />
      </svg>
      <span>Search or jump…</span>
      <span className="kbd">{isMac ? '⌘' : 'Ctrl'} K</span>
    </button>
  );
}

export default function Navbar() {
  const { isAuthenticated, isDeveloper, isRecruiter, user, logout } = useAuth();
  const { count: savedCount } = useSavedJobs();
  const [stuck, setStuck] = useState(false);
  const [open, setOpen] = useState(false);
  const location = useLocation();
  const navigate = useNavigate();

  const onJobs = location.pathname === '/jobs';
  const viewingSaved = onJobs && new URLSearchParams(location.search).get('saved') === '1';

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
          {/* Plain links with explicit current state: NavLink matches on pathname
              alone, so both of these would light up together on /jobs. */}
          <Link to="/jobs" className="nav-link" aria-current={onJobs && !viewingSaved ? 'page' : undefined}>
            Browse jobs
          </Link>
          {savedCount > 0 && (
            <Link to="/jobs?saved=1" className="nav-link" aria-current={viewingSaved ? 'page' : undefined}>
              Saved <span className="mono tiny faint">{savedCount}</span>
            </Link>
          )}
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
          <PaletteTrigger />
          <ThemeControl />

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
