import React, { useState, useMemo } from 'react';
import LoadingSpinner from './LoadingSpinner';
import { deleteAnalysis, downloadReport } from '../services/api';
import './AnalysisHistory.css';

/**
 * Historical list of log analyses with metadata, quick export, and deletion.
 */
const AnalysisHistory = ({
  analyses = [],
  loading = false,
  onRefresh,
  onSelectAnalysis,
  onNavigateToUpload,
}) => {
  const [searchQuery, setSearchQuery] = useState('');
  const [deleteModalId, setDeleteModalId] = useState(null);
  const [isDeleting, setIsDeleting] = useState(false);
  const [downloadingId, setDownloadingId] = useState(null);

  // Filter analyses by filename or date
  const filteredAnalyses = useMemo(() => {
    if (!searchQuery.trim()) return analyses;
    const q = searchQuery.toLowerCase();
    return analyses.filter((item) => {
      const nameMatch = item.originalFilename && item.originalFilename.toLowerCase().includes(q);
      const idMatch = String(item.id).includes(q);
      return nameMatch || idMatch;
    });
  }, [analyses, searchQuery]);

  const handleDeleteConfirm = async () => {
    if (!deleteModalId) return;
    setIsDeleting(true);
    try {
      await deleteAnalysis(deleteModalId);
      setDeleteModalId(null);
      if (onRefresh) {
        await onRefresh();
      }
    } catch (err) {
      console.error('Failed to delete analysis:', err);
      alert('Could not delete analysis. Backend server may be offline.');
    } finally {
      setIsDeleting(false);
    }
  };

  const handleQuickDownload = async (e, id, filename, format) => {
    e.stopPropagation();
    setDownloadingId(id);
    try {
      const baseName = filename ? filename.replace(/\.[^/.]+$/, '') : `analysis-${id}`;
      await downloadReport(id, format, `${baseName}-report.${format}`);
    } catch (err) {
      console.error(`Failed to download ${format}:`, err);
      alert(`Could not download ${format.toUpperCase()} report.`);
    } finally {
      setDownloadingId(null);
    }
  };

  const formatDate = (dateStr) => {
    if (!dateStr) return 'N/A';
    try {
      const date = new Date(dateStr);
      return date.toLocaleDateString(undefined, {
        month: 'short',
        day: 'numeric',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      });
    } catch {
      return dateStr;
    }
  };

  if (loading) {
    return (
      <div className="history-container">
        <LoadingSpinner size="lg" message="Loading historical analyses..." />
      </div>
    );
  }

  return (
    <div className="history-container animate-fade-in">
      {/* Header */}
      <div className="history-header">
        <div className="history-title-group">
          <h1 className="history-title">
            Analysis <span className="gradient-text">History</span>
          </h1>
          <p className="history-subtitle">
            Archive of past log sessions, error metrics, and diagnostic reports.
          </p>
        </div>

        <button className="btn-primary" onClick={onNavigateToUpload}>
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
            <polyline points="17 8 12 3 7 8" />
            <line x1="12" y1="3" x2="12" y2="15" />
          </svg>
          Upload New Log
        </button>
      </div>

      {/* Toolbar / Search */}
      {analyses.length > 0 && (
        <div className="history-toolbar">
          <div className="search-input-wrapper history-search-input-wrapper">
            <svg className="search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <circle cx="11" cy="11" r="8" />
              <line x1="21" y1="21" x2="16.65" y2="16.65" />
            </svg>
            <input
              type="text"
              className="search-input"
              placeholder="Search history by log file name or ID..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
          </div>

          <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
            Showing {filteredAnalyses.length} of {analyses.length} records
          </span>
        </div>
      )}

      {/* Table of Analyses */}
      {filteredAnalyses.length > 0 ? (
        <div className="glass-panel history-table-card">
          <table className="history-table">
            <thead>
              <tr>
                <th>Log File</th>
                <th>Timestamp</th>
                <th>Entries</th>
                <th>Error Breakdown</th>
                <th style={{ textAlign: 'right' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filteredAnalyses.map((item) => (
                <tr
                  key={item.id}
                  className="history-row"
                  onClick={() => onSelectAnalysis(item.id)}
                  title="Click to view full analysis results"
                >
                  <td>
                    <div className="history-file-cell">
                      <div className="file-symbol-icon">
                        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                          <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
                          <polyline points="14 2 14 8 20 8" />
                        </svg>
                      </div>
                      <div>
                        <span className="history-file-name">{item.originalFilename}</span>
                        <span className="history-file-id">Analysis ID: #{item.id}</span>
                      </div>
                    </div>
                  </td>

                  <td className="history-date-cell">
                    {formatDate(item.analysisTimestamp)}
                  </td>

                  <td style={{ fontFamily: 'var(--font-mono)', fontWeight: 600 }}>
                    {(item.totalEntries || 0).toLocaleString()}
                  </td>

                  <td>
                    <div className="history-counts-group">
                      {item.fatalCount > 0 && (
                        <span className="badge badge-fatal">{item.fatalCount} fatal</span>
                      )}
                      <span className="badge badge-error">
                        {item.errorCount || 0} errors
                      </span>
                      {item.warningCount > 0 && (
                        <span className="badge badge-warn">
                          {item.warningCount} warn
                        </span>
                      )}
                    </div>
                  </td>

                  <td>
                    <div className="history-actions-cell">
                      <button
                        className="action-icon-btn"
                        onClick={(e) => handleQuickDownload(e, item.id, item.originalFilename, 'csv')}
                        disabled={downloadingId === item.id}
                        title="Download CSV"
                        aria-label="Download CSV"
                      >
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                          <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
                          <polyline points="7 10 12 15 17 10" />
                          <line x1="12" y1="15" x2="12" y2="3" />
                        </svg>
                      </button>

                      <button
                        className="action-icon-btn danger"
                        onClick={(e) => {
                          e.stopPropagation();
                          setDeleteModalId(item.id);
                        }}
                        title="Delete analysis record"
                        aria-label="Delete"
                      >
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                          <polyline points="3 6 5 6 21 6" />
                          <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
                        </svg>
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : (
        /* Empty State */
        <div className="glass-panel history-empty-panel">
          <svg className="history-empty-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
            <circle cx="12" cy="12" r="10" />
            <polyline points="12 6 12 12 16 14" />
          </svg>
          <h3 style={{ fontSize: '1.25rem', fontWeight: 700, color: '#ffffff' }}>No Analyses in History</h3>
          <p style={{ color: 'var(--text-secondary)', maxWidth: '420px', fontSize: '0.92rem' }}>
            {searchQuery
              ? 'No historical logs match your search term.'
              : 'You have not uploaded any logs for analysis yet. Start by uploading a log file.'}
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
      )}

      {/* Delete Confirmation Modal */}
      {deleteModalId && (
        <div className="modal-overlay" onClick={() => !isDeleting && setDeleteModalId(null)}>
          <div className="modal-content animate-fade-in" onClick={(e) => e.stopPropagation()}>
            <h3 className="modal-title">
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="var(--color-error)" strokeWidth="2">
                <circle cx="12" cy="12" r="10" />
                <line x1="15" y1="9" x2="9" y2="15" />
                <line x1="9" y1="9" x2="15" y2="15" />
              </svg>
              Confirm Deletion
            </h3>
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.92rem', lineHeight: 1.5 }}>
              Are you sure you want to permanently delete Analysis record #{deleteModalId}?
              All associated error cluster records and metric history will be removed.
            </p>
            <div className="modal-actions">
              <button
                className="btn-secondary"
                onClick={() => setDeleteModalId(null)}
                disabled={isDeleting}
              >
                Cancel
              </button>
              <button
                className="btn-danger"
                onClick={handleDeleteConfirm}
                disabled={isDeleting}
              >
                {isDeleting ? 'Deleting...' : 'Delete Permanently'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default AnalysisHistory;
