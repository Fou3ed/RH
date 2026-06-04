import { api } from '@/services/api';
import type {
  Department,
  DepartmentRequest,
  Employee,
  EmployeeFormValues,
  PageResponse,
} from '@/types/employee';

export interface EmployeeQuery {
  page?: number;
  limit?: number;
  departmentId?: number | null;
  status?: string | null;
  search?: string | null;
}

/** Strips empty/undefined values so they aren't sent as query params. */
function toParams(query: EmployeeQuery): Record<string, unknown> {
  const params: Record<string, unknown> = {
    page: query.page ?? 0,
    limit: query.limit ?? 25,
  };
  if (query.departmentId) params.department_id = query.departmentId;
  if (query.status) params.status = query.status;
  if (query.search) params.search = query.search;
  return params;
}

export const employeeService = {
  async list(query: EmployeeQuery): Promise<PageResponse<Employee>> {
    const { data } = await api.get<PageResponse<Employee>>('/employees', { params: toParams(query) });
    return data;
  },

  async get(id: number): Promise<Employee> {
    const { data } = await api.get<Employee>(`/employees/${id}`);
    return data;
  },

  async create(values: EmployeeFormValues): Promise<Employee> {
    const { data } = await api.post<Employee>('/employees', values);
    return data;
  },

  async update(id: number, values: EmployeeFormValues): Promise<Employee> {
    const { data } = await api.put<Employee>(`/employees/${id}`, values);
    return data;
  },

  async remove(id: number): Promise<void> {
    await api.delete(`/employees/${id}`);
  },
};

export const departmentService = {
  async list(): Promise<Department[]> {
    const { data } = await api.get<Department[]>('/departments');
    return data;
  },

  async create(values: DepartmentRequest): Promise<Department> {
    const { data } = await api.post<Department>('/departments', values);
    return data;
  },

  async update(id: number, values: DepartmentRequest): Promise<Department> {
    const { data } = await api.put<Department>(`/departments/${id}`, values);
    return data;
  },

  async remove(id: number): Promise<void> {
    await api.delete(`/departments/${id}`);
  },
};
