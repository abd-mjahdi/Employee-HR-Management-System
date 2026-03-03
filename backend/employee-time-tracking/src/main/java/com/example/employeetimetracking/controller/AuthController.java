package com.example.employeetimetracking.controller;

import com.example.employeetimetracking.dto.request.LoginRequestDto;
import com.example.employeetimetracking.dto.response.JwtResponse;
import com.example.employeetimetracking.dto.response.LoginResponseDto;
import com.example.employeetimetracking.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
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
    public ResponseEntity<?> login(@RequestBody LoginRequestDto requestDto){
        try{
            String token = authService.login(requestDto);
            return ResponseEntity.ok(new JwtResponse(token));
        }catch(BadCredentialsException e){
            return ResponseEntity
                    .status(HttpServletResponse.SC_UNAUTHORIZED)
                    .body("Invalid email or password");
        }
    }

}
