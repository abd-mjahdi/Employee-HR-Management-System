import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  CreateDepartmentRequest,
  Department,
  UpdateDepartmentRequest
} from '../models/department.model';
import { TenantService } from '../tenant/tenant.service';

@Injectable({
  providedIn: 'root'
})
export class DepartmentService {
  private readonly http = inject(HttpClient);
  private readonly tenant = inject(TenantService);

  list(): Observable<Department[]> {
    return this.http.get<Department[]>(this.tenant.url('/departments'));
  }

  getById(id: number): Observable<Department> {
    return this.http.get<Department>(this.tenant.url(`/departments/${id}`));
  }

  create(body: CreateDepartmentRequest): Observable<Department> {
    return this.http.post<Department>(this.tenant.url('/departments'), body);
  }

  update(id: number, body: UpdateDepartmentRequest): Observable<Department> {
    return this.http.put<Department>(this.tenant.url(`/departments/${id}`), body);
  }
}
