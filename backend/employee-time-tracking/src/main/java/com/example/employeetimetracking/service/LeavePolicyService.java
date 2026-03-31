package com.example.employeetimetracking.service;

import com.example.employeetimetracking.exception.LeavePolicyNotFoundException;
import com.example.employeetimetracking.model.entities.LeavePolicy;
import com.example.employeetimetracking.repository.LeavePolicyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

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

    //public BigDecimal calculateAccruedBalance(...) { }

    //public boolean isEligibleForLeaveType(...) { }

    //public boolean isRequestWithinPolicy(...) { }
}