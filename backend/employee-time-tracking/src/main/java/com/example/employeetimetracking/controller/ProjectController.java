package com.example.employeetimetracking.controller;

import com.example.employeetimetracking.dto.request.CreateProjectDto;
import com.example.employeetimetracking.dto.response.ProjectDto;
import com.example.employeetimetracking.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/projects")
public class ProjectController {
    private final ProjectService projectService;

    @Autowired
    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping("/active")
    public ResponseEntity<List<ProjectDto>> listActive() {
        return ResponseEntity.ok(projectService.listActive());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(projectService.getByIdAsDto(id));
    }

    @PreAuthorize("hasRole('HR_ADMIN')")
    @PostMapping
    public ResponseEntity<ProjectDto> create(@Valid @RequestBody CreateProjectDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.create(request));
    }
}
