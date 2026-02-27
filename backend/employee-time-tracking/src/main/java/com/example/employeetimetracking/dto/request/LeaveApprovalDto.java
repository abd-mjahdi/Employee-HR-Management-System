package com.example.employeetimetracking.dto.request;

import com.example.employeetimetracking.model.enums.Status;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class LeaveApprovalDto {
    @NotNull
    private Status status;

    private String notes;
}
