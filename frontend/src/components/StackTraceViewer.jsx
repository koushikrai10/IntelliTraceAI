import React, { useState } from 'react';
import './StackTraceViewer.css';

/**
 * Intelligent stack trace parser and interactive viewer
 */
const StackTraceViewer = ({ stackTraceText = '', initialShowAppOnly = false }) => {
  const [showAppOnly, setShowAppOnly] = useState(initialShowAppOnly);
  const [selectedFrameIndex, setSelectedFrameIndex] = useState(null);
  const [copied, setCopied] = useState(false);

  if (!stackTraceText || !stackTraceText.trim()) {
    return (
      <div className="stack-trace-container">
        <div className="empty-stack-placeholder">
          No stack trace attached to this log entry.
        </div>
      </div>
    );
  }

  // Check if a line represents an application frame rather than standard framework code
  const isAppFrame = (line) => {
    const isLibrary =
      line.includes('java.base') ||
      line.includes('sun.reflect') ||
      line.includes('jdk.internal') ||
      line.includes('org.springframework') ||
      line.includes('org.apache') ||
      line.includes('org.hibernate') ||
      line.includes('com.mysql') ||
      line.includes('org.postgresql');

    return !isLibrary;
  };

  const handleCopy = () => {
    navigator.clipboard.writeText(stackTraceText);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  // Parse lines
  const lines = stackTraceText.split('\n');

  // Parse frame parts: "at com.example.service.UserService.findUser(UserService.java:42)"
  const parseFrameLine = (rawLine) => {
    const trimmed = rawLine.trim();
    if (!trimmed.startsWith('at ')) {
      return null;
    }

    const withoutAt = trimmed.substring(3).trim();
    const parenIndex = withoutAt.indexOf('(');

    if (parenIndex === -1) {
      return { classAndMethod: withoutAt, location: '' };
    }

    const classAndMethod = withoutAt.substring(0, parenIndex);
    const location = withoutAt.substring(parenIndex); // includes (File.java:123)

    const lastDot = classAndMethod.lastIndexOf('.');
    if (lastDot === -1) {
      return { classPart: '', methodPart: classAndMethod, location };
    }

    const fullClass = classAndMethod.substring(0, lastDot);
    const methodPart = classAndMethod.substring(lastDot + 1);

    const secondLastDot = fullClass.lastIndexOf('.');
    if (secondLastDot === -1) {
      return { packagePart: '', classPart: fullClass, methodPart, location };
    }

    const packagePart = fullClass.substring(0, secondLastDot + 1);
    const classPart = fullClass.substring(secondLastDot + 1);

    return { packagePart, classPart, methodPart, location };
  };

  return (
    <div className="stack-trace-container">
      {/* Toolbar */}
      <div className="stack-trace-toolbar">
        <div className="stack-trace-title">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <polyline points="4 17 10 11 4 5" />
            <line x1="12" y1="19" x2="20" y2="19" />
          </svg>
          <span>Stack Trace Analysis</span>
        </div>

        <div className="stack-trace-actions">
          <button
            className={`trace-tool-btn ${showAppOnly ? 'active' : ''}`}
            onClick={() => setShowAppOnly(!showAppOnly)}
            aria-pressed={showAppOnly}
            title="Toggle between showing only application frames or all third-party frames"
          >
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
              <polygon points="22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3" />
            </svg>
            <span>{showAppOnly ? 'App Frames Only' : 'All Frames'}</span>
          </button>

          <button
            className="trace-tool-btn"
            onClick={handleCopy}
            title="Copy stack trace to clipboard"
            aria-label="Copy stack trace"
          >
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
              {copied ? (
                <polyline points="20 6 9 17 4 12" />
              ) : (
                <>
                  <rect x="9" y="9" width="13" height="13" rx="2" ry="2" />
                  <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1" />
                </>
              )}
            </svg>
            <span>{copied ? 'Copied!' : 'Copy Trace'}</span>
          </button>
        </div>
      </div>

      {/* Body */}
      <div className="stack-trace-body">
        {lines.map((line, index) => {
          const trimmed = line.trim();
          if (!trimmed) return null;

          const isCausedBy = trimmed.startsWith('Caused by:');
          const isAtFrame = trimmed.startsWith('at ');
          const isApplication = isAtFrame && isAppFrame(trimmed);

          // If filtering to App Frames only, filter out non-app "at " frames
          if (showAppOnly && isAtFrame && !isApplication) {
            return null;
          }

          if (isCausedBy) {
            return (
              <div key={index} className="caused-by-header">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z" />
                  <line x1="12" y1="9" x2="12" y2="13" />
                  <line x1="12" y1="17" x2="12.01" y2="17" />
                </svg>
                <span>{trimmed}</span>
              </div>
            );
          }

          if (!isAtFrame) {
            // Root exception header or message line
            const colonIndex = trimmed.indexOf(':');
            const exName = colonIndex > -1 ? trimmed.substring(0, colonIndex) : trimmed;
            const exMsg = colonIndex > -1 ? trimmed.substring(colonIndex + 1) : '';

            return (
              <div key={index} className="exception-header">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <circle cx="12" cy="12" r="10" />
                  <line x1="15" y1="9" x2="9" y2="15" />
                  <line x1="9" y1="9" x2="15" y2="15" />
                </svg>
                <div>
                  <span className="exception-name">{exName}</span>
                  {exMsg && <span className="exception-msg">: {exMsg}</span>}
                </div>
              </div>
            );
          }

          // Frame line
          const parsed = parseFrameLine(trimmed);
          const isSelected = selectedFrameIndex === index;

          return (
            <div
              key={index}
              className={`stack-frame-row ${isApplication ? 'app-frame' : ''} ${isSelected ? 'selected' : ''}`}
              onClick={() => setSelectedFrameIndex(isSelected ? null : index)}
              title={isApplication ? 'Application code frame' : 'External framework frame'}
            >
              <span className="frame-prefix">at</span>
              {parsed ? (
                <>
                  <span className="frame-package">{parsed.packagePart}</span>
                  <span className="frame-class">{parsed.classPart}.</span>
                  <span className="frame-method">{parsed.methodPart}</span>
                  <span className="frame-location">{parsed.location}</span>
                </>
              ) : (
                <span>{trimmed.substring(3)}</span>
              )}

              {isApplication && <span className="frame-app-badge">App</span>}
            </div>
          );
        })}
      </div>
    </div>
  );
};

export default StackTraceViewer;
