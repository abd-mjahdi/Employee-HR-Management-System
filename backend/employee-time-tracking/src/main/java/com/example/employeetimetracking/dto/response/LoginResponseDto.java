package com.example.employeetimetracking.dto.response;

import com.example.employeetimetracking.model.enums.UserRole;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseDto {
    @NotBlank
    private String token;

    @NotBlank
    private String username;

    @NotBlank
    private UserRole role;
}
