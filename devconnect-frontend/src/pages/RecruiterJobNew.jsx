import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { Link, useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import { errorMessage } from '../api/client';
import { JOB_TYPES, createJob } from '../api/endpoints';
import Field from '../components/Field';
import SkillPicker from '../components/SkillPicker';
import { titleCase } from '../utils/format';

/** Default expiry a month out, matching what the API does when none is sent. */
function defaultExpiry() {
  const date = new Date();
  date.setDate(date.getDate() + 30);
  return date.toISOString().slice(0, 10);
}

export default function RecruiterJobNew() {
  const navigate = useNavigate();
  const [skills, setSkills] = useState([]);

  const {
    register,
    handleSubmit,
    watch,
    formState: { errors, isSubmitting },
  } = useForm({
    defaultValues: {
      title: '',
      description: '',
      jobType: 'REMOTE',
      location: '',
      experienceRequired: 0,
      expiresAt: defaultExpiry(),
    },
  });

  const description = watch('description') || '';

  const onSubmit = async (values) => {
    const payload = {
      title: values.title.trim(),
      description: values.description.trim(),
      jobType: values.jobType,
      location: values.location?.trim() || null,
      experienceRequired:
        values.experienceRequired === '' ? null : Number(values.experienceRequired),
      // Sent explicitly so the posting is live immediately.
      status: 'ACTIVE',
      requiredSkills: skills,
      // Date only from the input; the end of that day keeps it valid all day.
      expiresAt: values.expiresAt ? new Date(`${values.expiresAt}T23:59:59`).toISOString() : null,
    };

    try {
      const message = await createJob(payload);
      toast.success(message || 'Job posted.');
      navigate('/recruiter/dashboard');
    } catch (error) {
      const message = errorMessage(error, 'Could not post the job.');
      toast.error(message);
      if (/recruiter profile/i.test(message)) {
        toast('Create your company profile first.', { icon: '🏢' });
        navigate('/recruiter/dashboard');
      }
    }
  };

  return (
    <div className="wrap wrap--narrow section--tight page-enter" style={{ paddingTop: 32, paddingBottom: 72 }}>
      <div className="stack" style={{ gap: 8, marginBottom: 26 }}>
        <span className="eyebrow">New posting</span>
        <h1 style={{ fontSize: 'clamp(1.8rem, 3.6vw, 2.5rem)' }}>Post a role</h1>
        <p className="lede">
          The skills you list here are what candidates are scored against, so be specific about
          what the work actually needs.
        </p>
      </div>

      <form className="card card--pad stack" style={{ gap: 18 }} onSubmit={handleSubmit(onSubmit)} noValidate>
        <Field label="Job title" htmlFor="title" error={errors.title}>
          <input
            id="title"
            type="text"
            placeholder="Backend Engineer — Spring Boot"
            aria-invalid={Boolean(errors.title)}
            {...register('title', {
              required: 'Title is required',
              maxLength: { value: 150, message: 'Keep the title under 150 characters' },
            })}
          />
        </Field>

        <Field
          label="Description"
          htmlFor="description"
          error={errors.description}
          hint={`${description.length} characters — describe the team, the work and what success looks like.`}
        >
          <textarea
            id="description"
            style={{ minHeight: 160 }}
            placeholder="You will own our payments service…"
            aria-invalid={Boolean(errors.description)}
            {...register('description', {
              required: 'Description is required',
              minLength: { value: 30, message: 'Give candidates a bit more detail (30+ characters)' },
            })}
          />
        </Field>

        <div className="grid-2">
          <Field label="Work style" htmlFor="jobType" error={errors.jobType}>
            <select id="jobType" {...register('jobType', { required: true })}>
              {JOB_TYPES.map((entry) => (
                <option key={entry} value={entry}>{titleCase(entry)}</option>
              ))}
            </select>
          </Field>

          <Field label="Location" htmlFor="location" optional error={errors.location} hint="Used by the location filter.">
            <input id="location" type="text" placeholder="Bengaluru, India" {...register('location')} />
          </Field>
        </div>

        <div className="grid-2">
          <Field label="Years of experience required" htmlFor="experienceRequired" error={errors.experienceRequired}>
            <input
              id="experienceRequired"
              type="number"
              min="0"
              max="60"
              aria-invalid={Boolean(errors.experienceRequired)}
              {...register('experienceRequired', {
                min: { value: 0, message: 'Cannot be negative' },
                max: { value: 60, message: 'That looks too high' },
              })}
            />
          </Field>

          <Field label="Applications close" htmlFor="expiresAt" error={errors.expiresAt} hint="Defaults to 30 days out.">
            <input
              id="expiresAt"
              type="date"
              min={new Date().toISOString().slice(0, 10)}
              aria-invalid={Boolean(errors.expiresAt)}
              {...register('expiresAt', {
                validate: (value) =>
                  !value || new Date(`${value}T23:59:59`) > new Date() || 'Pick a date in the future',
              })}
            />
          </Field>
        </div>

        <div className="field">
          <label htmlFor="job-skills">Required skills</label>
          <SkillPicker id="job-skills" value={skills} onChange={setSkills} placeholder="Java, Spring Boot, MySQL…" />
          <span className="field-hint">
            Only skills already in the catalogue can be saved — an unknown one is rejected with its name.
          </span>
        </div>

        <div className="row" style={{ gap: 10, flexWrap: 'wrap' }}>
          <button type="submit" className="btn btn--lg" disabled={isSubmitting}>
            {isSubmitting && <span className="spinner" />}
            {isSubmitting ? 'Posting…' : 'Publish job'}
          </button>
          <Link to="/recruiter/dashboard" className="btn btn--lg btn--outline">Cancel</Link>
        </div>
      </form>
    </div>
  );
}
