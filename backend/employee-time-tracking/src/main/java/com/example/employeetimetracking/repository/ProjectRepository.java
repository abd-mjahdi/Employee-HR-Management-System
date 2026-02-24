package com.example.employeetimetracking.repository;

import com.example.employeetimetracking.model.entities.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    Project findByProjectCode(String projectCode);
    List<Project> findByIsActive(Boolean isActive);
}