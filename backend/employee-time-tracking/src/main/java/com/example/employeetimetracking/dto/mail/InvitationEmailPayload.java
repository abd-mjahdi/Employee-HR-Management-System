package com.example.employeetimetracking.dto.mail;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record InvitationEmailPayload(
        String companyName,
        String companySlug,
        String recipient,
        String rawToken,
        String expiresAt
) {
    public InvitationEmailPayload withoutToken() {
        return new InvitationEmailPayload(companyName, companySlug, recipient, null, expiresAt);
    }
}
