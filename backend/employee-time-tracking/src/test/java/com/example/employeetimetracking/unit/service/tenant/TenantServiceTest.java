package com.example.employeetimetracking.unit.service.tenant;

import com.example.employeetimetracking.exception.InactiveTenantException;
import com.example.employeetimetracking.exception.InvalidTenantException;
import com.example.employeetimetracking.model.entities.Company;
import com.example.employeetimetracking.model.enums.CompanyStatus;
import com.example.employeetimetracking.repository.CompanyRepository;
import com.example.employeetimetracking.service.TenantService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantServiceTest {

    @Mock
    CompanyRepository companyRepository;

    @InjectMocks
    TenantService tenantService;

    @Test
    void requireActiveBySlug_returnsActiveCompany() {
        Company acme = new Company();
        acme.setId(1L);
        acme.setSlug("acme");
        acme.setStatus(CompanyStatus.ACTIVE);
        when(companyRepository.findBySlugIgnoreCase("acme")).thenReturn(Optional.of(acme));

        Company found = tenantService.requireActiveBySlug("acme");
        assertEquals(1L, found.getId());
    }

    @Test
    void requireActiveBySlug_unknownSlug_throwsInvalidTenant() {
        when(companyRepository.findBySlugIgnoreCase("nope")).thenReturn(Optional.empty());
        assertThrows(InvalidTenantException.class, () -> tenantService.requireActiveBySlug("nope"));
    }

    @Test
    void requireActiveBySlug_inactive_throwsInactiveTenant() {
        Company globex = new Company();
        globex.setSlug("globex");
        globex.setStatus(CompanyStatus.INACTIVE);
        when(companyRepository.findBySlugIgnoreCase("globex")).thenReturn(Optional.of(globex));
        assertThrows(InactiveTenantException.class, () -> tenantService.requireActiveBySlug("globex"));
    }
}
