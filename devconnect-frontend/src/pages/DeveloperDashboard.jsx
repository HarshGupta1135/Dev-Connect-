import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import toast from 'react-hot-toast';
import { errorMessage } from '../api/client';
import { fetchMyApplications, fetchMyDeveloperProfile } from '../api/endpoints';
import EmptyState from '../components/EmptyState';
import ResumeUpload from '../components/ResumeUpload';
import StatusBadge from '../components/StatusBadge';
import { RowSkeleton, Skeleton } from '../components/Skeleton';
import { experienceLabel, formatDate, relativeTime } from '../utils/format';
import { useAuth } from '../context/AuthContext';

const TABS = [
  { id: 'applications', label: 'My applications' },
  { id: 'profile', label: 'My profile' },
];

export default function DeveloperDashboard() {
  const { user } = useAuth();
  const [tab, setTab] = useState('applications');
  const [profile, setProfile] = useState(null);
  const [profileMissing, setProfileMissing] = useState(false);
  const [applications, setApplications] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;

    const loadProfile = fetchMyDeveloperProfile()
      .then((data) => !cancelled && setProfile(data))
      .catch(() => !cancelled && setProfileMissing(true));

    const loadApplications = fetchMyApplications()
      .then((data) => !cancelled && setApplications(data || []))
      .catch((error) => {
        if (cancelled) return;
        setApplications([]);
        // A missing profile also fails this call; the banner already explains that.
        if (!/profile/i.test(errorMessage(error, ''))) {
          toast.error(errorMessage(error, 'Could not load your applications.'));
        }
      });

    Promise.allSettled([loadProfile, loadApplications]).then(() => {
      if (!cancelled) setLoading(false);
    });

    return () => {
      cancelled = true;
    };
  }, []);

  const counts = useMemo(() => {
    const list = applications || [];
    return {
      total: list.length,
      applied: list.filter((entry) => entry.status === 'APPLIED').length,
      shortlisted: list.filter((entry) => entry.status === 'SHORTLISTED').length,
      rejected: list.filter((entry) => entry.status === 'REJECTED').length,
    };
  }, [applications]);

  return (
    <div className="wrap section--tight page-enter" style={{ paddingTop: 32, paddingBottom: 72 }}>
      <div className="dash-head">
        <span className="eyebrow">Developer</span>
        <h1 style={{ fontSize: 'clamp(1.8rem, 3.6vw, 2.4rem)' }}>
          {profile?.fullName ? `Hello, ${profile.fullName.split(' ')[0]}` : 'Your dashboard'}
        </h1>
        <p className="muted small">{user?.email}</p>
      </div>

      {profileMissing && (
        <div className="card card--pad spread" style={{ marginBottom: 22, borderColor: 'var(--accent)' }}>
          <div className="stack" style={{ gap: 3 }}>
            <strong>Finish your profile to start applying</strong>
            <span className="small muted">
              Applications need a developer profile with your skills — it is also what match scores use.
            </span>
          </div>
          <Link to="/developer/profile" className="btn">Set up profile</Link>
        </div>
      )}

      <div className="stat-strip" style={{ marginBottom: 24 }}>
        <div className="stat-cell">
          <span className="stat-cell__value">{loading ? '—' : counts.total}</span>
          <span className="eyebrow">Applications</span>
        </div>
        <div className="stat-cell">
          <span className="stat-cell__value" style={{ color: 'var(--accent)' }}>{loading ? '—' : counts.applied}</span>
          <span className="eyebrow">In review</span>
        </div>
        <div className="stat-cell">
          <span className="stat-cell__value" style={{ color: 'var(--good)' }}>{loading ? '—' : counts.shortlisted}</span>
          <span className="eyebrow">Shortlisted</span>
        </div>
        <div className="stat-cell">
          <span className="stat-cell__value" style={{ color: 'var(--bad)' }}>{loading ? '—' : counts.rejected}</span>
          <span className="eyebrow">Not selected</span>
        </div>
      </div>

      <div className="tabs" role="tablist" aria-label="Dashboard sections" style={{ marginBottom: 20 }}>
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

      {tab === 'applications' && (
        <section role="tabpanel" aria-label="My applications" className="list">
          {loading ? (
            Array.from({ length: 3 }, (_, index) => <RowSkeleton key={index} />)
          ) : applications?.length ? (
            applications.map((application) => (
              <article key={application.id} className="card list-row">
                <div className="list-row__main">
                  <strong style={{ fontSize: '1rem' }}>{application.jobTitle || 'Role removed'}</strong>
                  <span className="tiny faint mono">
                    Applied {relativeTime(application.appliedAt)} · {formatDate(application.appliedAt)}
                    {application.updatedAt && application.updatedAt !== application.appliedAt
                      ? ` · updated ${relativeTime(application.updatedAt)}`
                      : ''}
                  </span>
                  {application.coverNote && (
                    <span className="small muted" style={{ marginTop: 4, maxWidth: '70ch' }}>
                      “{application.coverNote}”
                    </span>
                  )}
                </div>
                <StatusBadge status={application.status} />
              </article>
            ))
          ) : (
            <EmptyState
              mark="✦"
              title="No applications yet"
              message="Browse the open roles and apply to the ones that match your skills."
              action={<Link to="/jobs" className="btn">Browse jobs</Link>}
            />
          )}
        </section>
      )}

      {tab === 'profile' && (
        <section role="tabpanel" aria-label="My profile" className="stack" style={{ gap: 16 }}>
          {loading ? (
            <div className="card card--pad stack" style={{ gap: 12 }}>
              <Skeleton width="40%" height={16} />
              <Skeleton height={12} />
              <Skeleton width="70%" height={12} />
            </div>
          ) : profile ? (
            <>
              <div className="card card--pad stack" style={{ gap: 18 }}>
                <div className="spread">
                  <h3>{profile.fullName || 'Unnamed profile'}</h3>
                  <Link to="/developer/profile" className="btn btn--outline btn--sm">Edit profile</Link>
                </div>
                <dl className="kv">
                  <dt>Location</dt>
                  <dd>{profile.location || '—'}</dd>
                  <dt>Experience</dt>
                  <dd>{experienceLabel(profile.yearsExp)}</dd>
                  <dt>Bio</dt>
                  <dd style={{ whiteSpace: 'pre-line' }}>{profile.bio || '—'}</dd>
                  <dt>LinkedIn</dt>
                  <dd>
                    {profile.linkedinUrl ? (
                      <a href={profile.linkedinUrl} target="_blank" rel="noreferrer" style={{ color: 'var(--accent)' }}>
                        {profile.linkedinUrl}
                      </a>
                    ) : (
                      '—'
                    )}
                  </dd>
                  <dt>Skills</dt>
                  <dd>
                    {profile.skills?.length ? (
                      <div className="row" style={{ gap: 6, flexWrap: 'wrap' }}>
                        {profile.skills.map((skill) => (
                          <span key={skill} className="chip chip--accent">{skill}</span>
                        ))}
                      </div>
                    ) : (
                      <span className="muted">No skills added — match scores will read 0%.</span>
                    )}
                  </dd>
                </dl>
              </div>

              <div className="card card--pad stack" style={{ gap: 14 }}>
                <h3>Resume</h3>
                <ResumeUpload
                  resumeUrl={profile.resumeUrl}
                  onUploaded={(url) => setProfile((current) => ({ ...current, resumeUrl: url }))}
                />
              </div>
            </>
          ) : (
            <EmptyState
              mark="👤"
              title="No profile yet"
              message="Create your developer profile to apply for roles and get match scores."
              action={<Link to="/developer/profile" className="btn">Create profile</Link>}
            />
          )}
        </section>
      )}
    </div>
  );
}
