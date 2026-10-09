package com.example.employeetimetracking.event;

public record InvitationCreatedEvent(
        String companyName,
        String companySlug,
        String recipient,
        String rawToken
) {}
