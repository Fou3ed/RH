import { api } from '@/services/api';
import type { ImportCommitResult, ImportPreview } from '@/types/import';

function formData(file: File): FormData {
  const fd = new FormData();
  fd.append('file', file);
  return fd;
}

export const importService = {
  async preview(file: File): Promise<ImportPreview> {
    const { data } = await api.post<ImportPreview>('/employees/import/preview', formData(file), {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return data;
  },

  /**
   * Commit the import. The backend returns 200 with a commit result on success, or
   * 400 with the {@link ImportPreview} (per-row errors) if any row is invalid — the
   * caller distinguishes via the thrown AxiosError's response.
   */
  async commit(file: File): Promise<ImportCommitResult> {
    const { data } = await api.post<ImportCommitResult>('/employees/import', formData(file), {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return data;
  },
};
