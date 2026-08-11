import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function NotFound() {
  const { isAuthenticated, homeFor } = useAuth();

  return (
    <div className="wrap wrap--narrow section stack page-enter" style={{ gap: 16, alignItems: 'flex-start' }}>
      <span className="eyebrow">404</span>
      <h1 style={{ fontSize: 'clamp(2rem, 5vw, 3rem)' }}>
        This page took a <em className="italic-serif">different job</em>.
      </h1>
      <p className="lede">The link may be old, or the role behind it was removed.</p>
      <div className="row" style={{ gap: 10, flexWrap: 'wrap' }}>
        <Link to="/jobs" className="btn btn--lg">Browse jobs</Link>
        <Link to={isAuthenticated ? homeFor : '/'} className="btn btn--lg btn--outline">
          {isAuthenticated ? 'My dashboard' : 'Home'}
        </Link>
      </div>
    </div>
  );
}
