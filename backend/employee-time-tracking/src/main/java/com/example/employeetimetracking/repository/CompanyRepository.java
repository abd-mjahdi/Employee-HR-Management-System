package com.example.employeetimetracking.repository;

/**
 * Spring Data JPA mapping is added in Phase 3 after Liquibase 005.
 * Does not extend {@code JpaRepository} yet because {@link com.example.employeetimetracking.model.entities.Company}
 * is not a JPA entity in this phase.
 */
public interface CompanyRepository {
}
