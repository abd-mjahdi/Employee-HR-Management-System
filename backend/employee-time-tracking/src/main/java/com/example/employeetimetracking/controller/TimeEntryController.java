package com.example.employeetimetracking.controller;

import com.example.employeetimetracking.dto.request.CreateTimeEntryDto;
import com.example.employeetimetracking.dto.response.TimeEntryDto;
import com.example.employeetimetracking.model.enums.Status;
import com.example.employeetimetracking.security.CustomUserDetails;
import com.example.employeetimetracking.service.TimeEntryService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/time-entries")
public class TimeEntryController {
    private final TimeEntryService timeEntryService;
    @Autowired
    public TimeEntryController(TimeEntryService timeEntryService){
        this.timeEntryService = timeEntryService;
    }

    @PostMapping
    public ResponseEntity<TimeEntryDto> create(@Valid @RequestBody CreateTimeEntryDto request , @AuthenticationPrincipal CustomUserDetails authenticatedUser){
        TimeEntryDto response = timeEntryService.create(request, authenticatedUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/me")
    public ResponseEntity<List<TimeEntryDto>> getAllUser(@RequestParam(required = false) Status status,
                                                         @RequestParam(required = false) LocalDate startDate,
                                                         @RequestParam(required = false) LocalDate endDate,
            @AuthenticationPrincipal CustomUserDetails authenticatedUser){
        List<TimeEntryDto> response = timeEntryService.getByUserId(authenticatedUser.getId(),
                status,
                startDate,
                endDate);
        return ResponseEntity.ok(response);
    }
    @PreAuthorize("hasAnyRole('MANAGER','HR_ADMIN')")
    @GetMapping("/team")
    public ResponseEntity<List<TimeEntryDto>> getTeamEntries(@RequestParam(required = false) Status status,
                                                             @RequestParam(required = false) LocalDate startDate,
                                                             @RequestParam(required = false) LocalDate endDate,
                                                             @RequestParam(required = false) String name,
                                                             @AuthenticationPrincipal CustomUserDetails authenticatedUser){
        List<TimeEntryDto> response = timeEntryService.getTeamEntries(authenticatedUser.getId(), status, startDate, endDate, name);
        return ResponseEntity.ok(response);
    }



}
