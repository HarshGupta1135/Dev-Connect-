import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import toast from 'react-hot-toast';
import { errorMessage } from '../api/client';
import { applyToJob, fetchJob, fetchMyApplications, fetchMyDeveloperProfile } from '../api/endpoints';
import MatchRing from '../components/MatchRing';
import Modal from '../components/Modal';
import SaveJobButton from '../components/SaveJobButton';
import ShareButton from '../components/ShareButton';
import StatusBadge from '../components/StatusBadge';
import { rememberViewedJob } from '../hooks/useSavedJobs';
import { missingProfileFields } from '../utils/profile';
import { Skeleton } from '../components/Skeleton';
import { ReadingProgress } from '../components/ScrollHelpers';
import { useAuth } from '../context/AuthContext';
import {
  daysUntil,
  effectiveJobStatus,
  experienceLabel,
  formatDate,
  isJobExpired,
  relativeTime,
  titleCase,
} from '../utils/format';

export default function JobDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { isAuthenticated, isDeveloper } = useAuth();

  const [job, setJob] = useState(null);
  const [loading, setLoading] = useState(true);
  const [myApplication, setMyApplication] = useState(null);
  const [modalOpen, setModalOpen] = useState(false);
  const [coverNote, setCoverNote] = useState('');
  const [sending, setSending] = useState(false);
  // undefined = still loading, null = no profile at all, object = the profile
  const [myProfile, setMyProfile] = useState(undefined);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);

    fetchJob(id)
      .then((data) => {
        if (cancelled) return;
        setJob(data);
        // Feeds the command palette's "recently viewed" list.
        rememberViewedJob(data);
      })
      .catch((error) => {
        if (!cancelled) toast.error(errorMessage(error, 'That job could not be loaded.'));
      })
      .finally(() => !cancelled && setLoading(false));

    return () => {
      cancelled = true;
    };
  }, [id]);

  /*
   * There is no "did I apply to job X" endpoint, so this scans the developer's own
   * applications for one against this posting.
   *
   * Matched on jobId, not title: a recruiter reposting the same role creates a
   * genuinely separate posting, and a decision on the earlier one says nothing about
   * this one. Matching on title used to block a rejected candidate from ever applying
   * to that role again.
   */
  useEffect(() => {
    if (!isDeveloper || !job?.id) return;
    fetchMyApplications()
      .then((applications) => {
        const mine = (applications || []).find((application) => application.jobId === job.id);
        setMyApplication(mine || null);
      })
      .catch(() => {
        /* Not critical: the Apply button stays available and the API decides. */
      });
  }, [isDeveloper, job?.id]);

  /*
   * The API refuses applications from an incomplete profile, so the page finds
   * out first and turns the refusal into a checklist instead of an error toast.
   */
  useEffect(() => {
    if (!isDeveloper) return;
    fetchMyDeveloperProfile()
      .then(setMyProfile)
      .catch(() => setMyProfile(null));
  }, [isDeveloper]);

  /*
   * Real particle physics for the one moment worth it. canvas-confetti draws to a
   * single canvas (cheaper than a screen of DOM nodes), loads on demand so its
   * bytes are only ever paid by someone who actually applies, and its own
   * disableForReducedMotion honours the preference. Decoration: if the import
   * fails, the application still succeeded and nothing else notices.
   */
  const fireConfetti = async () => {
    try {
      const { default: confetti } = await import('canvas-confetti');
      const base = { disableForReducedMotion: true, zIndex: 500 };
      confetti({ ...base, particleCount: 90, spread: 75, origin: { y: 0.7 } });
      setTimeout(() => confetti({ ...base, particleCount: 50, angle: 60, spread: 60, origin: { x: 0, y: 0.9 } }), 140);
      setTimeout(() => confetti({ ...base, particleCount: 50, angle: 120, spread: 60, origin: { x: 1, y: 0.9 } }), 280);
    } catch {
      /* nothing to do — the celebration is optional, the application is not */
    }
  };

  const submitApplication = async () => {
    setSending(true);
    try {
      await applyToJob({ jobId: job.id, coverNote: coverNote.trim() || null });
      toast.success('Application submitted. Check your email for confirmation.');
      setMyApplication({ jobId: job.id, status: 'APPLIED' });
      setModalOpen(false);
      setCoverNote('');
      fireConfetti();
    } catch (error) {
      const message = errorMessage(error, 'Could not submit your application.');
      toast.error(message);
      if (/already applied/i.test(message)) {
        setMyApplication({ jobId: job.id, status: 'APPLIED' });
        setModalOpen(false);
      }
      if (/profile not found/i.test(message)) {
        toast('Create your developer profile first.', { icon: '👤' });
        navigate('/developer/profile');
      }
    } finally {
      setSending(false);
    }
  };

  if (loading) {
    return (
      <div className="wrap wrap--narrow section">
        <div className="stack" style={{ gap: 14 }}>
          <Skeleton width="30%" height={12} />
          <Skeleton width="70%" height={34} />
          <Skeleton width="45%" height={14} />
          <Skeleton height={180} radius={16} style={{ marginTop: 18 }} />
        </div>
      </div>
    );
  }

  if (!job) {
    return (
      <div className="wrap wrap--narrow section stack" style={{ gap: 14, alignItems: 'flex-start' }}>
        <h2>This role is not available</h2>
        <p className="muted">It may have been removed by the recruiter.</p>
        <Link to="/jobs" className="btn">Back to all jobs</Link>
      </div>
    );
  }

  const expiresIn = daysUntil(job.expiresAt);
  const isClosed = job.status !== 'ACTIVE';
  const isExpired = isJobExpired(job);
  // Scoped to this posting: a decision on any other one, including an earlier version
  // of the same role, does not stand in the way here.
  const applied = Boolean(myApplication);
  const missing = isDeveloper && myProfile !== undefined ? missingProfileFields(myProfile) : [];
  const profileReady = isDeveloper && myProfile !== undefined && missing.length === 0;
  const canApply = isDeveloper && !isClosed && !isExpired && !applied && profileReady;
  const needsProfileWork =
    isDeveloper && !isClosed && !isExpired && !applied && myProfile !== undefined && missing.length > 0;

  return (
    <>
      <ReadingProgress />
      <div className="wrap section--tight page-enter" style={{ paddingTop: 30, paddingBottom: 72 }}>
        <Link to="/jobs" className="small muted" style={{ display: 'inline-block', marginBottom: 18 }}>
          ← All jobs
        </Link>

        <div className="card card--pad stack" style={{ gap: 22 }}>
          <div className="spread" style={{ alignItems: 'flex-start' }}>
            <div className="stack" style={{ gap: 6, minWidth: 0 }}>
              <span className="eyebrow">{job.companyName || 'Company undisclosed'}</span>
              <h1 style={{ fontSize: 'clamp(1.7rem, 3.6vw, 2.5rem)' }}>{job.title}</h1>
              <div className="row" style={{ gap: 6, flexWrap: 'wrap', marginTop: 4 }}>
                <StatusBadge status={effectiveJobStatus(job)} />
                {job.jobType && <span className="chip">{titleCase(job.jobType)}</span>}
                {job.location && <span className="chip">{job.location}</span>}
                <span className="chip">{experienceLabel(job.experienceRequired)}</span>
              </div>
            </div>
            <div className="row" style={{ gap: 10, flex: 'none' }}>
              <SaveJobButton jobId={job.id} title={job.title} />
              {typeof job.matchPercentage === 'number' && (
                <div className="stack" style={{ alignItems: 'center', gap: 6 }}>
                  <MatchRing value={job.matchPercentage} size={68} stroke={5} />
                  <span className="tiny faint mono">skill match</span>
                </div>
              )}
            </div>
          </div>

          <hr className="divider" />

          <div className="stack" style={{ gap: 10 }}>
            <h3>About this role</h3>
            <p className="muted" style={{ whiteSpace: 'pre-line', maxWidth: '68ch' }}>
              {job.description || 'No description was provided for this role.'}
            </p>
          </div>

          {job.requiredSkills?.length > 0 && (
            <div className="stack" style={{ gap: 10 }}>
              <h3>Required skills</h3>
              <div className="row" style={{ gap: 6, flexWrap: 'wrap' }}>
                {job.requiredSkills.map((skill) => (
                  <span key={skill} className="chip chip--accent">{skill}</span>
                ))}
              </div>
            </div>
          )}

          <div className="panel">
            <dl className="kv">
              <dt>Posted</dt>
              <dd>{formatDate(job.createdAt)} <span className="faint small">({relativeTime(job.createdAt)})</span></dd>
              <dt>Closes</dt>
              <dd>
                {formatDate(job.expiresAt)}
                {expiresIn !== null && (
                  <span className="faint small">
                    {' '}
                    {expiresIn < 0 ? '(expired)' : expiresIn === 0 ? '(today)' : `(in ${expiresIn} days)`}
                  </span>
                )}
              </dd>
              <dt>Work style</dt>
              <dd>{job.jobType ? titleCase(job.jobType) : '—'}</dd>
              <dt>Experience</dt>
              <dd>{experienceLabel(job.experienceRequired)}</dd>
            </dl>
          </div>

          <div className="spread">
            {canApply && (
              <button type="button" className="btn btn--lg btn--glow" onClick={() => setModalOpen(true)}>
                Apply now
              </button>
            )}

            {applied && (
              <div className="stack" style={{ gap: 6 }}>
                <div className="row" style={{ gap: 10, flexWrap: 'wrap' }}>
                  {/* The real decision on this posting, rather than a flat "Applied". */}
                  <StatusBadge status={myApplication.status || 'APPLIED'} />
                  <Link to="/developer/dashboard" className="small muted">Track it in your dashboard →</Link>
                </div>
                {myApplication.status === 'REJECTED' && (
                  <span className="tiny faint">
                    This decision covers this posting only — you can apply again if the role is reposted.
                  </span>
                )}
              </div>
            )}

            {!applied && (isClosed || isExpired) && (
              <span className="small muted">
                {isClosed ? 'This role is closed and no longer accepting applications.' : 'This posting has expired.'}
              </span>
            )}

            {!isAuthenticated && !isClosed && !isExpired && (
              <div className="row" style={{ gap: 10, flexWrap: 'wrap' }}>
                <Link to="/login" state={{ from: { pathname: `/jobs/${job.id}` } }} className="btn">
                  Sign in to apply
                </Link>
                <span className="small muted">Developer accounts can apply in one click.</span>
              </div>
            )}

            {isAuthenticated && !isDeveloper && (
              <span className="small muted">You are signed in as a recruiter, so applying is disabled.</span>
            )}

            <ShareButton title={job.title} text={`${job.title} at ${job.companyName || 'a company'} on DevConnect`} />
          </div>

          {/* The refusal the API would give, turned into a checklist up front. */}
          {needsProfileWork && (
            <div className="panel lit stack" style={{ gap: 10 }}>
              <div className="spread">
                <strong style={{ fontSize: '0.95rem' }}>
                  {myProfile === null ? 'Create your profile to apply' : 'Finish your profile to apply'}
                </strong>
                <Link to="/developer/profile" className="btn btn--sm">
                  {myProfile === null ? 'Create profile' : 'Complete profile'}
                </Link>
              </div>
              <div className="row" style={{ gap: 6, flexWrap: 'wrap' }}>
                {missing.map((field) => (
                  <span key={field.key} className="badge badge--warn">{field.label}</span>
                ))}
              </div>
              <span className="tiny muted">
                Recruiters receive these with every application, so the API requires all of them.
              </span>
            </div>
          )}
        </div>

        {/* Follows the reader down a long description, so applying never means
            scrolling back up. Only while there is something to act on. */}
        {canApply && (
          <div className="sticky-cta" style={{ marginTop: 18 }}>
            <div className="stack" style={{ gap: 1, minWidth: 0, marginRight: 'auto' }}>
              <strong style={{ fontSize: '0.95rem' }}>{job.title}</strong>
              <span className="tiny faint">
                {job.companyName || 'Company undisclosed'}
                {expiresIn !== null && expiresIn >= 0 ? ` · closes in ${expiresIn} day${expiresIn === 1 ? '' : 's'}` : ''}
              </span>
            </div>
            <SaveJobButton jobId={job.id} title={job.title} />
            <button type="button" className="btn btn--glow" onClick={() => setModalOpen(true)}>
              Apply now
            </button>
          </div>
        )}
      </div>

      <Modal
        open={modalOpen}
        onClose={() => setModalOpen(false)}
        title={`Apply — ${job.title}`}
        footer={
          <div className="row" style={{ gap: 10, justifyContent: 'flex-end' }}>
            <button type="button" className="btn btn--ghost" onClick={() => setModalOpen(false)}>
              Cancel
            </button>
            <button type="button" className="btn" onClick={submitApplication} disabled={sending}>
              {sending && <span className="spinner" />}
              {sending ? 'Submitting…' : 'Submit application'}
            </button>
          </div>
        }
      >
        <div className="stack" style={{ gap: 14 }}>
          <p className="small muted">
            Your profile and resume are sent with this application. Add a short note if you want to
            tell the recruiter why you are a fit.
          </p>
          <div className="field">
            <label htmlFor="cover-note">Cover note <span className="faint tiny">— optional</span></label>
            <textarea
              id="cover-note"
              value={coverNote}
              maxLength={5000}
              placeholder="I have shipped three production Spring Boot services and…"
              onChange={(event) => setCoverNote(event.target.value)}
            />
            <span className="field-hint">{coverNote.length}/5000</span>
          </div>
        </div>
      </Modal>
    </>
  );
}
