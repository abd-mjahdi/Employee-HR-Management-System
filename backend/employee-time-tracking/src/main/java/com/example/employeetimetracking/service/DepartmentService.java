package com.example.employeetimetracking.service;

import com.example.employeetimetracking.dto.response.DepartmentDto;
import com.example.employeetimetracking.exception.DepartmentNotFoundException;
import com.example.employeetimetracking.model.entities.Department;
import com.example.employeetimetracking.repository.DepartmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DepartmentService {
    private final DepartmentRepository departmentRepository;
    @Autowired
    public DepartmentService(DepartmentRepository departmentRepository){
        this.departmentRepository = departmentRepository;
    }

    public Department getById(Long id){
        return departmentRepository.findById(id).orElseThrow(() -> new DepartmentNotFoundException("Department does not exist"));
    }

    public Department getByDepartmentCode(String code){
        return departmentRepository.findByDepartmentCode(code).orElseThrow(()-> new DepartmentNotFoundException("Department doesn't exist"));
    }

    private DepartmentDto convertToDto(Department dep){
        return new DepartmentDto(dep.getId(),
                dep.getDepartmentName(),
                dep.getDepartmentCode(),
                dep.getIsActive());
    }

    public List<DepartmentDto> getAllDepartments(){
        return departmentRepository.findAll().stream().map(this::convertToDto).toList();
    }

    public DepartmentDto getDepartmentById(Long id){
        Department dep = departmentRepository.findById(id).orElseThrow(()-> new DepartmentNotFoundException("Department " + id + " does not exist"));
        return convertToDto(dep);
    }


}
