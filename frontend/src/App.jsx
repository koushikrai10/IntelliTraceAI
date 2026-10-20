import React, { useState, useEffect, useCallback } from 'react';
import Navbar from './components/Navbar';
import Dashboard from './components/Dashboard';
import LogUpload from './components/LogUpload';
import AnalysisResults from './components/AnalysisResults';
import AnalysisHistory from './components/AnalysisHistory';
import { getAnalysis, getAnalysisHistory } from './services/api';
import './App.css';

/**
 * IntelliTrace AI Main Application Component
 */
function App() {
  const [currentView, setCurrentView] = useState('dashboard'); // 'dashboard' | 'upload' | 'results' | 'history'
  const [activeAnalysis, setActiveAnalysis] = useState(null);
  const [historyList, setHistoryList] = useState([]);
  const [loadingHistory, setLoadingHistory] = useState(false);
  const [serverOnline, setServerOnline] = useState(true);

  // Load history from backend
  const fetchHistory = useCallback(async () => {
    setLoadingHistory(true);
    try {
      const data = await getAnalysisHistory();
      setHistoryList(Array.isArray(data) ? data : []);
      setServerOnline(true);
    } catch (err) {
      console.warn('Backend server may not be reachable:', err.message);
      setServerOnline(false);
    } finally {
      setLoadingHistory(false);
    }
  }, []);

  useEffect(() => {
    fetchHistory();
  }, [fetchHistory]);

  // Handle selecting an analysis to view
  const handleSelectAnalysis = async (id) => {
    try {
      const fullAnalysis = await getAnalysis(id);
      setActiveAnalysis(fullAnalysis);
      setCurrentView('results');
    } catch (err) {
      console.error('Failed to load analysis #' + id, err);
      // If full fetch failed, try finding in local history list
      const fallback = historyList.find((item) => item.id === id);
      if (fallback) {
        setActiveAnalysis(fallback);
        setCurrentView('results');
      } else {
        alert('Could not retrieve analysis details from server.');
      }
    }
  };

  // Handle successful upload of a new log
  const handleUploadSuccess = (newAnalysis) => {
    setActiveAnalysis(newAnalysis);
    setCurrentView('results');
    fetchHistory(); // Refresh history list in background
  };

  return (
    <div className="app-container">
      {/* Top Navbar */}
      <Navbar
        currentView={currentView}
        onNavigate={setCurrentView}
        activeAnalysis={activeAnalysis}
      />

      {/* Main Content View Switcher */}
      <main className="main-content" role="main">
        {currentView === 'dashboard' && (
          <Dashboard
            analyses={historyList}
            activeAnalysis={activeAnalysis}
            onNavigate={setCurrentView}
            onSelectAnalysis={handleSelectAnalysis}
          />
        )}

        {currentView === 'upload' && (
          <LogUpload
            onUploadSuccess={handleUploadSuccess}
          />
        )}

        {currentView === 'results' && (
          <AnalysisResults
            analysis={activeAnalysis}
            onNavigateToUpload={() => setCurrentView('upload')}
          />
        )}

        {currentView === 'history' && (
          <AnalysisHistory
            analyses={historyList}
            loading={loadingHistory}
            onRefresh={fetchHistory}
            onSelectAnalysis={handleSelectAnalysis}
            onNavigateToUpload={() => setCurrentView('upload')}
          />
        )}
      </main>

      {/* Footer */}
      <footer className="app-footer">
        <div className="footer-container">
          <div className="footer-brand-group">
            <span className="footer-logo-text">IntelliTrace AI</span>
            <span>&copy; {new Date().getFullYear()} Enterprise Log Diagnostics</span>
          </div>

          <div className="footer-links">
            <span className="footer-link" onClick={() => setCurrentView('dashboard')}>Dashboard</span>
            <span className="footer-link" onClick={() => setCurrentView('upload')}>Upload</span>
            <span className="footer-link" onClick={() => setCurrentView('history')}>History</span>
          </div>

          <div className="backend-indicator-footer">
            <span
              className="status-dot"
              style={{
                background: serverOnline ? '#10b981' : '#f59e0b',
                boxShadow: serverOnline ? '0 0 8px #10b981' : '0 0 8px #f59e0b',
              }}
            />
            <span style={{ color: serverOnline ? '#34d399' : '#fbbf24' }}>
              {serverOnline ? 'Backend: http://localhost:8080' : 'Offline / Standalone Mode'}
            </span>
          </div>
        </div>
      </footer>
    </div>
  );
}

export default App;
