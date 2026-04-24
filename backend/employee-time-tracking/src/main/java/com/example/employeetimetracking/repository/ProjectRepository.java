package com.example.employeetimetracking.repository;

import com.example.employeetimetracking.model.entities.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    Optional<Project> findByProjectCode(String projectCode);
    List<Project> findByIsActive(Boolean isActive);
    boolean existsByProjectCode(String projectCode);
}