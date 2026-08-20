import { Injectable } from '@angular/core';
import { apiBaseUrlForHost, parseTenantSlug } from './tenant.util';

@Injectable({
  providedIn: 'root'
})
export class TenantService {
  readonly slug: string | null;
  readonly apiBaseUrl: string;
  readonly hasTenant: boolean;

  constructor() {
    const hostname = window.location.hostname;
    this.slug = parseTenantSlug(hostname);
    this.hasTenant = this.slug != null;
    this.apiBaseUrl = this.hasTenant
      ? apiBaseUrlForHost(hostname, window.location.protocol)
      : '';
  }

  url(path: string): string {
    const suffix = path.startsWith('/') ? path : `/${path}`;
    return `${this.apiBaseUrl}${suffix}`;
  }
}
