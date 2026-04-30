package com.example.employeetimetracking.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateTimeEntryBreakDto {
    @NotNull
    private LocalTime breakStart;
    @NotNull
    private LocalTime breakEnd;
    // default true if null
    private Boolean isUnpaid;
}

