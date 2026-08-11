/**
 * Page numbers with ellipses, driven by the backend's CustomPageResponse.
 * `page` is zero-based, matching the API.
 */
export default function Pagination({ page, totalPages, onChange }) {
  if (!totalPages || totalPages <= 1) return null;

  const numbers = [];
  // Named "spread", not "window": a local const called window would shadow the
  // global inside this module and break anything here that needs it later.
  const spread = 1;
  for (let i = 0; i < totalPages; i += 1) {
    const isEdge = i === 0 || i === totalPages - 1;
    const isNear = Math.abs(i - page) <= spread;
    if (isEdge || isNear) numbers.push(i);
    else if (numbers[numbers.length - 1] !== '…') numbers.push('…');
  }

  return (
    <nav className="pagination" aria-label="Pagination">
      <button
        type="button"
        className="page-btn"
        onClick={() => onChange(page - 1)}
        disabled={page <= 0}
        aria-label="Previous page"
      >
        ←
      </button>

      {numbers.map((entry, index) =>
        entry === '…' ? (
          <span key={`gap-${index}`} className="faint mono small" style={{ padding: '0 4px' }}>…</span>
        ) : (
          <button
            key={entry}
            type="button"
            className="page-btn"
            aria-current={entry === page}
            aria-label={`Page ${entry + 1}`}
            onClick={() => onChange(entry)}
          >
            {entry + 1}
          </button>
        )
      )}

      <button
        type="button"
        className="page-btn"
        onClick={() => onChange(page + 1)}
        disabled={page >= totalPages - 1}
        aria-label="Next page"
      >
        →
      </button>
    </nav>
  );
}
