import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { openCommandPalette } from './CommandPalette';
import { useSavedJobs } from '../hooks/useSavedJobs';

/**
 * The API's own origin, for links that must reach the backend rather than this
 * app. In development it is empty and the Vite proxy covers the difference; in
 * production a relative /swagger-ui/... would be swallowed by the SPA rewrite —
 * Vercel rewrites every path to index.html, so the old footer link opened the
 * app's 404 page instead of Swagger.
 */
const API_ORIGIN = import.meta.env.VITE_API_BASE_URL || '';

export default function Footer() {
  const { isAuthenticated, isDeveloper, isRecruiter, homeFor } = useAuth();
  const { count: savedCount } = useSavedJobs();

  return (
    <footer className="footer">
      <div className="wrap">
        <div className="footer__grid">
          <div className="stack" style={{ gap: 10, alignItems: 'flex-start' }}>
            <span className="brand" style={{ fontSize: '0.98rem' }}>
              <span className="brand-mark" aria-hidden="true">&lt;/&gt;</span>
              DevConnect
            </span>
            <p className="small muted" style={{ maxWidth: '32ch' }}>
              A job board that scores every role against the skills you actually have —
              matched on skills, not keywords.
            </p>
            <button
              type="button"
              className="cmdk-trigger"
              onClick={openCommandPalette}
              aria-label="Open command palette"
              style={{ marginTop: 4 }}
            >
              <span>Search or jump…</span>
              <span className="kbd">Ctrl K</span>
            </button>
          </div>

          <nav className="footer__col" aria-label="Product">
            <span className="footer__head">Product</span>
            <Link to="/jobs" className="footer__link">Browse jobs</Link>
            {savedCount > 0 && <Link to="/jobs?saved=1" className="footer__link">Saved jobs</Link>}
            {isDeveloper && <Link to="/developer/profile" className="footer__link">My profile</Link>}
            {isRecruiter && <Link to="/recruiter/jobs/new" className="footer__link">Post a job</Link>}
            {isAuthenticated ? (
              <Link to={homeFor} className="footer__link">Dashboard</Link>
            ) : (
              <>
                <Link to="/register" className="footer__link">Create account</Link>
                <Link to="/login" className="footer__link">Sign in</Link>
              </>
            )}
          </nav>

          <nav className="footer__col" aria-label="Developers">
            <span className="footer__head">Developers</span>
            <a
              className="footer__link"
              href={`${API_ORIGIN}/swagger-ui/index.html`}
              target="_blank"
              rel="noreferrer"
            >
              API documentation
            </a>
            <a
              className="footer__link"
              href={`${API_ORIGIN}/health`}
              target="_blank"
              rel="noreferrer"
            >
              API status
            </a>
            <a
              className="footer__link"
              href="https://github.com/HarshGupta1135/Dev-Connect-"
              target="_blank"
              rel="noreferrer"
            >
              Source on GitHub
            </a>
          </nav>
        </div>

        <div className="footer__base">
          <span>© {new Date().getFullYear()} DevConnect</span>
          <span className="mono">Spring Boot · React · MySQL · Redis</span>
        </div>
      </div>
    </footer>
  );
}
