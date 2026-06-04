export interface AuthUser {
  id: number;
  username: string;
  email: string;
  employeeId: number | null;
  status: string;
  roles: string[];
  permissions: string[];
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  refreshToken: string;
  expiresIn: number;
  user: AuthUser;
}

export interface RefreshResponse {
  token: string;
  refreshToken: string;
  expiresIn: number;
}
