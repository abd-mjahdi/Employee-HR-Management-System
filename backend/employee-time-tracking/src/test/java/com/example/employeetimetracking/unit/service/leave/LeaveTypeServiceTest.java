package com.example.employeetimetracking.unit.service.leave;

import com.example.employeetimetracking.dto.response.LeaveTypeDto;
import com.example.employeetimetracking.exception.LeaveTypeNotFoundException;
import com.example.employeetimetracking.mapper.LeaveTypeMapper;
import com.example.employeetimetracking.model.entities.Company;
import com.example.employeetimetracking.model.entities.LeavePolicy;
import com.example.employeetimetracking.model.entities.LeaveType;
import com.example.employeetimetracking.model.enums.CompanyStatus;
import com.example.employeetimetracking.repository.LeaveTypeRepository;
import com.example.employeetimetracking.service.LeaveTypeService;
import com.example.employeetimetracking.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaveTypeServiceTest {

    @Mock
    LeaveTypeRepository leaveTypeRepository;

    LeaveTypeService leaveTypeService;

    Company company;
    LeaveType vacation;

    @BeforeEach
    void setUp() {
        TenantContext.set(new TenantContext.TenantInfo(1L, "acme", CompanyStatus.ACTIVE));
        leaveTypeService = new LeaveTypeService(leaveTypeRepository, new LeaveTypeMapper());

        company = new Company();
        company.setId(1L);
        company.setSlug("acme");
        company.setStatus(CompanyStatus.ACTIVE);

        vacation = leaveType(10L, "Vacation", true);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void getAll_usesCurrentCompanyOnly() {
        when(leaveTypeRepository.findAllByCompanyId(1L)).thenReturn(List.of(vacation));

        List<LeaveType> result = leaveTypeService.getAll();

        assertEquals(1, result.size());
        assertEquals("Vacation", result.get(0).getTypeName());
        verify(leaveTypeRepository).findAllByCompanyId(1L);
        verify(leaveTypeRepository, never()).findAll();
    }

    @Test
    void getAllActiveDto_usesCurrentCompanyOnly() {
        when(leaveTypeRepository.findByCompanyIdAndIsActive(1L, true)).thenReturn(List.of(vacation));

        List<LeaveTypeDto> result = leaveTypeService.getAllActiveDto();

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).getId());
        assertEquals("Vacation", result.get(0).getTypeName());
        verify(leaveTypeRepository).findByCompanyIdAndIsActive(1L, true);
        verify(leaveTypeRepository, never()).findByIsActive(true);
    }

    @Test
    void getById_missingInCurrentCompany_notFound() {
        when(leaveTypeRepository.findByIdAndCompanyIdAndIsActive(99L, 1L, true)).thenReturn(Optional.empty());

        assertThrows(LeaveTypeNotFoundException.class, () -> leaveTypeService.getById(99L));
        verify(leaveTypeRepository, never()).findById(99L);
        verify(leaveTypeRepository, never()).findByIdAndIsActive(99L, true);
    }

    @Test
    void getAllWithPolicy_usesCurrentCompanyOnly() {
        vacation.setLeavePolicy(new LeavePolicy());
        when(leaveTypeRepository.findAllWithPolicyByCompanyId(1L)).thenReturn(List.of(vacation));

        List<LeaveType> result = leaveTypeService.getAllWithPolicy();

        assertEquals(1, result.size());
        verify(leaveTypeRepository).findAllWithPolicyByCompanyId(1L);
        verify(leaveTypeRepository, never()).findAllWithPolicy();
    }

    private LeaveType leaveType(Long id, String name, boolean active) {
        LeaveType leaveType = new LeaveType();
        leaveType.setId(id);
        leaveType.setCompany(company);
        leaveType.setTypeName(name);
        leaveType.setIsActive(active);
        return leaveType;
    }
}
