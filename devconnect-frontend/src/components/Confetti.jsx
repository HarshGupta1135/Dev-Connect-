import { useMemo } from 'react';

const COLORS = ['#6366f1', '#22d3ee', '#a855f7', '#4ade80', '#fbbf24', '#fb7185'];

/**
 * A one-off celebration burst, rendered for the moment an application goes in.
 *
 * Pure CSS animation on transform and opacity, so every piece is composited —
 * no layout, no paint after the first frame. The parent mounts it on success and
 * unmounts it a couple of seconds later; nothing here loops or persists.
 *
 * Skipped entirely under reduced motion: a screenful of falling pieces is
 * exactly what that preference asks not to see.
 */
export default function Confetti({ count = 44 }) {
  const reduced =
    typeof window !== 'undefined' &&
    window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  const pieces = useMemo(
    () =>
      Array.from({ length: count }, (_, index) => ({
        id: index,
        x: `${Math.random() * 100}%`,
        dx: `${(Math.random() - 0.5) * 30}vw`,
        d: `${1.4 + Math.random() * 1.1}s`,
        dl: `${Math.random() * 0.35}s`,
        r0: `${Math.random() * 360}deg`,
        r1: `${360 + Math.random() * 540}deg`,
        c: COLORS[index % COLORS.length],
      })),
    [count]
  );

  if (reduced) return null;

  return (
    <div className="confetti" aria-hidden="true">
      {pieces.map((piece) => (
        <i
          key={piece.id}
          style={{
            '--x': piece.x,
            '--dx': piece.dx,
            '--d': piece.d,
            '--dl': piece.dl,
            '--r0': piece.r0,
            '--r1': piece.r1,
            '--c': piece.c,
          }}
        />
      ))}
    </div>
  );
}
