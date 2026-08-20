package com.example.employeetimetracking.dto.request;

import com.example.employeetimetracking.model.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateInvitationRequestDto {
    @NotBlank
    @Email
    private String email;

    @NotNull
    private UserRole role;

    @NotNull
    private Long departmentId;

    private Long managerMembershipId;
}
