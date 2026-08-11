export default function EmptyState({ mark = '∅', title, message, action }) {
  return (
    <div className="card empty">
      <span className="empty__mark" aria-hidden="true">{mark}</span>
      <h3>{title}</h3>
      {message && <p className="muted small" style={{ maxWidth: '46ch' }}>{message}</p>}
      {action}
    </div>
  );
}
