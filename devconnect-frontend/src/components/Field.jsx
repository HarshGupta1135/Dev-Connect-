/**
 * Label + control + hint/error, wired for react-hook-form's error objects.
 * The control is passed as children so any input type can use it.
 */
export default function Field({ label, htmlFor, hint, error, children, optional }) {
  return (
    <div className="field">
      {label && (
        <label htmlFor={htmlFor}>
          {label}
          {optional && <span className="faint tiny" style={{ fontWeight: 500 }}> — optional</span>}
        </label>
      )}
      {children}
      {error ? (
        <span className="field-error" role="alert">{error.message || 'Please check this field'}</span>
      ) : (
        hint && <span className="field-hint">{hint}</span>
      )}
    </div>
  );
}
