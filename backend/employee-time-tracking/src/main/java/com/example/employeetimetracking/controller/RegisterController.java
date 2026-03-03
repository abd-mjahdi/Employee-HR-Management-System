package com.example.employeetimetracking.controller;

import com.example.employeetimetracking.dto.request.RegisterRequestDto;
import com.example.employeetimetracking.dto.response.RegisterResponseDto;
import com.example.employeetimetracking.service.RegisterService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class RegisterController {
    private final RegisterService registerService;

    @Autowired
    public RegisterController(RegisterService registerService){
        this.registerService = registerService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponseDto> register(@Valid @RequestBody RegisterRequestDto requestDto){
        try{
            RegisterResponseDto response = registerService.register(requestDto);
            return ResponseEntity.ok(response);
        }catch(IllegalArgumentException e){
            RegisterResponseDto response = new RegisterResponseDto(e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }
    }

}
