package com.example.employeetimetracking.service;

import com.example.employeetimetracking.model.entities.LeaveBalance;
import com.example.employeetimetracking.repository.LeaveBalanceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LeaveBalanceService {
    private final LeaveBalanceRepository leaveBalanceRepository;
    @Autowired
    public LeaveBalanceService(LeaveBalanceRepository leaveBalanceRepository){
        this.leaveBalanceRepository = leaveBalanceRepository;
    }
    public LeaveBalance save(LeaveBalance balance){
        return leaveBalanceRepository.save(balance);
    }
}
