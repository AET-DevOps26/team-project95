import { useEffect, useId, useMemo, useRef, useState } from 'react';
import styles from '../style/HomePage.module.css';
import xIcon from '/assets/icons/x.svg';

interface FilterOption {
  value: string;
  label: string;
}

interface FilterDropdownProps {
  label: string;
  values: string[];
  options: FilterOption[];
  onChange: (values: string[]) => void;
}

export default function FilterDropdown({ label, values, options, onChange }: FilterDropdownProps) {
  const [query, setQuery] = useState('');
  const [isOpen, setIsOpen] = useState(false);
  const wrapperRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);
  const outsidePointerStartRef = useRef<{ x: number; y: number } | null>(null);
  const selectedCount = values.length;
  const menuId = useId();
  const placeholderText =
    selectedCount > 0 ? `${selectedCount} item${selectedCount > 1 ? 's' : ''} selected` : `Search ${label.toLowerCase()}...`;

  const filteredOptions = useMemo(() => {
    const normalizedQuery = query.trim().toLowerCase();

    if (!normalizedQuery) {
      return options;
    }

    return options.filter((option) => option.label.toLowerCase().includes(normalizedQuery));
  }, [options, query]);

  const closeDropdown = () => {
    setIsOpen(false);
    setQuery('');
  };

  const openDropdown = () => {
    setIsOpen(true);

    if (window.matchMedia('(max-width: 700px)').matches) {
      window.setTimeout(() => wrapperRef.current?.scrollIntoView({ block: 'start', behavior: 'smooth' }), 80);
      window.setTimeout(() => wrapperRef.current?.scrollIntoView({ block: 'start', behavior: 'smooth' }), 320);
    }
  };

  const blurInputOnTouch = () => {
    if (window.matchMedia('(pointer: coarse)').matches) {
      inputRef.current?.blur();
    }
  };

  const toggleSelection = (optionValue: string) => {
    if (values.includes(optionValue)) {
      onChange(values.filter((value) => value !== optionValue));
      return;
    }

    onChange([...values, optionValue]);
  };

  useEffect(() => {
    if (!isOpen) {
      return;
    }

    const handlePointerDown = (event: PointerEvent) => {
      if (wrapperRef.current?.contains(event.target as Node)) {
        outsidePointerStartRef.current = null;
        return;
      }

      outsidePointerStartRef.current = { x: event.clientX, y: event.clientY };
    };

    const handlePointerUp = (event: PointerEvent) => {
      const pointerStart = outsidePointerStartRef.current;
      outsidePointerStartRef.current = null;

      if (!pointerStart || wrapperRef.current?.contains(event.target as Node)) {
        return;
      }

      const deltaX = Math.abs(event.clientX - pointerStart.x);
      const deltaY = Math.abs(event.clientY - pointerStart.y);
      const isTap = deltaX < 8 && deltaY < 8;

      if (isTap) {
        closeDropdown();
      }
    };

    document.addEventListener('pointerdown', handlePointerDown);
    document.addEventListener('pointerup', handlePointerUp);

    return () => {
      document.removeEventListener('pointerdown', handlePointerDown);
      document.removeEventListener('pointerup', handlePointerUp);
    };
  }, [isOpen]);

  return (
    <div
      ref={wrapperRef}
      className={`${styles.filterBox} ${isOpen ? styles.filterBoxOpen : ''}`}
    >
      <div className={styles.filterHeader}>
        <span className={styles.filterLabel}>{label}</span>
        <button
          className={styles.filterClearButton}
          type="button"
          disabled={selectedCount === 0}
          onClick={() => {
            onChange([]);
            closeDropdown();
          }}
        >
          Clear
        </button>
      </div>
      <div className={styles.filterSearchWrapper}>
        <input
          ref={inputRef}
          className={`${styles.filterInput} ${selectedCount > 0 ? styles.filterInputSelected : ''}`}
          type="text"
          value={query}
          placeholder={placeholderText}
          aria-label={`${label} filter search`}
          aria-expanded={isOpen}
          aria-controls={menuId}
          onFocus={openDropdown}
          onChange={(event) => {
            setQuery(event.target.value);
            openDropdown();
          }}
        />

        {isOpen && (
          <ul id={menuId} className={styles.filterMenu} role="listbox" aria-label={`${label} options`}>
            {filteredOptions.map((option) => {
              const isSelected = values.includes(option.value);

              return (
                <li key={option.value} role="option" aria-selected={isSelected}>
                  <button
                    className={`${styles.filterMenuItem} ${isSelected ? styles.filterMenuItemSelected : ''}`}
                    type="button"
                    onMouseDown={(event) => {
                      event.preventDefault();
                    }}
                    onClick={() => {
                      toggleSelection(option.value);
                      blurInputOnTouch();
                    }}
                  >
                    <span>{option.label}</span>
                    {isSelected && (
                      <span className={styles.filterUnselectMark} aria-hidden="true">
                        <img src={xIcon} alt="" className={styles.filterUnselectIcon} />
                      </span>
                    )}
                  </button>
                </li>
              );
            })}
            {filteredOptions.length === 0 && <li className={styles.filterMenuEmpty}>No matches</li>}
          </ul>
        )}
      </div>
    </div>
  );
}
