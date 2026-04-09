package com.example.employeetimetracking.controller;

import com.example.employeetimetracking.dto.response.UserDashboardDto;
import com.example.employeetimetracking.security.CustomUserDetails;
import com.example.employeetimetracking.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users/me/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public ResponseEntity<UserDashboardDto> getDashboardData(@AuthenticationPrincipal CustomUserDetails authenticatedUser) {
        UserDashboardDto userDashboardDto = dashboardService.getDashboardData(authenticatedUser.getId());
        return ResponseEntity.ok(userDashboardDto);
    }
}