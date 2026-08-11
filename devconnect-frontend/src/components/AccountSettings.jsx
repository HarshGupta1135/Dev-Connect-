import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import toast from 'react-hot-toast';
import { errorMessage } from '../api/client';
import { fetchMyAccount, updateMyAccount } from '../api/endpoints';
import { useAuth } from '../context/AuthContext';
import Field from './Field';
import { Skeleton } from './Skeleton';

/**
 * Username and login email live on the user record rather than on the developer or
 * recruiter profile, so they get their own form and their own endpoint. Both roles
 * use this same card.
 *
 * Changing the email changes what you sign in with, and the API answers with a
 * replacement token because the old one was signed against the previous address.
 */
export default function AccountSettings({ title = 'Account', description }) {
  const { updateSession } = useAuth();
  const [loading, setLoading] = useState(true);
  const [account, setAccount] = useState(null);

  const {
    register,
    handleSubmit,
    reset,
    watch,
    formState: { errors, isSubmitting, isDirty },
  } = useForm({
    defaultValues: { userName: '', email: '', secondaryEmail: '', emailPreference: 'PRIMARY' },
  });

  const formValues = (data) => ({
    userName: data.userName || '',
    email: data.email || '',
    secondaryEmail: data.secondaryEmail || '',
    emailPreference: data.emailPreference || 'PRIMARY',
  });

  useEffect(() => {
    fetchMyAccount()
      .then((data) => {
        setAccount(data);
        reset(formValues(data));
      })
      .catch((error) => toast.error(errorMessage(error, 'Could not load your account details.')))
      .finally(() => setLoading(false));
  }, [reset]);

  // Choosing the secondary address only makes sense once one exists, and the API
  // refuses the combination outright.
  const secondaryEmail = watch('secondaryEmail');
  const hasSecondary = Boolean(secondaryEmail?.trim());

  const onSubmit = async (values) => {
    const payload = {
      userName: values.userName.trim(),
      email: values.email.trim(),
      secondaryEmail: values.secondaryEmail.trim(),
      // Guard the one combination the API rejects, so clearing the address and
      // forgetting the radio is not an error the user has to read about.
      emailPreference: values.secondaryEmail.trim() ? values.emailPreference : 'PRIMARY',
    };

    try {
      const data = await updateMyAccount(payload);
      setAccount(data);
      reset(formValues(data));

      // data.token is present only when the primary email changed; updateSession keeps
      // the old token in that case, so this is safe to call either way.
      updateSession({ email: data.email, token: data.token });

      toast.success(
        data.token
          ? 'Account updated — sign in with your new email from now on.'
          : 'Account updated.'
      );
    } catch (error) {
      toast.error(errorMessage(error, 'Could not update your account.'));
    }
  };

  if (loading) {
    return (
      <div className="card card--pad stack" style={{ gap: 14 }}>
        <Skeleton width="24%" height={16} />
        <Skeleton width="60%" height={11} />
        <Skeleton height={72} radius={12} style={{ marginTop: 8 }} />
      </div>
    );
  }

  return (
    <form className="card card--pad stack" style={{ gap: 18 }} onSubmit={handleSubmit(onSubmit)} noValidate>
      <div className="stack" style={{ gap: 3 }}>
        <h3>{title}</h3>
        <p className="small muted">
          {description || 'Your username and the email address you sign in with.'}
        </p>
      </div>

      <div className="grid-2">
        <Field label="Username" htmlFor="userName" error={errors.userName}>
          <input
            id="userName"
            type="text"
            autoComplete="username"
            placeholder="harshg"
            aria-invalid={Boolean(errors.userName)}
            {...register('userName', {
              required: 'Username is required',
              minLength: { value: 3, message: 'At least 3 characters' },
              maxLength: { value: 40, message: 'At most 40 characters' },
            })}
          />
        </Field>

        <Field
          label="Login email"
          htmlFor="email"
          error={errors.email}
          hint="Must be a @gmail.com address, same as at sign-up."
        >
          <input
            id="email"
            type="email"
            autoComplete="email"
            placeholder="you@gmail.com"
            aria-invalid={Boolean(errors.email)}
            {...register('email', {
              required: 'Email is required',
              pattern: { value: /^[^\s@]+@[^\s@]+\.[^\s@]+$/, message: 'Enter a valid email address' },
            })}
          />
        </Field>
      </div>

      <hr className="divider" />

      <div className="stack" style={{ gap: 3 }}>
        <h3 style={{ fontSize: '1rem' }}>Notifications</h3>
        <p className="small muted">
          Add a second address if you would rather not get application mail at your
          sign-in one. Any provider works here — this address is only ever a delivery
          target, never a way to sign in.
        </p>
      </div>

      <div className="grid-2">
        <Field label="Secondary email" htmlFor="secondaryEmail" optional error={errors.secondaryEmail}>
          <input
            id="secondaryEmail"
            type="email"
            autoComplete="email"
            placeholder="you@work-address.com"
            aria-invalid={Boolean(errors.secondaryEmail)}
            {...register('secondaryEmail', {
              pattern: { value: /^[^\s@]+@[^\s@]+\.[^\s@]+$/, message: 'Enter a valid email address' },
              validate: (value) =>
                !value?.trim() ||
                value.trim().toLowerCase() !== watch('email').trim().toLowerCase() ||
                'Use a different address from your primary email',
            })}
          />
        </Field>

        <Field
          label="Send my email to"
          htmlFor="emailPreference"
          error={errors.emailPreference}
          hint={
            hasSecondary
              ? 'Applies to application confirmations and shortlist or rejection notices.'
              : 'Add a secondary email above to enable this choice.'
          }
        >
          <select id="emailPreference" disabled={!hasSecondary} {...register('emailPreference')}>
            <option value="PRIMARY">My primary email</option>
            <option value="SECONDARY">My secondary email</option>
          </select>
        </Field>
      </div>

      <div className="panel small muted">
        {account?.notificationEmail && (
          <>
            Mail currently goes to <strong>{account.notificationEmail}</strong>.<br />
          </>
        )}
        Changing your primary email changes how you sign in. You stay logged in here, but
        any other tab or device will need the new address.
      </div>

      <button type="submit" className="btn" style={{ alignSelf: 'flex-start' }} disabled={isSubmitting || !isDirty}>
        {isSubmitting && <span className="spinner" />}
        {isSubmitting ? 'Saving…' : 'Save account details'}
      </button>
    </form>
  );
}
