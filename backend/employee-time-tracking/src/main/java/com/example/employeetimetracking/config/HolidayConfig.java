package com.example.employeetimetracking.config;

import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Setter
@Component
@ConfigurationProperties(prefix = "holidays")
public class HolidayConfig {

    private List<String> dates;

    public Set<LocalDate> getDates() {
        return dates.stream()
                .map(LocalDate::parse)
                .collect(Collectors.toSet());
    }
}
