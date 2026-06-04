import { api } from '@/services/api';
import { tokenStorage } from '@/services/tokenStorage';
import type { AuthResponse, AuthUser, LoginRequest } from '@/types/auth';

export const authService = {
  async login(credentials: LoginRequest): Promise<AuthUser> {
    const { data } = await api.post<AuthResponse>('/auth/login', credentials);
    tokenStorage.setTokens(data.token, data.refreshToken);
    tokenStorage.setUser(data.user);
    return data.user;
  },

  async me(): Promise<AuthUser> {
    const { data } = await api.get<AuthUser>('/auth/me');
    tokenStorage.setUser(data);
    return data;
  },

  async logout(): Promise<void> {
    const refreshToken = tokenStorage.getRefreshToken();
    try {
      if (refreshToken) {
        await api.post('/auth/logout', { refreshToken });
      }
    } finally {
      tokenStorage.clear();
    }
  },
};
