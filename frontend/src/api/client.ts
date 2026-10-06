import axios from 'axios';

export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1',
  timeout: 10000, // 10 second timeout
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request Interceptor: Attach JWT securely
apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('auth_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Response Interceptor: Global Error Handling
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.code === 'ECONNABORTED' || error.message.includes('timeout')) {
      console.error('Network timeout: The backend is taking too long to respond.');
      // Fallback UI notification trigger could go here
    } else if (error.response?.status === 401) {
      console.error('Unauthorized: Your session has expired.');
      // Automatically redirect to login or clear token here
      localStorage.removeItem('auth_token');
      window.location.href = '/'; 
    } else {
      console.error('API Error:', error.response?.data?.message || error.message);
    }
    return Promise.reject(error);
  }
);
