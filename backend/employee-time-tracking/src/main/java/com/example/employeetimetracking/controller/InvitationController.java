package com.example.employeetimetracking.controller;

import com.example.employeetimetracking.dto.request.CreateInvitationRequestDto;
import com.example.employeetimetracking.dto.response.InvitationCreatedResponseDto;
import com.example.employeetimetracking.security.CustomUserDetails;
import com.example.employeetimetracking.service.InvitationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/invitations")
public class InvitationController {

    private final InvitationService invitationService;

    public InvitationController(InvitationService invitationService) {
        this.invitationService = invitationService;
    }

    @PreAuthorize("hasRole('HR_ADMIN')")
    @PostMapping
    public ResponseEntity<InvitationCreatedResponseDto> create(
            @Valid @RequestBody CreateInvitationRequestDto request,
            @AuthenticationPrincipal CustomUserDetails actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(invitationService.create(request, actor));
    }

    @PreAuthorize("hasRole('HR_ADMIN')")
    @PostMapping("/{id}/revoke")
    public ResponseEntity<Void> revoke(@PathVariable Long id) {
        invitationService.revoke(id);
        return ResponseEntity.noContent().build();
    }
}
