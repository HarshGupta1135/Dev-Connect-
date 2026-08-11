export function Skeleton({ width = '100%', height = 14, radius = 6, style }) {
  return <span className="sk" style={{ display: 'block', width, height, borderRadius: radius, ...style }} />;
}

/** Placeholder shaped like a JobCard, so the layout does not jump on load. */
export function JobCardSkeleton() {
  return (
    <div className="card job-card" aria-hidden="true">
      <div className="job-card__head">
        <div className="stack" style={{ gap: 8, flex: 1 }}>
          <Skeleton width="62%" height={18} />
          <Skeleton width="38%" height={12} />
        </div>
        <Skeleton width={52} height={52} radius={999} />
      </div>
      <div className="stack" style={{ gap: 6 }}>
        <Skeleton height={11} />
        <Skeleton width="80%" height={11} />
      </div>
      <div className="row" style={{ gap: 6 }}>
        <Skeleton width={68} height={22} radius={999} />
        <Skeleton width={54} height={22} radius={999} />
        <Skeleton width={80} height={22} radius={999} />
      </div>
    </div>
  );
}

export function RowSkeleton() {
  return (
    <div className="card list-row" aria-hidden="true">
      <div className="stack" style={{ gap: 7, flex: 1 }}>
        <Skeleton width="42%" height={15} />
        <Skeleton width="24%" height={11} />
      </div>
      <Skeleton width={92} height={24} radius={999} />
    </div>
  );
}
