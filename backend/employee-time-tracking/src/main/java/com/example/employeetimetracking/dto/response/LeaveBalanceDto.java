package com.example.employeetimetracking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class LeaveBalanceDto {

    private Long id;
    private UserResponseDto user;
    private LeaveTypeDto leaveType;
    private short year;
    private BigDecimal currentBalance;


}
