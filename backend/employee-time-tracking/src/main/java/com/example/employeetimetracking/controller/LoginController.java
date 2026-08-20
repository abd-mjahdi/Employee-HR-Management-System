package com.example.employeetimetracking.controller;

import com.example.employeetimetracking.dto.request.LoginRequestDto;
import com.example.employeetimetracking.dto.response.LoginResponseDto;
import com.example.employeetimetracking.service.LoginService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class LoginController {
    private final LoginService loginService;

    @Autowired
    public LoginController(LoginService loginService) {
        this.loginService = loginService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto requestDto,
                                                  HttpServletRequest request) {
        LoginResponseDto response = loginService.login(requestDto, request.getRemoteAddr());
        return ResponseEntity.ok(response);
    }
}
