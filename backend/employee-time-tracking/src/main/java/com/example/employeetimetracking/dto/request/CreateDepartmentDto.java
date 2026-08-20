package com.example.employeetimetracking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CreateDepartmentDto {
    @NotBlank
    @Size(max = 50)
    private String departmentName;

    @NotBlank
    @Size(max = 50)
    private String departmentCode;
}
