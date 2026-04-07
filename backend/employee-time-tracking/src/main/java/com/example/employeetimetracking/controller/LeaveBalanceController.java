package com.example.employeetimetracking.controller;

import com.example.employeetimetracking.dto.response.LeaveBalanceDto;
import com.example.employeetimetracking.model.entities.User;
import com.example.employeetimetracking.repository.LeaveBalanceRepository;
import com.example.employeetimetracking.service.LeaveBalanceService;
import com.example.employeetimetracking.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/leave-balances")
public class LeaveBalanceController {
    private final LeaveBalanceService leaveBalanceService;
    private final UserService userService;
    @Autowired
    public LeaveBalanceController(LeaveBalanceService leaveBalanceService, UserService userService){
        this.leaveBalanceService = leaveBalanceService;
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<List<LeaveBalanceDto>> getLeaveBalances(){
        User authenticatedUser = userService.getByEmail((String) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        List<LeaveBalanceDto> leaveBalances = leaveBalanceService.getByUserIdAndYear(authenticatedUser.getId() , LocalDate.now().getYear());
        return ResponseEntity.ok(leaveBalances);
    }


}
