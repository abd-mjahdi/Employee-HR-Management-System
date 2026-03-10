package com.example.employeetimetracking.dto.request;

import com.example.employeetimetracking.dto.response.DepartmentDto;
import com.example.employeetimetracking.model.enums.UserRole;
import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.validation.constraints.*;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserRequestDto {

    @NotBlank(message = "Username is required")
    private String username;

    @Column(unique = true)
    @Email(message = "Email should be valid")
    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    private UserRole userRole;

    @NotNull(message = "Department Id is required")
    private Long departmentId;

    private Boolean isActive;
}