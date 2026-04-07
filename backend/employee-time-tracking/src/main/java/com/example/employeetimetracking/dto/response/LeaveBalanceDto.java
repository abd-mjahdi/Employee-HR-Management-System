package com.example.employeetimetracking.dto.response;

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
public class LeaveBalanceDto {

    private Long id;
    private Long userId;
    private Long leaveTypeId;
    private String leaveTypeName;
    private LocalDate lastAccrualDate;
    private short year;
    private BigDecimal currentBalance;


}
