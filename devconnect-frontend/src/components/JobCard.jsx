import { Link } from 'react-router-dom';
import MatchRing from './MatchRing';
import StatusBadge from './StatusBadge';
import { daysUntil, excerpt, experienceLabel, relativeTime, titleCase } from '../utils/format';

export default function JobCard({ job, showStatus = false }) {
  const expiresIn = daysUntil(job.expiresAt);
  const closingSoon = expiresIn !== null && expiresIn >= 0 && expiresIn <= 7;
  const hasMatch = typeof job.matchPercentage === 'number';

  return (
    <article className="card card--hover job-card">
      <div className="job-card__head">
        <div className="stack" style={{ gap: 3, minWidth: 0 }}>
          <Link to={`/jobs/${job.id}`} className="job-card__title">
            {job.title}
          </Link>
          <span className="job-card__company">{job.companyName || 'Company undisclosed'}</span>
        </div>
        {hasMatch && <MatchRing value={job.matchPercentage} />}
      </div>

      <div className="job-card__meta">
        {job.jobType && <span className="chip">{titleCase(job.jobType)}</span>}
        {job.location && <span className="chip">{job.location}</span>}
        <span className="chip">{experienceLabel(job.experienceRequired)}</span>
        {showStatus && <StatusBadge status={job.status} />}
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
        <Link to={`/jobs/${job.id}`} className="btn btn--soft btn--sm">
          View details →
        </Link>
      </div>
    </article>
  );
}
