/**
 * The soft colour field behind the app, plus a fine grain over it.
 *
 * One element painted with radial gradients rather than several blurred circles:
 * the blurred version looked identical and made scrolling stutter, because a wide
 * blur is re-rasterised as it moves. Gradients are painted once and cost nothing
 * per frame.
 *
 * Decorative, so it is hidden from assistive technology and cannot take a pointer
 * event. The drift is CSS, which lets the reduced-motion and small-screen rules in
 * futuristic.css switch it off without this component knowing.
 */
export default function Ambient() {
  return (
    <>
      <div className="aurora" aria-hidden="true">
        <div className="aurora__field" />
      </div>
      <div className="grain" aria-hidden="true" />
    </>
  );
}
