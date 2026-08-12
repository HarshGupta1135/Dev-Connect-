import { useCallback, useRef } from 'react';

/**
 * Tracks the pointer within an element and writes it to --mx / --my, which
 * `.spot::after` in futuristic.css reads to place a soft highlight.
 *
 * Written straight to the style attribute rather than through state: this fires on
 * every pointer move, and a re-render per frame would be absurd for a decoration.
 * Skipped entirely for coarse pointers, where there is no hover to follow, and
 * when the visitor has asked for reduced motion.
 */
export function useSpotlight() {
  const ref = useRef(null);

  const onPointerMove = useCallback((event) => {
    const node = ref.current;
    if (!node) return;
    if (window.matchMedia('(pointer: coarse)').matches) return;
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return;

    const rect = node.getBoundingClientRect();
    node.style.setProperty('--mx', `${event.clientX - rect.left}px`);
    node.style.setProperty('--my', `${event.clientY - rect.top}px`);
  }, []);

  return { ref, onPointerMove };
}
