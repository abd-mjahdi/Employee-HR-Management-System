package com.example.employeetimetracking.model.entities;

import com.example.employeetimetracking.model.enums.CompanyStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * JPA mapping is added in Phase 3 after Liquibase 005. Not an {@code @Entity} yet so
 * {@code ddl-auto: validate} continues to match the current schema.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Company {
    private Long id;
    private String name;
    private String slug;
    private CompanyStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
