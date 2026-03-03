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
    private boolean success;
    private String message;
    private String token;
    private String email;
    private UserRole role;

    //constructor for success
    public LoginResponseDto(String token, String email, UserRole role) {
        this.success = true;
        this.message = "Login successful";
        this.token = token;
        this.email = email;
        this.role = role;
    }

    //constructor for failure
    public LoginResponseDto(String message, String email) {
        this.success = false;
        this.message = message;
        this.token = null;
        this.email = email;
        this.role = null;
    }
}
