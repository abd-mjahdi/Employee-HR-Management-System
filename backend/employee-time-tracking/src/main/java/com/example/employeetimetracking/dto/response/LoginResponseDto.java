package com.example.employeetimetracking.dto.response;

import com.example.employeetimetracking.model.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseDto {
    private boolean success;
    private String message;
    private String token;
    private String email;
    private UserRole role;
    private String companySlug;
    private String companyName;

    public LoginResponseDto(String token, String email, UserRole role, String companySlug, String companyName) {
        this.success = true;
        this.message = "Login successful";
        this.token = token;
        this.email = email;
        this.role = role;
        this.companySlug = companySlug;
        this.companyName = companyName;
    }

    public LoginResponseDto(String message) {
        this.success = false;
        this.message = message;
        this.token = null;
        this.email = null;
        this.role = null;
        this.companySlug = null;
        this.companyName = null;
    }
}
