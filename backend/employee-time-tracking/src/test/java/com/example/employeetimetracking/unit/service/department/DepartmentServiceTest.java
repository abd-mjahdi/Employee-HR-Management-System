package com.example.employeetimetracking.unit.service.department;

import com.example.employeetimetracking.dto.request.CreateDepartmentDto;
import com.example.employeetimetracking.dto.request.UpdateDepartmentDto;
import com.example.employeetimetracking.dto.response.DepartmentDto;
import com.example.employeetimetracking.exception.DepartmentAlreadyExistsException;
import com.example.employeetimetracking.exception.DepartmentNotFoundException;
import com.example.employeetimetracking.mapper.DepartmentMapper;
import com.example.employeetimetracking.model.entities.Company;
import com.example.employeetimetracking.model.entities.Department;
import com.example.employeetimetracking.model.enums.CompanyStatus;
import com.example.employeetimetracking.repository.CompanyRepository;
import com.example.employeetimetracking.repository.DepartmentRepository;
import com.example.employeetimetracking.service.DepartmentService;
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
class DepartmentServiceTest {

    @Mock
    DepartmentRepository departmentRepository;
    @Mock
    CompanyRepository companyRepository;

    DepartmentService departmentService;

    Company company;
    Department engineering;

    @BeforeEach
    void setUp() {
        TenantContext.set(new TenantContext.TenantInfo(1L, "acme", CompanyStatus.ACTIVE));
        departmentService = new DepartmentService(departmentRepository, companyRepository, new DepartmentMapper());

        company = new Company();
        company.setId(1L);
        company.setSlug("acme");
        company.setStatus(CompanyStatus.ACTIVE);

        engineering = department(10L, "Engineering", "ENG", true);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void list_usesCurrentCompanyOnly() {
        when(departmentRepository.findAllByCompanyId(1L)).thenReturn(List.of(engineering));

        List<DepartmentDto> result = departmentService.getAllDepartments();

        assertEquals(1, result.size());
        assertEquals("ENG", result.get(0).getDepartmentCode());
        verify(departmentRepository).findAllByCompanyId(1L);
        verify(departmentRepository, never()).findAll();
    }

    @Test
    void getById_missingInCurrentCompany_notFound() {
        when(departmentRepository.findByIdAndCompanyId(99L, 1L)).thenReturn(Optional.empty());

        assertThrows(DepartmentNotFoundException.class, () -> departmentService.getDepartmentById(99L));
        verify(departmentRepository, never()).findById(99L);
    }

    @Test
    void create_bindsCompanyFromContext() {
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(departmentRepository.existsByCompanyIdAndDepartmentCode(1L, "OPS")).thenReturn(false);
        when(departmentRepository.existsByCompanyIdAndDepartmentName(1L, "Operations")).thenReturn(false);
        when(departmentRepository.save(any(Department.class))).thenAnswer(inv -> {
            Department saved = inv.getArgument(0);
            saved.setId(20L);
            return saved;
        });

        DepartmentDto dto = departmentService.create(new CreateDepartmentDto("  Operations ", " OPS "));

        assertEquals(20L, dto.getId());
        assertEquals("Operations", dto.getDepartmentName());
        assertEquals("OPS", dto.getDepartmentCode());
        assertTrue(dto.getIsActive());

        ArgumentCaptor<Department> captor = ArgumentCaptor.forClass(Department.class);
        verify(departmentRepository).save(captor.capture());
        assertEquals(1L, captor.getValue().getCompany().getId());
    }

    @Test
    void create_duplicateCodeInSameCompany_conflict() {
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(departmentRepository.existsByCompanyIdAndDepartmentCode(1L, "ENG")).thenReturn(true);

        assertThrows(DepartmentAlreadyExistsException.class,
                () -> departmentService.create(new CreateDepartmentDto("Engineering 2", "ENG")));
        verify(departmentRepository, never()).save(any());
    }

    @Test
    void create_sameCodeAllowedIfUniquenessIsPerCompany() {
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(departmentRepository.existsByCompanyIdAndDepartmentCode(1L, "ENG")).thenReturn(false);
        when(departmentRepository.existsByCompanyIdAndDepartmentName(1L, "Platform")).thenReturn(false);
        when(departmentRepository.save(any(Department.class))).thenAnswer(inv -> inv.getArgument(0));

        departmentService.create(new CreateDepartmentDto("Platform", "ENG"));

        verify(departmentRepository).existsByCompanyIdAndDepartmentCode(1L, "ENG");
        verify(departmentRepository, never()).existsByCompanyIdAndDepartmentCode(2L, "ENG");
    }

    @Test
    void update_wrongCompanyId_notFound() {
        when(departmentRepository.findByIdAndCompanyId(10L, 1L)).thenReturn(Optional.empty());

        assertThrows(DepartmentNotFoundException.class,
                () -> departmentService.update(10L, new UpdateDepartmentDto("X", "X", null)));
        verify(departmentRepository, never()).findById(10L);
    }

    @Test
    void update_duplicateNameInSameCompany_conflict() {
        when(departmentRepository.findByIdAndCompanyId(10L, 1L)).thenReturn(Optional.of(engineering));
        when(departmentRepository.existsByCompanyIdAndDepartmentCodeAndIdNot(1L, "ENG", 10L)).thenReturn(false);
        when(departmentRepository.existsByCompanyIdAndDepartmentNameAndIdNot(1L, "Marketing", 10L)).thenReturn(true);

        UpdateDepartmentDto dto = new UpdateDepartmentDto();
        dto.setDepartmentName("Marketing");

        assertThrows(DepartmentAlreadyExistsException.class, () -> departmentService.update(10L, dto));
    }

    @Test
    void update_deactivatesInCurrentCompany() {
        when(departmentRepository.findByIdAndCompanyId(10L, 1L)).thenReturn(Optional.of(engineering));
        when(departmentRepository.existsByCompanyIdAndDepartmentCodeAndIdNot(1L, "ENG", 10L)).thenReturn(false);
        when(departmentRepository.existsByCompanyIdAndDepartmentNameAndIdNot(1L, "Engineering", 10L)).thenReturn(false);

        UpdateDepartmentDto dto = new UpdateDepartmentDto();
        dto.setIsActive(false);

        DepartmentDto result = departmentService.update(10L, dto);

        assertFalse(result.getIsActive());
        assertFalse(engineering.getIsActive());
        assertEquals("ENG", result.getDepartmentCode());
    }

    private Department department(Long id, String name, String code, boolean active) {
        Department department = new Department();
        department.setId(id);
        department.setCompany(company);
        department.setDepartmentName(name);
        department.setDepartmentCode(code);
        department.setIsActive(active);
        return department;
    }
}
