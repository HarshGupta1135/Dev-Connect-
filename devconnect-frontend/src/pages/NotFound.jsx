import { Link } from 'react-router-dom';
import { openCommandPalette } from '../components/CommandPalette';
import { useAuth } from '../context/AuthContext';

export default function NotFound() {
  const { isAuthenticated, homeFor } = useAuth();

  return (
    <div className="wrap wrap--narrow section stack page-enter" style={{ gap: 14, alignItems: 'flex-start' }}>
      <span className="err-code" aria-hidden="true">404</span>
      <h1 style={{ fontSize: 'clamp(1.8rem, 4.5vw, 2.6rem)' }}>
        This page took a <em className="italic-serif">different job</em>.
      </h1>
      <p className="lede">The link may be old, or the role behind it was removed.</p>

      <div className="row" style={{ gap: 10, flexWrap: 'wrap' }}>
        <Link to="/jobs" className="btn btn--lg btn--glow">Browse jobs</Link>
        <Link to={isAuthenticated ? homeFor : '/'} className="btn btn--lg btn--outline">
          {isAuthenticated ? 'My dashboard' : 'Home'}
        </Link>
      </div>

      <button
        type="button"
        onClick={openCommandPalette}
        className="small muted"
        style={{ background: 'none', border: 0, cursor: 'pointer', padding: 0, font: 'inherit', display: 'inline-flex', alignItems: 'center', gap: 8 }}
      >
        Or press <kbd>Ctrl</kbd><kbd>K</kbd> and search for what you were after
      </button>
    </div>
  );
}
