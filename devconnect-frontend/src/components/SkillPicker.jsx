import { useEffect, useMemo, useRef, useState } from 'react';
import { fetchSkills } from '../api/endpoints';

/**
 * Skill chips with autocomplete.
 *
 * The catalogue endpoint (/api/get/all/skills) requires authentication in the
 * current backend, so for anonymous visitors the suggestion list simply is not
 * available — the input then accepts free text instead of breaking. Skills are
 * submitted as names because that is what the API matches on.
 */
export default function SkillPicker({
  value = [],
  onChange,
  placeholder = 'Type a skill and press Enter',
  id = 'skill-picker',
}) {
  const [catalogue, setCatalogue] = useState([]);
  const [query, setQuery] = useState('');
  const [open, setOpen] = useState(false);
  const [activeIndex, setActiveIndex] = useState(0);
  const wrapRef = useRef(null);

  useEffect(() => {
    let cancelled = false;
    fetchSkills()
      .then((skills) => {
        if (!cancelled && Array.isArray(skills)) {
          setCatalogue(skills.map((skill) => skill.name).filter(Boolean));
        }
      })
      .catch(() => {
        /* Not signed in, or the endpoint is unavailable: free text still works. */
      });
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    const onClickOutside = (event) => {
      if (wrapRef.current && !wrapRef.current.contains(event.target)) setOpen(false);
    };
    document.addEventListener('mousedown', onClickOutside);
    return () => document.removeEventListener('mousedown', onClickOutside);
  }, []);

  const suggestions = useMemo(() => {
    const chosen = value.map((entry) => entry.toLowerCase());
    const needle = query.trim().toLowerCase();
    return catalogue
      .filter((skill) => !chosen.includes(skill.toLowerCase()))
      .filter((skill) => (needle ? skill.toLowerCase().includes(needle) : true))
      .slice(0, 8);
  }, [catalogue, query, value]);

  const add = (skill) => {
    const clean = skill.trim();
    if (!clean) return;
    if (value.some((entry) => entry.toLowerCase() === clean.toLowerCase())) {
      setQuery('');
      return;
    }
    onChange([...value, clean]);
    setQuery('');
    setActiveIndex(0);
  };

  const remove = (skill) => onChange(value.filter((entry) => entry !== skill));

  const onKeyDown = (event) => {
    if (event.key === 'Enter') {
      event.preventDefault();
      const picked = open && suggestions[activeIndex];
      add(picked || query);
      return;
    }
    if (event.key === 'Backspace' && !query && value.length) {
      remove(value[value.length - 1]);
      return;
    }
    if (event.key === 'ArrowDown' && suggestions.length) {
      event.preventDefault();
      setOpen(true);
      setActiveIndex((index) => (index + 1) % suggestions.length);
      return;
    }
    if (event.key === 'ArrowUp' && suggestions.length) {
      event.preventDefault();
      setActiveIndex((index) => (index - 1 + suggestions.length) % suggestions.length);
      return;
    }
    if (event.key === 'Escape') setOpen(false);
  };

  return (
    <div className="picker picker-wrap" ref={wrapRef}>
      <div className="picker-box">
        {value.map((skill) => (
          <span key={skill} className="chip chip--accent">
            {skill}
            <button type="button" onClick={() => remove(skill)} aria-label={`Remove ${skill}`}>
              ×
            </button>
          </span>
        ))}
        <input
          id={id}
          type="text"
          value={query}
          placeholder={value.length ? 'Add another…' : placeholder}
          onChange={(event) => {
            setQuery(event.target.value);
            setOpen(true);
            setActiveIndex(0);
          }}
          onFocus={() => setOpen(true)}
          onKeyDown={onKeyDown}
          autoComplete="off"
          role="combobox"
          aria-expanded={open && suggestions.length > 0}
          aria-controls={`${id}-list`}
        />
      </div>

      {open && suggestions.length > 0 && (
        <div className="picker-menu" id={`${id}-list`} role="listbox">
          {suggestions.map((skill, index) => (
            <button
              key={skill}
              type="button"
              role="option"
              aria-selected={index === activeIndex}
              data-active={index === activeIndex}
              onMouseEnter={() => setActiveIndex(index)}
              onClick={() => add(skill)}
            >
              {skill}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
