package com.example.employeetimetracking.util;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;

@Component
public class WorkingDaysCalculator {

    public BigDecimal calculate(LocalDate startDate, LocalDate endDate) {
        long days = 0;
        LocalDate current = startDate;

        while (!current.isAfter(endDate)) {
            DayOfWeek day = current.getDayOfWeek();
            if (day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY) {
                days++;
            }
            current = current.plusDays(1);
        }

        return BigDecimal.valueOf(days);
    }
}