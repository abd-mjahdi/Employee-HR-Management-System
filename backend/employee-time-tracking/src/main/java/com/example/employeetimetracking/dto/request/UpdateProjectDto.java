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
public class UpdateProjectDto {
    @Size(max = 50)
    private String projectName;

    @Size(max = 50)
    private String projectCode;

    private String description;

    private Boolean isActive;
}
