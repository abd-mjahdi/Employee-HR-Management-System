package com.example.employeetimetracking.unit.service.audit;

import com.example.employeetimetracking.dto.response.AuditLogDto;
import com.example.employeetimetracking.exception.AuditLogNotFoundException;
import com.example.employeetimetracking.mapper.AuditLogMapper;
import com.example.employeetimetracking.model.entities.AuditLog;
import com.example.employeetimetracking.model.entities.Company;
import com.example.employeetimetracking.model.entities.User;
import com.example.employeetimetracking.model.enums.ActionType;
import com.example.employeetimetracking.model.enums.CompanyStatus;
import com.example.employeetimetracking.repository.AuditLogRepository;
import com.example.employeetimetracking.repository.CompanyRepository;
import com.example.employeetimetracking.service.AuditLogService;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock AuditLogRepository auditLogRepository;
    @Mock CompanyRepository companyRepository;

    AuditLogService auditLogService;
    Company company;
    User actor;

    @BeforeEach
    void setUp() {
        TenantContext.set(new TenantContext.TenantInfo(1L, "acme", CompanyStatus.ACTIVE));
        auditLogService = new AuditLogService(auditLogRepository, companyRepository, new AuditLogMapper());

        company = new Company();
        company.setId(1L);
        company.setSlug("acme");
        company.setStatus(CompanyStatus.ACTIVE);

        actor = new User();
        actor.setId(2L);
        actor.setEmail("hr@acme.com");
        actor.setPasswordHash("hash");
        actor.setIsActive(true);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void record_setsCompanyFromTenantContext() {
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> {
            AuditLog saved = inv.getArgument(0);
            saved.setId(50L);
            return saved;
        });

        AuditLog log = auditLogService.record(
                actor, ActionType.CREATE, "users", 2L, null, "{\"email\":\"hr@acme.com\"}", "127.0.0.1");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertEquals(1L, captor.getValue().getCompany().getId());
        assertEquals(1L, log.getCompany().getId());
        assertEquals(ActionType.CREATE, log.getActionType());
        verify(companyRepository).findById(1L);
        verify(companyRepository, never()).findById(99L);
    }

    @Test
    void list_usesCurrentCompanyOnly() {
        AuditLog log = new AuditLog();
        log.setId(7L);
        log.setCompany(company);
        log.setActionType(ActionType.UPDATE);
        log.setTableName("departments");
        log.setRecordId(10L);
        when(auditLogRepository.findAllByCompanyId(1L)).thenReturn(List.of(log));

        List<AuditLogDto> result = auditLogService.listCurrentCompany();

        assertEquals(1, result.size());
        assertEquals(7L, result.get(0).getId());
        verify(auditLogRepository).findAllByCompanyId(1L);
        verify(auditLogRepository, never()).findAll();
        verify(auditLogRepository, never()).findByUserId(any());
        verify(auditLogRepository, never()).findByTableName(any());
    }

    @Test
    void getById_missingInCurrentCompany_notFound() {
        when(auditLogRepository.findByIdAndCompanyId(9L, 1L)).thenReturn(Optional.empty());

        assertThrows(AuditLogNotFoundException.class, () -> auditLogService.getById(9L));
        verify(auditLogRepository, never()).findById(9L);
    }
}
