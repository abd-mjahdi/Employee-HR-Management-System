export interface Project {
  id: number;
  projectName: string;
  projectCode: string;
  description: string | null;
  isActive: boolean;
}

export interface CreateProjectRequest {
  projectName: string;
  projectCode: string;
  description?: string | null;
}

export interface UpdateProjectRequest {
  projectName?: string;
  projectCode?: string;
  description?: string | null;
  isActive?: boolean;
}
