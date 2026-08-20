package com.example.employeetimetracking.repository;

import com.example.employeetimetracking.model.entities.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    Optional<Project> findByIdAndCompanyId(Long id, Long companyId);

    List<Project> findAllByCompanyId(Long companyId);

    List<Project> findByCompanyIdAndIsActive(Long companyId, Boolean isActive);

    Optional<Project> findByCompanyIdAndProjectCode(Long companyId, String projectCode);

    boolean existsByCompanyIdAndProjectCode(Long companyId, String projectCode);

    boolean existsByCompanyIdAndProjectCodeAndIdNot(Long companyId, String projectCode, Long id);
}
