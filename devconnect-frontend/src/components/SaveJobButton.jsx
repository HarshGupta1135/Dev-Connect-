import toast from 'react-hot-toast';
import { useSavedJobs } from '../hooks/useSavedJobs';

/**
 * Bookmark toggle for a job.
 *
 * aria-pressed rather than a checkbox, because it is a two-state button; the CSS
 * fills the icon from that same attribute, so the visual and the announced state
 * cannot drift apart.
 */
export default function SaveJobButton({ jobId, title, className = '' }) {
  const { isSaved, toggle } = useSavedJobs();
  const saved = isSaved(jobId);

  const onClick = (event) => {
    // Job cards wrap this in a link to the detail page.
    event.preventDefault();
    event.stopPropagation();
    const nowSaved = toggle(jobId);
    toast(nowSaved ? 'Saved to this device' : 'Removed from saved', {
      icon: nowSaved ? '★' : '☆',
      duration: 1600,
    });
  };

  return (
    <button
      type="button"
      className={`save-btn ${className}`.trim()}
      aria-pressed={saved}
      aria-label={saved ? `Remove ${title || 'this role'} from saved` : `Save ${title || 'this role'}`}
      title={saved ? 'Saved — click to remove' : 'Save for later'}
      onClick={onClick}
    >
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinejoin="round">
        <path d="M7 4h10a1 1 0 0 1 1 1v15l-6-4.5L6 20V5a1 1 0 0 1 1-1z" />
      </svg>
    </button>
  );
}
