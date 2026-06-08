import { api } from '@/services/api';
import type { CreateUserRequest, UpdateUserRolesRequest, UserSummary } from '@/types/user';

export const userService = {
  async list(): Promise<UserSummary[]> {
    const { data } = await api.get<UserSummary[]>('/users');
    return data;
  },

  async get(id: number): Promise<UserSummary> {
    const { data } = await api.get<UserSummary>(`/users/${id}`);
    return data;
  },

  async create(values: CreateUserRequest): Promise<UserSummary> {
    const { data } = await api.post<UserSummary>('/users', values);
    return data;
  },

  async updateRoles(id: number, values: UpdateUserRolesRequest): Promise<UserSummary> {
    const { data } = await api.put<UserSummary>(`/users/${id}/roles`, values);
    return data;
  },

  async deactivate(id: number): Promise<UserSummary> {
    const { data } = await api.post<UserSummary>(`/users/${id}/deactivate`);
    return data;
  },
};
