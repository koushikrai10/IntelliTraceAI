import React, { useState } from 'react';
import LoadingSpinner from './LoadingSpinner';
import './AiExplanation.css';

/**
 * AI Diagnostic Explanation and Remediation Panel
 */
const AiExplanation = ({
  loading = false,
  error = null,
  data = null,
  targetError = null,
  onRetry = null,
  onClose = null,
}) => {
  const [copied, setCopied] = useState(false);

  const handleCopyExplanation = () => {
    if (!data) return;
    const textToCopy = `IntelliTrace AI Diagnostic Report
---------------------------------
Error: ${targetError?.errorPattern || 'Unknown'}
Severity Assessment: ${data.severityAssessment || 'N/A'}

Explanation:
${data.explanation || ''}

Root Cause:
${data.rootCause || ''}

Recommended Fixes:
${(data.recommendedFixes || []).map((step, i) => `${i + 1}. ${step}`).join('\n')}
`;
    navigator.clipboard.writeText(textToCopy);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  if (loading) {
    return (
      <div className="glass-panel ai-panel animate-fade-in" aria-busy="true">
        <LoadingSpinner
          size="lg"
          message="Analyzing error patterns, deducing root cause, and crafting remediation steps with AI..."
        />
      </div>
    );
  }

  if (error) {
    return (
      <div className="glass-panel ai-panel animate-fade-in">
        <div className="ai-error-box">
          <svg className="ai-error-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <circle cx="12" cy="12" r="10" />
            <line x1="12" y1="8" x2="12" y2="12" />
            <line x1="12" y1="16" x2="12.01" y2="16" />
          </svg>
          <h4 className="ai-error-title">AI Diagnostics Unavailable</h4>
          <p className="ai-error-desc">
            {error || 'Unable to connect to the IntelliTrace AI backend service. Please ensure the backend is running at http://localhost:8080.'}
          </p>
          <div style={{ display: 'flex', gap: '10px', marginTop: '10px' }}>
            {onRetry && (
              <button className="btn-primary" onClick={onRetry}>
                Try Again
              </button>
            )}
            {onClose && (
              <button className="btn-secondary" onClick={onClose}>
                Dismiss
              </button>
            )}
          </div>
        </div>
      </div>
    );
  }

  if (!data) {
    return null;
  }

  const renderDiagnosticSourceBadge = () => {
    const source = data.diagnosticSource;
    if (!source) return null;

    if (source === 'GEMINI') {
      return (
        <span
          className="ai-source-badge gemini"
          title="Generated via live Google Gemini AI API"
        >
          <span className="source-dot-pulse gemini" />
          <span>✦ Gemini 3.8-Flash Live</span>
        </span>
      );
    }

    if (source === 'HEURISTIC_FALLBACK') {
      return (
        <span
          className="ai-source-badge fallback"
          title="External Gemini API timed out or unavailable. Switched to heuristic engine."
        >
          <span className="source-dot-pulse fallback" />
          <span>⚠️ Heuristic Fallback</span>
        </span>
      );
    }

    // Default HEURISTIC or other
    return (
      <span
        className="ai-source-badge heuristic"
        title="Generated via deterministic rule-based heuristic engine"
      >
        <span className="source-dot-pulse heuristic" />
        <span>⚙ Heuristic Engine</span>
      </span>
    );
  };

  return (
    <div
      className="glass-panel ai-panel animate-fade-in"
      role="region"
      aria-label="AI Diagnostic Analysis"
    >
      {/* Header */}
      <div className="ai-header">
        <div className="ai-title-group">
          <div className="ai-sparkle-icon" aria-hidden="true">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#ffffff" strokeWidth="2.5">
              <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2" />
            </svg>
          </div>
          <div>
            <h3 className="ai-title">AI Diagnostic Analysis</h3>
            <p className="ai-subtitle">
              Automated root cause deduction & remediation guidance
            </p>
          </div>
        </div>

        <div className="ai-header-controls">
          {renderDiagnosticSourceBadge()}

          {data.severityAssessment && (
            <span className="ai-assessment-badge" title="AI Severity Assessment">
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
                <circle cx="12" cy="12" r="10" />
                <polyline points="12 6 12 12 14 14" />
              </svg>
              <span>{data.severityAssessment}</span>
            </span>
          )}

          <button
            className="btn-secondary copy-report-btn"
            onClick={handleCopyExplanation}
            title="Copy diagnostic report to clipboard"
          >
            {copied ? (
              <>
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="var(--color-success)" strokeWidth="2.5">
                  <polyline points="20 6 9 17 4 12" />
                </svg>
                <span>Copied!</span>
              </>
            ) : (
              <>
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <rect x="9" y="9" width="13" height="13" rx="2" ry="2" />
                  <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1" />
                </svg>
                <span>Copy Summary</span>
              </>
            )}
          </button>

          {onClose && (
            <button
              className="btn-secondary close-ai-btn"
              onClick={onClose}
              title="Close AI Panel"
              aria-label="Close AI Diagnostic Panel"
            >
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <line x1="18" y1="6" x2="6" y2="18" />
                <line x1="6" y1="6" x2="18" y2="18" />
              </svg>
            </button>
          )}
        </div>
      </div>

      {/* Fallback Notice when applicable */}
      {data.diagnosticSource === 'HEURISTIC_FALLBACK' && (
        <div className="ai-fallback-notice animate-fade-in" role="note">
          <svg className="fallback-notice-icon" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z" />
            <line x1="12" y1="9" x2="12" y2="13" />
            <line x1="12" y1="17" x2="12.01" y2="17" />
          </svg>
          <div className="fallback-notice-text">
            <strong>Heuristic Fallback Active:</strong> Live Gemini AI request could not be completed (e.g. rate limit, network timeout, or offline backend). The diagnosis below was generated using IntelliTrace AI's built-in heuristic rule engine.
          </div>
        </div>
      )}

      {/* 1. Explanation */}
      {data.explanation && (
        <div className="ai-section overview-section">
          <h4 className="ai-section-title">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
              <circle cx="12" cy="12" r="10" />
              <line x1="12" y1="16" x2="12" y2="12" />
              <line x1="12" y1="8" x2="12.01" y2="8" />
            </svg>
            Diagnostic Overview
          </h4>
          <p className="ai-text">{data.explanation}</p>
        </div>
      )}

      {/* 2. Root Cause Deduction */}
      {data.rootCause && (
        <div className="ai-section root-cause-section">
          <h4 className="ai-section-title root-cause">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
              <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z" />
              <line x1="12" y1="9" x2="12" y2="13" />
              <line x1="12" y1="17" x2="12.01" y2="17" />
            </svg>
            Root Cause Identification
          </h4>
          <p className="ai-text root-cause-text">{data.rootCause}</p>
        </div>
      )}

      {/* 3. Remediation Steps */}
      {data.recommendedFixes && data.recommendedFixes.length > 0 && (
        <div className="ai-section steps-section">
          <h4 className="ai-section-title steps">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
              <polyline points="9 11 12 14 22 4" />
              <path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11" />
            </svg>
            Recommended Action Steps
          </h4>
          <ol className="ai-steps-list">
            {data.recommendedFixes.map((step, index) => (
              <li key={index} className="ai-step-item">
                <span className="step-number" aria-hidden="true">{index + 1}</span>
                <span className="step-content">{step}</span>
              </li>
            ))}
          </ol>
        </div>
      )}

      {/* 4. Target Error Context / Evidence */}
      {targetError && (
        <div className="ai-section evidence-section">
          <h4 className="ai-section-title evidence">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
              <polyline points="16 18 22 12 16 6" />
              <polyline points="8 6 2 12 8 18" />
            </svg>
            Error Context & Evidence
          </h4>
          <div className="ai-code-evidence">
            {targetError.sampleMessage || targetError.errorPattern}
          </div>
        </div>
      )}

      {/* 5. Disclaimer */}
      <div className="ai-disclaimer" role="note">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
          <circle cx="12" cy="12" r="10" />
          <line x1="12" y1="8" x2="12" y2="12" />
          <line x1="12" y1="16" x2="12.01" y2="16" />
        </svg>
        <span>
          <strong>Engineering Advisory:</strong> Automated diagnostic inferences should be verified in a staging environment prior to applying remediation in production.
        </span>
      </div>
    </div>
  );
};

export default AiExplanation;
