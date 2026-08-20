package com.example.employeetimetracking.model.entities;

import com.example.employeetimetracking.model.enums.MembershipStatus;
import com.example.employeetimetracking.model.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA mapping is added in Phase 3 after Liquibase 005. Not an {@code @Entity} yet so
 * {@code ddl-auto: validate} continues to match the current schema.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CompanyMembership {
    private Long id;
    private User user;
    private Company company;
    private UserRole role;
    private MembershipStatus status;
    private Department department;
    private CompanyMembership managerMembership;
}
