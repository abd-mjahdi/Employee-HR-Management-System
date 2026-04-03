package com.example.employeetimetracking.service;

import com.example.employeetimetracking.exception.LeavePolicyNotFoundException;
import com.example.employeetimetracking.model.entities.*;
import com.example.employeetimetracking.model.enums.AccrualMethod;
import com.example.employeetimetracking.repository.LeaveBalanceRepository;
import com.example.employeetimetracking.repository.LeavePolicyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
public class LeavePolicyService {
    private final LeavePolicyRepository leavePolicyRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    @Autowired
    public LeavePolicyService(LeavePolicyRepository leavePolicyRepository, LeaveBalanceRepository leaveBalanceRepository){
        this.leavePolicyRepository = leavePolicyRepository;
        this.leaveBalanceRepository = leaveBalanceRepository;
    }
    public LeavePolicy getPolicyByLeaveType(Long leaveTypeId) {
        return leavePolicyRepository.findByLeaveTypeId(leaveTypeId)
                .orElseThrow(() -> new LeavePolicyNotFoundException("Policy not found for leave type"));
    }

    public BigDecimal calculateAccruedBalance(User user, LeaveType leaveType, LocalDate asOfDate) {
        LeavePolicy policy = getPolicyByLeaveType(leaveType.getId());

        if (policy.getAccrualMethod().equals(AccrualMethod.ANNUAL)) {
            // Annual: user gets full allocation at start of year
            return policy.getAnnualAllocation();
        } else {
            // Monthly: calculate months elapsed and multiply by monthly rate
            LocalDate startOfYear = LocalDate.of(asOfDate.getYear(), 1, 1);
            long monthsElapsed = ChronoUnit.MONTHS.between(startOfYear, asOfDate) + 1;

            BigDecimal monthlyRate = policy.getAnnualAllocation().divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
            BigDecimal accruedAmount = monthlyRate.multiply(BigDecimal.valueOf(monthsElapsed));

            // Cap at annual allocation
            if (accruedAmount.compareTo(policy.getAnnualAllocation()) > 0) {
                return policy.getAnnualAllocation();
            }

            return accruedAmount;
        }
    }

    public boolean isEligibleForLeaveType(User user, LeaveType leaveType) {
        // Simple eligibility - all active users are eligible for all leave types
        // future check:
        // - Employment duration (probation period)
        // - User role restrictions
        // - Department-specific leave types
        return user.getIsActive();
    }

    public boolean isRequestWithinPolicy(LeaveRequest request, User user) {
        LeavePolicy policy = getPolicyByLeaveType(request.getLeaveType().getId());

        // Check minimum notice period
        long daysUntilStart = ChronoUnit.DAYS.between(LocalDate.now(), request.getStartDate());
        if (daysUntilStart < policy.getMinNoticeDays()) {
            return false;
        }

        // Check if user has enough balance
        LeaveBalance balance = leaveBalanceRepository.findByUserIdAndLeaveTypeIdAndYear(
                user.getId(),
                request.getLeaveType().getId(),
                request.getStartDate().getYear()
        ).orElse(null);

        if (balance == null) {
            return false;
        }

        BigDecimal balanceAfterRequest = balance.getCurrentBalance().subtract(request.getTotalDays());

        // Check if negative balance is allowed
        if (balanceAfterRequest.compareTo(BigDecimal.ZERO) < 0 && !policy.getAllowsNegativeBalance()) {
            return false;
        }

        // Check if request days don't exceed annual allocation
        if (request.getTotalDays().compareTo(policy.getAnnualAllocation()) > 0) {
            return false;
        }

        return true;
    }


}