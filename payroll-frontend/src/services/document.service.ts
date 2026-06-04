import { api } from '@/services/api';
import type { EmployeeDocument } from '@/types/document';

export const documentService = {
  async list(employeeId: number): Promise<EmployeeDocument[]> {
    const { data } = await api.get<EmployeeDocument[]>(`/employees/${employeeId}/documents`);
    return data;
  },

  async upload(
    employeeId: number,
    file: File,
    type: string,
    description?: string,
  ): Promise<EmployeeDocument> {
    const fd = new FormData();
    fd.append('file', file);
    fd.append('type', type);
    if (description) fd.append('description', description);
    const { data } = await api.post<EmployeeDocument>(`/employees/${employeeId}/documents`, fd, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return data;
  },

  /** Fetches the file as a blob (with the auth header) and triggers a browser download. */
  async download(doc: EmployeeDocument): Promise<void> {
    const response = await api.get(`/documents/${doc.id}/download`, { responseType: 'blob' });
    const url = window.URL.createObjectURL(response.data as Blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = doc.fileName;
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
  },

  async remove(documentId: number): Promise<void> {
    await api.delete(`/documents/${documentId}`);
  },
};
