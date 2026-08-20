package com.example.employeetimetracking.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDepartmentDto {
    @Size(max = 50)
    private String departmentName;

    @Size(max = 50)
    private String departmentCode;

    private Boolean isActive;
}
