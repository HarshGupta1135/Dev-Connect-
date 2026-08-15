import { useCallback, useEffect, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { m } from 'motion/react';
import toast from 'react-hot-toast';
import { errorMessage } from '../api/client';
import { JOB_TYPES, fetchJobs } from '../api/endpoints';
import JobCard from '../components/JobCard';
import EmptyState from '../components/EmptyState';
import Pagination from '../components/Pagination';
import SkillPicker from '../components/SkillPicker';
import { JobCardSkeleton } from '../components/Skeleton';
import { useAuth } from '../context/AuthContext';
import { useSavedJobs } from '../hooks/useSavedJobs';
import { titleCase } from '../utils/format';

const PAGE_SIZE = 9;

/* Spring stagger for the result grid: each search's results pour in rather than
   pop. The container re-keys on the query, so every new result set replays it. */
const gridStagger = {
  hidden: {},
  show: { transition: { staggerChildren: 0.05, delayChildren: 0.02 } },
};
const cardSpring = {
  hidden: { opacity: 0, y: 20, scale: 0.98 },
  show: { opacity: 1, y: 0, scale: 1, transition: { type: 'spring', stiffness: 260, damping: 24 } },
};

const SORTS = [
  { value: 'createdAt,desc', label: 'Newest first' },
  { value: 'createdAt,asc', label: 'Oldest first' },
  { value: 'title,asc', label: 'Title A–Z' },
  { value: 'experienceRequired,asc', label: 'Least experience' },
];

/**
 * Filters live in the URL, so a filtered search can be shared, bookmarked and
 * survives a refresh or the back button.
 */
export default function Jobs() {
  const [params, setParams] = useSearchParams();
  const { isDeveloper } = useAuth();
  const { count: savedCount, isSaved, clear } = useSavedJobs();

  /*
   * ?saved=1 filters the fetched page to bookmarked roles.
   *
   * Client-side because bookmarks live in localStorage and the API cannot filter
   * on them. A larger page is requested in that mode so the filter has the whole
   * catalogue to work against rather than page one of it — fine at this scale,
   * and the alternative is one request per saved id.
   */
  const savedOnly = params.get('saved') === '1';

  const page = Number(params.get('page') || 0);
  const sort = params.get('sort') || 'createdAt,desc';
  const type = params.get('type') || '';
  const skills = useMemo(() => params.getAll('skill'), [params]);
  const urlLocation = params.get('location') || '';

  const [locationDraft, setLocationDraft] = useState(urlLocation);
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => setLocationDraft(urlLocation), [urlLocation]);

  const patch = useCallback(
    (changes, { resetPage = true } = {}) => {
      const next = new URLSearchParams(params);
      Object.entries(changes).forEach(([key, value]) => {
        next.delete(key);
        if (Array.isArray(value)) value.forEach((entry) => next.append(key, entry));
        else if (value) next.set(key, value);
      });
      if (resetPage) next.delete('page');
      setParams(next, { replace: true });
    },
    [params, setParams]
  );

  // Typing in the location box should not fire a request per keystroke.
  useEffect(() => {
    if (locationDraft === urlLocation) return undefined;
    const timer = setTimeout(() => patch({ location: locationDraft }), 400);
    return () => clearTimeout(timer);
  }, [locationDraft, urlLocation, patch]);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);

    fetchJobs({ skills, location: urlLocation, type, page, size: savedOnly ? 100 : PAGE_SIZE, sort })
      .then((data) => {
        if (!cancelled) setResult(data);
      })
      .catch((error) => {
        if (!cancelled) {
          toast.error(errorMessage(error, 'Could not load jobs.'));
          setResult({ content: [], totalPages: 0, totalElements: 0 });
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [skills, urlLocation, type, page, sort, savedOnly]);

  const fetched = result?.content || [];
  const jobs = savedOnly ? fetched.filter((job) => isSaved(job.id)) : fetched;
  const activeFilters = skills.length + (urlLocation ? 1 : 0) + (type ? 1 : 0);

  const setSavedOnly = (value) => patch({ saved: value ? '1' : '' });

  return (
    <div className="wrap section--tight page-enter" style={{ paddingTop: 34, paddingBottom: 72 }}>
      <div className="stack" style={{ gap: 8, marginBottom: 22 }}>
        <span className="eyebrow">{savedOnly ? 'Your shortlist' : 'Open roles'}</span>
        <h1 style={{ fontSize: 'clamp(1.9rem, 4vw, 2.6rem)' }}>
          {savedOnly ? <>Saved <span className="grad-text">jobs</span></> : <>Browse <span className="grad-text">jobs</span></>}
        </h1>
        <p className="lede">
          {savedOnly
            ? 'Bookmarked on this device — they are not tied to your account, so another browser will not see them.'
            : isDeveloper
              ? 'Ordered by how well each role matches the skills on your profile.'
              : 'Sign in as a developer to see how well each role matches your skills.'}
        </p>
      </div>

      {/* Only offered once something is saved; an empty toggle is just noise. */}
      {savedCount > 0 && (
        <div className="row" style={{ gap: 8, marginBottom: 20, flexWrap: 'wrap' }}>
          <div className="tabs" role="tablist" aria-label="Job view">
            <button type="button" className="tab" role="tab" aria-selected={!savedOnly} onClick={() => setSavedOnly(false)}>
              All roles
            </button>
            <button type="button" className="tab" role="tab" aria-selected={savedOnly} onClick={() => setSavedOnly(true)}>
              Saved <span className="faint mono tiny">{savedCount}</span>
            </button>
          </div>
          {savedOnly && (
            <button type="button" className="btn btn--ghost btn--sm" onClick={clear}>
              Clear saved
            </button>
          )}
        </div>
      )}

      <div className="jobs-layout">
        <aside className="filter-rail">
          <div className="card card--pad stack" style={{ gap: 16 }}>
            <div className="spread">
              <strong style={{ fontSize: '0.95rem' }}>Filters</strong>
              {activeFilters > 0 && (
                <button
                  type="button"
                  className="btn btn--ghost btn--sm"
                  onClick={() => setParams(new URLSearchParams(), { replace: true })}
                >
                  Clear all
                </button>
              )}
            </div>

            <div className="field">
              <label htmlFor="skill-filter">Skills</label>
              <SkillPicker
                id="skill-filter"
                value={skills}
                onChange={(next) => patch({ skill: next })}
                placeholder="React, Java…"
              />
              <span className="field-hint">Matches roles requiring any of these.</span>
            </div>

            <div className="field">
              <label htmlFor="location-filter">Location</label>
              <input
                id="location-filter"
                type="text"
                value={locationDraft}
                placeholder="Bengaluru, remote…"
                onChange={(event) => setLocationDraft(event.target.value)}
              />
            </div>

            <div className="field">
              <label htmlFor="type-filter">Work style</label>
              <select id="type-filter" value={type} onChange={(event) => patch({ type: event.target.value })}>
                <option value="">Any</option>
                {JOB_TYPES.map((entry) => (
                  <option key={entry} value={entry}>{titleCase(entry)}</option>
                ))}
              </select>
            </div>
          </div>
        </aside>

        <section className="stack" style={{ gap: 18 }}>
          <div className="spread">
            <span className="small muted" aria-live="polite">
              {loading
                ? 'Searching…'
                : savedOnly
                  ? `${jobs.length} of your ${savedCount} saved role${savedCount === 1 ? '' : 's'} still open`
                  : `${result?.totalElements ?? 0} role${result?.totalElements === 1 ? '' : 's'} found`}
            </span>
            <div className="field" style={{ flexDirection: 'row', alignItems: 'center', gap: 8 }}>
              <label htmlFor="sort" className="tiny faint mono" style={{ textTransform: 'uppercase', letterSpacing: '0.1em' }}>
                Sort
              </label>
              <select
                id="sort"
                value={sort}
                onChange={(event) => patch({ sort: event.target.value })}
                style={{ width: 'auto', padding: '7px 34px 7px 11px', fontSize: '0.86rem' }}
              >
                {SORTS.map((entry) => (
                  <option key={entry.value} value={entry.value}>{entry.label}</option>
                ))}
              </select>
            </div>
          </div>

          {loading ? (
            <div className="job-grid">
              {Array.from({ length: 4 }, (_, index) => (
                <JobCardSkeleton key={index} />
              ))}
            </div>
          ) : jobs.length === 0 ? (
            <EmptyState
              mark={savedOnly ? '★' : '⌕'}
              title={savedOnly ? 'None of your saved roles are still open' : 'No roles match those filters'}
              message={
                savedOnly
                  ? 'Saved roles disappear from here once they close or expire. The bookmarks stay until you clear them.'
                  : activeFilters > 0
                    ? 'Try removing a filter, or widening the location.'
                    : 'There are no active job postings yet. Check back soon.'
              }
              action={
                savedOnly ? (
                  <button type="button" className="btn btn--outline btn--sm" onClick={() => setSavedOnly(false)}>
                    Browse all roles
                  </button>
                ) : (
                  activeFilters > 0 && (
                    <button
                      type="button"
                      className="btn btn--outline btn--sm"
                      onClick={() => setParams(new URLSearchParams(), { replace: true })}
                    >
                      Clear filters
                    </button>
                  )
                )
              }
            />
          ) : (
            <m.div
              className="job-grid"
              variants={gridStagger}
              initial="hidden"
              animate="show"
              key={`${page}|${sort}|${skills.join(',')}|${urlLocation}|${type}|${savedOnly}`}
            >
              {jobs.map((job) => (
                <m.div key={job.id} variants={cardSpring}>
                  <JobCard job={job} />
                </m.div>
              ))}
            </m.div>
          )}

          {/* Hidden while filtering saved roles: that view fetches one large page
              and filters it here, so the API's page count no longer describes it. */}
          {!savedOnly && (
            <Pagination
              page={page}
              totalPages={result?.totalPages || 0}
              onChange={(next) => patch({ page: String(next) }, { resetPage: false })}
            />
          )}
        </section>
      </div>
    </div>
  );
}
