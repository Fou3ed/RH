export interface ImportRowResult {
  rowNumber: number;
  employeeId: string | null;
  fullName: string | null;
  valid: boolean;
  errors: string[];
}

export interface ImportPreview {
  totalRows: number;
  validRows: number;
  invalidRows: number;
  rows: ImportRowResult[];
}

export interface ImportCommitResult {
  imported: number;
  message: string;
}
