package com.example.employeetimetracking.controller;

import com.example.employeetimetracking.dto.response.DepartmentDto;
import com.example.employeetimetracking.dto.response.UserDto;
import com.example.employeetimetracking.model.entities.Department;
import com.example.employeetimetracking.model.entities.User;
import com.example.employeetimetracking.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class UserDetailsController {

    private final UserService userService;
    @Autowired
    public UserDetailsController(UserService userService){
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> getUserDetails(){
        String email = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UserDto userDto = userService.getCurrentUserDetails(email);
        return ResponseEntity.ok(userDto);
    }
}
