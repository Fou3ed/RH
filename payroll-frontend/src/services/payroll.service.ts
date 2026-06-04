import { api } from '@/services/api';
import type { PayrollRow, PayrollRunSummary } from '@/types/payroll';

export const payrollService = {
  async list(periodId: number): Promise<PayrollRow[]> {
    const { data } = await api.get<PayrollRow[]>('/payroll', { params: { periodId } });
    return data;
  },

  async calculate(periodId: number): Promise<PayrollRunSummary> {
    const { data } = await api.post<PayrollRunSummary>('/payroll/calculate', null, { params: { periodId } });
    return data;
  },

  async approve(id: number): Promise<PayrollRow> {
    const { data } = await api.post<PayrollRow>(`/payroll/${id}/approve`);
    return data;
  },
};
