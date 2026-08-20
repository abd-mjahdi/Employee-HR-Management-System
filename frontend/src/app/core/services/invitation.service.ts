import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CreateInvitationRequest, InvitationCreated } from '../models/invitation.model';
import { TenantService } from '../tenant/tenant.service';

@Injectable({
  providedIn: 'root'
})
export class InvitationService {
  private readonly http = inject(HttpClient);
  private readonly tenant = inject(TenantService);

  create(body: CreateInvitationRequest): Observable<InvitationCreated> {
    return this.http.post<InvitationCreated>(this.tenant.url('/invitations'), body);
  }
}
