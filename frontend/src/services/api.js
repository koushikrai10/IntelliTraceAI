import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Accept': 'application/json',
  },
  timeout: 60000, // 60s timeout for large log analysis or AI generation
});

/**
 * Upload a log file for analysis
 * @param {File} file - .log or .txt file
 * @param {Function} [onUploadProgress] - Progress callback
 * @returns {Promise<Object>} AnalysisResponseDto
 */
export const uploadLogFile = async (file, onUploadProgress) => {
  const formData = new FormData();
  formData.append('file', file);

  const response = await apiClient.post('/api/analysis/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
    onUploadProgress: (progressEvent) => {
      if (onUploadProgress && progressEvent.total) {
        const percentCompleted = Math.round((progressEvent.loaded * 100) / progressEvent.total);
        onUploadProgress(percentCompleted);
      }
    },
  });

  return response.data;
};

/**
 * Get an existing analysis by ID
 * @param {string|number} id
 * @returns {Promise<Object>} AnalysisResponseDto
 */
export const getAnalysis = async (id) => {
  const response = await apiClient.get(`/api/analysis/${id}`);
  return response.data;
};

/**
 * List all past analyses
 * @returns {Promise<Array>} List of analyses
 */
export const getAnalysisHistory = async () => {
  const response = await apiClient.get('/api/history');
  return response.data;
};

/**
 * Delete an analysis record by ID
 * @param {string|number} id
 * @returns {Promise<Object>}
 */
export const deleteAnalysis = async (id) => {
  const response = await apiClient.delete(`/api/history/${id}`);
  return response.data;
};

/**
 * Request an AI diagnostic explanation for an error
 * @param {Object} errorData - { errorPattern, sampleMessage, stackTrace, context }
 * @returns {Promise<Object>} AiExplanationResponseDto
 */
export const getAiExplanation = async (errorData) => {
  const payload = {
    errorPattern: errorData.errorPattern || errorData.sampleMessage || 'Unknown error',
    sampleMessage: errorData.sampleMessage || '',
    stackTrace: errorData.stackTrace || '',
    context: errorData.context || `Severity: ${errorData.severity || 'ERROR'}, Exception: ${errorData.exceptionType || 'None'}`
  };

  const response = await apiClient.post('/api/ai/explain', payload, {
    headers: {
      'Content-Type': 'application/json',
    },
  });

  return response.data;
};

/**
 * Download analysis report in CSV or TXT format
 * @param {string|number} id
 * @param {'csv'|'txt'} format
 * @param {string} [filename]
 */
export const downloadReport = async (id, format = 'csv', filename = null) => {
  const normalizedFormat = format.toLowerCase() === 'txt' ? 'txt' : 'csv';
  const response = await apiClient.get(`/api/report/${id}/${normalizedFormat}`, {
    responseType: 'blob',
  });

  // Create a blob URL and trigger download
  const blob = new Blob([response.data], {
    type: normalizedFormat === 'csv' ? 'text/csv' : 'text/plain',
  });
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.setAttribute('download', filename || `analysis-${id}-report.${normalizedFormat}`);
  document.body.appendChild(link);
  link.click();
  link.parentNode.removeChild(link);
  window.URL.revokeObjectURL(url);

  return true;
};

export default apiClient;
