/**
 * Reads the expiry out of a JWT payload without verifying it — the signature is
 * the server's business. This is only used to log a stale session out on the
 * client instead of firing doomed requests at the API.
 */
export function decodeJwt(token) {
  try {
    const payload = token.split('.')[1];
    const json = atob(payload.replace(/-/g, '+').replace(/_/g, '/'));
    return JSON.parse(decodeURIComponent(escape(json)));
  } catch {
    return null;
  }
}

/** Milliseconds until the token expires; 0 when expired, null when unknown. */
export function millisUntilExpiry(token) {
  const claims = decodeJwt(token);
  if (!claims?.exp) return null;
  return Math.max(0, claims.exp * 1000 - Date.now());
}

export function isExpired(token) {
  const remaining = millisUntilExpiry(token);
  return remaining !== null && remaining <= 0;
}
