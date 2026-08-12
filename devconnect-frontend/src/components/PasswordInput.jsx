import { useState } from 'react';

/**
 * Password field with a show/hide button inside it.
 *
 * Takes the same props as a bare <input>, including react-hook-form's register()
 * spread, so callers change by one word. The visibility state lives here because
 * nothing outside needs it.
 *
 * The button is type="button" — inside a form, a button without that submits it,
 * which would mean revealing your password by signing in.
 *
 * aria-pressed carries the state, and the label describes the action rather than
 * the state, so a screen reader announces "Show password, not pressed" instead of
 * leaving the two to contradict each other.
 */
export default function PasswordInput({ id, ...rest }) {
  const [visible, setVisible] = useState(false);

  return (
    <div className="pw">
      <input id={id} type={visible ? 'text' : 'password'} {...rest} />
      <button
        type="button"
        className="pw__toggle"
        onClick={() => setVisible((value) => !value)}
        aria-pressed={visible}
        aria-controls={id}
        aria-label={visible ? 'Hide password' : 'Show password'}
        title={visible ? 'Hide password' : 'Show password'}
      >
        {visible ? (
          <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.9" strokeLinecap="round">
            <path d="M3 3l18 18" />
            <path d="M10.6 10.7a2 2 0 0 0 2.8 2.8" />
            <path d="M9.4 5.2A9.6 9.6 0 0 1 12 4.9c4.6 0 8.2 3.4 9.5 7.1a12 12 0 0 1-2.4 3.6M6.2 6.7A12.4 12.4 0 0 0 2.5 12c1.3 3.7 4.9 7.1 9.5 7.1 1.6 0 3-.4 4.3-1" />
          </svg>
        ) : (
          <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.9" strokeLinecap="round">
            <path d="M2.5 12C3.8 8.3 7.4 4.9 12 4.9s8.2 3.4 9.5 7.1c-1.3 3.7-4.9 7.1-9.5 7.1S3.8 15.7 2.5 12z" />
            <circle cx="12" cy="12" r="2.6" />
          </svg>
        )}
      </button>
    </div>
  );
}
