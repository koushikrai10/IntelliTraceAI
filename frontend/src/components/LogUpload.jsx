import React, { useState, useRef } from 'react';
import { uploadLogFile } from '../services/api';
import './LogUpload.css';

/**
 * File upload component supporting Drag & Drop, validation,
 * progress feedback, and instant navigation to analysis results.
 */
const LogUpload = ({ onUploadSuccess }) => {
  const [dragOver, setDragOver] = useState(false);
  const [selectedFile, setSelectedFile] = useState(null);
  const [uploadProgress, setUploadProgress] = useState(0);
  const [isUploading, setIsUploading] = useState(false);
  const [statusMessage, setStatusMessage] = useState('');
  const [errorMessage, setErrorMessage] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  const fileInputRef = useRef(null);

  // Validate file extension
  const validateFile = (file) => {
    if (!file) return false;
    const name = file.name.toLowerCase();
    const isValidExt = name.endsWith('.log') || name.endsWith('.txt');

    if (!isValidExt) {
      setErrorMessage('Invalid file format. Please upload a .log or .txt file.');
      return false;
    }

    if (file.size > 50 * 1024 * 1024) {
      setErrorMessage('File size exceeds 50MB limit.');
      return false;
    }

    return true;
  };

  const handleDragOver = (e) => {
    e.preventDefault();
    setDragOver(true);
  };

  const handleDragLeave = (e) => {
    e.preventDefault();
    setDragOver(false);
  };

  const handleDrop = (e) => {
    e.preventDefault();
    setDragOver(false);
    setErrorMessage('');
    setSuccessMessage('');

    if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
      const file = e.dataTransfer.files[0];
      if (validateFile(file)) {
        setSelectedFile(file);
      }
    }
  };

  const handleFileInputChange = (e) => {
    setErrorMessage('');
    setSuccessMessage('');
    if (e.target.files && e.target.files.length > 0) {
      const file = e.target.files[0];
      if (validateFile(file)) {
        setSelectedFile(file);
      }
    }
  };

  const handleRemoveFile = (e) => {
    e.stopPropagation();
    setSelectedFile(null);
    setUploadProgress(0);
    setErrorMessage('');
    setSuccessMessage('');
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  const handleUpload = async () => {
    if (!selectedFile) {
      setErrorMessage('Please select a log file to upload.');
      return;
    }

    setIsUploading(true);
    setUploadProgress(0);
    setErrorMessage('');
    setStatusMessage('Uploading log file to server...');

    try {
      const result = await uploadLogFile(selectedFile, (progress) => {
        setUploadProgress(progress);
        if (progress === 100) {
          setStatusMessage('Processing & analyzing log entries with AI engine...');
        }
      });

      setSuccessMessage('Log analysis completed successfully!');
      setStatusMessage('');

      // Brief delay to display success animation, then navigate
      setTimeout(() => {
        if (onUploadSuccess) {
          onUploadSuccess(result);
        }
      }, 700);
    } catch (err) {
      console.error('Upload failed:', err);
      const serverMsg = err.response?.data?.message || err.message || 'Upload failed. Backend server may be offline at http://localhost:8080.';
      setErrorMessage(serverMsg);
      setStatusMessage('');
    } finally {
      setIsUploading(false);
    }
  };

  // Helper for formatting file size
  const formatBytes = (bytes) => {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  };

  // Quick preset sample logs
  const handleLoadSample = (sampleType) => {
    let filename = 'sample-application.log';
    let sampleContent = `2024-01-15 08:00:01.234 INFO  [com.example.app.Application] - Application starting on port 8080
2024-01-15 08:00:01.456 INFO  [com.example.app.config.DatabaseConfig] - Initializing database connection pool
2024-01-15 08:00:02.100 DEBUG [com.example.app.config.DatabaseConfig] - Connection pool size: min=5, max=20
2024-01-15 08:30:45.123 ERROR [com.example.app.service.OrderService] - Failed to process order for user 12345
java.lang.NullPointerException: Cannot invoke method on null object reference
\tat com.example.app.service.OrderService.processOrder(OrderService.java:87)
\tat com.example.app.service.OrderService.handleCheckout(OrderService.java:45)
\tat com.example.app.controller.OrderController.checkout(OrderController.java:32)
2024-01-15 08:45:12.345 ERROR [com.example.app.repository.ProductRepository] - Database query failed
org.springframework.dao.DataAccessException: Could not execute query
\tat org.springframework.jdbc.core.JdbcTemplate.query(JdbcTemplate.java:465)
\tat com.example.app.repository.ProductRepository.findByCategory(ProductRepository.java:56)
Caused by: java.sql.SQLException: Connection refused: connect
\tat com.mysql.cj.jdbc.ConnectionImpl.createNewIO(ConnectionImpl.java:828)
2024-01-15 09:15:33.456 ERROR [com.example.app.service.PaymentService] - Payment processing failed for transaction TXN-98765
java.io.IOException: Connection timed out
\tat com.example.app.service.PaymentService.processPayment(PaymentService.java:112)
\tat com.example.app.service.OrderService.finalizeOrder(OrderService.java:156)`;

    if (sampleType === 'microservice') {
      filename = 'sample-microservice.log';
      sampleContent = `2024-01-15 10:00:00.100 INFO  [com.example.gateway.GatewayFilter] - Inbound request POST /api/orders
2024-01-15 10:00:01.500 ERROR [com.example.gateway.Router] - Upstream service timeout after 5000ms
java.net.SocketTimeoutException: Read timed out
\tat java.net.SocketInputStream.read(SocketInputStream.java:140)
\tat com.example.gateway.client.HttpClient.execute(HttpClient.java:94)`;
    } else if (sampleType === 'edge') {
      filename = 'sample-edge-cases.txt';
    }

    const blob = new Blob([sampleContent], { type: 'text/plain' });
    const file = new File([blob], filename, { type: 'text/plain' });
    setSelectedFile(file);
    setErrorMessage('');
    setSuccessMessage('');
  };

  return (
    <div className="upload-page-container animate-fade-in">
      <div className="upload-header-section">
        <h1 className="upload-page-title">
          Upload & Analyze <span className="gradient-text">Log Files</span>
        </h1>
        <p className="upload-page-subtitle">
          Submit raw server logs or application stack traces for AI-assisted pattern clustering,
          error severity breakdown, and actionable root cause diagnosis.
        </p>
      </div>

      {/* Drag & Drop Card */}
      <div
        className={`drop-zone ${dragOver ? 'dragging' : ''} ${selectedFile ? 'has-file' : ''}`}
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        onDrop={handleDrop}
        onClick={() => !isUploading && fileInputRef.current?.click()}
      >
        <input
          type="file"
          ref={fileInputRef}
          className="file-input-hidden"
          accept=".log,.txt"
          onChange={handleFileInputChange}
          disabled={isUploading}
        />

        <div className="drop-icon-container">
          <svg className="drop-svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
            <polyline points="17 8 12 3 7 8" />
            <line x1="12" y1="3" x2="12" y2="15" />
          </svg>
        </div>

        <h3 className="drop-headline">
          {selectedFile ? 'File Ready for Analysis' : 'Drag & Drop Log File Here'}
        </h3>
        <p className="drop-subtext">
          or <span style={{ color: 'var(--accent-cyan)', textDecoration: 'underline' }}>browse from your computer</span>
        </p>

        <span className="file-type-hint">Supported formats: .log, .txt (up to 50MB)</span>

        {/* Selected file card */}
        {selectedFile && (
          <div className="selected-file-details" onClick={(e) => e.stopPropagation()}>
            <div className="file-info-group">
              <svg className="file-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
                <polyline points="14 2 14 8 20 8" />
              </svg>
              <div className="file-meta-text">
                <span className="file-name" title={selectedFile.name}>{selectedFile.name}</span>
                <span className="file-size">{formatBytes(selectedFile.size)}</span>
              </div>
            </div>

            {!isUploading && (
              <button
                className="remove-file-btn"
                onClick={handleRemoveFile}
                title="Remove selected file"
                aria-label="Remove file"
              >
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <line x1="18" y1="6" x2="6" y2="18" />
                  <line x1="6" y1="6" x2="18" y2="18" />
                </svg>
              </button>
            )}
          </div>
        )}

        {/* Progress Bar */}
        {isUploading && (
          <div className="upload-progress-container" onClick={(e) => e.stopPropagation()}>
            <div className="progress-header">
              <span>{statusMessage || 'Uploading...'}</span>
              <span>{uploadProgress}%</span>
            </div>
            <div className="progress-track">
              <div className="progress-fill" style={{ width: `${uploadProgress}%` }} />
            </div>
          </div>
        )}

        {/* Upload Button */}
        {selectedFile && !isUploading && (
          <button
            className="btn-primary"
            style={{ marginTop: '10px' }}
            onClick={(e) => {
              e.stopPropagation();
              handleUpload();
            }}
          >
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2" />
            </svg>
            Start AI Analysis
          </button>
        )}
      </div>

      {/* Error Alert */}
      {errorMessage && (
        <div className="upload-alert upload-alert-error animate-fade-in">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <circle cx="12" cy="12" r="10" />
            <line x1="12" y1="8" x2="12" y2="12" />
            <line x1="12" y1="16" x2="12.01" y2="16" />
          </svg>
          <div>{errorMessage}</div>
        </div>
      )}

      {/* Success Alert */}
      {successMessage && (
        <div className="upload-alert upload-alert-success animate-fade-in">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <polyline points="20 6 9 17 4 12" />
          </svg>
          <div>{successMessage}</div>
        </div>
      )}

      {/* Quick Test Samples */}
      <div className="glass-panel sample-logs-section">
        <h4 className="sample-logs-title">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="var(--accent-cyan)" strokeWidth="2">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
            <polyline points="14 2 14 8 20 8" />
          </svg>
          Quick Load Sample Logs
        </h4>
        <div className="sample-chips-grid">
          <div
            className="sample-chip-card"
            onClick={() => handleLoadSample('app')}
            role="button"
            tabIndex={0}
            aria-label="Load Spring Boot sample log with NPE, SQL timeouts and payment errors"
            onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); handleLoadSample('app'); } }}
          >
            <span className="sample-chip-name">sample-application.log</span>
            <span className="sample-chip-desc">Spring Boot app with NPE, SQL timeouts & payment errors</span>
          </div>

          <div
            className="sample-chip-card"
            onClick={() => handleLoadSample('microservice')}
            role="button"
            tabIndex={0}
            aria-label="Load microservice sample log with API Gateway latency errors"
            onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); handleLoadSample('microservice'); } }}
          >
            <span className="sample-chip-name">sample-microservice.log</span>
            <span className="sample-chip-desc">API Gateway latency and downstream connection resets</span>
          </div>

          <div
            className="sample-chip-card"
            onClick={() => handleLoadSample('edge')}
            role="button"
            tabIndex={0}
            aria-label="Load edge cases sample log with mixed multiline traces"
            onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); handleLoadSample('edge'); } }}
          >
            <span className="sample-chip-name">sample-edge-cases.txt</span>
            <span className="sample-chip-desc">Mixed multiline traces and unformatted exception dumps</span>
          </div>
        </div>
      </div>
    </div>
  );
};

export default LogUpload;
