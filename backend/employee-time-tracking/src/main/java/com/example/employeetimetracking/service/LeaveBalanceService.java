package com.example.employeetimetracking.service;

import com.example.employeetimetracking.dto.response.LeaveBalanceDto;
import com.example.employeetimetracking.dto.response.LeaveTypeDto;
import com.example.employeetimetracking.dto.response.UserResponseDto;
import com.example.employeetimetracking.model.entities.LeaveBalance;
import com.example.employeetimetracking.model.entities.User;
import com.example.employeetimetracking.repository.LeaveBalanceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LeaveBalanceService {
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final UserService userService;
    private final LeaveTypeService leaveTypeService;
    @Autowired
    public LeaveBalanceService(LeaveBalanceRepository leaveBalanceRepository , UserService userService ,LeaveTypeService leaveTypeService){
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.userService = userService;
        this.leaveTypeService = leaveTypeService;
    }
    public LeaveBalance save(LeaveBalance balance){
        return leaveBalanceRepository.save(balance);
    }

    public LeaveBalanceDto convertToDto(LeaveBalance leaveBalance){
        UserResponseDto userResponseDto = userService.convertToDto(leaveBalance.getUser());
        LeaveTypeDto leaveTypeDto = leaveTypeService.convertToDto(leaveBalance.getLeaveType());
        return new LeaveBalanceDto(leaveBalance.getId() ,
                userResponseDto,
                leaveTypeDto,
                leaveBalance.getYear(),
                leaveBalance.getCurrentBalance());
    }
}
