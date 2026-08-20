export interface Department {
  id: number;
  departmentName: string;
  departmentCode: string;
  isActive: boolean;
}

export interface CreateDepartmentRequest {
  departmentName: string;
  departmentCode: string;
}

export interface UpdateDepartmentRequest {
  departmentName?: string;
  departmentCode?: string;
  isActive?: boolean;
}
