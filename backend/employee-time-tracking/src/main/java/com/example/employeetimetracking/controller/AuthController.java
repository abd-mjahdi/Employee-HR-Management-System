package com.example.employeetimetracking.controller;

import com.example.employeetimetracking.dto.request.LoginRequestDto;
import com.example.employeetimetracking.dto.response.JwtResponse;
import com.example.employeetimetracking.dto.response.LoginResponseDto;
import com.example.employeetimetracking.model.entities.User;
import com.example.employeetimetracking.repository.UserRepository;
import com.example.employeetimetracking.service.AuthService;
import com.example.employeetimetracking.service.LoginResult;
import com.example.employeetimetracking.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;
    @Autowired
    public AuthController(AuthService authService){
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto requestDto){
        try{
            LoginResult result = authService.login(requestDto);
            User user = result.getUser();
            String token = result.getToken();
            LoginResponseDto response = new LoginResponseDto(true, "Login successful", token, user.getEmail(), user.getUserRole());

            return ResponseEntity.ok(response);
        }catch(BadCredentialsException e){
            LoginResponseDto response = new LoginResponseDto(false, "Invalid credentials", null, null, null);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }

}
