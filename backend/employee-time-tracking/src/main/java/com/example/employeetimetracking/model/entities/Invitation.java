package com.example.employeetimetracking.model.entities;

import com.example.employeetimetracking.model.enums.InvitationStatus;
import com.example.employeetimetracking.model.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * JPA mapping is added in Phase 3 after Liquibase 005. Store {@code tokenHash} only; never persist the raw token.
 * Not an {@code @Entity} yet so {@code ddl-auto: validate} continues to match the current schema.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Invitation {
    private Long id;
    private Company company;
    private String email;
    private UserRole role;
    private Department department;
    private CompanyMembership managerMembership;
    private String tokenHash;
    private InvitationStatus status;
    private CompanyMembership invitedByMembership;
    private LocalDateTime expiresAt;
    private LocalDateTime acceptedAt;
    private User acceptedUser;
    private LocalDateTime createdAt;
}
