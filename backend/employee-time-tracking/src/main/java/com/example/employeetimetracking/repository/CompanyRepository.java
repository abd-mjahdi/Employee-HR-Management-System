package com.example.employeetimetracking.repository;

import com.example.employeetimetracking.model.entities.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {
    Optional<Company> findBySlugIgnoreCase(String slug);

    boolean existsBySlugIgnoreCase(String slug);
}
