package com.example.employeetimetracking.dto.response;

import com.example.employeetimetracking.model.entities.User;
import com.example.employeetimetracking.model.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class LeaveRequestDto {
    private Long id;
    private UserDto user;
    private LeaveTypeDto leaveType;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal totalDays;
    private String reason;
    private Status status;
}
