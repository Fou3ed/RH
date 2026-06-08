/** Public projection of a user (mirrors backend UserSummary — never carries a password). */
export interface UserSummary {
  id: number;
  username: string;
  email: string;
  employeeId: number | null;
  status: string;
  roles: string[];
  permissions: string[];
}

/** Payload for POST /users. */
export interface CreateUserRequest {
  username: string;
  email: string;
  password: string;
  employeeId?: number | null;
  roles: string[];
}

/** Payload for PUT /users/{id}/roles. */
export interface UpdateUserRolesRequest {
  roles: string[];
}

/** Seeded role codes (no roles endpoint exists; these match db migration V6). */
export const ROLE_CODES = [
  'ADMIN',
  'HR_MANAGER',
  'FINANCE_MANAGER',
  'MANAGER',
  'EMPLOYEE',
] as const;

export type RoleCode = (typeof ROLE_CODES)[number];
