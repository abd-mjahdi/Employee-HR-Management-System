package com.example.employeetimetracking.repository;

import com.example.employeetimetracking.model.entities.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
    Optional<Department> findByIdAndCompanyId(Long id, Long companyId);

    List<Department> findAllByCompanyId(Long companyId);

    List<Department> findByCompanyIdAndIsActive(Long companyId, Boolean isActive);

    Optional<Department> findByCompanyIdAndDepartmentCode(Long companyId, String departmentCode);

    boolean existsByCompanyIdAndDepartmentCode(Long companyId, String departmentCode);

    boolean existsByCompanyIdAndDepartmentName(Long companyId, String departmentName);

    boolean existsByCompanyIdAndDepartmentCodeAndIdNot(Long companyId, String departmentCode, Long id);

    boolean existsByCompanyIdAndDepartmentNameAndIdNot(Long companyId, String departmentName, Long id);
}
