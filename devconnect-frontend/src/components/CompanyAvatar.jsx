/**
 * Initial block standing in for a company logo, which the API does not have.
 *
 * The hue is hashed from the company name, so the same company is always the
 * same colour across cards, pages and sessions — consistent enough that the
 * colour starts working as identity, which is the whole job of a logo. Saturation
 * and lightness stay fixed in the CSS so every hue lands in the same tonal range
 * as the rest of the theme.
 */
export default function CompanyAvatar({ name, size = 40 }) {
  const label = (name || '?').trim();

  let hash = 0;
  for (let i = 0; i < label.length; i += 1) {
    hash = (hash * 31 + label.charCodeAt(i)) % 360;
  }

  return (
    <span
      className="avatar"
      style={{ '--avatar-h': hash, width: size, height: size }}
      aria-hidden="true"
    >
      {label.charAt(0).toUpperCase()}
    </span>
  );
}
