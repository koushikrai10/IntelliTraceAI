import React from 'react';
import './SearchFilter.css';

/**
 * Filter and Search Controls for Error Groups and Logs
 */
const SearchFilter = ({
  searchQuery = '',
  onSearchChange,
  selectedSeverity = 'ALL',
  onSeverityChange,
  selectedExceptionType = 'ALL',
  onExceptionTypeChange,
  availableExceptionTypes = [],
  sortBy = 'count_desc',
  onSortChange,
  totalItems = 0,
  filteredCount = 0,
  onResetFilters,
}) => {
  const severityOptions = ['ALL', 'FATAL', 'ERROR', 'WARN', 'INFO', 'DEBUG'];

  const hasActiveFilters =
    searchQuery.trim() !== '' ||
    selectedSeverity !== 'ALL' ||
    selectedExceptionType !== 'ALL' ||
    sortBy !== 'count_desc';

  return (
    <div className="glass-panel search-filter-container">
      {/* Top Search and Select Controls */}
      <div className="search-filter-row">
        {/* Search Query Input */}
        <div className="search-input-wrapper">
          <svg className="search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <circle cx="11" cy="11" r="8" />
            <line x1="21" y1="21" x2="16.65" y2="16.65" />
          </svg>
          <input
            type="text"
            className="search-input"
            placeholder="Search error patterns, messages, stack frames..."
            value={searchQuery}
            onChange={(e) => onSearchChange(e.target.value)}
          />
          {searchQuery && (
            <button
              className="search-clear-btn"
              onClick={() => onSearchChange('')}
              title="Clear search"
              aria-label="Clear search"
            >
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <line x1="18" y1="6" x2="6" y2="18" />
                <line x1="6" y1="6" x2="18" y2="18" />
              </svg>
            </button>
          )}
        </div>

        {/* Exception Type Dropdown */}
        <div className="filter-dropdown-group">
          <select
            className="filter-select"
            value={selectedExceptionType}
            onChange={(e) => onExceptionTypeChange(e.target.value)}
            aria-label="Filter by Exception Type"
          >
            <option value="ALL">All Exception Types</option>
            {availableExceptionTypes.map((type) => (
              <option key={type} value={type}>
                {type}
              </option>
            ))}
          </select>

          {/* Sort By Dropdown */}
          <select
            className="filter-select"
            value={sortBy}
            onChange={(e) => onSortChange(e.target.value)}
            aria-label="Sort errors by"
          >
            <option value="count_desc">Highest Frequency</option>
            <option value="count_asc">Lowest Frequency</option>
            <option value="severity">Severity Rank</option>
            <option value="alphabetical">Alphabetical Pattern</option>
          </select>
        </div>
      </div>

      {/* Quick Severity Pills and Status Row */}
      <div className="search-filter-row">
        <div className="severity-pills-row">
          <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)', fontWeight: 600 }}>Severity:</span>
          {severityOptions.map((sev) => (
            <button
              key={sev}
              className={`severity-pill-btn ${selectedSeverity === sev ? 'active' : ''}`}
              onClick={() => onSeverityChange(sev)}
            >
              {sev}
            </button>
          ))}
        </div>

        <div className="filter-status-text">
          Showing <strong style={{ color: 'var(--text-primary)' }}>{filteredCount}</strong> of {totalItems} groups
          {hasActiveFilters && (
            <>
              {' • '}
              <button className="reset-filter-btn" onClick={onResetFilters}>
                Reset all
              </button>
            </>
          )}
        </div>
      </div>
    </div>
  );
};

export default SearchFilter;
