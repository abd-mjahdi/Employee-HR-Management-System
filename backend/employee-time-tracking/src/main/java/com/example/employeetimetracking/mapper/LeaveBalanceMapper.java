package com.example.employeetimetracking.mapper;

import com.example.employeetimetracking.dto.response.LeaveBalanceDto;
import com.example.employeetimetracking.model.entities.LeaveBalance;
import org.springframework.stereotype.Component;

@Component
public class LeaveBalanceMapper {
    public LeaveBalanceDto toDto(LeaveBalance leaveBalance) {
        return new LeaveBalanceDto(leaveBalance.getId() ,
                leaveBalance.getUser().getId(),
                leaveBalance.getLeaveType().getId(),
                leaveBalance.getLeaveType().getTypeName(),
                leaveBalance.getLastAccrualDate(),
                leaveBalance.getYear(),
                leaveBalance.getCurrentBalance());
    }
}
