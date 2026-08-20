import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  CreateProjectRequest,
  Project,
  UpdateProjectRequest
} from '../models/project.model';
import { TenantService } from '../tenant/tenant.service';

@Injectable({
  providedIn: 'root'
})
export class ProjectService {
  private readonly http = inject(HttpClient);
  private readonly tenant = inject(TenantService);

  listActive(): Observable<Project[]> {
    return this.http.get<Project[]>(this.tenant.url('/projects/active'));
  }

  getById(id: number): Observable<Project> {
    return this.http.get<Project>(this.tenant.url(`/projects/${id}`));
  }

  create(body: CreateProjectRequest): Observable<Project> {
    return this.http.post<Project>(this.tenant.url('/projects'), body);
  }

  update(id: number, body: UpdateProjectRequest): Observable<Project> {
    return this.http.put<Project>(this.tenant.url(`/projects/${id}`), body);
  }
}
