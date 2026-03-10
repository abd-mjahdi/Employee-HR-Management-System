package com.example.employeetimetracking.controller;

import com.example.employeetimetracking.dto.response.UserDto;
import com.example.employeetimetracking.model.entities.User;
import com.example.employeetimetracking.service.UserService;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Objects;

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
    public ResponseEntity<Page<UserDto>> getAllUsers(Pageable pageable) {
        Page<UserDto> users = userService.getAll(pageable);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    public UserDto getUser(@PathVariable @Min(1) Long id){

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = (String) auth.getPrincipal();
        Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
        boolean isHrAdmin = authorities.stream().anyMatch(authority -> authority.getAuthority().equals("ROLE_HR_ADMIN"));


        Long requesterId = userService.getByEmail(email).getId();
        User wantedUser = userService.getById(id);
        UserDto responseDto = userService.getUserDetails(wantedUser.getEmail());

        Long managerId = wantedUser.getManager() != null ? wantedUser.getManager().getId() : null;
        boolean isManager = Objects.equals(requesterId, managerId);

        if(isHrAdmin || Objects.equals(requesterId, id) || isManager){
            return responseDto;
        }
        throw new AccessDeniedException("You cannot access this resource");

    }
}
