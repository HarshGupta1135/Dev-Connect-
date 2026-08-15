import { useCallback, useRef } from 'react';

/**
 * Tracks the pointer within an element and writes it to --mx / --my, which
 * `.spot::after` reads to place a soft highlight. With { tilt: true } it also
 * writes --rx / --ry, which `.tilt` turns into a slight 3D lean toward the
 * cursor — the card feels like it faces you.
 *
 * Written straight to the style attribute rather than through state: this fires
 * on every pointer move, and a re-render per frame would be absurd for a
 * decoration. Both effects are transform/gradient only, so nothing repaints.
 * Skipped for coarse pointers, where there is no hover to follow, and under
 * reduced motion.
 */
export function useSpotlight({ tilt = false, maxTilt = 5 } = {}) {
  const ref = useRef(null);

  const onPointerMove = useCallback(
    (event) => {
      const node = ref.current;
      if (!node) return;
      if (window.matchMedia('(pointer: coarse)').matches) return;
      if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return;

      const rect = node.getBoundingClientRect();
      const x = event.clientX - rect.left;
      const y = event.clientY - rect.top;
      node.style.setProperty('--mx', `${x}px`);
      node.style.setProperty('--my', `${y}px`);

      if (tilt) {
        const ry = ((x / rect.width) - 0.5) * maxTilt;
        const rx = ((y / rect.height) - 0.5) * -maxTilt;
        node.style.setProperty('--rx', `${rx.toFixed(2)}deg`);
        node.style.setProperty('--ry', `${ry.toFixed(2)}deg`);
      }
    },
    [tilt, maxTilt]
  );

  // Without the reset a card stays leaning the way the cursor left it.
  const onPointerLeave = useCallback(() => {
    const node = ref.current;
    if (!node) return;
    node.style.setProperty('--rx', '0deg');
    node.style.setProperty('--ry', '0deg');
  }, []);

  return { ref, onPointerMove, onPointerLeave };
}
