package com.example.employeetimetracking.controller;

import com.example.employeetimetracking.dto.request.CreateTimeEntryDto;
import com.example.employeetimetracking.dto.request.CreateTimeEntryBreakDto;
import com.example.employeetimetracking.dto.request.CorrectionRequestDto;
import com.example.employeetimetracking.dto.request.TimeEntryRejectionDto;
import com.example.employeetimetracking.dto.response.TimeEntryBreakDto;
import com.example.employeetimetracking.dto.response.TimeEntryPersonalStatsDto;
import com.example.employeetimetracking.dto.response.TimeEntrySummaryDto;
import com.example.employeetimetracking.dto.response.TimeEntryDto;
import com.example.employeetimetracking.model.enums.Status;
import com.example.employeetimetracking.security.CustomUserDetails;
import com.example.employeetimetracking.service.TimeEntryService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

    @GetMapping("/stats/me")
    public ResponseEntity<TimeEntryPersonalStatsDto> getMyStats(
            @AuthenticationPrincipal CustomUserDetails authenticatedUser
    ) {
        return ResponseEntity.ok(timeEntryService.getMyStats(authenticatedUser.getId()));
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

    @PreAuthorize("hasAnyRole('MANAGER','HR_ADMIN')")
    @PostMapping("/{id}/approve")
    public ResponseEntity<Void> approve(@PathVariable Long id,
                                        @AuthenticationPrincipal CustomUserDetails authenticatedUser) {
        timeEntryService.approve(id, authenticatedUser.getId());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);
    }

    @PreAuthorize("hasAnyRole('MANAGER','HR_ADMIN')")
    @PostMapping("/{id}/reject")
    public ResponseEntity<Void> reject(@PathVariable Long id,
                                       @Valid @RequestBody TimeEntryRejectionDto request,
                                       @AuthenticationPrincipal CustomUserDetails authenticatedUser) {
        timeEntryService.reject(id, authenticatedUser.getId(), request.getReason());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TimeEntryDto> update(@PathVariable Long id,
                                               @Valid @RequestBody CreateTimeEntryDto request,
                                               @AuthenticationPrincipal CustomUserDetails authenticatedUser) {
        boolean isManager = authenticatedUser.hasRole("MANAGER") || authenticatedUser.hasRole("HR_ADMIN");
        TimeEntryDto response = timeEntryService.update(id, request, authenticatedUser.getId(), isManager);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/breaks")
    public ResponseEntity<TimeEntryBreakDto> addBreak(@PathVariable Long id,
                                                      @Valid @RequestBody CreateTimeEntryBreakDto request,
                                                      @AuthenticationPrincipal CustomUserDetails authenticatedUser) {
        boolean isManager = authenticatedUser.hasRole("MANAGER") || authenticatedUser.hasRole("HR_ADMIN");
        TimeEntryBreakDto dto = timeEntryService.addBreak(id, request, authenticatedUser.getId(), isManager);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping("/{id}/breaks")
    public ResponseEntity<List<TimeEntryBreakDto>> listBreaks(@PathVariable Long id,
                                                              @AuthenticationPrincipal CustomUserDetails authenticatedUser) {
        boolean isManager = authenticatedUser.hasRole("MANAGER") || authenticatedUser.hasRole("HR_ADMIN");
        return ResponseEntity.ok(timeEntryService.listBreaks(id, authenticatedUser.getId(), isManager));
    }

    @DeleteMapping("/{id}/breaks/{breakId}")
    public ResponseEntity<Void> deleteBreak(@PathVariable Long id,
                                            @PathVariable Long breakId,
                                            @AuthenticationPrincipal CustomUserDetails authenticatedUser) {
        boolean isManager = authenticatedUser.hasRole("MANAGER") || authenticatedUser.hasRole("HR_ADMIN");
        timeEntryService.deleteBreak(id, breakId, authenticatedUser.getId(), isManager);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePending(@PathVariable Long id,
                                              @AuthenticationPrincipal CustomUserDetails authenticatedUser) {
        timeEntryService.deletePending(id, authenticatedUser.getId());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);
    }

    @PostMapping("/bulk")
    public ResponseEntity<List<TimeEntryDto>> bulkCreate(@Valid @RequestBody List<CreateTimeEntryDto> requests,
                                                         @AuthenticationPrincipal CustomUserDetails authenticatedUser) {
        List<TimeEntryDto> response = timeEntryService.bulkCreate(requests, authenticatedUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasAnyRole('MANAGER','HR_ADMIN')")
    @GetMapping("/summary")
    public ResponseEntity<TimeEntrySummaryDto> summary(@RequestParam LocalDate startDate,
                                                       @RequestParam LocalDate endDate,
                                                       @RequestParam(required = false) Long userId,
                                                       @AuthenticationPrincipal CustomUserDetails authenticatedUser) {
        TimeEntrySummaryDto response = timeEntryService.summary(
                authenticatedUser.getId(),
                userId,
                startDate,
                endDate,
                authenticatedUser
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/correction-request")
    public ResponseEntity<Void> requestCorrection(@PathVariable Long id,
                                                 @Valid @RequestBody CorrectionRequestDto request,
                                                 @AuthenticationPrincipal CustomUserDetails authenticatedUser) {
        timeEntryService.requestCorrection(id, authenticatedUser.getId(), request.getExplanation());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PreAuthorize("hasAnyRole('MANAGER','HR_ADMIN')")
    @PostMapping("/{id}/correction-approve")
    public ResponseEntity<Void> approveCorrectionUnlock(@PathVariable Long id,
                                                        @AuthenticationPrincipal CustomUserDetails authenticatedUser) {
        timeEntryService.approveCorrectionUnlock(id, authenticatedUser.getId());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PreAuthorize("hasAnyRole('MANAGER','HR_ADMIN')")
    @GetMapping("/pending-approval")
    public ResponseEntity<List<TimeEntryDto>> pendingApproval(
            @AuthenticationPrincipal CustomUserDetails authenticatedUser) {
        boolean hr = authenticatedUser.hasRole("HR_ADMIN");
        return ResponseEntity.ok(
                timeEntryService.getPendingApprovalQueue(authenticatedUser.getId(), hr)
        );
    }

    @PreAuthorize("hasAnyRole('MANAGER','HR_ADMIN')")
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@RequestParam LocalDate startDate,
                                         @RequestParam LocalDate endDate,
                                         @RequestParam String format,
                                         @RequestParam(required = false) Long userId,
                                         @AuthenticationPrincipal CustomUserDetails authenticatedUser) {
        byte[] body = timeEntryService.export(
                authenticatedUser.getId(), startDate, endDate, userId, format, authenticatedUser
        );
        boolean csv = "csv".equalsIgnoreCase(format.trim());
        String ext = csv ? "csv" : "xlsx";
        MediaType media = csv
                ? MediaType.parseMediaType("text/csv; charset=UTF-8")
                : MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"time-entries." + ext + "\"")
                .contentType(media)
                .body(body);
    }

}
