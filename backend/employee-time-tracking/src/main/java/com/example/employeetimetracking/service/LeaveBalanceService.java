package com.example.employeetimetracking.service;

import com.example.employeetimetracking.dto.response.LeaveBalanceDto;
import com.example.employeetimetracking.dto.response.LeaveTypeDto;
import com.example.employeetimetracking.dto.response.UserResponseDto;
import com.example.employeetimetracking.model.entities.LeaveBalance;
import com.example.employeetimetracking.model.entities.LeavePolicy;
import com.example.employeetimetracking.model.entities.LeaveType;
import com.example.employeetimetracking.model.entities.User;
import com.example.employeetimetracking.model.enums.AccrualMethod;
import com.example.employeetimetracking.repository.LeaveBalanceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class LeaveBalanceService {
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final LeaveTypeService leaveTypeService;
    @Autowired
    public LeaveBalanceService(LeaveBalanceRepository leaveBalanceRepository,
                               LeaveTypeService leaveTypeService){
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.leaveTypeService = leaveTypeService;
    }

    public LeaveBalance save(LeaveBalance balance){
        return leaveBalanceRepository.save(balance);
    }

    public void initializeLeaveBalances(User user){
        List<LeaveType> leaveTypes = leaveTypeService.getAll();

        for(LeaveType leaveType : leaveTypes){
            LeavePolicy policy = leaveType.getLeavePolicy();
            LeaveBalance balance = new LeaveBalance();
            balance.setUser(user);
            balance.setLeaveType(leaveType);
            short year =(short) LocalDate.now().getYear();
            balance.setYear(year);

            if(policy.getAccrualMethod().equals(AccrualMethod.ANNUAL)){
                balance.setCurrentBalance(policy.getAnnualAllocation());
            }else{
                balance.setCurrentBalance(BigDecimal.ZERO);
            }
            balance.setLastAccrualDate(LocalDate.now());
            save(balance);
        }
    }

}
