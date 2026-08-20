package com.example.employeetimetracking.integration;

import com.example.employeetimetracking.config.BootstrapProperties;
import com.example.employeetimetracking.dto.request.BootstrapCompanyRequestDto;
import com.example.employeetimetracking.integration.persistence.AbstractPostgresIT;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BootstrapRegisterIT extends AbstractPostgresIT {

    @Test
    void secondBootstrap_isConflict() throws Exception {
        mockMvc.perform(post("/internal/bootstrap/company")
                        .header(BootstrapProperties.HEADER, BOOTSTRAP_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bootstrapBody())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Bootstrap already completed"));
    }

    @Test
    void missingBootstrapKey_isUnauthorized() throws Exception {
        mockMvc.perform(post("/internal/bootstrap/company")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bootstrapBody())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Unauthorized"));
    }

    @Test
    void wrongBootstrapKey_isUnauthorized() throws Exception {
        mockMvc.perform(post("/internal/bootstrap/company")
                        .header(BootstrapProperties.HEADER, "wrong-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bootstrapBody())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Unauthorized"));
    }

    @Test
    void authRegister_isDenied() throws Exception {
        int status = mockMvc.perform(post("/auth/register")
                        .with(tenantHost(ACME_HOST))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"new@company.com","password":"password","firstName":"New","lastName":"User"}
                                """))
                .andReturn()
                .getResponse()
                .getStatus();
        assertTrue(status == 401 || status == 403,
                "expected denyAll 401/403 for /auth/register but was " + status);
    }

    private static BootstrapCompanyRequestDto bootstrapBody() {
        BootstrapCompanyRequestDto request = new BootstrapCompanyRequestDto();
        request.setCompanyName("Initech");
        request.setSlug("initech");
        request.setAdminEmail("bill@initech.com");
        request.setAdminFirstName("Bill");
        request.setAdminLastName("Lumbergh");
        request.setAdminPassword("password");
        return request;
    }
}
