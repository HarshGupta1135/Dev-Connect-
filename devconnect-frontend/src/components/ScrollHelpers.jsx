import { useEffect, useState } from 'react';
import { useLocation } from 'react-router-dom';

/** Every navigation should start at the top of the new page. */
export function ScrollToTopOnNavigate() {
  const { pathname } = useLocation();

  useEffect(() => {
    const reduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    window.scrollTo({ top: 0, behavior: reduced ? 'auto' : 'smooth' });
  }, [pathname]);

  return null;
}

/** Reading-progress line across the top of long pages. */
export function ReadingProgress() {
  const [width, setWidth] = useState(0);

  useEffect(() => {
    const onScroll = () => {
      const scrollable = document.documentElement.scrollHeight - window.innerHeight;
      setWidth(scrollable > 40 ? (window.scrollY / scrollable) * 100 : 0);
    };
    onScroll();
    window.addEventListener('scroll', onScroll, { passive: true });
    window.addEventListener('resize', onScroll);
    return () => {
      window.removeEventListener('scroll', onScroll);
      window.removeEventListener('resize', onScroll);
    };
  }, []);

  return <div className="progress-bar" style={{ width: `${width}%` }} aria-hidden="true" />;
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
