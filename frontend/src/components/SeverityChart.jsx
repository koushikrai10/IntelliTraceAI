import React from 'react';
import './SeverityChart.css';

/**
 * Visual severity distribution using CSS bars and stacked indicators
 */
const SeverityChart = ({
  fatalCount = 0,
  errorCount = 0,
  warningCount = 0,
  infoCount = 0,
  debugCount = 0,
  totalEntries = 0,
  selectedSeverity = null,
  onSelectSeverity = null,
}) => {
  const sumCount = fatalCount + errorCount + warningCount + infoCount + debugCount;
  const effectiveTotal = sumCount > 0 ? sumCount : (totalEntries || 1);

  const getPercent = (count) => {
    if (!count || effectiveTotal <= 0) return 0;
    return Math.round((count / effectiveTotal) * 100);
  };

  const fatalPct = getPercent(fatalCount);
  const errorPct = getPercent(errorCount);
  const warnPct = getPercent(warningCount);
  const infoPct = getPercent(infoCount);
  const debugPct = getPercent(debugCount);

  const items = [
    { key: 'FATAL', label: 'Fatal', count: fatalCount, pct: fatalPct, colorClass: 'fatal' },
    { key: 'ERROR', label: 'Error', count: errorCount, pct: errorPct, colorClass: 'error' },
    { key: 'WARN', label: 'Warning', count: warningCount, pct: warnPct, colorClass: 'warn' },
    { key: 'INFO', label: 'Info', count: infoCount, pct: infoPct, colorClass: 'info' },
    { key: 'DEBUG', label: 'Debug', count: debugCount, pct: debugPct, colorClass: 'debug' },
  ];

  return (
    <div className="glass-panel severity-chart-card">
      <div className="severity-chart-header">
        <h3 className="severity-chart-title">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="var(--accent-cyan)" strokeWidth="2">
            <line x1="18" y1="20" x2="18" y2="10" />
            <line x1="12" y1="20" x2="12" y2="4" />
            <line x1="6" y1="20" x2="6" y2="14" />
          </svg>
          Severity Distribution
        </h3>
        <span className="total-badge">{effectiveTotal.toLocaleString()} Events</span>
      </div>

      {/* Stacked Proportional Bar */}
      <div className="stacked-severity-bar" title="Proportional distribution of log levels">
        {fatalPct > 0 && (
          <div
            className="bar-segment bar-segment-fatal"
            style={{ width: `${fatalPct}%` }}
            title={`Fatal: ${fatalCount} (${fatalPct}%)`}
          />
        )}
        {errorPct > 0 && (
          <div
            className="bar-segment bar-segment-error"
            style={{ width: `${errorPct}%` }}
            title={`Error: ${errorCount} (${errorPct}%)`}
          />
        )}
        {warnPct > 0 && (
          <div
            className="bar-segment bar-segment-warn"
            style={{ width: `${warnPct}%` }}
            title={`Warning: ${warningCount} (${warnPct}%)`}
          />
        )}
        {infoPct > 0 && (
          <div
            className="bar-segment bar-segment-info"
            style={{ width: `${infoPct}%` }}
            title={`Info: ${infoCount} (${infoPct}%)`}
          />
        )}
        {debugPct > 0 && (
          <div
            className="bar-segment bar-segment-debug"
            style={{ width: `${debugPct}%` }}
            title={`Debug: ${debugCount} (${debugPct}%)`}
          />
        )}
      </div>

      {/* Detailed Breakdown List */}
      <div className="severity-breakdown-grid">
        {items.map((item) => (
          <div
            key={item.key}
            className={`severity-item ${selectedSeverity === item.key ? 'active' : ''}`}
            onClick={() => onSelectSeverity && onSelectSeverity(selectedSeverity === item.key ? null : item.key)}
            role="button"
            tabIndex={0}
            aria-pressed={selectedSeverity === item.key}
            title={onSelectSeverity ? `Filter by ${item.label}` : ''}
          >
            <div className="severity-item-top">
              <div className="severity-label-group">
                <span className={`severity-indicator-dot dot-${item.colorClass}`} />
                <span className="severity-name">{item.label}</span>
              </div>
              <div className="severity-stats">
                <span className="severity-count">{item.count.toLocaleString()}</span>
                <span className="severity-percent">({item.pct}%)</span>
              </div>
            </div>

            <div className="severity-individual-bar-bg">
              <div
                className={`severity-individual-bar-fill fill-${item.colorClass}`}
                style={{ width: `${item.pct}%` }}
              />
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

export default SeverityChart;
