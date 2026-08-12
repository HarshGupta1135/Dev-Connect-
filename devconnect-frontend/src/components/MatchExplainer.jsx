import { useMemo, useState } from 'react';
import MatchRing from './MatchRing';
import { useSpotlight } from '../hooks/useSpotlight';

/**
 * Shows the scoring instead of describing it.
 *
 * Toggle the skills on a pretend profile and the percentage recomputes with the
 * same Jaccard formula the backend uses — intersection over union — so the
 * behaviour a visitor plays with here is the behaviour they get on /jobs. Nothing
 * is fetched; the role is fixed and the maths is local.
 */

const ROLE_SKILLS = ['Java', 'Spring Boot', 'MySQL', 'Redis'];
const OFFERED = ['Java', 'Spring Boot', 'MySQL', 'Redis', 'React', 'Python'];

export default function MatchExplainer() {
  const [mine, setMine] = useState(['Java', 'Spring Boot']);
  const spotlight = useSpotlight();

  const { score, matched } = useMemo(() => {
    const overlap = mine.filter((skill) => ROLE_SKILLS.includes(skill));
    const union = new Set([...mine, ...ROLE_SKILLS]);
    return {
      score: union.size === 0 ? 0 : Math.round((overlap.length / union.size) * 100),
      matched: overlap,
    };
  }, [mine]);

  const toggle = (skill) =>
    setMine((current) =>
      current.includes(skill) ? current.filter((entry) => entry !== skill) : [...current, skill]
    );

  return (
    <div
      className="card card--pad spot lit stack"
      style={{ gap: 16, height: '100%' }}
      ref={spotlight.ref}
      onPointerMove={spotlight.onPointerMove}
    >
      <div className="spread" style={{ alignItems: 'flex-start' }}>
        <div className="stack" style={{ gap: 5 }}>
          <span className="feature__num">TRY THE MATCHING</span>
          <h3>Your skills, scored against a role</h3>
        </div>
        <MatchRing value={score} size={64} stroke={5} />
      </div>

      <div className="panel stack" style={{ gap: 8 }}>
        <span className="tiny faint mono" style={{ textTransform: 'uppercase', letterSpacing: '0.1em' }}>
          The role wants
        </span>
        <div className="row" style={{ gap: 6, flexWrap: 'wrap' }}>
          {ROLE_SKILLS.map((skill) => (
            <span key={skill} className={matched.includes(skill) ? 'chip chip--accent' : 'chip'}>
              {matched.includes(skill) ? '✓ ' : ''}{skill}
            </span>
          ))}
        </div>
      </div>

      <div className="stack" style={{ gap: 8 }}>
        <span className="tiny faint mono" style={{ textTransform: 'uppercase', letterSpacing: '0.1em' }}>
          Your profile — tap to change
        </span>
        <div className="row" style={{ gap: 6, flexWrap: 'wrap' }}>
          {OFFERED.map((skill) => {
            const active = mine.includes(skill);
            return (
              <button
                key={skill}
                type="button"
                className={active ? 'chip chip--accent' : 'chip'}
                aria-pressed={active}
                onClick={() => toggle(skill)}
                style={{ cursor: 'pointer', border: active ? '1px solid transparent' : undefined }}
              >
                {skill}
              </button>
            );
          })}
        </div>
      </div>

      <p className="tiny faint mono" style={{ marginTop: 'auto' }}>
        {matched.length} shared ÷ {new Set([...mine, ...ROLE_SKILLS]).size} total = {score}%
      </p>
      <p className="tiny muted">
        Adding a skill the role does not want lowers the score — that is the point. A role
        wanting one thing you have should not outrank one wanting five.
      </p>
    </div>
  );
}
