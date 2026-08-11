import { useEffect, useRef } from 'react';

/** Dialog with Escape-to-close, scroll lock and focus moved inside on open. */
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

  if (!open) return null;

  return (
    <div className="overlay" onMouseDown={(event) => event.target === event.currentTarget && onClose()}>
      <div
        className="modal stack"
        style={{ gap: 18 }}
        role="dialog"
        aria-modal="true"
        aria-label={title}
        tabIndex={-1}
        ref={panelRef}
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
      </div>
    </div>
  );
}
