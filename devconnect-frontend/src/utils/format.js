const DATE = new Intl.DateTimeFormat(undefined, { day: 'numeric', month: 'short', year: 'numeric' });

export function formatDate(value) {
  if (!value) return '—';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? '—' : DATE.format(date);
}

export function relativeTime(value) {
  if (!value) return '';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '';

  const seconds = Math.round((Date.now() - date.getTime()) / 1000);
  const units = [
    ['year', 31536000],
    ['month', 2592000],
    ['week', 604800],
    ['day', 86400],
    ['hour', 3600],
    ['minute', 60],
  ];
  const rtf = new Intl.RelativeTimeFormat(undefined, { numeric: 'auto' });
  for (const [unit, secondsPerUnit] of units) {
    if (Math.abs(seconds) >= secondsPerUnit) {
      return rtf.format(-Math.round(seconds / secondsPerUnit), unit);
    }
  }
  return 'just now';
}

export function daysUntil(value) {
  if (!value) return null;
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return null;
  return Math.ceil((date.getTime() - Date.now()) / 86400000);
}

/** True once the closing date has passed, whatever the stored status says. */
export function isJobExpired(job) {
  const days = daysUntil(job?.expiresAt);
  return days !== null && days < 0;
}

/**
 * What the posting actually is right now, rather than what its row says.
 *
 * The backend closes expired postings from a midnight cron, so a job stays
 * ACTIVE in the database for up to a day after its closing date — longer if the
 * app was not running at midnight. Applying is already refused in that window,
 * so showing "Active" would contradict the rest of the page.
 *
 * A recruiter closing a job early still wins over the date: that is CLOSED, not
 * expired.
 */
export function effectiveJobStatus(job) {
  if (!job?.status) return job?.status;
  if (String(job.status).toUpperCase() !== 'ACTIVE') return job.status;
  return isJobExpired(job) ? 'EXPIRED' : 'ACTIVE';
}

export function titleCase(value) {
  if (!value) return '';
  return value.charAt(0) + value.slice(1).toLowerCase();
}

export function experienceLabel(years) {
  if (years === null || years === undefined) return 'Any experience';
  if (years === 0) return 'Entry level';
  return `${years}+ yr${years === 1 ? '' : 's'}`;
}

/** "Full stack developer needed…" → a short excerpt for cards. */
export function excerpt(text, max = 160) {
  if (!text) return '';
  const clean = text.replace(/\s+/g, ' ').trim();
  return clean.length > max ? `${clean.slice(0, max)}…` : clean;
}
