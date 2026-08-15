import { useEffect, useId, useState } from 'react';

/**
 * Skill-match score as a ring. The stroke animates from empty on mount so the
 * number reads as a measurement rather than decoration, and is painted with the
 * brand gradient rather than a flat colour — an SVG stroke cannot take a CSS
 * gradient, so it comes from a <linearGradient> def. useId keeps the def's id
 * unique per instance; a shared id would make every ring on the page silently
 * borrow whichever def rendered first.
 */
export default function MatchRing({ value = 0, size = 52, stroke = 4, ariaLabel }) {
  const [shown, setShown] = useState(0);
  const gradientId = useId();
  const radius = (size - stroke) / 2;
  const circumference = 2 * Math.PI * radius;
  const pct = Math.max(0, Math.min(100, Number(value) || 0));

  useEffect(() => {
    const reduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    if (reduced) {
      setShown(pct);
      return undefined;
    }
    const frame = requestAnimationFrame(() => setShown(pct));
    return () => cancelAnimationFrame(frame);
  }, [pct]);

  return (
    <div
      className="ring"
      style={{ width: size, height: size }}
      role="img"
      aria-label={ariaLabel || `${Math.round(pct)} percent skill match`}
      title={ariaLabel || `${Math.round(pct)}% of the required skills matched`}
    >
      <svg width={size} height={size}>
        <defs>
          <linearGradient id={gradientId} x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stopColor="var(--accent)" />
            <stop offset="100%" stopColor="var(--accent-2)" />
          </linearGradient>
        </defs>
        <circle className="track" cx={size / 2} cy={size / 2} r={radius} strokeWidth={stroke} />
        <circle
          className="value"
          cx={size / 2}
          cy={size / 2}
          r={radius}
          strokeWidth={stroke}
          stroke={`url(#${gradientId})`}
          strokeDasharray={circumference}
          strokeDashoffset={circumference - (shown / 100) * circumference}
        />
      </svg>
      <span className="ring__label">{Math.round(pct)}</span>
    </div>
  );
}
