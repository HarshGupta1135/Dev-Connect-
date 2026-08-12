import { useEffect, useState } from 'react';
import { useLocation } from 'react-router-dom';

/**
 * A thin bar that runs across the top on navigation.
 *
 * Honest about what it measures: route changes here are instant, so there is no
 * real duration to report. It eases toward 90% and then completes, which is the
 * same convention browsers use — enough to acknowledge the tap on a slow device
 * without pretending to track a download.
 */
export default function RouteProgress() {
  const { pathname } = useLocation();
  const [width, setWidth] = useState(0);
  const [visible, setVisible] = useState(false);

  useEffect(() => {
    setVisible(true);
    setWidth(28);

    const climb = setTimeout(() => setWidth(72), 90);
    const nearly = setTimeout(() => setWidth(92), 200);
    const finish = setTimeout(() => setWidth(100), 320);
    const hide = setTimeout(() => setVisible(false), 520);
    const reset = setTimeout(() => setWidth(0), 700);

    return () => [climb, nearly, finish, hide, reset].forEach(clearTimeout);
  }, [pathname]);

  return (
    <div
      className="route-bar"
      aria-hidden="true"
      style={{ width: `${width}%`, opacity: visible ? 1 : 0 }}
    />
  );
}
