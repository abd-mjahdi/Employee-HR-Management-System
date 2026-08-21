package com.example.employeetimetracking.unit.jobs;

import com.example.employeetimetracking.jobs.LeaveAccrualScheduler;
import com.example.employeetimetracking.model.entities.LeaveBalance;
import com.example.employeetimetracking.model.entities.LeavePolicy;
import com.example.employeetimetracking.model.entities.LeaveType;
import com.example.employeetimetracking.model.enums.AccrualMethod;
import com.example.employeetimetracking.repository.LeaveBalanceRepository;
import com.example.employeetimetracking.service.LeaveBalanceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaveAccrualSchedulerTest {

    @Mock LeaveBalanceService leaveBalanceService;
    @Mock LeaveBalanceRepository leaveBalanceRepository;

    LeaveAccrualScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new LeaveAccrualScheduler(leaveBalanceService, leaveBalanceRepository);
    }

    @Test
    void monthlyAccrual_appliesOnlyMonthlyPolicies() {
        LeaveBalance monthly = balance(AccrualMethod.MONTHLY);
        LeaveBalance annual = balance(AccrualMethod.ANNUAL);
        when(leaveBalanceRepository.findAccrualCandidatesForYear(LocalDate.now().getYear()))
                .thenReturn(List.of(monthly, annual));

        scheduler.processMonthlyLeaveAccrual();

        verify(leaveBalanceService).applyMonthlyAccrual(monthly);
        verify(leaveBalanceService, never()).applyMonthlyAccrual(annual);
        verify(leaveBalanceRepository, never()).findByYear(LocalDate.now().getYear());
    }

    @Test
    void monthlyAccrual_skipsNullPolicy() {
        LeaveBalance broken = new LeaveBalance();
        broken.setLeaveType(new LeaveType());
        when(leaveBalanceRepository.findAccrualCandidatesForYear(LocalDate.now().getYear()))
                .thenReturn(List.of(broken));

        scheduler.processMonthlyLeaveAccrual();

        verify(leaveBalanceService, never()).applyMonthlyAccrual(any());
    }

    @Test
    void yearEnd_usesPreviousYearInJanuary() {
        assertEquals(2026, LeaveAccrualScheduler.rolloverSourceYear(LocalDate.of(2027, 1, 1)));
        assertEquals(2026, LeaveAccrualScheduler.rolloverSourceYear(LocalDate.of(2026, 12, 31)));
    }

    @Test
    void yearEnd_rollsFetchedBalances() {
        LeaveBalance row = balance(AccrualMethod.ANNUAL);
        int sourceYear = LeaveAccrualScheduler.rolloverSourceYear(LocalDate.now());
        when(leaveBalanceRepository.findAccrualCandidatesForYear(sourceYear)).thenReturn(List.of(row));

        scheduler.yearEndRolloverJob();

        verify(leaveBalanceService).rolloverBalance(row);
        verify(leaveBalanceRepository, never()).findByYear(sourceYear);
    }

    private static LeaveBalance balance(AccrualMethod method) {
        LeavePolicy policy = new LeavePolicy();
        policy.setAccrualMethod(method);
        LeaveType type = new LeaveType();
        type.setIsActive(true);
        type.setLeavePolicy(policy);
        LeaveBalance balance = new LeaveBalance();
        balance.setId(1L);
        balance.setLeaveType(type);
        return balance;
    }
}
