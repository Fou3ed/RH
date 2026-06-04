import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react';
import { authService } from '@/services/auth.service';
import { tokenStorage } from '@/services/tokenStorage';
import type { AuthUser, LoginRequest } from '@/types/auth';

interface AuthContextValue {
  user: AuthUser | null;
  isAuthenticated: boolean;
  login: (credentials: LoginRequest) => Promise<void>;
  logout: () => Promise<void>;
  hasPermission: (permission: string) => boolean;
  hasRole: (role: string) => boolean;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(() => tokenStorage.getUser());

  const logout = useCallback(async () => {
    await authService.logout();
    setUser(null);
  }, []);

  const login = useCallback(async (credentials: LoginRequest) => {
    const loggedIn = await authService.login(credentials);
    setUser(loggedIn);
  }, []);

  // React to forced logout dispatched by the API layer (failed token refresh).
  useEffect(() => {
    const handler = () => {
      tokenStorage.clear();
      setUser(null);
    };
    window.addEventListener('auth:logout', handler);
    return () => window.removeEventListener('auth:logout', handler);
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      isAuthenticated: Boolean(user),
      login,
      logout,
      hasPermission: (permission) => user?.permissions.includes(permission) ?? false,
      hasRole: (role) => user?.roles.includes(role) ?? false,
    }),
    [user, login, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return ctx;
}
