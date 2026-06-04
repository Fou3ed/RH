import { Routes, Route, Outlet } from 'react-router-dom';
import MainLayout from '@/components/layout/MainLayout';
import ProtectedRoute from '@/components/layout/ProtectedRoute';
import Dashboard from '@/pages/Dashboard';
import Login from '@/pages/Login';
import NotFound from '@/pages/NotFound';
import Unauthorized from '@/pages/Unauthorized';

/** Authenticated shell: guards access, then renders the matched page in the layout. */
function ProtectedShell() {
  return (
    <ProtectedRoute>
      <MainLayout>
        <Outlet />
      </MainLayout>
    </ProtectedRoute>
  );
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />

      <Route element={<ProtectedShell />}>
        <Route path="/" element={<Dashboard />} />
        <Route path="/unauthorized" element={<Unauthorized />} />
        <Route path="*" element={<NotFound />} />
      </Route>
    </Routes>
  );
}
