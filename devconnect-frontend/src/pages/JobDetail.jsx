import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import toast from 'react-hot-toast';
import { errorMessage } from '../api/client';
import { applyToJob, fetchJob, fetchMyApplications } from '../api/endpoints';
import MatchRing from '../components/MatchRing';
import Modal from '../components/Modal';
import StatusBadge from '../components/StatusBadge';
import { Skeleton } from '../components/Skeleton';
import { ReadingProgress } from '../components/ScrollHelpers';
import { useAuth } from '../context/AuthContext';
import { daysUntil, formatDate, experienceLabel, relativeTime, titleCase } from '../utils/format';

export default function JobDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { isAuthenticated, isDeveloper } = useAuth();

  const [job, setJob] = useState(null);
  const [loading, setLoading] = useState(true);
  const [applied, setApplied] = useState(false);
  const [modalOpen, setModalOpen] = useState(false);
  const [coverNote, setCoverNote] = useState('');
  const [sending, setSending] = useState(false);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);

    fetchJob(id)
      .then((data) => !cancelled && setJob(data))
      .catch((error) => {
        if (!cancelled) toast.error(errorMessage(error, 'That job could not be loaded.'));
      })
      .finally(() => !cancelled && setLoading(false));

    return () => {
      cancelled = true;
    };
  }, [id]);

  /*
   * There is no "did I apply to job X" endpoint, and the applications list
   * returns the job title rather than its id, so the check matches on title.
   * A duplicate attempt is still refused by the API, which is handled below.
   */
  useEffect(() => {
    if (!isDeveloper || !job?.title) return;
    fetchMyApplications()
      .then((applications) => {
        const already = (applications || []).some(
          (application) => application.jobTitle?.trim().toLowerCase() === job.title.trim().toLowerCase()
        );
        setApplied(already);
      })
      .catch(() => {
        /* Not critical: the Apply button stays available and the API decides. */
      });
  }, [isDeveloper, job?.title]);

  const submitApplication = async () => {
    setSending(true);
    try {
      await applyToJob({ jobId: job.id, coverNote: coverNote.trim() || null });
      toast.success('Application submitted. Check your email for confirmation.');
      setApplied(true);
      setModalOpen(false);
      setCoverNote('');
    } catch (error) {
      const message = errorMessage(error, 'Could not submit your application.');
      toast.error(message);
      if (/already applied/i.test(message)) {
        setApplied(true);
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
  const isExpired = expiresIn !== null && expiresIn < 0;
  const canApply = isDeveloper && !isClosed && !isExpired && !applied;

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
                <StatusBadge status={job.status} />
                {job.jobType && <span className="chip">{titleCase(job.jobType)}</span>}
                {job.location && <span className="chip">{job.location}</span>}
                <span className="chip">{experienceLabel(job.experienceRequired)}</span>
              </div>
            </div>
            {typeof job.matchPercentage === 'number' && (
              <div className="stack" style={{ alignItems: 'center', gap: 6 }}>
                <MatchRing value={job.matchPercentage} size={68} stroke={5} />
                <span className="tiny faint mono">skill match</span>
              </div>
            )}
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
              <button type="button" className="btn btn--lg" onClick={() => setModalOpen(true)}>
                Apply now
              </button>
            )}

            {applied && (
              <div className="row" style={{ gap: 10 }}>
                <span className="badge badge--applied">Applied</span>
                <Link to="/developer/dashboard" className="small muted">Track it in your dashboard →</Link>
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
          </div>
        </div>
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
