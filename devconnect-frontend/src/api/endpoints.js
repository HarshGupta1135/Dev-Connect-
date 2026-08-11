import api, { messageOf, unwrap } from './client';

/* ---------------- auth ---------------- */

export const registerUser = (payload) =>
  api.post('/api/auth/register', payload).then((r) => messageOf(r, 'Registered'));

export const loginUser = (payload) =>
  api.post('/api/auth/login', payload).then(unwrap); // { token, email, role: [...] }

/* ---------------- account (either role) ---------------- */

/** { id, userName, email, role: [...], createdAt } */
export const fetchMyAccount = () => api.get('/api/account/me').then(unwrap);

/**
 * Updates username and login email.
 * Returns the saved account plus `token` — a replacement JWT that is present only
 * when the email changed, since the previous one was signed against the old address.
 */
export const updateMyAccount = (payload) => api.put('/api/account', payload).then(unwrap);

/* ---------------- jobs (public) ---------------- */

/**
 * @param {{skills?: string[], location?: string, type?: string, page?: number, size?: number, sort?: string}} q
 * Returns CustomPageResponse: { content, pageNumber, pageSize, totalElements, totalPages, last }
 */
export const fetchJobs = (q = {}) => {
  const params = {
    page: q.page ?? 0,
    size: q.size ?? 9,
    // Sent as one comma-joined value; the backend splits it itself.
    sort: q.sort || 'createdAt,desc',
  };
  if (q.skills?.length) params.skills = q.skills;
  if (q.location) params.location = q.location;
  if (q.type) params.type = q.type;
  return api.get('/api/jobs', { params }).then(unwrap);
};

export const fetchJob = (id) => api.get(`/api/jobs/${id}`).then(unwrap);

/* ---------------- skills ---------------- */

/** Requires authentication in the current backend, so callers must handle 401/403. */
export const fetchSkills = () => api.get('/api/get/all/skills').then(unwrap);

/* ---------------- developer ---------------- */

export const fetchMyDeveloperProfile = () => api.get('/api/developer/profile/me').then(unwrap);

export const createDeveloperProfile = (payload) =>
  api.post('/api/developer/profile', payload).then((r) => messageOf(r, 'Profile created'));

export const updateDeveloperProfile = (payload) =>
  api.put('/api/developer/profile', payload).then((r) => messageOf(r, 'Profile updated'));

export const uploadResume = (file, onProgress) => {
  const form = new FormData();
  form.append('file', file);
  return api
    .post('/api/developer/profile/resume', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: (e) => {
        if (onProgress && e.total) onProgress(Math.round((e.loaded * 100) / e.total));
      },
    })
    .then(unwrap); // { resumeUrl }
};

export const applyToJob = (payload) =>
  api.post('/api/developer/apply', payload).then((r) => messageOf(r, 'Application submitted'));

export const fetchMyApplications = () => api.get('/api/developer/applications').then(unwrap);

/* ---------------- recruiter ---------------- */

export const fetchMyRecruiterProfile = () => api.get('/api/recruiter/profile/me').then(unwrap);

export const createRecruiterProfile = (payload) =>
  api.post('/api/recruiter/profile', payload).then((r) => messageOf(r, 'Profile created'));

export const updateRecruiterProfile = (payload) =>
  api.put('/api/recruiter/profile', payload).then((r) => messageOf(r, 'Profile updated'));

export const fetchRecruiterJobs = () => api.get('/api/recruiter/jobs').then(unwrap);

export const createJob = (payload) =>
  api.post('/api/recruiter/jobs', payload).then((r) => messageOf(r, 'Job posted'));

export const updateJob = (id, payload) =>
  api.put(`/api/recruiter/jobs/${id}`, payload).then((r) => messageOf(r, 'Job updated'));

export const closeJob = (id) =>
  api.patch(`/api/recruiter/jobs/${id}/close`).then((r) => messageOf(r, 'Job closed'));

/**
 * Each entry carries an `applicant` object — the candidate's name, email, location,
 * years of experience, skills, resume and LinkedIn — so the applicants page can show
 * a full profile without a second round trip per row.
 */
export const fetchJobApplicants = (jobId) =>
  api.get(`/api/recruiter/jobs/${jobId}/applications`).then(unwrap);

export const setApplicationStatus = (applicationId, newStatus) =>
  api
    .patch(`/api/recruiter/applications/${applicationId}/status`, { applicationId, newStatus })
    .then((r) => messageOf(r, 'Status updated'));

/* ---------------- constants mirrored from the backend enums ---------------- */

export const JOB_TYPES = ['REMOTE', 'ONSITE', 'HYBRID'];
export const JOB_STATUSES = ['ACTIVE', 'CLOSED'];
export const APPLICATION_STATUSES = ['APPLIED', 'SHORTLISTED', 'REJECTED'];
export const ROLES = ['DEVELOPER', 'RECRUITER'];
