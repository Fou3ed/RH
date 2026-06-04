import axios from 'axios';

/**
 * Shared Axios instance. The base URL comes from VITE_API_BASE_URL
 * (defaults to the Vite dev proxy at /api).
 */
export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '/api',
  headers: { 'Content-Type': 'application/json' },
  timeout: 15000,
});

// Attach the JWT access token (wired up fully in Sprint 2).
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Centralised response error handling.
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // TODO (Sprint 2): trigger refresh-token flow / redirect to login.
      localStorage.removeItem('accessToken');
    }
    return Promise.reject(error);
  },
);
