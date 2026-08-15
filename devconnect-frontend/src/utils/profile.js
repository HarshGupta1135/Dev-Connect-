/**
 * The fields a profile must have before the API accepts an application —
 * mirrored from ApplicationService.requireCompleteProfile, and kept in one
 * place so the profile meter and the apply gate cannot disagree.
 *
 * Email is not listed: it is the sign-in identity and cannot be missing.
 */
export const REQUIRED_PROFILE_FIELDS = [
  { key: 'fullName', label: 'Full name' },
  { key: 'phone', label: 'Phone number' },
  { key: 'address', label: 'Address' },
  { key: 'city', label: 'City' },
  { key: 'pincode', label: 'Pincode' },
  { key: 'location', label: 'Location' },
  { key: 'resumeUrl', label: 'Resume' },
];

export function missingProfileFields(profile) {
  if (!profile) return REQUIRED_PROFILE_FIELDS;
  return REQUIRED_PROFILE_FIELDS.filter(
    (field) => !String(profile[field.key] ?? '').trim()
  );
}

export function profileCompleteness(profile) {
  const missing = missingProfileFields(profile);
  const total = REQUIRED_PROFILE_FIELDS.length;
  return {
    missing,
    done: total - missing.length,
    total,
    percent: Math.round(((total - missing.length) / total) * 100),
  };
}
