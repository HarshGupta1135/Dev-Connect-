import { useEffect, useRef } from 'react';
import { AnimatePresence, m } from 'motion/react';

/**
 * Dialog with Escape-to-close, scroll lock and focus moved inside on open.
 *
 * Enter and exit are spring-driven through AnimatePresence — the part CSS cannot
 * do here, because React unmounts the node the instant `open` flips and a CSS
 * exit animation never gets to run. AnimatePresence holds the node in the tree
 * until the exit finishes, so closing settles instead of vanishing.
 */
export default function Modal({ open, onClose, title, children, footer }) {
  const panelRef = useRef(null);

  useEffect(() => {
    if (!open) return undefined;

    const onKey = (event) => {
      if (event.key === 'Escape') onClose();
    };
    document.addEventListener('keydown', onKey);

    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    panelRef.current?.focus();

    return () => {
      document.removeEventListener('keydown', onKey);
      document.body.style.overflow = previousOverflow;
    };
  }, [open, onClose]);

  return (
    <AnimatePresence>
      {open && (
        <m.div
          className="overlay"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          transition={{ duration: 0.16 }}
          onMouseDown={(event) => event.target === event.currentTarget && onClose()}
        >
          <m.div
            className="modal stack"
            style={{ gap: 18 }}
            role="dialog"
            aria-modal="true"
            aria-label={title}
            tabIndex={-1}
            ref={panelRef}
            initial={{ opacity: 0, y: 16, scale: 0.96 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 10, scale: 0.97 }}
            transition={{ type: 'spring', stiffness: 380, damping: 30 }}
          >
            <div className="spread">
              <h3 style={{ fontSize: '1.25rem' }}>{title}</h3>
              <button type="button" className="btn btn--ghost btn--icon" onClick={onClose} aria-label="Close">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <path d="M6 6l12 12M18 6L6 18" />
                </svg>
              </button>
            </div>
            {children}
            {footer}
          </m.div>
        </m.div>
      )}
    </AnimatePresence>
  );
}
