package com.example.employeetimetracking.unit.service.leave;

import com.example.employeetimetracking.exception.LeavePolicyNotFoundException;
import com.example.employeetimetracking.model.entities.LeavePolicy;
import com.example.employeetimetracking.model.enums.CompanyStatus;
import com.example.employeetimetracking.repository.LeaveBalanceRepository;
import com.example.employeetimetracking.repository.LeavePolicyRepository;
import com.example.employeetimetracking.service.LeavePolicyService;
import com.example.employeetimetracking.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeavePolicyServiceTest {

    @Mock
    LeavePolicyRepository leavePolicyRepository;
    @Mock
    LeaveBalanceRepository leaveBalanceRepository;

    LeavePolicyService leavePolicyService;

    @BeforeEach
    void setUp() {
        TenantContext.set(new TenantContext.TenantInfo(1L, "acme", CompanyStatus.ACTIVE));
        leavePolicyService = new LeavePolicyService(leavePolicyRepository, leaveBalanceRepository);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void getPolicyByLeaveTypeId_usesCurrentCompany() {
        LeavePolicy policy = new LeavePolicy();
        policy.setId(5L);
        when(leavePolicyRepository.findByCompanyIdAndLeaveTypeId(1L, 10L)).thenReturn(Optional.of(policy));

        LeavePolicy result = leavePolicyService.getPolicyByLeaveTypeId(10L);

        assertEquals(5L, result.getId());
        verify(leavePolicyRepository).findByCompanyIdAndLeaveTypeId(1L, 10L);
        verify(leavePolicyRepository, never()).findByLeaveTypeId(10L);
    }

    @Test
    void getPolicyByLeaveTypeId_otherCompany_notFound() {
        when(leavePolicyRepository.findByCompanyIdAndLeaveTypeId(1L, 99L)).thenReturn(Optional.empty());

        assertThrows(LeavePolicyNotFoundException.class, () -> leavePolicyService.getPolicyByLeaveTypeId(99L));
        verify(leavePolicyRepository, never()).findByLeaveTypeId(99L);
    }
}
