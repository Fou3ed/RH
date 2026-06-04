import type { AuthUser } from '@/types/auth';

/** Single source of truth for persisted auth state (localStorage). */
const ACCESS = 'accessToken';
const REFRESH = 'refreshToken';
const USER = 'authUser';

export const tokenStorage = {
  getAccessToken: () => localStorage.getItem(ACCESS),
  getRefreshToken: () => localStorage.getItem(REFRESH),

  getUser(): AuthUser | null {
    const raw = localStorage.getItem(USER);
    return raw ? (JSON.parse(raw) as AuthUser) : null;
  },

  setTokens(accessToken: string, refreshToken: string) {
    localStorage.setItem(ACCESS, accessToken);
    localStorage.setItem(REFRESH, refreshToken);
  },

  setUser(user: AuthUser) {
    localStorage.setItem(USER, JSON.stringify(user));
  },

  clear() {
    localStorage.removeItem(ACCESS);
    localStorage.removeItem(REFRESH);
    localStorage.removeItem(USER);
  },
};
