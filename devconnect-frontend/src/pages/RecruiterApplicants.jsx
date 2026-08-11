import { useEffect, useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import toast from 'react-hot-toast';
import { errorMessage } from '../api/client';
import { fetchJob, fetchJobApplicants, setApplicationStatus } from '../api/endpoints';
import EmptyState from '../components/EmptyState';
import Modal from '../components/Modal';
import StatusBadge from '../components/StatusBadge';
import { RowSkeleton, Skeleton } from '../components/Skeleton';
import { formatDate, relativeTime } from '../utils/format';

const FILTERS = [
  { id: 'ALL', label: 'All' },
  { id: 'APPLIED', label: 'In review' },
  { id: 'SHORTLISTED', label: 'Shortlisted' },
  { id: 'REJECTED', label: 'Not selected' },
];

/** Falls back through the profile name, the login username, then the application id. */
function candidateName(application) {
  const applicant = application.applicant;
  return (
    applicant?.fullName?.trim() ||
    applicant?.userName?.trim() ||
    `Application #${application.id}`
  );
}

function experienceText(years) {
  if (years === null || years === undefined) return null;
  if (years === 0) return 'Fresher';
  return `${years} yr${years === 1 ? '' : 's'} experience`;
}

/** Everything the applications API knows about the candidate, in a dialog. */
function CandidateDialog({ application, onClose }) {
  const applicant = application?.applicant;

  return (
    <Modal open={Boolean(application)} onClose={onClose} title={application ? candidateName(application) : ''}>
      {!applicant ? (
        <p className="small muted">
          This application has no candidate profile attached, which should not happen —
          it may predate the profile requirement.
        </p>
      ) : (
        <div className="stack" style={{ gap: 18 }}>
          <div className="row" style={{ gap: 8, flexWrap: 'wrap' }}>
            <StatusBadge status={application.status} />
            <span className="tiny faint mono">
              Applied {relativeTime(application.appliedAt)} · {formatDate(application.appliedAt)}
            </span>
          </div>

          <dl className="kv small">
            <dt>Email</dt>
            <dd>
              {applicant.email ? <a href={`mailto:${applicant.email}`}>{applicant.email}</a> : '—'}
            </dd>

            <dt>Username</dt>
            <dd>{applicant.userName || '—'}</dd>

            <dt>Location</dt>
            <dd>{applicant.location || '—'}</dd>

            <dt>Experience</dt>
            <dd>{experienceText(applicant.yearsExp) || 'Not stated'}</dd>

            <dt>LinkedIn</dt>
            <dd>
              {applicant.linkedinUrl ? (
                <a href={applicant.linkedinUrl} target="_blank" rel="noreferrer noopener">
                  {applicant.linkedinUrl.replace(/^https?:\/\//, '')}
                </a>
              ) : (
                '—'
              )}
            </dd>
          </dl>

          <div className="stack" style={{ gap: 7 }}>
            <span className="eyebrow">Skills</span>
            {applicant.skills?.length ? (
              <div className="row" style={{ gap: 5, flexWrap: 'wrap' }}>
                {applicant.skills.map((skill) => (
                  <span key={skill} className="chip">{skill}</span>
                ))}
              </div>
            ) : (
              <span className="small faint">No skills listed.</span>
            )}
          </div>

          <div className="stack" style={{ gap: 7 }}>
            <span className="eyebrow">About</span>
            <p className="small muted" style={{ whiteSpace: 'pre-line' }}>
              {applicant.bio?.trim() || 'No bio written yet.'}
            </p>
          </div>

          {application.coverNote && (
            <div className="stack" style={{ gap: 7 }}>
              <span className="eyebrow">Cover note</span>
              <p className="small muted" style={{ whiteSpace: 'pre-line' }}>“{application.coverNote}”</p>
            </div>
          )}

          <div className="row" style={{ gap: 8, flexWrap: 'wrap' }}>
            {applicant.resumeUrl ? (
              <a href={applicant.resumeUrl} target="_blank" rel="noreferrer noopener" className="btn btn--sm">
                Open resume
              </a>
            ) : (
              <span className="small faint">No resume uploaded.</span>
            )}
            {applicant.email && (
              <a href={`mailto:${applicant.email}`} className="btn btn--outline btn--sm">
                Email candidate
              </a>
            )}
          </div>
        </div>
      )}
    </Modal>
  );
}

export default function RecruiterApplicants() {
  const { jobId } = useParams();
  const [job, setJob] = useState(null);
  const [applications, setApplications] = useState(null);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState('ALL');
  const [busyId, setBusyId] = useState(null);
  const [openId, setOpenId] = useState(null);

  useEffect(() => {
    let cancelled = false;

    fetchJob(jobId)
      .then((data) => !cancelled && setJob(data))
      .catch(() => {
        /* The header just falls back to the id. */
      });

    fetchJobApplicants(jobId)
      .then((data) => !cancelled && setApplications(data || []))
      .catch((error) => {
        if (cancelled) return;
        setApplications([]);
        toast.error(errorMessage(error, 'Could not load applicants.'));
      })
      .finally(() => !cancelled && setLoading(false));

    return () => {
      cancelled = true;
    };
  }, [jobId]);

  const decide = async (application, newStatus) => {
    setBusyId(application.id);
    const previous = application.status;

    // Optimistic: the badge flips immediately and rolls back if the call fails.
    setApplications((current) =>
      current.map((entry) => (entry.id === application.id ? { ...entry, status: newStatus } : entry))
    );

    try {
      await setApplicationStatus(application.id, newStatus);
      toast.success(
        newStatus === 'SHORTLISTED'
          ? 'Shortlisted — the candidate will be emailed.'
          : 'Marked as not selected — the candidate will be emailed.'
      );
    } catch (error) {
      setApplications((current) =>
        current.map((entry) => (entry.id === application.id ? { ...entry, status: previous } : entry))
      );
      toast.error(errorMessage(error, 'Could not update the status.'));
    } finally {
      setBusyId(null);
    }
  };

  const counts = useMemo(() => {
    const list = applications || [];
    return {
      ALL: list.length,
      APPLIED: list.filter((entry) => entry.status === 'APPLIED').length,
      SHORTLISTED: list.filter((entry) => entry.status === 'SHORTLISTED').length,
      REJECTED: list.filter((entry) => entry.status === 'REJECTED').length,
    };
  }, [applications]);

  const visible = useMemo(() => {
    const list = applications || [];
    return filter === 'ALL' ? list : list.filter((entry) => entry.status === filter);
  }, [applications, filter]);

  // Kept as an id rather than the object itself so the dialog follows an optimistic
  // status flip made while it is open.
  const openApplication = useMemo(
    () => (applications || []).find((entry) => entry.id === openId) || null,
    [applications, openId]
  );

  return (
    <div className="wrap section--tight page-enter" style={{ paddingTop: 32, paddingBottom: 72 }}>
      <Link to="/recruiter/dashboard" className="small muted" style={{ display: 'inline-block', marginBottom: 16 }}>
        ← Back to my jobs
      </Link>

      <div className="dash-head">
        <span className="eyebrow">Applicants</span>
        {job ? (
          <h1 style={{ fontSize: 'clamp(1.7rem, 3.4vw, 2.3rem)' }}>{job.title}</h1>
        ) : (
          <Skeleton width="46%" height={30} />
        )}
        <p className="muted small">
          {job?.location ? `${job.location} · ` : ''}
          {counts.ALL} application{counts.ALL === 1 ? '' : 's'} · {counts.APPLIED} awaiting a decision
        </p>
      </div>

      <div className="tabs" role="tablist" aria-label="Filter applications" style={{ marginBottom: 18 }}>
        {FILTERS.map((entry) => (
          <button
            key={entry.id}
            type="button"
            className="tab"
            role="tab"
            aria-selected={filter === entry.id}
            onClick={() => setFilter(entry.id)}
          >
            {entry.label} <span className="faint mono tiny">{counts[entry.id] ?? 0}</span>
          </button>
        ))}
      </div>

      <section className="list" role="tabpanel">
        {loading ? (
          Array.from({ length: 3 }, (_, index) => <RowSkeleton key={index} />)
        ) : visible.length === 0 ? (
          <EmptyState
            mark="◎"
            title={counts.ALL === 0 ? 'No applications yet' : 'Nothing in this view'}
            message={
              counts.ALL === 0
                ? 'Candidates who match this role will show up here as they apply.'
                : 'Try another filter to see the rest of the applications.'
            }
          />
        ) : (
          visible.map((application) => {
            const applicant = application.applicant;
            const meta = [
              applicant?.location,
              experienceText(applicant?.yearsExp),
              applicant?.email,
            ].filter(Boolean);

            return (
              <article key={application.id} className="card list-row" style={{ alignItems: 'flex-start' }}>
                <div className="list-row__main">
                  <div className="row" style={{ gap: 10, flexWrap: 'wrap' }}>
                    <strong style={{ fontSize: '1rem' }}>{candidateName(application)}</strong>
                    <StatusBadge status={application.status} />
                    <span className="tiny faint mono">#{application.id}</span>
                  </div>

                  {meta.length > 0 && (
                    <span className="tiny faint">{meta.join(' · ')}</span>
                  )}

                  <span className="tiny faint mono">
                    Applied {relativeTime(application.appliedAt)} · {formatDate(application.appliedAt)}
                    {application.updatedAt && application.updatedAt !== application.appliedAt
                      ? ` · decided ${relativeTime(application.updatedAt)}`
                      : ''}
                  </span>

                  {applicant?.skills?.length > 0 && (
                    <div className="row" style={{ gap: 5, flexWrap: 'wrap', marginTop: 6 }}>
                      {applicant.skills.slice(0, 8).map((skill) => (
                        <span key={skill} className="chip">{skill}</span>
                      ))}
                      {applicant.skills.length > 8 && (
                        <span className="tiny faint">+{applicant.skills.length - 8} more</span>
                      )}
                    </div>
                  )}

                  {application.coverNote ? (
                    <p className="small muted" style={{ marginTop: 6, maxWidth: '72ch', whiteSpace: 'pre-line' }}>
                      “{application.coverNote}”
                    </p>
                  ) : (
                    <span className="tiny faint" style={{ marginTop: 4 }}>No cover note</span>
                  )}
                </div>

                <div className="row" style={{ gap: 8, flexWrap: 'wrap', justifyContent: 'flex-end' }}>
                  <button
                    type="button"
                    className="btn btn--soft btn--sm"
                    onClick={() => setOpenId(application.id)}
                  >
                    View profile
                  </button>

                  {applicant?.resumeUrl && (
                    <a
                      href={applicant.resumeUrl}
                      target="_blank"
                      rel="noreferrer noopener"
                      className="btn btn--outline btn--sm"
                    >
                      Resume
                    </a>
                  )}

                  {application.status === 'APPLIED' ? (
                    <>
                      <button
                        type="button"
                        className="btn btn--good btn--sm"
                        onClick={() => decide(application, 'SHORTLISTED')}
                        disabled={busyId === application.id}
                      >
                        Shortlist
                      </button>
                      <button
                        type="button"
                        className="btn btn--danger btn--sm"
                        onClick={() => decide(application, 'REJECTED')}
                        disabled={busyId === application.id}
                      >
                        Pass
                      </button>
                    </>
                  ) : (
                    <button
                      type="button"
                      className="btn btn--outline btn--sm"
                      onClick={() =>
                        decide(application, application.status === 'SHORTLISTED' ? 'REJECTED' : 'SHORTLISTED')
                      }
                      disabled={busyId === application.id}
                    >
                      {application.status === 'SHORTLISTED' ? 'Change to pass' : 'Change to shortlist'}
                    </button>
                  )}
                </div>
              </article>
            );
          })
        )}
      </section>

      <CandidateDialog application={openApplication} onClose={() => setOpenId(null)} />
    </div>
  );
}
