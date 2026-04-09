package com.example.employeetimetracking.controller;

import com.example.employeetimetracking.dto.response.LeaveBalanceDto;
import com.example.employeetimetracking.security.CustomUserDetails;
import com.example.employeetimetracking.service.LeaveBalanceService;
import com.example.employeetimetracking.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/leave-balances")
public class LeaveBalanceController {
    private final LeaveBalanceService leaveBalanceService;
    @Autowired
    public LeaveBalanceController(LeaveBalanceService leaveBalanceService){
        this.leaveBalanceService = leaveBalanceService;
    }

    @GetMapping("/me")
    public ResponseEntity<List<LeaveBalanceDto>> getLeaveBalances(@AuthenticationPrincipal CustomUserDetails authenticatedUser){
        List<LeaveBalanceDto> leaveBalances = leaveBalanceService.getByUserIdAndYear(authenticatedUser.getId() ,2025);
        return ResponseEntity.ok(leaveBalances);
    }

    @PreAuthorize("hasAnyRole('MANAGER', 'HR_ADMIN')")
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<LeaveBalanceDto>> getLeaveBalanceById(@PathVariable Long userId, @AuthenticationPrincipal CustomUserDetails authenticatedUser){
        List<LeaveBalanceDto> leaveBalances = leaveBalanceService.getLeaveBalanceIfAllowed(userId,authenticatedUser);
        return ResponseEntity.ok(leaveBalances);
    }


}
