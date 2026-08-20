package com.example.employeetimetracking.repository;

import com.example.employeetimetracking.model.entities.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
    Optional<Department> findByIdAndCompanyId(Long id, Long companyId);

    List<Department> findAllByCompanyId(Long companyId);

    Optional<Department> findByCompanyIdAndDepartmentCode(Long companyId, String departmentCode);

    List<Department> findByCompanyIdAndIsActive(Long companyId, Boolean isActive);

    Optional<Department> findByDepartmentCode(String departmentCode);

    List<Department> findByIsActive(Boolean isActive);

    Department findByDepartmentName(String departmentName);
}
