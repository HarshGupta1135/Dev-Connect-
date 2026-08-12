import { useForm } from 'react-hook-form';
import { Link, useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import { errorMessage } from '../api/client';
import { registerUser } from '../api/endpoints';
import Field from '../components/Field';
import PasswordInput from '../components/PasswordInput';

// Mirrored from the backend's own rules so users see the problem before a round trip.
const PASSWORD_RULE = /^(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&]).{8,}$/;

export default function Register() {
  const navigate = useNavigate();
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm({ defaultValues: { username: '', email: '', password: '', role: 'DEVELOPER' } });

  const onSubmit = async (values) => {
    try {
      const message = await registerUser(values);
      toast.success(message || 'Account created. Please sign in.');
      navigate('/login', { replace: true, state: { email: values.email } });
    } catch (error) {
      toast.error(errorMessage(error, 'Could not create your account.'));
    }
  };

  return (
    <div className="wrap wrap--narrow section page-enter">
      <div className="stack" style={{ gap: 8, marginBottom: 28 }}>
        <span className="eyebrow">Create account</span>
        <h1 style={{ fontSize: 'clamp(2rem, 4vw, 2.7rem)' }}>
          Join as a <em className="italic-serif">developer</em> or a recruiter
        </h1>
        <p className="lede">One account, two very different jobs to do. Pick the side you are on.</p>
      </div>

      <form className="card card--pad stack" style={{ gap: 18 }} onSubmit={handleSubmit(onSubmit)} noValidate>
        <Field label="Username" htmlFor="username" error={errors.username}>
          <input
            id="username"
            type="text"
            autoComplete="username"
            aria-invalid={Boolean(errors.username)}
            placeholder="harsh_dev"
            {...register('username', {
              required: 'Username is required',
              minLength: { value: 3, message: 'At least 3 characters' },
            })}
          />
        </Field>

        <Field
          label="Email"
          htmlFor="email"
          error={errors.email}
          hint="A Gmail address is required by the API."
        >
          <input
            id="email"
            type="email"
            autoComplete="email"
            aria-invalid={Boolean(errors.email)}
            placeholder="you@gmail.com"
            {...register('email', {
              required: 'Email is required',
              pattern: { value: /^[^\s@]+@[^\s@]+\.[^\s@]+$/, message: 'Enter a valid email address' },
              validate: (value) =>
                value.toLowerCase().endsWith('@gmail.com') || 'Only @gmail.com addresses are accepted',
            })}
          />
        </Field>

        <Field
          label="Password"
          htmlFor="password"
          error={errors.password}
          hint="At least 8 characters, with one capital, one number and one of @$!%*?&"
        >
          <PasswordInput
            id="password"
            autoComplete="new-password"
            aria-invalid={Boolean(errors.password)}
            placeholder="••••••••"
            {...register('password', {
              required: 'Password is required',
              pattern: {
                value: PASSWORD_RULE,
                message: 'Needs 8+ characters with a capital, a number and a special character',
              },
            })}
          />
        </Field>

        <Field label="I am joining as" error={errors.role}>
          <div className="role-picker">
            <label className="role-option">
              <input type="radio" value="DEVELOPER" {...register('role', { required: true })} />
              <strong>Developer</strong>
              <span className="tiny muted">Build a skills profile and apply to roles</span>
            </label>
            <label className="role-option">
              <input type="radio" value="RECRUITER" {...register('role', { required: true })} />
              <strong>Recruiter</strong>
              <span className="tiny muted">Post jobs and review matched candidates</span>
            </label>
          </div>
        </Field>

        <button type="submit" className="btn btn--lg btn--block" disabled={isSubmitting}>
          {isSubmitting && <span className="spinner" />}
          {isSubmitting ? 'Creating account…' : 'Create account'}
        </button>

        <p className="small muted" style={{ textAlign: 'center' }}>
          Already registered? <Link to="/login" style={{ color: 'var(--accent)', fontWeight: 600 }}>Sign in</Link>
        </p>
      </form>
    </div>
  );
}
