import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios';
import { tokenStorage } from '@/services/tokenStorage';
import type { RefreshResponse } from '@/types/auth';

const baseURL = import.meta.env.VITE_API_BASE_URL ?? '/api';

/** Main client — carries the access token and auto-refreshes on 401. */
export const api = axios.create({
  baseURL,
  headers: { 'Content-Type': 'application/json' },
  timeout: 15000,
});

/** Bare client used only for the refresh call, to avoid interceptor recursion. */
const refreshClient = axios.create({ baseURL, headers: { 'Content-Type': 'application/json' } });

api.interceptors.request.use((config) => {
  const token = tokenStorage.getAccessToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Single in-flight refresh shared by all queued requests.
let refreshing: Promise<string | null> | null = null;

async function refreshAccessToken(): Promise<string | null> {
  const refreshToken = tokenStorage.getRefreshToken();
  if (!refreshToken) return null;
  try {
    const { data } = await refreshClient.post<RefreshResponse>('/auth/refresh', { refreshToken });
    tokenStorage.setTokens(data.token, data.refreshToken);
    return data.token;
  } catch {
    tokenStorage.clear();
    return null;
  }
}

api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const original = error.config as (InternalAxiosRequestConfig & { _retried?: boolean }) | undefined;
    const isAuthCall = original?.url?.includes('/auth/');

    if (error.response?.status === 401 && original && !original._retried && !isAuthCall) {
      original._retried = true;
      refreshing = refreshing ?? refreshAccessToken();
      const newToken = await refreshing;
      refreshing = null;

      if (newToken) {
        original.headers.Authorization = `Bearer ${newToken}`;
        return api(original);
      }
      // Refresh failed — force re-login.
      window.dispatchEvent(new Event('auth:logout'));
    }
    return Promise.reject(error);
  },
);
