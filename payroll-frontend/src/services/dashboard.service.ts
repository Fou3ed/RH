import { api } from '@/services/api';

export interface HrDashboard {
  totalEmployees: number;
  activeEmployees: number;
  inactiveEmployees: number;
  newHiresThisMonth: number;
  attendanceRateThisMonth: number;
  byDepartment: { department: string; count: number }[];
  recentHires: { employeeId: string; fullName: string; department: string | null; hireDate: string }[];
}

export interface PayrollDashboard {
  periodId: number | null;
  periodCode: string | null;
  status: string | null;
  activeEmployees: number;
  calculated: number;
  approved: number;
  pendingApproval: number;
  percentComplete: number;
  totalGross: number;
  totalNet: number;
}

export const dashboardService = {
  async hr(): Promise<HrDashboard> {
    const { data } = await api.get<HrDashboard>('/dashboard/hr');
    return data;
  },
  async payroll(): Promise<PayrollDashboard> {
    const { data } = await api.get<PayrollDashboard>('/dashboard/payroll');
    return data;
  },
};
