import MatchRing from './MatchRing';
import { REQUIRED_PROFILE_FIELDS, profileCompleteness } from '../utils/profile';

/**
 * Live apply-readiness meter for the profile page.
 *
 * Fed the current form values rather than the saved record, so the ring moves
 * the moment a field is filled in — the point is to make finishing the profile
 * feel like progress rather than admin. The field list is the same one the API
 * enforces at apply time, so 100% here is exactly "the Apply button works".
 */
export default function ProfileCompleteness({ values }) {
  const { missing, done, total, percent } = profileCompleteness(values);
  const ready = missing.length === 0;

  return (
    <div className="card card--pad lit spread" style={{ gap: 18, marginBottom: 20, alignItems: 'center' }}>
      <div className="stack" style={{ gap: 8, minWidth: 0 }}>
        <div className="row" style={{ gap: 10, flexWrap: 'wrap' }}>
          <strong>{ready ? 'Apply-ready' : 'Almost apply-ready'}</strong>
          {ready ? (
            <span className="badge badge--shortlisted">All set</span>
          ) : (
            <span className="tiny faint">{done} of {total} required fields</span>
          )}
        </div>
        <div className="row" style={{ gap: 6, flexWrap: 'wrap' }}>
          {REQUIRED_PROFILE_FIELDS.map((field) => {
            const filled = !missing.some((entry) => entry.key === field.key);
            return (
              <span key={field.key} className={filled ? 'chip chip--accent' : 'badge badge--warn'}>
                {filled ? '✓ ' : ''}{field.label}
              </span>
            );
          })}
        </div>
        {!ready && (
          <span className="tiny muted">
            Applications are refused until every field above is filled — recruiters get this
            with each application you send.
          </span>
        )}
      </div>
      <MatchRing value={percent} size={64} stroke={5} ariaLabel={`Profile ${percent} percent complete`} />
    </div>
  );
}
