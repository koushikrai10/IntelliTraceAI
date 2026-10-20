import React, { useState } from 'react';
import './Navbar.css';

/**
 * Top navigation bar with logo, navigation links, and system status
 */
const Navbar = ({ currentView, onNavigate, activeAnalysis }) => {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  const handleNavClick = (view) => {
    onNavigate(view);
    setMobileMenuOpen(false);
  };

  return (
    <nav className="navbar" role="navigation" aria-label="Main Navigation">
      <div className="navbar-container">
        {/* Brand */}
        <div className="navbar-brand" onClick={() => handleNavClick('dashboard')}>
          <div className="brand-icon-wrapper">
            <svg
              className="brand-svg"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2.2"
              strokeLinecap="round"
              strokeLinejoin="round"
            >
              <polygon points="12 2 2 7 12 12 22 7 12 2" />
              <polyline points="2 17 12 22 22 17" />
              <polyline points="2 12 12 17 22 12" />
            </svg>
          </div>
          <span className="brand-title">
            IntelliTrace <span className="brand-ai-badge">AI</span>
          </span>
        </div>

        {/* Mobile toggle */}
        <button
          className="mobile-menu-toggle"
          onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
          aria-label="Toggle navigation menu"
        >
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            {mobileMenuOpen ? (
              <line x1="18" y1="6" x2="6" y2="18" strokeLinecap="round" />
            ) : (
              <>
                <line x1="3" y1="12" x2="21" y2="12" />
                <line x1="3" y1="6" x2="21" y2="6" />
                <line x1="3" y1="18" x2="21" y2="18" />
              </>
            )}
          </svg>
        </button>

        {/* Nav Links */}
        <ul className={`navbar-nav ${mobileMenuOpen ? 'open' : ''}`}>
          <li>
            <button
              className={`nav-link-btn ${currentView === 'dashboard' ? 'active' : ''}`}
              onClick={() => handleNavClick('dashboard')}
            >
              <svg className="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <rect x="3" y="3" width="7" height="9" rx="1" />
                <rect x="14" y="3" width="7" height="5" rx="1" />
                <rect x="14" y="12" width="7" height="9" rx="1" />
                <rect x="3" y="16" width="7" height="5" rx="1" />
              </svg>
              Dashboard
            </button>
          </li>
          <li>
            <button
              className={`nav-link-btn ${currentView === 'upload' ? 'active' : ''}`}
              onClick={() => handleNavClick('upload')}
            >
              <svg className="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
                <polyline points="17 8 12 3 7 8" />
                <line x1="12" y1="3" x2="12" y2="15" />
              </svg>
              Upload Logs
            </button>
          </li>
          <li>
            <button
              className={`nav-link-btn ${currentView === 'results' ? 'active' : ''}`}
              onClick={() => handleNavClick('results')}
              title={activeAnalysis ? 'View current analysis results' : 'No active analysis loaded'}
            >
              <svg className="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <polyline points="22 12 18 12 15 21 9 3 6 12 2 12" />
              </svg>
              Analysis
            </button>
          </li>
          <li>
            <button
              className={`nav-link-btn ${currentView === 'history' ? 'active' : ''}`}
              onClick={() => handleNavClick('history')}
            >
              <svg className="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <circle cx="12" cy="12" r="10" />
                <polyline points="12 6 12 12 16 14" />
              </svg>
              History
            </button>
          </li>
        </ul>

        {/* Right Actions */}
        <div className="navbar-actions">
          {activeAnalysis && (
            <div className="active-file-indicator" title={activeAnalysis.originalFilename}>
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
                <polyline points="14 2 14 8 20 8" />
              </svg>
              <span>{activeAnalysis.originalFilename}</span>
            </div>
          )}

          <div className="server-status-pill">
            <span className="status-dot"></span>
            <span>AI Ready</span>
          </div>
        </div>
      </div>
    </nav>
  );
};

export default Navbar;
