import React, { useState, useMemo } from 'react';
import SeverityChart from './SeverityChart';
import SearchFilter from './SearchFilter';
import ErrorGroups from './ErrorGroups';
import AiExplanation from './AiExplanation';
import { downloadReport, getAiExplanation } from '../services/api';
import './AnalysisResults.css';

/**
 * Full Analysis view with summary metrics, severity breakdown,
 * searchable & filterable error groups, expandable traces, AI explanations, and export.
 */
const AnalysisResults = ({ analysis, onNavigateToUpload }) => {
  // Filter States
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedSeverity, setSelectedSeverity] = useState('ALL');
  const [selectedExceptionType, setSelectedExceptionType] = useState('ALL');
  const [sortBy, setSortBy] = useState('count_desc');

  // Selected group for expansion or AI
  const [selectedGroupId, setSelectedGroupId] = useState(null);

  // AI Diagnostic State
  const [aiData, setAiData] = useState(null);
  const [aiLoading, setAiLoading] = useState(false);
  const [aiError, setAiError] = useState(null);
  const [activeAiGroup, setActiveAiGroup] = useState(null);

  // Download states
  const [downloadingCsv, setDownloadingCsv] = useState(false);
  const [downloadingTxt, setDownloadingTxt] = useState(false);

  // If no analysis is loaded
  if (!analysis) {
    return (
      <div className="glass-panel empty-analysis-screen animate-fade-in">
        <svg className="empty-screen-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
          <polyline points="22 12 18 12 15 21 9 3 6 12 2 12" />
        </svg>
        <h2 style={{ fontSize: '1.4rem', fontWeight: 700, color: '#ffffff' }}>No Active Analysis Found</h2>
        <p style={{ color: 'var(--text-secondary)', fontSize: '0.95rem' }}>
          Please upload a log file or select a previous analysis from history to review diagnostic metrics.
        </p>
        <button className="btn-primary" onClick={onNavigateToUpload}>
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
            <polyline points="17 8 12 3 7 8" />
            <line x1="12" y1="3" x2="12" y2="15" />
          </svg>
          Upload Log File
        </button>
      </div>
    );
  }

  const {
    id,
    originalFilename,
    analysisTimestamp,
    totalEntries = 0,
    errorCount = 0,
    warningCount = 0,
    infoCount = 0,
    debugCount = 0,
    fatalCount = 0,
    summary = '',
    errorGroups = [],
  } = analysis;

  // Extract unique exception types for the filter dropdown
  const availableExceptionTypes = useMemo(() => {
    const types = new Set();
    errorGroups.forEach((g) => {
      if (g.exceptionType && g.exceptionType.trim()) {
        types.add(g.exceptionType.trim());
      }
    });
    return Array.from(types).sort();
  }, [errorGroups]);

  // Filter and sort error groups
  const filteredGroups = useMemo(() => {
    let list = [...errorGroups];

    // Search query filter
    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase();
      list = list.filter((g) => {
        const patternMatch = g.errorPattern && g.errorPattern.toLowerCase().includes(q);
        const sampleMatch = g.sampleMessage && g.sampleMessage.toLowerCase().includes(q);
        const exceptionMatch = g.exceptionType && g.exceptionType.toLowerCase().includes(q);
        return patternMatch || sampleMatch || exceptionMatch;
      });
    }

    // Severity filter
    if (selectedSeverity !== 'ALL') {
      list = list.filter((g) => g.severity?.toUpperCase() === selectedSeverity);
    }

    // Exception type filter
    if (selectedExceptionType !== 'ALL') {
      list = list.filter((g) => g.exceptionType === selectedExceptionType);
    }

    // Sorting
    list.sort((a, b) => {
      if (sortBy === 'count_desc') {
        return (b.occurrenceCount || 0) - (a.occurrenceCount || 0);
      }
      if (sortBy === 'count_asc') {
        return (a.occurrenceCount || 0) - (b.occurrenceCount || 0);
      }
      if (sortBy === 'severity') {
        const weight = { FATAL: 5, ERROR: 4, WARN: 3, INFO: 2, DEBUG: 1 };
        const wA = weight[a.severity?.toUpperCase()] || 0;
        const wB = weight[b.severity?.toUpperCase()] || 0;
        return wB - wA;
      }
      if (sortBy === 'alphabetical') {
        return (a.errorPattern || '').localeCompare(b.errorPattern || '');
      }
      return 0;
    });

    return list;
  }, [errorGroups, searchQuery, selectedSeverity, selectedExceptionType, sortBy]);

  // Trigger AI explanation
  const handleRequestAiExplanation = async (group) => {
    setActiveAiGroup(group);
    setSelectedGroupId(group.id);
    setAiLoading(true);
    setAiError(null);
    setAiData(null);

    // Smooth scroll to AI panel if needed
    window.scrollTo({ top: 380, behavior: 'smooth' });

    try {
      const result = await getAiExplanation({
        errorPattern: group.errorPattern,
        sampleMessage: group.sampleMessage,
        stackTrace: group.sampleMessage || group.errorPattern,
        severity: group.severity,
        exceptionType: group.exceptionType,
      });
      setAiData(result);
    } catch (err) {
      console.error('Failed to get AI explanation:', err);
      const msg = err.response?.data?.message || err.message || 'AI service unavailable.';
      setAiError(msg);
    } finally {
      setAiLoading(false);
    }
  };

  // Download Report Handlers
  const handleDownloadCsv = async () => {
    if (!id) return;
    setDownloadingCsv(true);
    try {
      const baseName = originalFilename ? originalFilename.replace(/\.[^/.]+$/, '') : `analysis-${id}`;
      await downloadReport(id, 'csv', `${baseName}-report.csv`);
    } catch (err) {
      console.error('CSV download error:', err);
      alert('Failed to download CSV report.');
    } finally {
      setDownloadingCsv(false);
    }
  };

  const handleDownloadTxt = async () => {
    if (!id) return;
    setDownloadingTxt(true);
    try {
      const baseName = originalFilename ? originalFilename.replace(/\.[^/.]+$/, '') : `analysis-${id}`;
      await downloadReport(id, 'txt', `${baseName}-report.txt`);
    } catch (err) {
      console.error('TXT download error:', err);
      alert('Failed to download TXT report.');
    } finally {
      setDownloadingTxt(false);
    }
  };

  const formattedDate = analysisTimestamp
    ? new Date(analysisTimestamp).toLocaleString(undefined, {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      })
    : 'Recently';

  return (
    <div className="results-container animate-fade-in">
      {/* Top Header */}
      <div className="results-header">
        <div className="results-meta-group">
          <h1 className="results-file-title">
            <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="var(--accent-cyan)" strokeWidth="2">
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
              <polyline points="14 2 14 8 20 8" />
            </svg>
            <span>{originalFilename || `Analysis #${id}`}</span>
          </h1>
          <div className="results-timestamp">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <circle cx="12" cy="12" r="10" />
              <polyline points="12 6 12 12 16 14" />
            </svg>
            <span>Analyzed on {formattedDate}</span>
            <span>•</span>
            <span style={{ color: 'var(--accent-cyan)', fontWeight: 600 }}>ID #{id}</span>
          </div>
        </div>

        {/* Action Buttons: CSV, TXT, Upload New */}
        <div className="results-header-actions">
          <button
            className="download-btn"
            onClick={handleDownloadCsv}
            disabled={downloadingCsv}
            title="Download structured CSV report"
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
              <polyline points="7 10 12 15 17 10" />
              <line x1="12" y1="15" x2="12" y2="3" />
            </svg>
            {downloadingCsv ? 'Exporting CSV...' : 'Export CSV'}
          </button>

          <button
            className="download-btn"
            onClick={handleDownloadTxt}
            disabled={downloadingTxt}
            title="Download plain-text diagnostic report"
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
              <polyline points="14 2 14 8 20 8" />
              <line x1="16" y1="13" x2="8" y2="13" />
              <line x1="16" y1="17" x2="8" y2="17" />
              <polyline points="10 9 9 9 8 9" />
            </svg>
            {downloadingTxt ? 'Exporting TXT...' : 'Export TXT'}
          </button>

          <button className="btn-primary" onClick={onNavigateToUpload}>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
              <polyline points="17 8 12 3 7 8" />
              <line x1="12" y1="3" x2="12" y2="15" />
            </svg>
            Upload Another Log
          </button>
        </div>
      </div>

      {/* Summary Cards Grid */}
      <div className="summary-cards-grid">
        <div className="glass-panel metric-card total">
          <div className="metric-card-top">
            <span className="metric-label">Total Entries</span>
            <svg className="metric-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
            </svg>
          </div>
          <span className="metric-value">{totalEntries.toLocaleString()}</span>
          <span className="metric-sub">Processed lines</span>
        </div>

        <div className="glass-panel metric-card fatal">
          <div className="metric-card-top">
            <span className="metric-label">Fatal</span>
            <svg className="metric-icon" viewBox="0 0 24 24" fill="none" stroke="var(--color-fatal)" strokeWidth="2">
              <polygon points="12 2 2 22 22 22" />
            </svg>
          </div>
          <span className="metric-value">{fatalCount.toLocaleString()}</span>
          <span className="metric-sub">Critical crashes</span>
        </div>

        <div className="glass-panel metric-card error">
          <div className="metric-card-top">
            <span className="metric-label">Errors</span>
            <svg className="metric-icon" viewBox="0 0 24 24" fill="none" stroke="var(--color-error)" strokeWidth="2">
              <circle cx="12" cy="12" r="10" />
              <line x1="15" y1="9" x2="9" y2="15" />
              <line x1="9" y1="9" x2="15" y2="15" />
            </svg>
          </div>
          <span className="metric-value">{errorCount.toLocaleString()}</span>
          <span className="metric-sub">Failure events</span>
        </div>

        <div className="glass-panel metric-card warn">
          <div className="metric-card-top">
            <span className="metric-label">Warnings</span>
            <svg className="metric-icon" viewBox="0 0 24 24" fill="none" stroke="var(--color-warn)" strokeWidth="2">
              <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z" />
            </svg>
          </div>
          <span className="metric-value">{warningCount.toLocaleString()}</span>
          <span className="metric-sub">Potential hazards</span>
        </div>

        <div className="glass-panel metric-card info">
          <div className="metric-card-top">
            <span className="metric-label">Info</span>
            <svg className="metric-icon" viewBox="0 0 24 24" fill="none" stroke="var(--color-info)" strokeWidth="2">
              <circle cx="12" cy="12" r="10" />
              <line x1="12" y1="16" x2="12" y2="12" />
            </svg>
          </div>
          <span className="metric-value">{infoCount.toLocaleString()}</span>
          <span className="metric-sub">System notices</span>
        </div>

        <div className="glass-panel metric-card debug">
          <div className="metric-card-top">
            <span className="metric-label">Debug</span>
            <svg className="metric-icon" viewBox="0 0 24 24" fill="none" stroke="var(--color-debug)" strokeWidth="2">
              <circle cx="12" cy="12" r="10" />
            </svg>
          </div>
          <span className="metric-value">{debugCount.toLocaleString()}</span>
          <span className="metric-sub">Trace markers</span>
        </div>
      </div>

      {/* Summary Narrative Banner if available */}
      {summary && (
        <div className="analysis-overview-banner animate-fade-in">
          <svg className="overview-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2" />
          </svg>
          <div className="overview-content">
            <h3 className="overview-title">Executive Log Synthesis</h3>
            <p className="overview-text">{summary}</p>
          </div>
        </div>
      )}

      {/* Main Split Layout */}
      <div className="results-split-layout">
        {/* Left Column: Filter Controls and Error Group Cards */}
        <div className="results-left-col">
          {/* AI Panel display at the top of the left col if active */}
          {(aiLoading || aiData || aiError) && (
            <AiExplanation
              loading={aiLoading}
              error={aiError}
              data={aiData}
              targetError={activeAiGroup}
              onRetry={() => activeAiGroup && handleRequestAiExplanation(activeAiGroup)}
              onClose={() => {
                setAiData(null);
                setAiError(null);
                setActiveAiGroup(null);
              }}
            />
          )}

          <SearchFilter
            searchQuery={searchQuery}
            onSearchChange={setSearchQuery}
            selectedSeverity={selectedSeverity}
            onSeverityChange={setSelectedSeverity}
            selectedExceptionType={selectedExceptionType}
            onExceptionTypeChange={setSelectedExceptionType}
            availableExceptionTypes={availableExceptionTypes}
            sortBy={sortBy}
            onSortChange={setSortBy}
            totalItems={errorGroups.length}
            filteredCount={filteredGroups.length}
            onResetFilters={() => {
              setSearchQuery('');
              setSelectedSeverity('ALL');
              setSelectedExceptionType('ALL');
              setSortBy('count_desc');
            }}
          />

          <ErrorGroups
            groups={filteredGroups}
            selectedGroupId={selectedGroupId}
            onSelectGroup={setSelectedGroupId}
            onRequestAiExplanation={handleRequestAiExplanation}
            loadingAiGroupId={aiLoading ? activeAiGroup?.id : null}
          />
        </div>

        {/* Right Column: Severity Chart */}
        <div className="results-right-col">
          <SeverityChart
            fatalCount={fatalCount}
            errorCount={errorCount}
            warningCount={warningCount}
            infoCount={infoCount}
            debugCount={debugCount}
            totalEntries={totalEntries}
            selectedSeverity={selectedSeverity !== 'ALL' ? selectedSeverity : null}
            onSelectSeverity={(sev) => setSelectedSeverity(sev || 'ALL')}
          />
        </div>
      </div>
    </div>
  );
};

export default AnalysisResults;
