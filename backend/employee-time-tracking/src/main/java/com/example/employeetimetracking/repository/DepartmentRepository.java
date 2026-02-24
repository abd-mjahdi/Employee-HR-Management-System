package com.example.employeetimetracking.repository;

import com.example.employeetimetracking.model.entities.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DepartmentRepository extends JpaRepository<Department,Long> {
    Department findByDepartmentCode(String departmentCode);
    List<Department> findByIsActive(Boolean isActive);
    Department findByDepartmentName(String departmentName);
}
