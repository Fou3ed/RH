import { api } from '@/services/api';

/** Fetches a file as a blob (with the auth header) and triggers a browser download. */
async function downloadBlob(url: string, fallbackName: string, params?: Record<string, unknown>): Promise<void> {
  const response = await api.get(url, { params, responseType: 'blob' });

  // Prefer the server-provided filename from Content-Disposition.
  const disposition = response.headers['content-disposition'] as string | undefined;
  const match = disposition?.match(/filename="?([^"]+)"?/);
  const fileName = match?.[1] ?? fallbackName;

  const blobUrl = window.URL.createObjectURL(response.data as Blob);
  const link = document.createElement('a');
  link.href = blobUrl;
  link.download = fileName;
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(blobUrl);
}

export const reportService = {
  payslip: (payrollId: number) => downloadBlob(`/reports/payslip/${payrollId}`, `payslip_${payrollId}.pdf`),
  payrollExcel: (periodId: number) =>
    downloadBlob('/reports/payroll/export', `payroll_${periodId}.xlsx`, { periodId }),
  irppCsv: (periodId: number) => downloadBlob('/reports/tax/irpp', `irpp_${periodId}.csv`, { periodId }),
  cnssCsv: (periodId: number) => downloadBlob('/reports/tax/cnss', `cnss_${periodId}.csv`, { periodId }),
  attendanceExcel: (year: number, month: number) =>
    downloadBlob('/reports/attendance/export', `attendance_${year}-${month}.xlsx`, { year, month }),
};
