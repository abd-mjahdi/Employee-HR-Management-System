package com.example.employeetimetracking.integration.persistence;

import com.example.employeetimetracking.dto.request.LoginRequestDto;
import com.example.employeetimetracking.dto.response.LoginResponseDto;
import com.example.employeetimetracking.security.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractPostgresIT {

    protected static final String ACME_HOST = "acme.localhost";
    protected static final String GLOBEX_HOST = "globex.localhost";
    protected static final String ACME_HR_EMAIL = "alice.morgan@company.com";
    protected static final String GLOBEX_HR_EMAIL = "emma.frost@globex.com";
    protected static final String SEED_PASSWORD = "password";
    protected static final String BOOTSTRAP_KEY = "it-bootstrap-key";

    /**
     * One container for the JVM. {@code @Container} would start/stop per test class and
     * leave a cached Spring context pointing at a dead JDBC URL.
     */
    protected static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("password");

    static {
        postgres.start();
    }

    @DynamicPropertySource
    static void injectDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected ObjectMapper objectMapper;
    @Autowired
    protected JwtUtil jwtUtil;

    /**
     * TenantResolver reads {@code request.getServerName()}, not the Host header.
     */
    protected static RequestPostProcessor tenantHost(String serverName) {
        return request -> {
            request.setServerName(serverName);
            request.addHeader("Host", serverName);
            return request;
        };
    }

    protected String login(String host, String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .with(tenantHost(host))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequestDto(email, password))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), LoginResponseDto.class)
                .getToken();
    }

    protected String bearer(String token) {
        return "Bearer " + token;
    }
}
