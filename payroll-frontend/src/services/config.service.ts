import { api } from '@/services/api';
import type {
  AllowanceConfig,
  PayrollPeriod,
  SalaryScale,
  TaxConfig,
} from '@/types/config';

export const salaryScaleService = {
  async list(year: number): Promise<SalaryScale[]> {
    const { data } = await api.get<SalaryScale[]>('/config/salary-scales', { params: { year } });
    return data;
  },
  create: (payload: Partial<SalaryScale>) => api.post('/config/salary-scales', payload).then((r) => r.data),
  update: (id: number, payload: Partial<SalaryScale>) =>
    api.put(`/config/salary-scales/${id}`, payload).then((r) => r.data),
  remove: (id: number) => api.delete(`/config/salary-scales/${id}`).then(() => undefined),
};

export const taxConfigService = {
  async list(year: number): Promise<TaxConfig[]> {
    const { data } = await api.get<TaxConfig[]>('/config/tax', { params: { year } });
    return data;
  },
  create: (payload: Partial<TaxConfig>) => api.post('/config/tax', payload).then((r) => r.data),
  update: (id: number, payload: Partial<TaxConfig>) => api.put(`/config/tax/${id}`, payload).then((r) => r.data),
  remove: (id: number) => api.delete(`/config/tax/${id}`).then(() => undefined),
};

export const allowanceConfigService = {
  async list(): Promise<AllowanceConfig[]> {
    const { data } = await api.get<AllowanceConfig[]>('/config/allowances');
    return data;
  },
  create: (payload: Partial<AllowanceConfig>) => api.post('/config/allowances', payload).then((r) => r.data),
  update: (id: number, payload: Partial<AllowanceConfig>) =>
    api.put(`/config/allowances/${id}`, payload).then((r) => r.data),
  remove: (id: number) => api.delete(`/config/allowances/${id}`).then(() => undefined),
};

export const payrollPeriodService = {
  async list(): Promise<PayrollPeriod[]> {
    const { data } = await api.get<PayrollPeriod[]>('/payroll-periods');
    return data;
  },
  create: (payload: { periodMonth: number; periodYear: number }) =>
    api.post('/payroll-periods', payload).then((r) => r.data),
  transition: (id: number, status: string) =>
    api.post(`/payroll-periods/${id}/transition`, { status }).then((r) => r.data),
  remove: (id: number) => api.delete(`/payroll-periods/${id}`).then(() => undefined),
};
