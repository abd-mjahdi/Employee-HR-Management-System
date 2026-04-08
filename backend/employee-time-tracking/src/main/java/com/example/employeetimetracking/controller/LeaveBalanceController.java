package com.example.employeetimetracking.controller;

import com.example.employeetimetracking.dto.response.LeaveBalanceDto;
import com.example.employeetimetracking.model.entities.User;
import com.example.employeetimetracking.service.LeaveBalanceService;
import com.example.employeetimetracking.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
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
        List<LeaveBalanceDto> leaveBalances = leaveBalanceService.getByUserIdAndYear(authenticatedUser.getId() ,2025);
        return ResponseEntity.ok(leaveBalances);
    }

    @PreAuthorize("hasAnyRole('ROLE_MANAGER', 'ROLE_HR_ADMIN')")
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<LeaveBalanceDto>> getLeaveBalanceById(@PathVariable Long userId){
        User authenticatedUser = userService.getByEmail((String) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        Collection<? extends GrantedAuthority> authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities();
        List<LeaveBalanceDto> leaveBalances = leaveBalanceService.getLeaveBalanceIfAllowed(userId,authenticatedUser ,authorities);
        return ResponseEntity.ok(leaveBalances);
    }


}
