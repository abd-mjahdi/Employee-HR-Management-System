package com.example.employeetimetracking.mapper;

import com.example.employeetimetracking.dto.response.DepartmentDto;
import com.example.employeetimetracking.model.entities.Department;
import org.springframework.stereotype.Component;

@Component
public class DepartmentMapper {
    public DepartmentDto toDto(Department dep){
        return new DepartmentDto(dep.getId(),
                dep.getDepartmentName(),
                dep.getDepartmentCode(),
                dep.getIsActive());
    }
}
