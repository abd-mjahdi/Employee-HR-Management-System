package com.example.employeetimetracking.dto.request;

import com.example.employeetimetracking.dto.response.DepartmentDto;
import com.example.employeetimetracking.model.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserRequestDto {
    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private UserRole userRole;
    private DepartmentDto department;
    private Boolean isActive;
}