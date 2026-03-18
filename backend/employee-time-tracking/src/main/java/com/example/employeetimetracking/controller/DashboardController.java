package com.example.employeetimetracking.controller;

import com.example.employeetimetracking.dto.response.UserDashboardDto;
import com.example.employeetimetracking.model.entities.User;
import com.example.employeetimetracking.service.DashboardService;
import com.example.employeetimetracking.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users/me/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final UserService userService;

    public DashboardController(DashboardService dashboardService, UserService userService) {
        this.dashboardService = dashboardService;
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<UserDashboardDto> getDashboardData() {
        String email = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User authenticatedUser = userService.getByEmail(email);
        UserDashboardDto userDashboardDto = dashboardService.getDashboardData(authenticatedUser);
        return ResponseEntity.ok(userDashboardDto);
    }
}