package com.example.employeetimetracking.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CreateProjectDto {
    @NotBlank
    private String projectName;
    @NotBlank
    private String projectCode;
    private String description;
}
