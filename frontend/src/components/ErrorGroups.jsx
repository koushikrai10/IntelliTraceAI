import React, { useState } from 'react';
import StackTraceViewer from './StackTraceViewer';
import './ErrorGroups.css';

/**
 * List of categorized Error Group cards with occurrence counts,
 * severity badges, expandable stack trace details, and AI explanation triggers.
 */
const ErrorGroups = ({
  groups = [],
  selectedGroupId = null,
  onSelectGroup = null,
  onRequestAiExplanation = null,
  loadingAiGroupId = null,
}) => {
  const [internalExpandedId, setInternalExpandedId] = useState(null);

  const activeId = selectedGroupId !== null ? selectedGroupId : internalExpandedId;

  const handleToggleCard = (groupId, e) => {
    // If clicking a button inside the card, don't toggle
    if (e.target.closest('button') || e.target.closest('a') || e.target.closest('.stack-frame-row')) {
      return;
    }

    const nextId = activeId === groupId ? null : groupId;
    if (onSelectGroup) {
      onSelectGroup(nextId);
    } else {
      setInternalExpandedId(nextId);
    }
  };

  const renderSeverityBadge = (severity) => {
    const sev = severity?.toUpperCase() || 'ERROR';
    let badgeClass = 'badge-error';
    let icon = (
      <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" aria-hidden="true">
        <circle cx="12" cy="12" r="10" />
        <line x1="15" y1="9" x2="9" y2="15" />
        <line x1="9" y1="9" x2="15" y2="15" />
      </svg>
    );

    if (sev === 'FATAL') {
      badgeClass = 'badge-fatal';
      icon = (
        <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" aria-hidden="true">
          <polygon points="12 2 2 22 22 22" />
        </svg>
      );
    } else if (sev === 'WARN' || sev === 'WARNING') {
      badgeClass = 'badge-warn';
      icon = (
        <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" aria-hidden="true">
          <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z" />
          <line x1="12" y1="9" x2="12" y2="13" />
        </svg>
      );
    } else if (sev === 'INFO') {
      badgeClass = 'badge-info';
      icon = (
        <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" aria-hidden="true">
          <circle cx="12" cy="12" r="10" />
          <line x1="12" y1="16" x2="12" y2="12" />
          <line x1="12" y1="8" x2="12.01" y2="8" />
        </svg>
      );
    } else if (sev === 'DEBUG') {
      badgeClass = 'badge-debug';
      icon = (
        <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" aria-hidden="true">
          <circle cx="12" cy="12" r="10" />
        </svg>
      );
    }

    return (
      <span className={`badge ${badgeClass}`} title={`Severity: ${sev}`}>
        {icon}
        <span>{sev}</span>
      </span>
    );
  };

  const getSeverityBorderClass = (severity) => {
    switch (severity?.toUpperCase()) {
      case 'FATAL':
        return 'sev-fatal';
      case 'ERROR':
        return 'sev-error';
      case 'WARN':
      case 'WARNING':
        return 'sev-warn';
      case 'INFO':
        return 'sev-info';
      case 'DEBUG':
        return 'sev-debug';
      default:
        return '';
    }
  };

  if (!groups || groups.length === 0) {
    return (
      <div className="empty-groups-state" role="status">
        <svg className="empty-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" aria-hidden="true">
          <circle cx="12" cy="12" r="10" />
          <path d="M8 12h8" />
        </svg>
        <h4 className="empty-title">No Error Groups Found</h4>
        <p className="empty-subtitle">
          No error patterns match your current filter criteria or the uploaded log contains no recognized errors.
        </p>
      </div>
    );
  }

  return (
    <div className="error-groups-list" role="list">
      {groups.map((group, index) => {
        const isExpanded = activeId === (group.id || index);
        const cardId = group.id || index;
        const isLoadingAi = loadingAiGroupId === cardId;

        // Extract stack trace text: either explicit sampleMessage or errorPattern
        const stackTraceContent = group.sampleMessage || group.errorPattern || '';

        return (
          <div
            key={cardId}
            className={`error-group-card ${getSeverityBorderClass(group.severity)} ${isExpanded ? 'expanded' : ''}`}
            onClick={(e) => handleToggleCard(cardId, e)}
            onKeyDown={(e) => {
              if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault();
                handleToggleCard(cardId, e);
              }
            }}
            tabIndex={0}
            role="button"
            aria-expanded={isExpanded}
            aria-label={`Error Group: ${group.errorPattern}, ${group.occurrenceCount} occurrences. Press enter to toggle details.`}
          >
            {/* Top row */}
            <div className="card-top-row">
              <div className="card-title-group">
                <div className="card-meta-badges">
                  {renderSeverityBadge(group.severity)}

                  {group.exceptionType && (
                    <span className="exception-pill" title="Exception Type">
                      {group.exceptionType}
                    </span>
                  )}

                  {group.firstOccurrence && (
                    <span className="first-seen-text">
                      <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
                        <circle cx="12" cy="12" r="10" />
                        <polyline points="12 6 12 12 16 14" />
                      </svg>
                      <span>{group.firstOccurrence}</span>
                    </span>
                  )}
                </div>

                <h3 className="error-pattern-heading">{group.errorPattern}</h3>
              </div>

              {/* Stats & Chevron */}
              <div className="card-stats-group">
                <div className="count-bubble">
                  <span className="count-number">{group.occurrenceCount.toLocaleString()}</span>
                  <span className="count-label">Occurrences</span>
                </div>

                {group.percentage !== undefined && group.percentage > 0 && (
                  <span className="percentage-chip" title="Percentage of total errors">
                    {group.percentage.toFixed(1)}%
                  </span>
                )}

                <svg
                  className="expand-chevron"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="2.5"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  aria-hidden="true"
                >
                  <polyline points="6 9 12 15 18 9" />
                </svg>
              </div>
            </div>

            {/* Sample message preview if collapsed */}
            {!isExpanded && group.sampleMessage && (
              <div className="sample-message-preview">
                {group.sampleMessage.slice(0, 220)}
                {group.sampleMessage.length > 220 ? '...' : ''}
              </div>
            )}

            {/* Expanded Detailed View */}
            {isExpanded && (
              <div className="card-expanded-content">
                {/* Actions Bar */}
                <div className="card-actions-bar">
                  <span style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', fontWeight: 600 }}>
                    Detailed Stack Trace & Diagnostic Context
                  </span>

                  {onRequestAiExplanation && (
                    <button
                      className="btn-ai"
                      disabled={isLoadingAi}
                      onClick={() => onRequestAiExplanation(group)}
                    >
                      <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                        <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2" />
                      </svg>
                      {isLoadingAi ? 'Analyzing with AI...' : 'Explain with AI'}
                    </button>
                  )}
                </div>

                {/* Stack Trace Viewer */}
                <StackTraceViewer stackTraceText={stackTraceContent} />
              </div>
            )}
          </div>
        );
      })}
    </div>
  );
};

export default ErrorGroups;
