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
    formState: { errors, isSubmitting, isDirty },
  } = useForm({ defaultValues: { userName: '', email: '' } });

  useEffect(() => {
    fetchMyAccount()
      .then((data) => {
        setAccount(data);
        reset({ userName: data.userName || '', email: data.email || '' });
      })
      .catch((error) => toast.error(errorMessage(error, 'Could not load your account details.')))
      .finally(() => setLoading(false));
  }, [reset]);

  const onSubmit = async (values) => {
    const payload = { userName: values.userName.trim(), email: values.email.trim() };

    if (account && payload.userName === account.userName && payload.email === account.email) {
      toast('Nothing to save — both fields are unchanged.');
      return;
    }

    try {
      const data = await updateMyAccount(payload);
      setAccount(data);
      reset({ userName: data.userName || '', email: data.email || '' });

      // data.token is present only when the email changed; updateSession keeps the
      // old token in that case, so this is safe to call either way.
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

      <div className="panel small muted">
        Changing your email changes how you sign in. You stay logged in here, but any
        other tab or device will need the new address.
      </div>

      <button type="submit" className="btn" style={{ alignSelf: 'flex-start' }} disabled={isSubmitting || !isDirty}>
        {isSubmitting && <span className="spinner" />}
        {isSubmitting ? 'Saving…' : 'Save account details'}
      </button>
    </form>
  );
}
