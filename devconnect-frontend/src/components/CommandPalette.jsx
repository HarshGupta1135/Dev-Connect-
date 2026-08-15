import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useTheme } from '../context/ThemeContext';
import { useSavedJobs, readViewedJobs } from '../hooks/useSavedJobs';

/**
 * Ctrl/⌘ K — navigate, search by skill, or run an action.
 *
 * Deliberately built on what is already in the app rather than a new API: routes
 * come from the router, skills from a short static list, recent jobs from
 * localStorage. Nothing here needs the backend, so it works while the API is
 * asleep on a free tier.
 *
 * Opening is owned here rather than by a parent, because the shortcut must work
 * from any page.
 */

const COMMON_SKILLS = ['Java', 'Python', 'React', 'Spring Boot', 'MySQL', 'Redis', 'Docker', 'AWS', 'TypeScript', 'Node.js'];

/**
 * Lets anything open the palette without lifting its state into a provider —
 * the navbar button uses this rather than faking a keypress.
 */
export const OPEN_PALETTE = 'devconnect:openPalette';
export const openCommandPalette = () => window.dispatchEvent(new CustomEvent(OPEN_PALETTE));

function Icon({ d, fill = 'none' }) {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill={fill} stroke="currentColor" strokeWidth="2" strokeLinecap="round">
      <path d={d} />
    </svg>
  );
}

const ICONS = {
  search: 'M11 4a7 7 0 1 0 0 14 7 7 0 0 0 0-14zM20 20l-3.5-3.5',
  home: 'M4 10.5 12 4l8 6.5V20H4z',
  briefcase: 'M4 8h16v12H4zM9 8V5h6v3',
  user: 'M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8zM4 20c0-3.3 3.6-5 8-5s8 1.7 8 5',
  bookmark: 'M7 4h10v16l-5-4-5 4z',
  sun: 'M12 8a4 4 0 1 0 0 8 4 4 0 0 0 0-8zM12 2v2M12 20v2M2 12h2M20 12h2',
  moon: 'M20 13A8 8 0 1 1 11 4a6.5 6.5 0 0 0 9 9z',
  monitor: 'M4 5h16v11H4zM9 20h6',
  exit: 'M15 4h4v16h-4M11 8l-4 4 4 4M7 12h9',
  plus: 'M12 5v14M5 12h14',
  book: 'M4 5h7v15H4zM13 5h7v15h-7',
};

export default function CommandPalette() {
  const [open, setOpen] = useState(false);
  const [query, setQuery] = useState('');
  const [cursor, setCursor] = useState(0);
  const inputRef = useRef(null);
  const listRef = useRef(null);

  const navigate = useNavigate();
  const { isAuthenticated, isDeveloper, isRecruiter, logout, homeFor } = useAuth();
  const { theme, setTheme } = useTheme();
  const { count: savedCount } = useSavedJobs();

  const close = useCallback(() => {
    setOpen(false);
    setQuery('');
    setCursor(0);
  }, []);

  /* Global shortcuts. Ctrl/⌘K opens; "/" opens too, but only when the visitor is
     not already typing somewhere, or it would swallow a slash mid-sentence. */
  useEffect(() => {
    const onKey = (event) => {
      const key = event.key.toLowerCase();
      const inField = /^(input|textarea|select)$/i.test(event.target?.tagName || '');

      if ((event.metaKey || event.ctrlKey) && key === 'k') {
        event.preventDefault();
        setOpen((value) => !value);
        return;
      }
      if (key === '/' && !inField && !open) {
        event.preventDefault();
        setOpen(true);
      }
    };
    const onRequest = () => setOpen(true);
    window.addEventListener('keydown', onKey);
    window.addEventListener(OPEN_PALETTE, onRequest);
    return () => {
      window.removeEventListener('keydown', onKey);
      window.removeEventListener(OPEN_PALETTE, onRequest);
    };
  }, [open]);

  // Focus the field and lock the page behind it, mirroring Modal's behaviour.
  useEffect(() => {
    if (!open) return undefined;
    const previous = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    const timer = setTimeout(() => inputRef.current?.focus(), 20);
    return () => {
      document.body.style.overflow = previous;
      clearTimeout(timer);
    };
  }, [open]);

  const go = useCallback(
    (to) => {
      close();
      navigate(to);
    },
    [close, navigate]
  );

  const items = useMemo(() => {
    const trimmed = query.trim();
    const list = [];

    /* A free-text query is most likely a skill search, so that goes first. */
    if (trimmed) {
      list.push({
        group: 'Search',
        icon: ICONS.search,
        label: `Find roles matching “${trimmed}”`,
        run: () => go(`/jobs?skill=${encodeURIComponent(trimmed)}`),
      });
    }

    list.push(
      { group: 'Go to', icon: ICONS.home, label: 'Home', keywords: 'landing start', run: () => go('/') },
      { group: 'Go to', icon: ICONS.briefcase, label: 'Browse jobs', keywords: 'roles search openings', run: () => go('/jobs') }
    );

    if (isAuthenticated) {
      list.push({ group: 'Go to', icon: ICONS.home, label: 'My dashboard', keywords: 'account overview', run: () => go(homeFor) });
    }
    if (isDeveloper) {
      list.push(
        { group: 'Go to', icon: ICONS.user, label: 'My profile', keywords: 'skills resume bio edit', run: () => go('/developer/profile') },
        { group: 'Go to', icon: ICONS.briefcase, label: 'My applications', keywords: 'applied status', run: () => go('/developer/dashboard') }
      );
    }
    if (isRecruiter) {
      list.push(
        { group: 'Go to', icon: ICONS.plus, label: 'Post a job', keywords: 'new role create', run: () => go('/recruiter/jobs/new') },
        { group: 'Go to', icon: ICONS.briefcase, label: 'My postings', keywords: 'jobs applicants', run: () => go('/recruiter/dashboard') }
      );
    }

    list.push({
      group: 'Go to',
      icon: ICONS.bookmark,
      label: 'Saved jobs',
      sub: savedCount ? `${savedCount} saved` : 'none yet',
      keywords: 'bookmarks shortlist',
      run: () => go('/jobs?saved=1'),
    });

    COMMON_SKILLS.forEach((skill) => {
      list.push({
        group: 'Search by skill',
        icon: ICONS.search,
        label: skill,
        keywords: `skill ${skill}`,
        run: () => go(`/jobs?skill=${encodeURIComponent(skill)}`),
      });
    });

    readViewedJobs().forEach((job) => {
      list.push({
        group: 'Recently viewed',
        icon: ICONS.briefcase,
        label: job.title || `Job #${job.id}`,
        sub: job.companyName || undefined,
        keywords: `${job.title} ${job.companyName || ''}`,
        run: () => go(`/jobs/${job.id}`),
      });
    });

    list.push(
      { group: 'Appearance', icon: ICONS.sun, label: 'Light theme', sub: theme === 'light' ? 'current' : undefined, keywords: 'day bright', run: () => { setTheme('light'); close(); } },
      { group: 'Appearance', icon: ICONS.moon, label: 'Dark theme', sub: theme === 'dark' ? 'current' : undefined, keywords: 'night', run: () => { setTheme('dark'); close(); } },
      { group: 'Appearance', icon: ICONS.monitor, label: 'Match system theme', sub: theme === 'system' ? 'current' : undefined, keywords: 'auto os', run: () => { setTheme('system'); close(); } }
    );

    // Absolute against the API origin: relative would stay on this app's domain,
    // where the SPA rewrite serves index.html for every path — a 404 in disguise.
    list.push({ group: 'Help', icon: ICONS.book, label: 'API documentation', keywords: 'swagger openapi', run: () => { close(); window.open(`${import.meta.env.VITE_API_BASE_URL || ''}/swagger-ui/index.html`, '_blank', 'noopener'); } });

    if (isAuthenticated) {
      list.push({ group: 'Account', icon: ICONS.exit, label: 'Sign out', keywords: 'logout leave', run: () => { close(); logout('Signed out.'); navigate('/'); } });
    } else {
      list.push(
        { group: 'Account', icon: ICONS.user, label: 'Sign in', keywords: 'login', run: () => go('/login') },
        { group: 'Account', icon: ICONS.plus, label: 'Create an account', keywords: 'register signup join', run: () => go('/register') }
      );
    }

    if (!trimmed) return list;

    const needle = trimmed.toLowerCase();
    return list.filter(
      (item) =>
        item.group === 'Search' ||
        `${item.label} ${item.keywords || ''} ${item.sub || ''}`.toLowerCase().includes(needle)
    );
  }, [query, isAuthenticated, isDeveloper, isRecruiter, homeFor, savedCount, theme, setTheme, logout, navigate, go, close]);

  // A shrinking result list must never leave the cursor pointing past the end.
  useEffect(() => setCursor((value) => Math.min(value, Math.max(0, items.length - 1))), [items.length]);

  const onKeyDown = (event) => {
    if (event.key === 'Escape') {
      event.preventDefault();
      close();
      return;
    }
    if (event.key === 'ArrowDown' || (event.key === 'Tab' && !event.shiftKey)) {
      event.preventDefault();
      setCursor((value) => (value + 1) % Math.max(1, items.length));
      return;
    }
    if (event.key === 'ArrowUp' || (event.key === 'Tab' && event.shiftKey)) {
      event.preventDefault();
      setCursor((value) => (value - 1 + items.length) % Math.max(1, items.length));
      return;
    }
    if (event.key === 'Enter') {
      event.preventDefault();
      items[cursor]?.run();
    }
  };

  // Keep the highlighted row in view when arrowing past the fold.
  useEffect(() => {
    listRef.current?.querySelector('[data-active="true"]')?.scrollIntoView({ block: 'nearest' });
  }, [cursor]);

  if (!open) return null;

  let lastGroup = null;

  return (
    <div
      className="cmdk-overlay"
      onMouseDown={(event) => event.target === event.currentTarget && close()}
    >
      {/* Handled on the panel, not the input: clicking a row moves focus off the
          field, and Escape has to keep working after that. */}
      <div className="cmdk" role="dialog" aria-modal="true" aria-label="Command palette" onKeyDown={onKeyDown}>
        <div className="cmdk__input">
          <span style={{ color: 'var(--ink-faint)', display: 'grid' }}>
            <Icon d={ICONS.search} />
          </span>
          <input
            ref={inputRef}
            value={query}
            onChange={(event) => { setQuery(event.target.value); setCursor(0); }}
            placeholder="Search jobs, pages and actions…"
            aria-label="Search commands"
            autoComplete="off"
            spellCheck="false"
          />
          <kbd>esc</kbd>
        </div>

        <div className="cmdk__list" ref={listRef}>
          {items.length === 0 ? (
            <p className="small muted" style={{ padding: '18px 12px', textAlign: 'center' }}>
              Nothing matches “{query.trim()}”.
            </p>
          ) : (
            items.map((item, index) => {
              const heading = item.group !== lastGroup ? item.group : null;
              lastGroup = item.group;
              return (
                <div key={`${item.group}-${item.label}-${index}`}>
                  {heading && <div className="cmdk__group">{heading}</div>}
                  <button
                    type="button"
                    className="cmdk__item"
                    data-active={index === cursor}
                    onMouseMove={() => setCursor(index)}
                    onClick={item.run}
                  >
                    <span className="cmdk__icon"><Icon d={item.icon} /></span>
                    <span>{item.label}</span>
                    {item.sub && <span className="cmdk__sub">{item.sub}</span>}
                  </button>
                </div>
              );
            })
          )}
        </div>

        <div className="cmdk__foot">
          <span><kbd>↑</kbd> <kbd>↓</kbd> navigate</span>
          <span><kbd>↵</kbd> open</span>
          <span style={{ marginLeft: 'auto' }}><kbd>/</kbd> anywhere</span>
        </div>
      </div>
    </div>
  );
}
