export const ATTENDANCE_STATUSES = [
  'PRESENT',
  'HALF_DAY',
  'ABSENT',
  'LEAVE',
  'HOLIDAY',
  'WEEKEND',
] as const;

export type AttendanceStatus = (typeof ATTENDANCE_STATUSES)[number];

export interface AttendanceRecord {
  id: number;
  employeeId: number;
  employeeName: string;
  attendanceDate: string;
  attendanceStatus: AttendanceStatus;
  attendanceCode: string | null;
  daysFraction: number | null;
  hoursWorked: number | null;
  notes: string | null;
  absenceReason: string | null;
  paidLeave: boolean;
}

export interface AttendanceSummary {
  employeeId: number;
  employeeName: string;
  year: number;
  month: number;
  presentDays: number;
  halfDays: number;
  absentDays: number;
  leaveDays: number;
  daysWorked: number;
  attendanceRate: number;
  records: AttendanceRecord[];
}

export interface AttendanceReportRow {
  employeeId: number;
  employeeCode: string;
  employeeName: string;
  departmentName: string | null;
  presentDays: number;
  halfDays: number;
  absentDays: number;
  leaveDays: number;
  daysWorked: number;
  attendanceRate: number;
}

export interface AttendanceImportRowResult {
  rowNumber: number;
  employeeId: string;
  recognizedDays: number;
  valid: boolean;
  errors: string[];
}

export interface AttendanceImportPreview {
  year: number;
  month: number;
  totalRows: number;
  validRows: number;
  invalidRows: number;
  totalRecords: number;
  rows: AttendanceImportRowResult[];
}

/** Status → MUI palette colour for calendar cells. */
export const STATUS_COLOR: Record<AttendanceStatus, string> = {
  PRESENT: '#c8e6c9',
  HALF_DAY: '#fff9c4',
  ABSENT: '#ffcdd2',
  LEAVE: '#bbdefb',
  HOLIDAY: '#e1bee7',
  WEEKEND: '#eceff1',
};
