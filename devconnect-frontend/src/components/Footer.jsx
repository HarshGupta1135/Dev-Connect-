import { Link } from 'react-router-dom';

export default function Footer() {
  return (
    <footer className="footer">
      <div className="wrap spread">
        <div className="stack" style={{ gap: 2 }}>
          <span className="brand" style={{ fontSize: '0.95rem' }}>
            <span className="brand-mark" aria-hidden="true">&lt;/&gt;</span>
            DevConnect
          </span>
          <span className="tiny faint">Matched on skills, not keywords.</span>
        </div>
        <nav className="row" style={{ gap: 18 }} aria-label="Footer">
          <Link to="/jobs" className="small muted">Browse jobs</Link>
          <Link to="/register" className="small muted">Create account</Link>
          <a className="small muted" href="/swagger-ui/index.html" target="_blank" rel="noreferrer">
            API docs
          </a>
        </nav>
      </div>
    </footer>
  );
}
