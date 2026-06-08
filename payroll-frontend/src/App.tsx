import { lazy, Suspense } from 'react';
import { Routes, Route, Outlet } from 'react-router-dom';
import { Box, CircularProgress } from '@mui/material';
import MainLayout from '@/components/layout/MainLayout';
import ProtectedRoute from '@/components/layout/ProtectedRoute';

// Eager: entry screen, shown on first paint.
import Login from '@/pages/Login';

// Lazy: each page is split into its own chunk and loaded on demand.
const Dashboard = lazy(() => import('@/pages/Dashboard'));
const NotFound = lazy(() => import('@/pages/NotFound'));
const Unauthorized = lazy(() => import('@/pages/Unauthorized'));
const EmployeeList = lazy(() => import('@/pages/employee/EmployeeList'));
const EmployeeForm = lazy(() => import('@/pages/employee/EmployeeForm'));
const EmployeeDetail = lazy(() => import('@/pages/employee/EmployeeDetail'));
const EmployeeImport = lazy(() => import('@/pages/employee/EmployeeImport'));
const AttendanceCalendar = lazy(() => import('@/pages/attendance/AttendanceCalendar'));
const AttendanceImport = lazy(() => import('@/pages/attendance/AttendanceImport'));
const Departments = lazy(() => import('@/pages/configuration/Departments'));
const Configuration = lazy(() => import('@/pages/configuration/Configuration'));
const PayrollPeriods = lazy(() => import('@/pages/configuration/PayrollPeriods'));
const PayrollReview = lazy(() => import('@/pages/payroll/PayrollReview'));
const UserManagement = lazy(() => import('@/pages/admin/UserManagement'));

/** Centered spinner shown while a lazy page chunk loads. */
function PageFallback() {
  return (
    <Box sx={{ display: 'grid', placeItems: 'center', minHeight: '50vh' }}>
      <CircularProgress />
    </Box>
  );
}

/** Authenticated shell: guards access, then renders the matched page in the layout. */
function ProtectedShell() {
  return (
    <ProtectedRoute>
      <MainLayout>
        <Suspense fallback={<PageFallback />}>
          <Outlet />
        </Suspense>
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
          path="/attendance"
          element={
            <ProtectedRoute permission="attendance.view">
              <AttendanceCalendar />
            </ProtectedRoute>
          }
        />
        <Route
          path="/attendance/import"
          element={
            <ProtectedRoute permission="attendance.import">
              <AttendanceImport />
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
        <Route
          path="/configuration"
          element={
            <ProtectedRoute permission="payroll.view">
              <Configuration />
            </ProtectedRoute>
          }
        />
        <Route
          path="/payroll"
          element={
            <ProtectedRoute permission="payroll.view">
              <PayrollReview />
            </ProtectedRoute>
          }
        />
        <Route
          path="/payroll-periods"
          element={
            <ProtectedRoute permission="payroll.view">
              <PayrollPeriods />
            </ProtectedRoute>
          }
        />

        <Route
          path="/users"
          element={
            <ProtectedRoute permission="user.manage">
              <UserManagement />
            </ProtectedRoute>
          }
        />

        <Route path="/unauthorized" element={<Unauthorized />} />
        <Route path="*" element={<NotFound />} />
      </Route>
    </Routes>
  );
}
