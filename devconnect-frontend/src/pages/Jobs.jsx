import { useCallback, useEffect, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import toast from 'react-hot-toast';
import { errorMessage } from '../api/client';
import { JOB_TYPES, fetchJobs } from '../api/endpoints';
import JobCard from '../components/JobCard';
import EmptyState from '../components/EmptyState';
import Pagination from '../components/Pagination';
import Reveal from '../components/Reveal';
import SkillPicker from '../components/SkillPicker';
import { JobCardSkeleton } from '../components/Skeleton';
import { useAuth } from '../context/AuthContext';
import { titleCase } from '../utils/format';

const PAGE_SIZE = 9;

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

    fetchJobs({ skills, location: urlLocation, type, page, size: PAGE_SIZE, sort })
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
  }, [skills, urlLocation, type, page, sort]);

  const jobs = result?.content || [];
  const activeFilters = skills.length + (urlLocation ? 1 : 0) + (type ? 1 : 0);

  return (
    <div className="wrap section--tight page-enter" style={{ paddingTop: 34, paddingBottom: 72 }}>
      <div className="stack" style={{ gap: 8, marginBottom: 26 }}>
        <span className="eyebrow">Open roles</span>
        <h1 style={{ fontSize: 'clamp(1.9rem, 4vw, 2.6rem)' }}>Browse jobs</h1>
        <p className="lede">
          {isDeveloper
            ? 'Ordered by how well each role matches the skills on your profile.'
            : 'Sign in as a developer to see how well each role matches your skills.'}
        </p>
      </div>

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
              mark="⌕"
              title="No roles match those filters"
              message={
                activeFilters > 0
                  ? 'Try removing a filter, or widening the location.'
                  : 'There are no active job postings yet. Check back soon.'
              }
              action={
                activeFilters > 0 && (
                  <button
                    type="button"
                    className="btn btn--outline btn--sm"
                    onClick={() => setParams(new URLSearchParams(), { replace: true })}
                  >
                    Clear filters
                  </button>
                )
              }
            />
          ) : (
            <div className="job-grid">
              {jobs.map((job, index) => (
                <Reveal key={job.id} delay={Math.min(index * 45, 240)}>
                  <JobCard job={job} />
                </Reveal>
              ))}
            </div>
          )}

          <Pagination
            page={page}
            totalPages={result?.totalPages || 0}
            onChange={(next) => patch({ page: String(next) }, { resetPage: false })}
          />
        </section>
      </div>
    </div>
  );
}
