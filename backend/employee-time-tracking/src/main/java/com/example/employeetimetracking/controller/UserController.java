package com.example.employeetimetracking.controller;

import com.example.employeetimetracking.dto.request.CreateUserRequestDto;
import com.example.employeetimetracking.dto.request.UserRequestDto;
import com.example.employeetimetracking.dto.response.UserCreatedResponse;
import com.example.employeetimetracking.dto.response.UserResponseDto;
import com.example.employeetimetracking.model.entities.User;
import com.example.employeetimetracking.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@RestController
@Validated
@RequestMapping("/users")
public class UserController {
    private final UserService userService;
    @Autowired
    public UserController(UserService userService){
        this.userService = userService;
    }


    @PreAuthorize("hasRole('ROLE_HR_ADMIN')")
    @GetMapping
    public ResponseEntity<Page<UserResponseDto>> getAllUsers(Pageable pageable) {
        Page<UserResponseDto> users = userService.getAll(pageable);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDto> getUser(@PathVariable @Min(1) Long id){
        User authenticatedUser = userService.getByEmail((String) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        Collection<? extends GrantedAuthority> authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities();
        UserResponseDto userResponseDto = userService.getUserIfAllowed(id , authenticatedUser , authorities);
        return ResponseEntity.ok(userResponseDto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDto> updateUser(@Valid @RequestBody UserRequestDto userRequestDto, @PathVariable Long id){
        User authenticatedUser = userService.getByEmail((String) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        Collection<? extends GrantedAuthority> authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities();
        UserResponseDto updatedUserDto = userService.updateUserIfAllowed(id , userRequestDto, authenticatedUser , authorities);
        return ResponseEntity.ok(updatedUserDto);
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponseDto> getCurrentUser(){
        User authenticatedUser = userService.getByEmail((String) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        UserResponseDto userResponseDto = userService.getUserIfAllowed(authenticatedUser);
        return ResponseEntity.ok(userResponseDto);
    }

    @GetMapping("/team")
    public ResponseEntity<List<UserResponseDto>> getTeamMembers(){
        User authenticatedUser = userService.getByEmail((String) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        Collection<? extends GrantedAuthority> authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities();
        List<UserResponseDto> teamMemberList = userService.getTeamMembers(authenticatedUser , authorities);
        return ResponseEntity.ok(teamMemberList);
    }

    @PostMapping
    public ResponseEntity<UserCreatedResponse> createUser(@Valid @RequestBody CreateUserRequestDto requestDto){
        User authenticatedUser = userService.getByEmail((String) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        Collection<? extends GrantedAuthority> authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities();
        UserCreatedResponse response = userService.createUserIfAllowed(requestDto , authorities);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
