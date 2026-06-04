import { Navigate, useLocation } from 'react-router-dom';
import type { ReactNode } from 'react';
import { useAuth } from '@/context/AuthContext';

interface ProtectedRouteProps {
  children: ReactNode;
  /** Optional permission required to view this route. */
  permission?: string;
}

/** Blocks unauthenticated users (→ /login) and, optionally, those lacking a permission (→ /unauthorized). */
export default function ProtectedRoute({ children, permission }: ProtectedRouteProps) {
  const { isAuthenticated, hasPermission } = useAuth();
  const location = useLocation();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }

  if (permission && !hasPermission(permission)) {
    return <Navigate to="/unauthorized" replace />;
  }

  return <>{children}</>;
}
