import { api } from '@/services/api';
import type {
  AttendanceImportPreview,
  AttendanceRecord,
  AttendanceReportRow,
  AttendanceSummary,
} from '@/types/attendance';

export interface RecordAttendancePayload {
  employeeId: number;
  attendanceDate: string;
  status: string;
  hoursWorked?: number;
  notes?: string;
  absenceReason?: string;
}

export const attendanceService = {
  async summary(employeeId: number, year: number, month: number): Promise<AttendanceSummary> {
    const { data } = await api.get<AttendanceSummary>(`/attendance/employee/${employeeId}/summary`, {
      params: { year, month },
    });
    return data;
  },

  async record(payload: RecordAttendancePayload): Promise<AttendanceRecord> {
    const { data } = await api.post<AttendanceRecord>('/attendance', payload);
    return data;
  },

  async update(
    id: number,
    payload: { status: string; hoursWorked?: number; notes?: string; absenceReason?: string },
  ): Promise<AttendanceRecord> {
    const { data } = await api.put<AttendanceRecord>(`/attendance/${id}`, payload);
    return data;
  },

  async remove(id: number): Promise<void> {
    await api.delete(`/attendance/${id}`);
  },

  async report(year: number, month: number, departmentId?: number | null): Promise<AttendanceReportRow[]> {
    const params: Record<string, unknown> = { year, month };
    if (departmentId) params.department_id = departmentId;
    const { data } = await api.get<AttendanceReportRow[]>('/attendance/report', { params });
    return data;
  },

  async importPreview(file: File, year: number, month: number): Promise<AttendanceImportPreview> {
    const fd = new FormData();
    fd.append('file', file);
    const { data } = await api.post<AttendanceImportPreview>('/attendance/import/preview', fd, {
      params: { year, month },
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return data;
  },

  async importCommit(file: File, year: number, month: number): Promise<{ imported: number; message: string }> {
    const fd = new FormData();
    fd.append('file', file);
    const { data } = await api.post('/attendance/import', fd, {
      params: { year, month },
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return data;
  },
};
