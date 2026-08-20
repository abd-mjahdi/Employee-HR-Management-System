package com.example.employeetimetracking.controller;

import com.example.employeetimetracking.dto.request.CreateDepartmentDto;
import com.example.employeetimetracking.dto.request.UpdateDepartmentDto;
import com.example.employeetimetracking.dto.response.DepartmentDto;
import com.example.employeetimetracking.service.DepartmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/departments")
public class DepartmentController {
    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @GetMapping
    public ResponseEntity<List<DepartmentDto>> getAllDepartments() {
        return ResponseEntity.ok(departmentService.getAllDepartments());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DepartmentDto> getDepartment(@PathVariable Long id) {
        return ResponseEntity.ok(departmentService.getDepartmentById(id));
    }

    @PreAuthorize("hasRole('HR_ADMIN')")
    @PostMapping
    public ResponseEntity<DepartmentDto> create(@Valid @RequestBody CreateDepartmentDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(departmentService.create(request));
    }

    @PreAuthorize("hasRole('HR_ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<DepartmentDto> update(@PathVariable Long id,
                                                @Valid @RequestBody UpdateDepartmentDto request) {
        return ResponseEntity.ok(departmentService.update(id, request));
    }
}
