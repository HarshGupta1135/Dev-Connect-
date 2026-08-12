/**
 * The drifting colour field behind the whole app, plus a fine grain over it.
 *
 * Purely decorative, so it is hidden from assistive technology and cannot receive
 * a pointer event. The animation is CSS, which means the reduced-motion rule in
 * futuristic.css freezes it without this component knowing anything about it.
 */
export default function Ambient() {
  return (
    <>
      <div className="aurora" aria-hidden="true">
        <span className="aurora__blob" />
        <span className="aurora__blob" />
        <span className="aurora__blob" />
      </div>
      <div className="grain" aria-hidden="true" />
    </>
  );
}
