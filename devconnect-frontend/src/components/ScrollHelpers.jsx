import { useEffect, useRef, useState } from 'react';
import { useLocation } from 'react-router-dom';

/**
 * Every navigation starts at the top of the new page.
 *
 * Instantly, not smoothly: the new page has already rendered by this point, so a
 * smooth scroll means watching the previous scroll position animate away before
 * the content you asked for is readable. Snapping is what makes a navigation feel
 * immediate.
 */
export function ScrollToTopOnNavigate() {
  const { pathname } = useLocation();

  useEffect(() => {
    window.scrollTo({ top: 0, behavior: 'auto' });
  }, [pathname]);

  return null;
}

/**
 * Reading-progress line across the top of long pages.
 *
 * Writes the width straight to the node inside one requestAnimationFrame per
 * frame. It used to setState on every scroll event, which re-rendered React
 * continuously while scrolling, and measured scrollHeight and innerHeight each
 * time — forcing a synchronous layout mid-scroll. Both are why long pages
 * stuttered. The page height is now measured only when it can actually change.
 */
export function ReadingProgress() {
  const ref = useRef(null);

  useEffect(() => {
    let frame = 0;
    let scrollable = 0;

    const measure = () => {
      scrollable = document.documentElement.scrollHeight - window.innerHeight;
    };

    const paint = () => {
      frame = 0;
      if (!ref.current) return;
      const value = scrollable > 40 ? Math.min(100, (window.scrollY / scrollable) * 100) : 0;
      ref.current.style.width = `${value}%`;
    };

    const onScroll = () => {
      if (!frame) frame = requestAnimationFrame(paint);
    };

    const onResize = () => {
      measure();
      onScroll();
    };

    measure();
    paint();
    window.addEventListener('scroll', onScroll, { passive: true });
    window.addEventListener('resize', onResize);

    /* Images and late-arriving data change the page height without a resize or a
       scroll, which would otherwise leave the bar reading against a stale total. */
    const observer = typeof ResizeObserver !== 'undefined' ? new ResizeObserver(onResize) : null;
    observer?.observe(document.body);

    return () => {
      if (frame) cancelAnimationFrame(frame);
      window.removeEventListener('scroll', onScroll);
      window.removeEventListener('resize', onResize);
      observer?.disconnect();
    };
  }, []);

  return <div className="progress-bar" style={{ width: 0 }} aria-hidden="true" ref={ref} />;
}

/** Appears once the viewer is far enough down to want it. */
export function BackToTopButton() {
  const [show, setShow] = useState(false);

  useEffect(() => {
    const onScroll = () => setShow(window.scrollY > 700);
    onScroll();
    window.addEventListener('scroll', onScroll, { passive: true });
    return () => window.removeEventListener('scroll', onScroll);
  }, []);

  return (
    <button
      type="button"
      className="scroll-top"
      data-show={show}
      aria-label="Back to top"
      onClick={() => window.scrollTo({ top: 0, behavior: 'smooth' })}
    >
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
        <path d="M12 19V5M12 5l-6 6M12 5l6 6" />
      </svg>
    </button>
  );
}
