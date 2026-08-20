package com.example.employeetimetracking.controller;

import com.example.employeetimetracking.dto.request.AcceptInvitationRequestDto;
import com.example.employeetimetracking.dto.response.InvitationAcceptedResponseDto;
import com.example.employeetimetracking.service.InvitationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/invitations")
public class InvitationAcceptController {

    private final InvitationService invitationService;

    public InvitationAcceptController(InvitationService invitationService) {
        this.invitationService = invitationService;
    }

    @PostMapping("/accept")
    public ResponseEntity<InvitationAcceptedResponseDto> accept(
            @Valid @RequestBody AcceptInvitationRequestDto request) {
        return ResponseEntity.ok(invitationService.accept(request));
    }
}
