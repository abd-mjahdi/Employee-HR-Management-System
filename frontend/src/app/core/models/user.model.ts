import { UserRole } from './auth.model';

export interface UserResponse {
  id: number;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  userRole: UserRole;
  departmentId: number | null;
  managerId: number | null;
  isActive: boolean;
}

export interface UserCreatedResponse {
  userResponseDto: UserResponse;
  temporaryPass: string;
}

export interface CreateUserRequest {
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  userRole: UserRole;
  departmentId: number;
  managerMembershipId?: number | null;
}

export interface UserUpdateRequest {
  firstName: string;
  lastName: string;
}

export interface UserWriteRequest {
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  userRole: UserRole;
  departmentId: number;
  managerMembershipId?: number | null;
}
