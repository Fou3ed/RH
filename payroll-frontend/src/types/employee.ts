export interface PageResponse<T> {
  data: T[];
  total: number;
  page: number;
  limit: number;
}

export interface Department {
  id: number;
  code: string;
  name: string;
  description: string | null;
  parentId: number | null;
  parentName: string | null;
}

export interface Position {
  id: number;
  code: string;
  name: string;
  description: string | null;
  departmentId: number;
  departmentName: string;
  salaryGrade: number | null;
}

export interface Employee {
  id: number;
  employeeId: string;
  firstName: string;
  lastName: string;
  fullName: string;
  dateOfBirth: string | null;
  gender: string | null;
  hireDate: string;
  departmentId: number;
  departmentName: string;
  positionId: number | null;
  positionName: string | null;
  categoryId: number;
  categoryCode: string;
  echelon: number | null;
  baseSalary: number | null;
  familyStatus: string | null;
  numberOfChildren: number | null;
  nationalId: string | null;
  cnssNumber: string | null;
  phoneNumber: string | null;
  email: string | null;
  address: string | null;
  city: string | null;
  zipCode: string | null;
  paymentMethod: string | null;
  bankAccountNumber: string | null;
  bankCode: string | null;
  employmentStatus: string;
  terminationDate: string | null;
  terminationReason: string | null;
}

/** Shape of the create/edit form payload sent to the API. */
export interface EmployeeFormValues {
  employeeId?: string;
  firstName: string;
  lastName: string;
  hireDate: string;
  departmentId: number | '';
  positionId?: number | '';
  categoryId: number | '';
  echelon?: number | '';
  gender?: string;
  email?: string;
  cnssNumber?: string;
  baseSalary?: number | '';
  familyStatus?: string;
  numberOfChildren?: number | '';
  phoneNumber?: string;
  employmentStatus?: string;
}

export interface DepartmentRequest {
  code: string;
  name: string;
  description?: string;
  parentId?: number | null;
}

export const EMPLOYMENT_STATUSES = ['ACTIVE', 'INACTIVE', 'LEAVE', 'TERMINATED'] as const;
