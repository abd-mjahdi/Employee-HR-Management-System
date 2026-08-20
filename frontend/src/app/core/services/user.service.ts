import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { UserRole } from '../models/auth.model';
import { SpringPage } from '../models/page.model';
import {
  CreateUserRequest,
  UserCreatedResponse,
  UserResponse,
  UserUpdateRequest,
  UserWriteRequest
} from '../models/user.model';
import { TenantService } from '../tenant/tenant.service';

export interface UserSearchParams {
  departmentId?: number;
  role?: UserRole;
  active?: boolean;
  name?: string;
}

export interface UserPageParams {
  page?: number;
  size?: number;
  sort?: string;
}

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private readonly http = inject(HttpClient);
  private readonly tenant = inject(TenantService);

  getPage(params?: UserPageParams): Observable<SpringPage<UserResponse>> {
    let httpParams = new HttpParams();
    if (params?.page != null) {
      httpParams = httpParams.set('page', String(params.page));
    }
    if (params?.size != null) {
      httpParams = httpParams.set('size', String(params.size));
    }
    if (params?.sort) {
      httpParams = httpParams.set('sort', params.sort);
    }
    return this.http.get<SpringPage<UserResponse>>(this.tenant.url('/users'), { params: httpParams });
  }

  getById(id: number): Observable<UserResponse> {
    return this.http.get<UserResponse>(this.tenant.url(`/users/${id}`));
  }

  getMe(): Observable<UserResponse> {
    return this.http.get<UserResponse>(this.tenant.url('/users/me'));
  }

  getTeam(): Observable<UserResponse[]> {
    return this.http.get<UserResponse[]>(this.tenant.url('/users/team'));
  }

  search(filters?: UserSearchParams): Observable<UserResponse[]> {
    let httpParams = new HttpParams();
    if (filters?.departmentId != null) {
      httpParams = httpParams.set('departmentId', String(filters.departmentId));
    }
    if (filters?.role) {
      httpParams = httpParams.set('role', filters.role);
    }
    if (filters?.active != null) {
      httpParams = httpParams.set('active', String(filters.active));
    }
    if (filters?.name) {
      httpParams = httpParams.set('name', filters.name);
    }
    return this.http.get<UserResponse[]>(this.tenant.url('/users/search'), { params: httpParams });
  }

  create(body: CreateUserRequest): Observable<UserCreatedResponse> {
    return this.http.post<UserCreatedResponse>(this.tenant.url('/users'), body);
  }

  update(id: number, body: UserWriteRequest): Observable<UserResponse> {
    return this.http.put<UserResponse>(this.tenant.url(`/users/${id}`), body);
  }

  deactivate(id: number): Observable<void> {
    return this.http.patch<void>(this.tenant.url(`/users/${id}/deactivate`), null);
  }

  activate(id: number): Observable<void> {
    return this.http.patch<void>(this.tenant.url(`/users/${id}/activate`), null);
  }

  updateProfile(body: UserUpdateRequest): Observable<void> {
    return this.http.patch<void>(this.tenant.url('/users/me/profile'), body);
  }
}
