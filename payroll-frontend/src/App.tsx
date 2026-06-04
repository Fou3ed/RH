import { Routes, Route, Outlet } from 'react-router-dom';
import MainLayout from '@/components/layout/MainLayout';
import ProtectedRoute from '@/components/layout/ProtectedRoute';
import Dashboard from '@/pages/Dashboard';
import Login from '@/pages/Login';
import NotFound from '@/pages/NotFound';
import Unauthorized from '@/pages/Unauthorized';
import EmployeeList from '@/pages/employee/EmployeeList';
import EmployeeForm from '@/pages/employee/EmployeeForm';
import EmployeeDetail from '@/pages/employee/EmployeeDetail';
import EmployeeImport from '@/pages/employee/EmployeeImport';
import Departments from '@/pages/configuration/Departments';

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

        <Route
          path="/employees"
          element={
            <ProtectedRoute permission="employee.view">
              <EmployeeList />
            </ProtectedRoute>
          }
        />
        <Route
          path="/employees/new"
          element={
            <ProtectedRoute permission="employee.create">
              <EmployeeForm />
            </ProtectedRoute>
          }
        />
        <Route
          path="/employees/import"
          element={
            <ProtectedRoute permission="employee.import">
              <EmployeeImport />
            </ProtectedRoute>
          }
        />
        <Route
          path="/employees/:id"
          element={
            <ProtectedRoute permission="employee.view">
              <EmployeeDetail />
            </ProtectedRoute>
          }
        />
        <Route
          path="/employees/:id/edit"
          element={
            <ProtectedRoute permission="employee.edit">
              <EmployeeForm />
            </ProtectedRoute>
          }
        />
        <Route
          path="/departments"
          element={
            <ProtectedRoute permission="employee.view">
              <Departments />
            </ProtectedRoute>
          }
        />

        <Route path="/unauthorized" element={<Unauthorized />} />
        <Route path="*" element={<NotFound />} />
      </Route>
    </Routes>
  );
}
