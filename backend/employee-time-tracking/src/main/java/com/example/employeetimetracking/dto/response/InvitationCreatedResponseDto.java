package com.example.employeetimetracking.dto.response;

import com.example.employeetimetracking.model.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InvitationCreatedResponseDto {
    private Long id;
    private String email;
    private UserRole role;
    private LocalDateTime expiresAt;
    private String token;
}
