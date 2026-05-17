import axios from 'axios';

const API_BASE = '/api/v1';

const api = axios.create({
  baseURL: API_BASE,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Dashboard APIs
export const fetchDashboardStats = () => api.get('/dashboard/stats');
export const fetchRecentTransactions = (limit = 50) =>
  api.get(`/dashboard/recent-transactions?limit=${limit}`);
export const fetchFraudTrends = () => api.get('/dashboard/fraud-trends');
export const fetchActiveAlerts = () => api.get('/dashboard/alerts');
export const acknowledgeAlert = (alertId) =>
  api.post(`/dashboard/alerts/${alertId}/acknowledge`);
export const fetchAIStatus = () => api.get('/dashboard/ai-status');

// Transaction APIs
export const submitTransaction = (data) => api.post('/transactions', data);
export const getTransactionStatus = (txnId) =>
  api.get(`/transactions/${txnId}/status`);
export const getUserTransactions = (userId) =>
  api.get(`/transactions/user/${userId}`);

export default api;
