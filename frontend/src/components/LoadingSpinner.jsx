import React from 'react';
import './LoadingSpinner.css';

/**
 * Reusable Loading Spinner component
 * @param {Object} props
 * @param {'sm'|'md'|'lg'} [props.size='md']
 * @param {string} [props.message]
 * @param {string} [props.className]
 */
const LoadingSpinner = ({ size = 'md', message = '', className = '' }) => {
  return (
    <div className={`spinner-container spinner-${size} ${className}`} role="status" aria-live="polite">
      <div className="spinner-wrapper">
        <div className="spinner-ring" />
        <div className="spinner-pulse-center" />
      </div>
      {message && <p className="spinner-text">{message}</p>}
      <span className="sr-only" style={{ position: 'absolute', width: 1, height: 1, overflow: 'hidden', clip: 'rect(0,0,0,0)' }}>
        Loading...
      </span>
    </div>
  );
};

export default LoadingSpinner;
