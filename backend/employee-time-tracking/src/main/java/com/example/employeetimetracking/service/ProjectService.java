package com.example.employeetimetracking.service;

import com.example.employeetimetracking.dto.request.CreateProjectDto;
import com.example.employeetimetracking.dto.request.UpdateProjectDto;
import com.example.employeetimetracking.dto.response.ProjectDto;
import com.example.employeetimetracking.exception.InvalidTenantException;
import com.example.employeetimetracking.exception.ProjectCodeAlreadyExistsException;
import com.example.employeetimetracking.exception.ProjectNotFoundException;
import com.example.employeetimetracking.model.entities.Company;
import com.example.employeetimetracking.model.entities.Project;
import com.example.employeetimetracking.repository.CompanyRepository;
import com.example.employeetimetracking.repository.ProjectRepository;
import com.example.employeetimetracking.tenant.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final CompanyRepository companyRepository;

    @Autowired
    public ProjectService(ProjectRepository projectRepository, CompanyRepository companyRepository) {
        this.projectRepository = projectRepository;
        this.companyRepository = companyRepository;
    }

    public Project getById(Long id) {
        return projectRepository.findByIdAndCompanyId(id, currentCompanyId())
                .orElseThrow(() -> new ProjectNotFoundException("project not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public List<ProjectDto> listActive() {
        return projectRepository.findByCompanyIdAndIsActive(currentCompanyId(), true).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectDto getByIdAsDto(Long id) {
        return toDto(getById(id));
    }

    @Transactional
    public ProjectDto create(CreateProjectDto dto) {
        Long companyId = currentCompanyId();
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new InvalidTenantException("Tenant not found"));

        String name = normalize(dto.getProjectName());
        String code = normalize(dto.getProjectCode());
        assertUniqueCode(companyId, code, null);

        Project project = new Project();
        project.setCompany(company);
        project.setProjectName(name);
        project.setProjectCode(code);
        project.setDescription(dto.getDescription());
        project.setIsActive(true);
        return toDto(projectRepository.save(project));
    }

    @Transactional
    public ProjectDto update(Long id, UpdateProjectDto request) {
        Project project = getById(id);
        Long companyId = currentCompanyId();

        String name = request.getProjectName() == null
                ? project.getProjectName()
                : normalize(request.getProjectName());
        String code = request.getProjectCode() == null
                ? project.getProjectCode()
                : normalize(request.getProjectCode());
        if (name.isBlank() || code.isBlank()) {
            throw new ProjectCodeAlreadyExistsException("Project name and code are required");
        }
        assertUniqueCode(companyId, code, project.getId());

        project.setProjectName(name);
        project.setProjectCode(code);
        if (request.getDescription() != null) {
            project.setDescription(request.getDescription());
        }
        if (request.getIsActive() != null) {
            project.setIsActive(request.getIsActive());
        }
        return toDto(project);
    }

    private void assertUniqueCode(Long companyId, String code, Long excludeId) {
        boolean codeTaken = excludeId == null
                ? projectRepository.existsByCompanyIdAndProjectCode(companyId, code)
                : projectRepository.existsByCompanyIdAndProjectCodeAndIdNot(companyId, code, excludeId);
        if (codeTaken) {
            throw new ProjectCodeAlreadyExistsException("Project code already in use: " + code);
        }
    }

    private ProjectDto toDto(Project project) {
        return new ProjectDto(
                project.getId(),
                project.getProjectName(),
                project.getProjectCode(),
                project.getDescription(),
                project.getIsActive()
        );
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static Long currentCompanyId() {
        return TenantContext.require().companyId();
    }
}
