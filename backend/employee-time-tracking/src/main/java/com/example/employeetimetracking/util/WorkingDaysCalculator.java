package com.example.employeetimetracking.util;

import com.example.employeetimetracking.config.HolidayConfig;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

@Component
public class WorkingDaysCalculator {

    private final HolidayConfig holidayConfig;
    @Autowired
    public WorkingDaysCalculator(HolidayConfig holidayConfig) {
        this.holidayConfig = holidayConfig;
    }

    public BigDecimal calculate(LocalDate startDate, LocalDate endDate) {
        Set<LocalDate> holidays = holidayConfig.getDates();
        long days = 0;
        LocalDate current = startDate;

        while (!current.isAfter(endDate)) {
            DayOfWeek day = current.getDayOfWeek();
            boolean isWeekend = day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
            boolean isHoliday = holidays.contains(current);

            if (!isWeekend && !isHoliday) {
                days++;
            }
            current = current.plusDays(1);
        }

        return BigDecimal.valueOf(days);
    }
}