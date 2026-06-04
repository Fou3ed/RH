export interface EmployeeDocument {
  id: number;
  employeeId: number;
  documentType: string;
  fileName: string;
  contentType: string | null;
  fileSize: number | null;
  description: string | null;
  uploadedBy: string | null;
  createdAt: string;
}

export const DOCUMENT_TYPES = ['CONTRACT', 'CERTIFICATION', 'ID', 'OTHER'] as const;
