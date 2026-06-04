import { api } from '@/services/api';
import type { SystemInfo } from '@/types/api';

/** Calls the backend system/info endpoint to confirm connectivity. */
export const systemService = {
  async getInfo(): Promise<SystemInfo> {
    const { data } = await api.get<SystemInfo>('/system/info');
    return data;
  },
};
