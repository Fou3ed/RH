import { api } from '@/services/api';

export interface SalaryCategory {
  id: number;
  code: string;
  name: string | null;
}

export interface Position {
  id: number;
  code: string;
  name: string;
  departmentId: number;
}

export const referenceService = {
  async categories(): Promise<SalaryCategory[]> {
    const { data } = await api.get<SalaryCategory[]>('/salary-categories');
    return data;
  },

  async positions(departmentId?: number): Promise<Position[]> {
    const { data } = await api.get<Position[]>('/positions', {
      params: departmentId ? { department_id: departmentId } : {},
    });
    return data;
  },
};
