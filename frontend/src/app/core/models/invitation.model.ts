import { UserRole } from './auth.model';

export interface InvitationCreated {
  id: number;
  email: string;
  role: UserRole;
  expiresAt: string;
  token: string;
}

export interface CreateInvitationRequest {
  email: string;
  role: UserRole;
  departmentId: number;
  managerMembershipId?: number | null;
}
