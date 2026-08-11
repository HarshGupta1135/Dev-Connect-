const LABELS = {
  APPLIED: 'Applied',
  SHORTLISTED: 'Shortlisted',
  REJECTED: 'Not selected',
  ACTIVE: 'Active',
  CLOSED: 'Closed',
};

const TONES = {
  APPLIED: 'applied',
  SHORTLISTED: 'shortlisted',
  REJECTED: 'rejected',
  ACTIVE: 'active',
  CLOSED: 'closed',
};

export default function StatusBadge({ status }) {
  if (!status) return <span className="badge badge--closed">Unknown</span>;
  const key = String(status).toUpperCase();
  return <span className={`badge badge--${TONES[key] || 'closed'}`}>{LABELS[key] || key}</span>;
}
