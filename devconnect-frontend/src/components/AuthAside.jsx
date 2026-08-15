import MatchRing from './MatchRing';

/**
 * The panel beside the sign-in and registration forms.
 *
 * Auth pages are where a first-time visitor decides whether to bother, so the
 * space next to the form restates what the product does instead of sitting
 * empty. Hidden below 900px by the .auth-aside rule — on a phone the form is
 * the whole job.
 */
export default function AuthAside({ title, points }) {
  return (
    <aside className="auth-aside" aria-label="About DevConnect">
      <div className="spread" style={{ alignItems: 'flex-start' }}>
        <div className="stack" style={{ gap: 4 }}>
          <span className="eyebrow">DevConnect</span>
          <h3 style={{ fontSize: '1.05rem' }}>{title}</h3>
        </div>
        <MatchRing value={87} size={52} stroke={4} />
      </div>

      <ul className="stack" style={{ gap: 10 }}>
        {points.map((point) => (
          <li key={point}>{point}</li>
        ))}
      </ul>

      <p className="tiny faint" style={{ marginTop: 'auto' }}>
        Free while in beta. No card, no spam — every email it sends is about one of
        your applications.
      </p>
    </aside>
  );
}
