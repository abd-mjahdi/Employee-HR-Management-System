package com.example.employeetimetracking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AcceptInvitationRequestDto {
    @NotBlank
    private String token;

    @Size(min = 6, max = 72)
    private String password;

    @Size(max = 50)
    private String firstName;

    @Size(max = 50)
    private String lastName;

    public void setPassword(String password) {
        this.password = (password == null || password.isBlank()) ? null : password;
    }
}
