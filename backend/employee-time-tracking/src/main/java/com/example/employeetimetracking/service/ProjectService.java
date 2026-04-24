package com.example.employeetimetracking.service;

import com.example.employeetimetracking.dto.request.CreateProjectDto;
import com.example.employeetimetracking.dto.response.ProjectDto;
import com.example.employeetimetracking.exception.ProjectCodeAlreadyExistsException;
import com.example.employeetimetracking.exception.ProjectNotFoundException;
import com.example.employeetimetracking.model.entities.Project;
import com.example.employeetimetracking.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProjectService {
    private final ProjectRepository projectRepository;

    @Autowired
    public ProjectService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    public Project getById(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ProjectNotFoundException("project not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public List<ProjectDto> listActive() {
        return projectRepository.findByIsActive(true).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectDto getByIdAsDto(Long id) {
        return toDto(getById(id));
    }

    @Transactional
    public ProjectDto create(CreateProjectDto dto) {
        String code = dto.getProjectCode() == null ? "" : dto.getProjectCode().trim();
        if (projectRepository.existsByProjectCode(code)) {
            throw new ProjectCodeAlreadyExistsException("Project code already in use: " + code);
        }
        Project p = new Project();
        p.setProjectName(dto.getProjectName() != null ? dto.getProjectName().trim() : null);
        p.setProjectCode(code);
        p.setDescription(dto.getDescription());
        p.setIsActive(true);
        return toDto(projectRepository.save(p));
    }

    private ProjectDto toDto(Project p) {
        return new ProjectDto(
                p.getId(),
                p.getProjectName(),
                p.getProjectCode(),
                p.getDescription(),
                p.getIsActive()
        );
    }
}
