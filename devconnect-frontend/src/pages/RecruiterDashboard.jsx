import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import toast from 'react-hot-toast';
import { errorMessage } from '../api/client';
import {
  closeJob,
  createRecruiterProfile,
  fetchJobApplicants,
  fetchMyRecruiterProfile,
  fetchRecruiterJobs,
  updateRecruiterProfile,
} from '../api/endpoints';
import AccountSettings from '../components/AccountSettings';
import EmptyState from '../components/EmptyState';
import Field from '../components/Field';
import StatusBadge from '../components/StatusBadge';
import { RowSkeleton } from '../components/Skeleton';
import {
  effectiveJobStatus,
  experienceLabel,
  formatDate,
  isJobExpired,
  relativeTime,
  titleCase,
} from '../utils/format';
import { useAuth } from '../context/AuthContext';

const TABS = [
  { id: 'jobs', label: 'My jobs' },
  { id: 'company', label: 'Company profile' },
];

function CompanyForm({ profile, onSaved }) {
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm({
    defaultValues: {
      fullName: profile?.fullName || '',
      companyName: profile?.companyName || '',
      description: profile?.description || '',
      website: profile?.website || '',
      location: profile?.location || '',
    },
  });

  const onSubmit = async (values) => {
    try {
      const message = profile ? await updateRecruiterProfile(values) : await createRecruiterProfile(values);
      toast.success(message);
      onSaved({ ...(profile || {}), ...values });
    } catch (error) {
      toast.error(errorMessage(error, 'Could not save the company profile.'));
    }
  };

  return (
    <form className="card card--pad stack" style={{ gap: 18 }} onSubmit={handleSubmit(onSubmit)} noValidate>
      <div className="grid-2">
        <Field label="Your name" htmlFor="fullName" error={errors.fullName}>
          <input
            id="fullName"
            type="text"
            placeholder="Harsh Gupta"
            aria-invalid={Boolean(errors.fullName)}
            {...register('fullName', { required: 'Your name is required' })}
          />
        </Field>
        <Field label="Company name" htmlFor="companyName" error={errors.companyName}>
          <input id="companyName" type="text" placeholder="Acme Systems" {...register('companyName')} />
        </Field>
      </div>

      <Field label="About the company" htmlFor="description" optional error={errors.description}>
        <textarea id="description" placeholder="What the company does, team size, tech…" {...register('description')} />
      </Field>

      <div className="grid-2">
        <Field label="Website" htmlFor="website" optional error={errors.website}>
          <input id="website" type="url" placeholder="https://acme.com" {...register('website')} />
        </Field>
        <Field label="Location" htmlFor="location" optional error={errors.location}>
          <input id="location" type="text" placeholder="Bengaluru, India" {...register('location')} />
        </Field>
      </div>

      <button type="submit" className="btn" style={{ alignSelf: 'flex-start' }} disabled={isSubmitting}>
        {isSubmitting && <span className="spinner" />}
        {isSubmitting ? 'Saving…' : profile ? 'Save changes' : 'Create company profile'}
      </button>
    </form>
  );
}

export default function RecruiterDashboard() {
  const { user } = useAuth();
  const [tab, setTab] = useState('jobs');
  const [profile, setProfile] = useState(null);
  const [profileMissing, setProfileMissing] = useState(false);
  const [jobs, setJobs] = useState(null);
  const [counts, setCounts] = useState({});
  const [loading, setLoading] = useState(true);
  const [closingId, setClosingId] = useState(null);

  useEffect(() => {
    let cancelled = false;

    fetchMyRecruiterProfile()
      .then((data) => !cancelled && setProfile(data))
      .catch(() => !cancelled && setProfileMissing(true));

    fetchRecruiterJobs()
      .then(async (data) => {
        if (cancelled) return;
        const list = data || [];
        setJobs(list);

        // Applicant counts come from one call per job — the API has no summary endpoint.
        const results = await Promise.allSettled(list.map((job) => fetchJobApplicants(job.id)));
        if (cancelled) return;
        const next = {};
        results.forEach((result, index) => {
          if (result.status === 'fulfilled') next[list[index].id] = (result.value || []).length;
        });
        setCounts(next);
      })
      .catch(() => !cancelled && setJobs([]))
      .finally(() => !cancelled && setLoading(false));

    return () => {
      cancelled = true;
    };
  }, []);

  const stats = useMemo(() => {
    const list = jobs || [];
    const applicants = Object.values(counts).reduce((total, value) => total + value, 0);
    // An expired posting still stored as ACTIVE is not accepting applications, so it
    // counts with the closed ones — matching the badge on its row.
    const open = list.filter((job) => job.status === 'ACTIVE' && !isJobExpired(job));
    return {
      total: list.length,
      active: open.length,
      closed: list.length - open.length,
      applicants,
    };
  }, [jobs, counts]);

  const handleClose = async (job) => {
    setClosingId(job.id);
    try {
      await closeJob(job.id);
      toast.success(`“${job.title}” is now closed.`);
      setJobs((current) => current.map((entry) => (entry.id === job.id ? { ...entry, status: 'CLOSED' } : entry)));
    } catch (error) {
      toast.error(errorMessage(error, 'Could not close that job.'));
    } finally {
      setClosingId(null);
    }
  };

  return (
    <div className="wrap section--tight page-enter" style={{ paddingTop: 32, paddingBottom: 72 }}>
      <div className="dash-head">
        <span className="eyebrow">Recruiter</span>
        <h1 style={{ fontSize: 'clamp(1.8rem, 3.6vw, 2.4rem)' }}>
          {profile?.companyName || 'Your dashboard'}
        </h1>
        <p className="muted small">{profile?.fullName ? `${profile.fullName} · ${user?.email}` : user?.email}</p>
      </div>

      {profileMissing && (
        <div className="card card--pad spread" style={{ marginBottom: 22, borderColor: 'var(--accent)' }}>
          <div className="stack" style={{ gap: 3 }}>
            <strong>Create your company profile first</strong>
            <span className="small muted">Job postings are attached to it, so posting is blocked until it exists.</span>
          </div>
          <button type="button" className="btn" onClick={() => setTab('company')}>
            Create profile
          </button>
        </div>
      )}

      <div className="stat-strip" style={{ marginBottom: 24 }}>
        <div className="stat-cell">
          <span className="stat-cell__value">{loading ? '—' : stats.total}</span>
          <span className="eyebrow">Jobs posted</span>
        </div>
        <div className="stat-cell">
          <span className="stat-cell__value" style={{ color: 'var(--good)' }}>{loading ? '—' : stats.active}</span>
          <span className="eyebrow">Active</span>
        </div>
        <div className="stat-cell">
          <span className="stat-cell__value" style={{ color: 'var(--ink-muted)' }}>{loading ? '—' : stats.closed}</span>
          <span className="eyebrow">Closed</span>
        </div>
        <div className="stat-cell">
          <span className="stat-cell__value" style={{ color: 'var(--accent)' }}>{loading ? '—' : stats.applicants}</span>
          <span className="eyebrow">Applications</span>
        </div>
      </div>

      <div className="spread" style={{ marginBottom: 20 }}>
        <div className="tabs" role="tablist" aria-label="Dashboard sections">
          {TABS.map((entry) => (
            <button
              key={entry.id}
              type="button"
              className="tab"
              role="tab"
              aria-selected={tab === entry.id}
              onClick={() => setTab(entry.id)}
            >
              {entry.label}
            </button>
          ))}
        </div>
        <Link to="/recruiter/jobs/new" className="btn btn--sm">+ Post a job</Link>
      </div>

      {tab === 'jobs' && (
        <section role="tabpanel" aria-label="My jobs" className="list">
          {loading ? (
            Array.from({ length: 3 }, (_, index) => <RowSkeleton key={index} />)
          ) : jobs?.length ? (
            jobs.map((job) => (
              <article key={job.id} className="card list-row">
                <div className="list-row__main">
                  <div className="row" style={{ gap: 10, flexWrap: 'wrap' }}>
                    <Link to={`/jobs/${job.id}`} style={{ fontWeight: 650, fontSize: '1rem' }}>
                      {job.title}
                    </Link>
                    <StatusBadge status={effectiveJobStatus(job)} />
                  </div>
                  <span className="tiny faint mono">
                    {job.jobType ? `${titleCase(job.jobType)} · ` : ''}
                    {job.location ? `${job.location} · ` : ''}
                    {experienceLabel(job.experienceRequired)} · posted {relativeTime(job.createdAt)} · closes{' '}
                    {formatDate(job.expiresAt)}
                  </span>
                  {job.requiredSkills?.length > 0 && (
                    <div className="row" style={{ gap: 5, flexWrap: 'wrap', marginTop: 5 }}>
                      {job.requiredSkills.slice(0, 6).map((skill) => (
                        <span key={skill} className="chip">{skill}</span>
                      ))}
                    </div>
                  )}
                </div>

                <div className="row" style={{ gap: 8, flexWrap: 'wrap', justifyContent: 'flex-end' }}>
                  <Link to={`/recruiter/jobs/${job.id}/applicants`} className="btn btn--soft btn--sm">
                    {counts[job.id] ?? 0} applicant{counts[job.id] === 1 ? '' : 's'}
                  </Link>
                  {job.status === 'ACTIVE' && (
                    <button
                      type="button"
                      className="btn btn--danger btn--sm"
                      onClick={() => handleClose(job)}
                      disabled={closingId === job.id}
                    >
                      {closingId === job.id ? 'Closing…' : 'Close'}
                    </button>
                  )}
                </div>
              </article>
            ))
          ) : (
            <EmptyState
              mark="✎"
              title="No jobs posted yet"
              message={
                profileMissing
                  ? 'Create your company profile, then post your first role.'
                  : 'Post your first role and candidates can start applying against their skill match.'
              }
              action={
                !profileMissing && (
                  <Link to="/recruiter/jobs/new" className="btn">Post a job</Link>
                )
              }
            />
          )}
        </section>
      )}

      {tab === 'company' && (
        <section role="tabpanel" aria-label="Company profile" className="stack" style={{ gap: 34 }}>
          <CompanyForm
            profile={profile}
            onSaved={(saved) => {
              setProfile(saved);
              setProfileMissing(false);
            }}
          />
          <AccountSettings
            title="Account & sign-in"
            description="Your username and the email you sign in with — separate from the company details above."
          />
        </section>
      )}
    </div>
  );
}
