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

    private String username;

    @Column(unique = true)
    @Email(message = "Email should be valid")
    private String email;

    private String firstName;

    private String lastName;

    private UserRole userRole;

    private Long departmentId;
}