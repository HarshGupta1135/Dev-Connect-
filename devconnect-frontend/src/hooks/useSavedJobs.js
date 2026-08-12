import { useCallback, useEffect, useState } from 'react';

/**
 * Bookmarked jobs, kept in localStorage.
 *
 * There is no backend for this — the API has no saved-jobs endpoint — so it is
 * deliberately per-device rather than per-account, and the UI says so. Storing an
 * id list rather than whole job objects keeps it honest: titles and salaries can
 * change, so the list is re-read from the API and stale ids simply disappear.
 *
 * Every hook instance listens for a custom event, so a save in one component is
 * reflected in the others without a shared provider.
 */

const KEY = 'devconnect.savedJobs';
const CHANGED = 'devconnect:savedJobsChanged';

function read() {
  try {
    const parsed = JSON.parse(localStorage.getItem(KEY) || '[]');
    return Array.isArray(parsed) ? parsed.filter((id) => Number.isFinite(id)) : [];
  } catch {
    return [];
  }
}

function write(ids) {
  localStorage.setItem(KEY, JSON.stringify(ids));
  window.dispatchEvent(new CustomEvent(CHANGED));
}

export function useSavedJobs() {
  const [ids, setIds] = useState(read);

  useEffect(() => {
    const sync = () => setIds(read());
    window.addEventListener(CHANGED, sync);
    // 'storage' only fires in *other* tabs, which is exactly the case the custom
    // event above cannot cover.
    window.addEventListener('storage', sync);
    return () => {
      window.removeEventListener(CHANGED, sync);
      window.removeEventListener('storage', sync);
    };
  }, []);

  const toggle = useCallback((id) => {
    const numeric = Number(id);
    const current = read();
    const next = current.includes(numeric)
      ? current.filter((entry) => entry !== numeric)
      : [numeric, ...current];
    write(next);
    return next.includes(numeric);
  }, []);

  const isSaved = useCallback((id) => ids.includes(Number(id)), [ids]);

  const clear = useCallback(() => write([]), []);

  return { savedIds: ids, count: ids.length, isSaved, toggle, clear };
}

/**
 * The last few jobs whose detail page was opened, newest first. Same reasoning as
 * above, and capped so the list stays a shortcut rather than a history.
 */

const RECENT_KEY = 'devconnect.recentJobs';
const RECENT_LIMIT = 6;

export function rememberViewedJob(job) {
  if (!job?.id) return;
  try {
    const previous = JSON.parse(localStorage.getItem(RECENT_KEY) || '[]');
    const entry = { id: job.id, title: job.title, companyName: job.companyName || null };
    const next = [entry, ...previous.filter((item) => item.id !== job.id)].slice(0, RECENT_LIMIT);
    localStorage.setItem(RECENT_KEY, JSON.stringify(next));
  } catch {
    /* A full or disabled localStorage is not worth failing a page render over. */
  }
}

export function readViewedJobs() {
  try {
    const parsed = JSON.parse(localStorage.getItem(RECENT_KEY) || '[]');
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}
