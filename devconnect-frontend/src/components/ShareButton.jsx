import toast from 'react-hot-toast';

/**
 * Share a job.
 *
 * Prefers the native share sheet where there is one — that is what a phone user
 * expects — and falls back to the clipboard. Both are permission-gated and both
 * reject if the user dismisses the sheet, so a cancelled share must not be
 * reported as a failure.
 */
export default function ShareButton({ title, text, className = 'btn btn--outline btn--sm' }) {
  const share = async () => {
    const url = window.location.href;

    if (navigator.share) {
      try {
        await navigator.share({ title, text, url });
        return;
      } catch (error) {
        // AbortError is the visitor closing the sheet: nothing went wrong.
        if (error?.name === 'AbortError') return;
        // Anything else (no permission, unsupported payload) falls through to copy.
      }
    }

    try {
      await navigator.clipboard.writeText(url);
      toast.success('Link copied');
    } catch {
      toast.error('Could not copy the link — your browser blocked clipboard access.');
    }
  };

  return (
    <button type="button" className={className} onClick={share}>
      <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
        <path d="M8.6 13.5 15.4 17M15.4 7 8.6 10.5" />
        <circle cx="18" cy="5.5" r="2.6" />
        <circle cx="6" cy="12" r="2.6" />
        <circle cx="18" cy="18.5" r="2.6" />
      </svg>
      Share
    </button>
  );
}
