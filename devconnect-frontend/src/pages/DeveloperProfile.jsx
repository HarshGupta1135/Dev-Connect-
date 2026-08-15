import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { Link, useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import { errorMessage } from '../api/client';
import {
  createDeveloperProfile,
  fetchMyDeveloperProfile,
  updateDeveloperProfile,
} from '../api/endpoints';
import AccountSettings from '../components/AccountSettings';
import Field from '../components/Field';
import ProfileCompleteness from '../components/ProfileCompleteness';
import ResumeUpload from '../components/ResumeUpload';
import SkillPicker from '../components/SkillPicker';
import { Skeleton } from '../components/Skeleton';

/**
 * One page for both first-time setup and later edits: the API distinguishes
 * create (POST) from update (PUT), so the page checks for an existing profile
 * and switches mode. Full name is required on create only.
 */
export default function DeveloperProfile() {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [existing, setExisting] = useState(null);
  const [skills, setSkills] = useState([]);
  const [resumeUrl, setResumeUrl] = useState(null);

  const {
    register,
    handleSubmit,
    reset,
    watch,
    formState: { errors, isSubmitting },
  } = useForm({
    defaultValues: {
      fullName: '', bio: '', location: '', yearsExp: '', linkedinUrl: '',
      phone: '', address: '', city: '', pincode: '',
    },
  });

  useEffect(() => {
    fetchMyDeveloperProfile()
      .then((profile) => {
        setExisting(profile);
        setSkills(profile.skills || []);
        setResumeUrl(profile.resumeUrl || null);
        reset({
          fullName: profile.fullName || '',
          bio: profile.bio || '',
          location: profile.location || '',
          yearsExp: profile.yearsExp ?? '',
          linkedinUrl: profile.linkedinUrl || '',
          phone: profile.phone || '',
          address: profile.address || '',
          city: profile.city || '',
          pincode: profile.pincode || '',
        });
      })
      .catch(() => {
        // No profile yet — this is the create path, not an error worth a toast.
        setExisting(null);
      })
      .finally(() => setLoading(false));
  }, [reset]);

  const onSubmit = async (values) => {
    const payload = {
      fullName: values.fullName.trim(),
      bio: values.bio?.trim() || null,
      location: values.location?.trim() || null,
      yearsExp: values.yearsExp === '' ? null : Number(values.yearsExp),
      linkedinUrl: values.linkedinUrl?.trim() || null,
      phone: values.phone?.trim() || null,
      address: values.address?.trim() || null,
      city: values.city?.trim() || null,
      pincode: values.pincode?.trim() || null,
      skills,
    };

    try {
      const message = existing
        ? await updateDeveloperProfile(payload)
        : await createDeveloperProfile(payload);
      toast.success(message);
      if (!existing) {
        navigate('/developer/dashboard');
        return;
      }
      setExisting({ ...existing, ...payload });
    } catch (error) {
      toast.error(errorMessage(error, 'Could not save your profile.'));
    }
  };

  if (loading) {
    return (
      <div className="wrap wrap--narrow section stack" style={{ gap: 14 }}>
        <Skeleton width="34%" height={12} />
        <Skeleton width="58%" height={30} />
        <Skeleton height={320} radius={16} style={{ marginTop: 16 }} />
      </div>
    );
  }

  return (
    <div className="wrap section--tight page-enter" style={{ paddingTop: 32, paddingBottom: 72, maxWidth: 1060 }}>
      <div className="stack" style={{ gap: 8, marginBottom: 26 }}>
        <span className="eyebrow">{existing ? 'Developer profile' : 'Set up your profile'}</span>
        <h1 style={{ fontSize: 'clamp(1.8rem, 3.6vw, 2.5rem)' }}>
          {existing ? 'Your profile' : 'Tell recruiters what you build'}
        </h1>
        <p className="lede">
          {existing
            ? 'Keep your skills current — match scores are recalculated from them on every search.'
            : 'This profile is what job matching scores against, and it is required before you can apply.'}
        </p>
      </div>

      {/* Form on the left; readiness and resume stay pinned on the right, so the
          two things the apply gate cares about are visible from any scroll depth. */}
      <div className="profile-layout">
      <form className="stack" style={{ gap: 18 }} onSubmit={handleSubmit(onSubmit)} noValidate>
        <div className="card card--pad stack" style={{ gap: 18 }}>
          <Field label="Full name" htmlFor="fullName" error={errors.fullName}>
            <input
              id="fullName"
              type="text"
              placeholder="Harsh Gupta"
              aria-invalid={Boolean(errors.fullName)}
              {...register('fullName', { required: 'Full name is required' })}
            />
          </Field>

          <Field label="Bio" htmlFor="bio" optional error={errors.bio} hint="A few lines on what you work on and what you want next.">
            <textarea
              id="bio"
              placeholder="Backend developer working mostly in Java and Spring Boot…"
              {...register('bio', { maxLength: { value: 5000, message: 'Keep it under 5000 characters' } })}
            />
          </Field>

          <div className="grid-2">
            <Field label="Location" htmlFor="location" error={errors.location} hint="Region or country, e.g. Karnataka, India">
              <input
                id="location"
                type="text"
                placeholder="Karnataka, India"
                aria-invalid={Boolean(errors.location)}
                {...register('location', { required: 'Location is required to apply' })}
              />
            </Field>

            <Field label="Years of experience" htmlFor="yearsExp" optional error={errors.yearsExp}>
              <input
                id="yearsExp"
                type="number"
                min="0"
                max="60"
                placeholder="3"
                aria-invalid={Boolean(errors.yearsExp)}
                {...register('yearsExp', {
                  min: { value: 0, message: 'Cannot be negative' },
                  max: { value: 60, message: 'That looks too high' },
                })}
              />
            </Field>
          </div>

          <Field label="Address" htmlFor="address" error={errors.address} hint="Street or locality — recruiters see this with your application.">
            <input
              id="address"
              type="text"
              autoComplete="street-address"
              placeholder="12, MG Road"
              aria-invalid={Boolean(errors.address)}
              {...register('address', { required: 'Address is required to apply' })}
            />
          </Field>

          <div className="grid-2">
            <Field label="City" htmlFor="city" error={errors.city}>
              <input
                id="city"
                type="text"
                autoComplete="address-level2"
                placeholder="Bengaluru"
                aria-invalid={Boolean(errors.city)}
                {...register('city', { required: 'City is required to apply' })}
              />
            </Field>

            <Field label="Pincode" htmlFor="pincode" error={errors.pincode}>
              <input
                id="pincode"
                type="text"
                inputMode="numeric"
                autoComplete="postal-code"
                placeholder="560001"
                aria-invalid={Boolean(errors.pincode)}
                {...register('pincode', {
                  required: 'Pincode is required to apply',
                  pattern: { value: /^[0-9]{4,8}$/, message: 'Digits only, 4–8 of them' },
                })}
              />
            </Field>
          </div>

          <Field label="Phone number" htmlFor="phone" error={errors.phone}>
            <input
              id="phone"
              type="tel"
              autoComplete="tel"
              placeholder="+91 98765 43210"
              aria-invalid={Boolean(errors.phone)}
              {...register('phone', {
                required: 'Phone number is required to apply',
                pattern: { value: /^\+?[0-9][0-9 \-]{8,14}$/, message: 'Enter a valid phone number' },
              })}
            />
          </Field>

          <Field label="LinkedIn" htmlFor="linkedinUrl" optional error={errors.linkedinUrl}>
            <input
              id="linkedinUrl"
              type="url"
              placeholder="https://linkedin.com/in/you"
              {...register('linkedinUrl')}
            />
          </Field>

          <div className="field">
            <label htmlFor="skills">Skills</label>
            <SkillPicker id="skills" value={skills} onChange={setSkills} />
            <span className="field-hint">
              Pick from the catalogue where you can. A skill the admin has not added yet will be
              rejected on save.
            </span>
          </div>
        </div>

        <div className="row" style={{ gap: 10, flexWrap: 'wrap' }}>
          <button type="submit" className="btn btn--lg" disabled={isSubmitting}>
            {isSubmitting && <span className="spinner" />}
            {isSubmitting ? 'Saving…' : existing ? 'Save changes' : 'Create profile'}
          </button>
          {existing && (
            <Link to="/developer/dashboard" className="btn btn--lg btn--outline">
              Back to dashboard
            </Link>
          )}
        </div>
      </form>

      <aside className="profile-rail">
        {/* Live: fed the form values, so the ring moves as fields are typed. */}
        <ProfileCompleteness values={{ ...watch(), resumeUrl }} />

        {/* Outside the form on purpose: the upload is its own request, and a file
            input inside the profile form suggests it saves with the Save button. */}
        <div className="card card--pad stack lit" style={{ gap: 14 }}>
          <div className="stack" style={{ gap: 3 }}>
            <h3>Resume</h3>
            <p className="small muted">
              {existing
                ? 'Recruiters reviewing your applications can open this file.'
                : 'Save your profile first, then upload a resume.'}
            </p>
          </div>
          {existing ? (
            <ResumeUpload resumeUrl={resumeUrl} onUploaded={setResumeUrl} />
          ) : (
            <div className="panel small muted">Available once your profile exists.</div>
          )}
        </div>
      </aside>
      </div>

      {/*
        Its own <form>, kept outside the profile one: it posts to a different endpoint
        and nesting forms is invalid HTML.
      */}
      <div style={{ marginTop: 34 }}>
        <AccountSettings
          title="Account & sign-in"
          description="Your username and the email you sign in with. These are separate from the profile above, which is what recruiters see."
        />
      </div>
    </div>
  );
}
