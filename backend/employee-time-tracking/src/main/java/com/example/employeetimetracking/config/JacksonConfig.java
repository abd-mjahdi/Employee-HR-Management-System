package com.example.employeetimetracking.config;

import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer ignoreClientCompanyId() {
        return builder -> builder.mixIn(Object.class, IgnoreClientCompanyIdMixin.class);
    }
}
