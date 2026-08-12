import { useTheme } from '../context/ThemeContext';

/**
 * Three-way theme control, replacing the two-way toggle.
 *
 * "System" was always supported by ThemeContext but had no way to get back to it
 * from the UI: the old toggle only ever set light or dark, so once a visitor
 * touched it their choice was pinned for good and stopped following the OS.
 */

const OPTIONS = [
  {
    value: 'light',
    label: 'Light',
    icon: (
      <>
        <circle cx="12" cy="12" r="4.2" />
        <path d="M12 2v2M12 20v2M2 12h2M20 12h2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M19.1 4.9l-1.4 1.4M6.3 17.7l-1.4 1.4" />
      </>
    ),
  },
  {
    value: 'system',
    label: 'System',
    icon: (
      <>
        <rect x="3" y="5" width="18" height="11" rx="1.5" />
        <path d="M9 20h6" />
      </>
    ),
  },
  {
    value: 'dark',
    label: 'Dark',
    icon: <path d="M21 12.8A8.5 8.5 0 1 1 11.2 3a6.6 6.6 0 0 0 9.8 9.8z" />,
  },
];

export default function ThemeControl() {
  const { theme, setTheme } = useTheme();

  return (
    <div className="seg" role="group" aria-label="Colour theme">
      {OPTIONS.map((option) => (
        <button
          key={option.value}
          type="button"
          aria-pressed={theme === option.value}
          aria-label={`${option.label} theme`}
          title={`${option.label} theme`}
          onClick={() => setTheme(option.value)}
        >
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
            {option.icon}
          </svg>
        </button>
      ))}
    </div>
  );
}
