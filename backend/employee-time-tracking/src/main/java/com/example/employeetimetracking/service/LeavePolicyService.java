package com.example.employeetimetracking.service;

import com.example.employeetimetracking.exception.LeavePolicyNotFoundException;
import com.example.employeetimetracking.model.entities.*;
import com.example.employeetimetracking.model.enums.AccrualMethod;
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
    @Autowired
    public LeavePolicyService(LeavePolicyRepository leavePolicyRepository){
        this.leavePolicyRepository = leavePolicyRepository;
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


}