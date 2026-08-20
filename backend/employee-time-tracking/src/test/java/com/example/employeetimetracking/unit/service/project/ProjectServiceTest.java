package com.example.employeetimetracking.unit.service.project;

import com.example.employeetimetracking.dto.request.CreateProjectDto;
import com.example.employeetimetracking.dto.request.UpdateProjectDto;
import com.example.employeetimetracking.dto.response.ProjectDto;
import com.example.employeetimetracking.exception.ProjectCodeAlreadyExistsException;
import com.example.employeetimetracking.exception.ProjectNotFoundException;
import com.example.employeetimetracking.model.entities.Company;
import com.example.employeetimetracking.model.entities.Project;
import com.example.employeetimetracking.model.enums.CompanyStatus;
import com.example.employeetimetracking.repository.CompanyRepository;
import com.example.employeetimetracking.repository.ProjectRepository;
import com.example.employeetimetracking.service.ProjectService;
import com.example.employeetimetracking.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    ProjectRepository projectRepository;
    @Mock
    CompanyRepository companyRepository;

    ProjectService projectService;

    Company company;
    Project alpha;

    @BeforeEach
    void setUp() {
        TenantContext.set(new TenantContext.TenantInfo(1L, "acme", CompanyStatus.ACTIVE));
        projectService = new ProjectService(projectRepository, companyRepository);

        company = new Company();
        company.setId(1L);
        company.setSlug("acme");
        company.setStatus(CompanyStatus.ACTIVE);

        alpha = project(10L, "Alpha", "ALP", true);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void list_usesCurrentCompanyOnly() {
        when(projectRepository.findByCompanyIdAndIsActive(1L, true)).thenReturn(List.of(alpha));

        List<ProjectDto> result = projectService.listActive();

        assertEquals(1, result.size());
        assertEquals("ALP", result.get(0).getProjectCode());
        verify(projectRepository).findByCompanyIdAndIsActive(1L, true);
        verify(projectRepository, never()).findAll();
        verify(projectRepository, never()).findAllByCompanyId(any());
    }

    @Test
    void getById_missingInCurrentCompany_notFound() {
        when(projectRepository.findByIdAndCompanyId(99L, 1L)).thenReturn(Optional.empty());

        assertThrows(ProjectNotFoundException.class, () -> projectService.getByIdAsDto(99L));
        verify(projectRepository, never()).findById(99L);
    }

    @Test
    void create_bindsCompanyFromContext() {
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(projectRepository.existsByCompanyIdAndProjectCode(1L, "BETA")).thenReturn(false);
        when(projectRepository.save(any(Project.class))).thenAnswer(inv -> {
            Project saved = inv.getArgument(0);
            saved.setId(20L);
            return saved;
        });

        ProjectDto dto = projectService.create(new CreateProjectDto("  Beta ", " BETA ", "desc"));

        assertEquals(20L, dto.getId());
        assertEquals("Beta", dto.getProjectName());
        assertEquals("BETA", dto.getProjectCode());
        assertTrue(dto.getIsActive());

        ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);
        verify(projectRepository).save(captor.capture());
        assertEquals(1L, captor.getValue().getCompany().getId());
    }

    @Test
    void create_duplicateCodeInSameCompany_conflict() {
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(projectRepository.existsByCompanyIdAndProjectCode(1L, "ALP")).thenReturn(true);

        assertThrows(ProjectCodeAlreadyExistsException.class,
                () -> projectService.create(new CreateProjectDto("Alpha 2", "ALP", null)));
        verify(projectRepository, never()).save(any());
    }

    @Test
    void create_sameCodeAllowedIfUniquenessIsPerCompany() {
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(projectRepository.existsByCompanyIdAndProjectCode(1L, "ALP")).thenReturn(false);
        when(projectRepository.save(any(Project.class))).thenAnswer(inv -> inv.getArgument(0));

        projectService.create(new CreateProjectDto("Platform", "ALP", null));

        verify(projectRepository).existsByCompanyIdAndProjectCode(1L, "ALP");
        verify(projectRepository, never()).existsByCompanyIdAndProjectCode(2L, "ALP");
    }

    @Test
    void update_wrongCompanyId_notFound() {
        when(projectRepository.findByIdAndCompanyId(10L, 1L)).thenReturn(Optional.empty());

        assertThrows(ProjectNotFoundException.class,
                () -> projectService.update(10L, new UpdateProjectDto("X", "X", null, null)));
        verify(projectRepository, never()).findById(10L);
    }

    @Test
    void update_duplicateCodeInSameCompany_conflict() {
        when(projectRepository.findByIdAndCompanyId(10L, 1L)).thenReturn(Optional.of(alpha));
        when(projectRepository.existsByCompanyIdAndProjectCodeAndIdNot(1L, "BETA", 10L)).thenReturn(true);

        UpdateProjectDto dto = new UpdateProjectDto();
        dto.setProjectCode("BETA");

        assertThrows(ProjectCodeAlreadyExistsException.class, () -> projectService.update(10L, dto));
    }

    @Test
    void update_deactivatesInCurrentCompany() {
        when(projectRepository.findByIdAndCompanyId(10L, 1L)).thenReturn(Optional.of(alpha));
        when(projectRepository.existsByCompanyIdAndProjectCodeAndIdNot(1L, "ALP", 10L)).thenReturn(false);

        UpdateProjectDto dto = new UpdateProjectDto();
        dto.setIsActive(false);

        ProjectDto result = projectService.update(10L, dto);

        assertFalse(result.getIsActive());
        assertFalse(alpha.getIsActive());
        assertEquals("ALP", result.getProjectCode());
    }

    private Project project(Long id, String name, String code, boolean active) {
        Project project = new Project();
        project.setId(id);
        project.setCompany(company);
        project.setProjectName(name);
        project.setProjectCode(code);
        project.setIsActive(active);
        return project;
    }
}
