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
public class RegisterResponseDto {

    private boolean success;
    private String message;
    private String email;
    private UserRole role;

    //constructor for success
    public RegisterResponseDto(String email, UserRole role) {
        this.success = true;
        this.message = "Registration successful";
        this.email = email;
        this.role = role;
    }

    //constructor for failure
    public RegisterResponseDto(String message) {
        this.success = false;
        this.message = message;
        this.email = null;
        this.role = null;
    }
}