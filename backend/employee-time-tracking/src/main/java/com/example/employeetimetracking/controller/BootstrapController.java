package com.example.employeetimetracking.controller;

import com.example.employeetimetracking.config.BootstrapProperties;
import com.example.employeetimetracking.dto.request.BootstrapCompanyRequestDto;
import com.example.employeetimetracking.dto.response.BootstrapCompanyResponseDto;
import com.example.employeetimetracking.service.BootstrapService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Hidden
@RestController
@RequestMapping("/internal/bootstrap")
public class BootstrapController {

    private final BootstrapService bootstrapService;

    public BootstrapController(BootstrapService bootstrapService) {
        this.bootstrapService = bootstrapService;
    }

    @PostMapping("/company")
    public ResponseEntity<BootstrapCompanyResponseDto> bootstrapCompany(
            @RequestHeader(value = BootstrapProperties.HEADER, required = false) String bootstrapKey,
            @Valid @RequestBody BootstrapCompanyRequestDto request) {
        BootstrapCompanyResponseDto response = bootstrapService.bootstrap(bootstrapKey, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
