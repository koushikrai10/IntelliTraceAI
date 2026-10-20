import React, { useMemo } from 'react';
import SeverityChart from './SeverityChart';
import './Dashboard.css';

/**
 * Main IntelliTrace AI Dashboard with aggregate statistics,
 * severity breakdown, top frequent error patterns, and quick navigation.
 */
const Dashboard = ({
  analyses = [],
  activeAnalysis = null,
  onNavigate,
  onSelectAnalysis,
}) => {
  // Aggregate stats across all analyses or latest analysis
  const aggregateStats = useMemo(() => {
    if (!analyses || analyses.length === 0) {
      if (activeAnalysis) {
        return {
          totalEntries: activeAnalysis.totalEntries || 0,
          errorCount: activeAnalysis.errorCount || 0,
          warningCount: activeAnalysis.warningCount || 0,
          infoCount: activeAnalysis.infoCount || 0,
          debugCount: activeAnalysis.debugCount || 0,
          fatalCount: activeAnalysis.fatalCount || 0,
          totalAnalyses: 1,
        };
      }
      return {
        totalEntries: 0,
        errorCount: 0,
        warningCount: 0,
        infoCount: 0,
        debugCount: 0,
        fatalCount: 0,
        totalAnalyses: 0,
      };
    }

    return analyses.reduce(
      (acc, item) => {
        acc.totalEntries += item.totalEntries || 0;
        acc.errorCount += item.errorCount || 0;
        acc.warningCount += item.warningCount || 0;
        acc.infoCount += item.infoCount || 0;
        acc.debugCount += item.debugCount || 0;
        acc.fatalCount += item.fatalCount || 0;
        acc.totalAnalyses += 1;
        return acc;
      },
      {
        totalEntries: 0,
        errorCount: 0,
        warningCount: 0,
        infoCount: 0,
        debugCount: 0,
        fatalCount: 0,
        totalAnalyses: 0,
      }
    );
  }, [analyses, activeAnalysis]);

  // Extract top frequent error groups
  const topFrequentErrors = useMemo(() => {
    // If active analysis has error groups, prioritize it; else collect from all analyses
    let groups = [];
    if (activeAnalysis?.errorGroups?.length) {
      groups = [...activeAnalysis.errorGroups];
    } else {
      analyses.forEach((a) => {
        if (a.errorGroups) {
          groups.push(...a.errorGroups);
        }
      });
    }

    // Sort descending by occurrence count
    groups.sort((a, b) => (b.occurrenceCount || 0) - (a.occurrenceCount || 0));
    return groups.slice(0, 5);
  }, [analyses, activeAnalysis]);

  const hasData = aggregateStats.totalAnalyses > 0 || !!activeAnalysis;

  return (
    <div className="dashboard-container animate-fade-in">
      {/* Hero Welcome Banner */}
      <section className="dashboard-hero">
        <div className="hero-text-content">
          <div className="hero-pill">
            <span className="status-dot" />
            <span>AI Diagnostic Engine v1.0</span>
          </div>
          <h1 className="hero-title">
            Intelligent Log Analysis & <span className="gradient-text">Troubleshooting</span>
          </h1>
          <p className="hero-description">
            Accelerate MTTR with automated root cause deduction, error clustering,
            and remediation recommendations for cloud, microservice, and enterprise systems.
          </p>

          <div className="hero-actions">
            <button className="btn-primary" onClick={() => onNavigate('upload')}>
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
                <polyline points="17 8 12 3 7 8" />
                <line x1="12" y1="3" x2="12" y2="15" />
              </svg>
              Upload New Log
            </button>

            <button className="btn-secondary" onClick={() => onNavigate('history')}>
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <circle cx="12" cy="12" r="10" />
                <polyline points="12 6 12 12 16 14" />
              </svg>
              View History ({analyses.length})
            </button>
          </div>
        </div>
      </section>

      {/* Aggregate Stats Cards */}
      <section className="dashboard-metrics-grid">
        <div className="glass-panel metric-card total">
          <div className="metric-card-top">
            <span className="metric-label">Total Entries</span>
            <svg className="metric-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
            </svg>
          </div>
          <span className="metric-value">{aggregateStats.totalEntries.toLocaleString()}</span>
          <span className="metric-sub">Across {aggregateStats.totalAnalyses} sessions</span>
        </div>

        <div className="glass-panel metric-card fatal">
          <div className="metric-card-top">
            <span className="metric-label">Fatal Crashes</span>
            <svg className="metric-icon" viewBox="0 0 24 24" fill="none" stroke="var(--color-fatal)" strokeWidth="2">
              <polygon points="12 2 2 22 22 22" />
            </svg>
          </div>
          <span className="metric-value">{aggregateStats.fatalCount.toLocaleString()}</span>
          <span className="metric-sub">Critical disruptions</span>
        </div>

        <div className="glass-panel metric-card error">
          <div className="metric-card-top">
            <span className="metric-label">Total Errors</span>
            <svg className="metric-icon" viewBox="0 0 24 24" fill="none" stroke="var(--color-error)" strokeWidth="2">
              <circle cx="12" cy="12" r="10" />
              <line x1="15" y1="9" x2="9" y2="15" />
              <line x1="9" y1="9" x2="15" y2="15" />
            </svg>
          </div>
          <span className="metric-value">{aggregateStats.errorCount.toLocaleString()}</span>
          <span className="metric-sub">Action required</span>
        </div>

        <div className="glass-panel metric-card warn">
          <div className="metric-card-top">
            <span className="metric-label">Warnings</span>
            <svg className="metric-icon" viewBox="0 0 24 24" fill="none" stroke="var(--color-warn)" strokeWidth="2">
              <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z" />
            </svg>
          </div>
          <span className="metric-value">{aggregateStats.warningCount.toLocaleString()}</span>
          <span className="metric-sub">Potential hazards</span>
        </div>

        <div className="glass-panel metric-card info">
          <div className="metric-card-top">
            <span className="metric-label">Informational</span>
            <svg className="metric-icon" viewBox="0 0 24 24" fill="none" stroke="var(--color-info)" strokeWidth="2">
              <circle cx="12" cy="12" r="10" />
              <line x1="12" y1="16" x2="12" y2="12" />
            </svg>
          </div>
          <span className="metric-value">{aggregateStats.infoCount.toLocaleString()}</span>
          <span className="metric-sub">Routine telemetry</span>
        </div>
      </section>

      {/* Main Split: Left (Top Frequent Errors), Right (Severity Distribution + Recent Analyses) */}
      {hasData ? (
        <div className="dashboard-main-grid">
          {/* Left Column: Top Frequent Error Groups */}
          <div className="glass-panel frequent-errors-card">
            <div className="dashboard-panel-header">
              <h3 className="dashboard-panel-title">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="var(--color-error)" strokeWidth="2">
                  <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z" />
                </svg>
                Most Frequent Error Clusters
              </h3>
              {activeAnalysis && (
                <button
                  className="btn-secondary"
                  style={{ fontSize: '0.8rem', padding: '6px 12px' }}
                  onClick={() => onNavigate('results')}
                >
                  View Details &rarr;
                </button>
              )}
            </div>

            {topFrequentErrors.length > 0 ? (
              topFrequentErrors.map((group, idx) => (
                <div
                  key={group.id || idx}
                  className="frequent-error-row"
                  tabIndex={0}
                  role="button"
                  aria-label={`Error group: ${group.errorPattern}, ${group.occurrenceCount} occurrences. Press Enter to view.`}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter' || e.key === ' ') {
                      e.preventDefault();
                      if (onSelectAnalysis && group.analysisRecordId) {
                        onSelectAnalysis(group.analysisRecordId);
                      } else if (onNavigate) {
                        onNavigate('results');
                      }
                    }
                  }}
                  onClick={() => {
                    if (onSelectAnalysis && group.analysisRecordId) {
                      onSelectAnalysis(group.analysisRecordId);
                    } else if (onNavigate) {
                      onNavigate('results');
                    }
                  }}
                >
                  <div className="frequent-error-info">
                    <span className="frequent-error-pattern" title={group.errorPattern}>
                      {group.errorPattern}
                    </span>
                    <div className="frequent-error-meta">
                      <span className={`badge ${group.severity === 'FATAL' ? 'badge-fatal' : 'badge-error'}`}>
                        {group.severity === 'FATAL' ? '🛑 FATAL' : '⚠️ ' + (group.severity || 'ERROR')}
                      </span>
                      {group.exceptionType && <span className="frequent-exception-text">• {group.exceptionType}</span>}
                    </div>
                  </div>

                  <div className="frequent-error-count">
                    <span>{group.occurrenceCount}</span>
                    <span style={{ fontSize: '0.7rem', color: 'var(--text-muted)', display: 'block' }}>times</span>
                  </div>
                </div>
              ))
            ) : (
              <p style={{ color: 'var(--text-muted)', textAlign: 'center', padding: '30px 0' }}>
                No error groups recorded yet.
              </p>
            )}
          </div>

          {/* Right Column: Severity Chart + Recent Analyses */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
            <SeverityChart
              fatalCount={aggregateStats.fatalCount}
              errorCount={aggregateStats.errorCount}
              warningCount={aggregateStats.warningCount}
              infoCount={aggregateStats.infoCount}
              debugCount={aggregateStats.debugCount}
              totalEntries={aggregateStats.totalEntries}
            />

            {analyses.length > 0 && (
              <div className="glass-panel recent-analyses-card">
                <div className="dashboard-panel-header">
                  <h3 className="dashboard-panel-title" style={{ fontSize: '1rem' }}>
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="var(--accent-cyan)" strokeWidth="2">
                      <circle cx="12" cy="12" r="10" />
                      <polyline points="12 6 12 12 16 14" />
                    </svg>
                    Recent Analyses
                  </h3>
                  <button
                    className="btn-secondary"
                    style={{ fontSize: '0.75rem', padding: '4px 8px' }}
                    onClick={() => onNavigate('history')}
                  >
                    All ({analyses.length})
                  </button>
                </div>

                {analyses.slice(0, 4).map((item) => (
                  <div
                    key={item.id}
                    className="recent-analysis-item"
                    tabIndex={0}
                    role="button"
                    aria-label={`Analysis ${item.originalFilename}, ${item.errorCount || 0} errors. Press Enter to view.`}
                    onKeyDown={(e) => {
                      if (e.key === 'Enter' || e.key === ' ') {
                        e.preventDefault();
                        onSelectAnalysis(item.id);
                      }
                    }}
                    onClick={() => onSelectAnalysis(item.id)}
                  >
                    <div>
                      <div className="recent-item-title">{item.originalFilename}</div>
                      <div className="recent-item-date">
                        {item.analysisTimestamp ? new Date(item.analysisTimestamp).toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' }) : 'Recent'}
                      </div>
                    </div>
                    <div style={{ textAlign: 'right' }}>
                      <span className="badge badge-error" style={{ fontSize: '0.72rem' }}>
                        ⚠️ {item.errorCount || 0} errors
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      ) : (
        /* Empty State */
        <div className="glass-panel dashboard-empty-card">
          <svg className="empty-dashboard-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
            <polyline points="22 12 18 12 15 21 9 3 6 12 2 12" />
          </svg>
          <h3 style={{ fontSize: '1.25rem', fontWeight: 700, color: '#ffffff' }}>No Log Analyses Yet</h3>
          <p style={{ color: 'var(--text-secondary)', maxWidth: '440px', fontSize: '0.92rem' }}>
            Upload a server log or sample file to view comprehensive error analytics, stack trace clustering, and AI diagnostics.
          </p>
          <button className="btn-primary" onClick={() => onNavigate('upload')}>
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
              <polyline points="17 8 12 3 7 8" />
              <line x1="12" y1="3" x2="12" y2="15" />
            </svg>
            Upload Your First Log
          </button>
        </div>
      )}
    </div>
  );
};

export default Dashboard;
