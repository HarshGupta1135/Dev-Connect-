import { Link } from 'react-router-dom';
import CompanyAvatar from './CompanyAvatar';
import MatchRing from './MatchRing';
import SaveJobButton from './SaveJobButton';
import StatusBadge from './StatusBadge';
import { useSpotlight } from '../hooks/useSpotlight';
import {
  daysUntil,
  effectiveJobStatus,
  excerpt,
  experienceLabel,
  relativeTime,
  titleCase,
} from '../utils/format';

export default function JobCard({ job, showStatus = false }) {
  const expiresIn = daysUntil(job.expiresAt);
  const closingSoon = expiresIn !== null && expiresIn >= 0 && expiresIn <= 7;
  const hasMatch = typeof job.matchPercentage === 'number';
  const spotlight = useSpotlight();

  return (
    <article
      className="card card--hover job-card spot"
      ref={spotlight.ref}
      onPointerMove={spotlight.onPointerMove}
    >
      <div className="job-card__head">
        <div className="row" style={{ gap: 12, minWidth: 0, alignItems: 'flex-start' }}>
          <CompanyAvatar name={job.companyName} />
          <div className="stack" style={{ gap: 3, minWidth: 0 }}>
            <Link to={`/jobs/${job.id}`} className="job-card__title">
              {job.title}
            </Link>
            <span className="job-card__company">{job.companyName || 'Company undisclosed'}</span>
          </div>
        </div>
        <div className="row" style={{ gap: 4, flex: 'none' }}>
          <SaveJobButton jobId={job.id} title={job.title} />
          {hasMatch && <MatchRing value={job.matchPercentage} />}
        </div>
      </div>

      <div className="job-card__meta">
        {job.jobType && <span className="chip">{titleCase(job.jobType)}</span>}
        {job.location && <span className="chip">{job.location}</span>}
        <span className="chip">{experienceLabel(job.experienceRequired)}</span>
        {showStatus && <StatusBadge status={effectiveJobStatus(job)} />}
        {closingSoon && (
          <span className="badge badge--warn">
            {expiresIn === 0 ? 'Closes today' : `${expiresIn}d left`}
          </span>
        )}
      </div>

      {job.description && <p className="job-card__excerpt">{excerpt(job.description)}</p>}

      {job.requiredSkills?.length > 0 && (
        <div className="job-card__meta">
          {job.requiredSkills.slice(0, 5).map((skill) => (
            <span key={skill} className="chip chip--accent">{skill}</span>
          ))}
          {job.requiredSkills.length > 5 && (
            <span className="chip">+{job.requiredSkills.length - 5}</span>
          )}
        </div>
      )}

      <hr className="divider" />

      <div className="job-card__foot">
        <span className="tiny faint mono">
          {job.createdAt ? `Posted ${relativeTime(job.createdAt)}` : ''}
        </span>
        <Link to={`/jobs/${job.id}`} className="btn btn--soft btn--sm job-card__go">
          View details
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
            <path d="M5 12h14M13 6l6 6-6 6" />
          </svg>
        </Link>
      </div>
    </article>
  );
}
