package com.example.employeetimetracking.service;

import com.example.employeetimetracking.dto.response.LeaveBalanceDto;
import com.example.employeetimetracking.exception.LeaveBalanceNotFoundException;
import com.example.employeetimetracking.exception.NegativeLeaveBalanceException;
import com.example.employeetimetracking.mapper.LeaveBalanceMapper;
import com.example.employeetimetracking.model.entities.*;
import com.example.employeetimetracking.model.enums.AccrualMethod;
import com.example.employeetimetracking.repository.LeaveBalanceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class LeaveBalanceService {
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final LeaveTypeService leaveTypeService;
    private final LeaveBalanceMapper leaveBalanceMapper;
    @Autowired
    public LeaveBalanceService(LeaveBalanceRepository leaveBalanceRepository,
                               LeaveTypeService leaveTypeService,
                               LeaveBalanceMapper leaveBalanceMapper
                               ){
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.leaveTypeService = leaveTypeService;
        this.leaveBalanceMapper = leaveBalanceMapper;
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

    public void applyMonthlyAccrual(LeaveBalance lb){
        LeavePolicy policy = lb.getLeaveType().getLeavePolicy();
        BigDecimal accrualAmount = policy.getAnnualAllocation()
                .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
        lb.setCurrentBalance(lb.getCurrentBalance().add(accrualAmount));
    }

    private LeaveBalance getLeaveBalance(User user, LeaveRequest lr) {
        return leaveBalanceRepository
                .findByUserIdAndLeaveTypeId(user.getId(), lr.getLeaveType().getId())
                .orElseThrow(() -> new LeaveBalanceNotFoundException(
                        "Leave balance not found for userId=" + user.getId() +
                                " and leaveTypeId=" + lr.getLeaveType().getId()
                ));
    }

    public void deductLeaveBalance(LeaveRequest lr ,User user){
        LeaveBalance lb = getLeaveBalance(user, lr);
        BigDecimal newBalance = lb.getCurrentBalance().subtract(lr.getTotalDays());
        if (newBalance.compareTo(BigDecimal.ZERO) < 0 &&
                !lb.getLeaveType().getLeavePolicy().getAllowsNegativeBalance()) {
            throw new NegativeLeaveBalanceException("Insufficient leave balance");
        }

        lb.setCurrentBalance(newBalance);

    }

    public void restoreLeaveBalance(LeaveRequest lr ,User user){
        LeaveBalance lb = getLeaveBalance(user, lr);
        lb.setCurrentBalance(lb.getCurrentBalance().add(lr.getTotalDays()));
    }

    public void setLeaveBalance(LeaveRequest lr ,User user ,BigDecimal value){
        LeaveBalance lb = getLeaveBalance(user, lr);
        LeavePolicy policy = lb.getLeaveType().getLeavePolicy();

        if (value.compareTo(BigDecimal.ZERO) < 0 && !policy.getAllowsNegativeBalance()) {
            throw new NegativeLeaveBalanceException("Negative Balance is not allowed for this leave type");
        }
        if (value.compareTo(policy.getAnnualAllocation()) > 0) {
            lb.setCurrentBalance(policy.getAnnualAllocation());
            return;
        }
        lb.setCurrentBalance(value);
    }

    public List<LeaveBalanceDto> getByUserIdAndYear(Long userId ,int year){
        List<LeaveBalance> leaveBalances = leaveBalanceRepository.findByUserIdAndYear(userId,year);
        return leaveBalances.stream().map(leaveBalanceMapper::toDto).toList();
    }

}
