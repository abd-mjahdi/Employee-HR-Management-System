export type UserRole = 'EMPLOYEE' | 'MANAGER' | 'HR_ADMIN';

export interface User {
  id?: number;
  email: string;
  firstName?: string;
  lastName?: string;
  role: UserRole;
  departmentId?: number;
  departmentName?: string;
  active?: boolean;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  success: boolean;
  message: string;
  token: string | null;
  email: string | null;
  role: UserRole | null;
  companySlug?: string | null;
  companyName?: string | null;
}
