import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { fetchJobs } from '../api/endpoints';
import JobCard from '../components/JobCard';
import MatchExplainer from '../components/MatchExplainer';
import Reveal from '../components/Reveal';
import { JobCardSkeleton } from '../components/Skeleton';
import { useAuth } from '../context/AuthContext';
import { useSpotlight } from '../hooks/useSpotlight';

/** Duplicated so the strip can loop seamlessly at -50%. */
const TICKER_SKILLS = [
  'Java', 'Spring Boot', 'React', 'TypeScript', 'Python', 'MySQL', 'Redis', 'Docker',
  'Kubernetes', 'AWS', 'Node.js', 'GraphQL', 'PostgreSQL', 'Kafka', 'Go', 'Rust',
];

/** Counts up to the real figure once it arrives, so the numbers feel live. */
function CountUp({ value = 0, duration = 900 }) {
  const [shown, setShown] = useState(0);

  useEffect(() => {
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
      setShown(value);
      return undefined;
    }
    let frame;
    const start = performance.now();
    const tick = (now) => {
      const progress = Math.min(1, (now - start) / duration);
      // ease-out so it settles rather than stopping dead
      setShown(Math.round(value * (1 - (1 - progress) ** 3)));
      if (progress < 1) frame = requestAnimationFrame(tick);
    };
    frame = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(frame);
  }, [value, duration]);

  return <span>{shown}</span>;
}

export default function Landing() {
  const navigate = useNavigate();
  const { isAuthenticated, isDeveloper, homeFor } = useAuth();
  const [featured, setFeatured] = useState(null);
  const [total, setTotal] = useState(0);
  const [query, setQuery] = useState('');

  useEffect(() => {
    fetchJobs({ page: 0, size: 3, sort: 'createdAt,desc' })
      .then((data) => {
        setFeatured(data?.content || []);
        setTotal(data?.totalElements || 0);
      })
      .catch(() => setFeatured([]));
  }, []);

  const search = (event) => {
    event.preventDefault();
    const trimmed = query.trim();
    navigate(trimmed ? `/jobs?skill=${encodeURIComponent(trimmed)}` : '/jobs');
  };

  return (
    <div className="page-enter">
      <section className="hero">
        <div className="hero__grid" aria-hidden="true" />
        <div className="wrap hero__inner">
          <span className="chip chip--accent" style={{ gap: 8 }}>
            <span style={{ width: 6, height: 6, borderRadius: '50%', background: 'currentColor', boxShadow: '0 0 8px currentColor' }} />
            Skill-matched hiring
          </span>
          <h1 style={{ maxWidth: '19ch' }}>
            Get found for what you <em className="grad-text">actually</em> build.
          </h1>
          <p className="lede">
            DevConnect scores every opening against the skills on your profile, so the roles you
            can genuinely do rise to the top — no keyword roulette.
          </p>

          <form className="searchbar" onSubmit={search} role="search">
            <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" style={{ color: 'var(--ink-faint)', flex: 'none' }}>
              <circle cx="11" cy="11" r="7" />
              <path d="M20 20l-3.5-3.5" />
            </svg>
            <input
              type="search"
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              placeholder="Search by skill — React, Spring Boot, Postgres…"
              aria-label="Search jobs by skill"
            />
            <button type="submit" className="btn btn--sm" style={{ borderRadius: 'var(--r-pill)', padding: '8px 16px' }}>
              Search
            </button>
          </form>

          <div className="hero__cta">
            {isAuthenticated ? (
              <Link to={homeFor} className="btn btn--lg btn--glow">Go to my dashboard</Link>
            ) : (
              <>
                <Link to="/register" className="btn btn--lg btn--glow">Create free account</Link>
                <Link to="/jobs" className="btn btn--lg btn--outline">Browse {total > 0 ? `${total} ` : ''}open roles</Link>
              </>
            )}
          </div>

          <p className="tiny faint" style={{ display: 'flex', alignItems: 'center', gap: 7, flexWrap: 'wrap' }}>
            Press <kbd>Ctrl</kbd><kbd>K</kbd> anywhere to search roles, jump between pages or switch theme.
          </p>
        </div>
      </section>

      {/* The skills the platform actually scores against — concrete, and it fills
          the band under the hero with motion rather than another paragraph. */}
      <section className="wrap" style={{ paddingBottom: 8 }}>
        <div className="ticker" aria-hidden="true">
          <div className="ticker__track">
            {[...TICKER_SKILLS, ...TICKER_SKILLS].map((skill, index) => (
              <span key={`${skill}-${index}`} className="chip">{skill}</span>
            ))}
          </div>
        </div>
      </section>

      <section className="wrap section--tight">
        <Reveal>
          <div className="stat-strip">
            <div className="stat-cell">
              <span className="stat-cell__value"><CountUp value={total} /></span>
              <span className="eyebrow">Active roles</span>
            </div>
            <div className="stat-cell">
              <span className="stat-cell__value">2</span>
              <span className="eyebrow">Sides, one platform</span>
            </div>
            <div className="stat-cell">
              <span className="stat-cell__value">%</span>
              <span className="eyebrow">Match score per role</span>
            </div>
            <div className="stat-cell">
              <span className="stat-cell__value">10h</span>
              <span className="eyebrow">Session length</span>
            </div>
          </div>
        </Reveal>
      </section>

      <section className="wrap section">
        <div className="spread" style={{ marginBottom: 22 }}>
          <div className="stack" style={{ gap: 6 }}>
            <span className="eyebrow">Fresh postings</span>
            <h2>Latest roles</h2>
          </div>
          <Link to="/jobs" className="btn btn--outline btn--sm">See all →</Link>
        </div>

        <div className="job-grid" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))' }}>
          {featured === null ? (
            Array.from({ length: 3 }, (_, index) => <JobCardSkeleton key={index} />)
          ) : featured.length === 0 ? (
            <div className="card card--pad muted">
              No active roles are posted yet. If you are a recruiter,{' '}
              <Link to="/register" style={{ color: 'var(--accent)', fontWeight: 600 }}>post the first one</Link>.
            </div>
          ) : (
            featured.map((job, index) => (
              <Reveal key={job.id} delay={index * 70}>
                <JobCard job={job} />
              </Reveal>
            ))
          )}
        </div>
      </section>

      <section className="wrap section" style={{ paddingTop: 0 }}>
        <Reveal>
          <div className="stack" style={{ gap: 8, marginBottom: 22 }}>
            <span className="eyebrow">How it works</span>
            <h2>Three steps, either side of the table</h2>
          </div>
        </Reveal>

        <div className="bento">
          <Reveal delay={0} className="span-4">
            <MatchExplainer />
          </Reveal>

          <Reveal delay={80}>
            <div className="card feature spot lit" style={{ height: '100%' }}>
              <span className="feature__num">FOR DEVELOPERS</span>
              <h3>List the skills you really have</h3>
              <p className="small muted">
                Build a profile once — bio, experience, skills, resume. Every role you browse is
                then scored against it.
              </p>
              {!isAuthenticated && (
                <Link to="/register" className="btn btn--soft btn--sm" style={{ alignSelf: 'flex-start', marginTop: 'auto' }}>
                  Create profile
                </Link>
              )}
              {isDeveloper && (
                <Link to="/developer/profile" className="btn btn--soft btn--sm" style={{ alignSelf: 'flex-start', marginTop: 'auto' }}>
                  Edit my profile
                </Link>
              )}
            </div>
          </Reveal>

          <Reveal delay={140} className="span-3">
            <div className="card feature spot" style={{ height: '100%' }}>
              <span className="feature__num">FOR RECRUITERS</span>
              <h3>Post a role with its real requirements</h3>
              <p className="small muted">
                Pick the skills that matter, set the experience bar, and let candidates self-select
                against a visible match score — then review each applicant's full profile,
                shortlist or pass in one click.
              </p>
            </div>
          </Reveal>

          <Reveal delay={200} className="span-3">
            <div className="card feature spot" style={{ height: '100%' }}>
              <span className="feature__num">FOR BOTH</span>
              <h3>Decisions, not silence</h3>
              <p className="small muted">
                Shortlist or pass, and the candidate hears about it by email — with the status
                visible in their dashboard either way. No application vanishes into a queue.
              </p>
            </div>
          </Reveal>
        </div>
      </section>
    </div>
  );
}
