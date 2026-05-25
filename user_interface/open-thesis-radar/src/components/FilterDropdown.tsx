import { useId, useMemo, useState } from 'react';
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

  const toggleSelection = (optionValue: string) => {
    if (values.includes(optionValue)) {
      onChange(values.filter((value) => value !== optionValue));
      return;
    }

    onChange([...values, optionValue]);
  };

  return (
    <label className={styles.filterBox}>
      <span className={styles.filterLabel}>{label}</span>
      <div className={styles.filterSearchWrapper}>
        <input
          className={`${styles.filterInput} ${selectedCount > 0 ? styles.filterInputSelected : ''}`}
          type="text"
          value={query}
          placeholder={placeholderText}
          aria-label={`${label} filter search`}
          aria-expanded={isOpen}
          aria-controls={menuId}
          onFocus={() => setIsOpen(true)}
          onBlur={() => {
            setTimeout(() => {
              setIsOpen(false);
              setQuery('');
            }, 120);
          }}
          onChange={(event) => {
            setQuery(event.target.value);
            setIsOpen(true);
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
                    onMouseDown={(event) => event.preventDefault()}
                    onClick={() => toggleSelection(option.value)}
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
    </label>
  );
}
