package com.example.employeetimetracking.controller;

import com.example.employeetimetracking.dto.response.UserResponseDto;
import com.example.employeetimetracking.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class MeController {

    private final UserService userService;
    @Autowired
    public MeController(UserService userService){
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponseDto> getUserDetails(){
        String email = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UserResponseDto userResponseDto = userService.getUserDetails(email);
        return ResponseEntity.ok(userResponseDto);
    }
}
