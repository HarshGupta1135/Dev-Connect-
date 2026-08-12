import { useForm } from 'react-hook-form';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import { errorMessage } from '../api/client';
import Field from '../components/Field';
import PasswordInput from '../components/PasswordInput';
import { useAuth } from '../context/AuthContext';

export default function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm({ defaultValues: { email: location.state?.email || '', password: '' } });

  const onSubmit = async (values) => {
    try {
      const account = await login(values);
      toast.success('Welcome back.');

      // Return the user to the page that sent them here, otherwise to the
      // dashboard that matches their role.
      const intended = location.state?.from?.pathname;
      const home = account.roles.includes('RECRUITER')
        ? '/recruiter/dashboard'
        : account.roles.includes('DEVELOPER')
          ? '/developer/dashboard'
          : '/jobs';
      navigate(intended || home, { replace: true });
    } catch (error) {
      toast.error(errorMessage(error, 'Invalid credentials'));
    }
  };

  return (
    <div className="wrap wrap--narrow section page-enter">
      <div className="stack" style={{ gap: 8, marginBottom: 28 }}>
        <span className="eyebrow">Sign in</span>
        <h1 style={{ fontSize: 'clamp(2rem, 4vw, 2.7rem)' }}>Welcome back</h1>
        <p className="lede">Pick up where you left off.</p>
      </div>

      <form className="card card--pad stack" style={{ gap: 18 }} onSubmit={handleSubmit(onSubmit)} noValidate>
        <Field label="Email" htmlFor="email" error={errors.email}>
          <input
            id="email"
            type="email"
            autoComplete="email"
            autoFocus
            aria-invalid={Boolean(errors.email)}
            placeholder="you@gmail.com"
            {...register('email', { required: 'Email is required' })}
          />
        </Field>

        <Field label="Password" htmlFor="password" error={errors.password}>
          <PasswordInput
            id="password"
            autoComplete="current-password"
            aria-invalid={Boolean(errors.password)}
            placeholder="••••••••"
            {...register('password', { required: 'Password is required' })}
          />
        </Field>

        <button type="submit" className="btn btn--lg btn--block" disabled={isSubmitting}>
          {isSubmitting && <span className="spinner" />}
          {isSubmitting ? 'Signing in…' : 'Sign in'}
        </button>

        <p className="small muted" style={{ textAlign: 'center' }}>
          New here? <Link to="/register" style={{ color: 'var(--accent)', fontWeight: 600 }}>Create an account</Link>
        </p>
      </form>
    </div>
  );
}
