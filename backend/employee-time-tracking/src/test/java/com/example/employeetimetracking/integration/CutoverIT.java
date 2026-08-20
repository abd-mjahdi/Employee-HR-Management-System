package com.example.employeetimetracking.integration;

import com.example.employeetimetracking.config.BootstrapProperties;
import com.example.employeetimetracking.dto.request.BootstrapCompanyRequestDto;
import com.example.employeetimetracking.integration.persistence.AbstractPostgresIT;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 13 cutover checklist (tasks 77–83).
 */
class CutoverIT extends AbstractPostgresIT {

    @Test
    void registerAndAnonymousUserCreate_areDenied() throws Exception {
        int registerStatus = mockMvc.perform(post("/auth/register")
                        .with(tenantHost(ACME_HOST))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"anon@company.com","password":"password","firstName":"A","lastName":"N"}
                                """))
                .andReturn()
                .getResponse()
                .getStatus();
        assertTrue(registerStatus == 401 || registerStatus == 403, "register was " + registerStatus);

        mockMvc.perform(post("/users")
                        .with(tenantHost(ACME_HOST))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"anon","email":"anon@company.com","firstName":"A","lastName":"N",
                                 "userRole":"EMPLOYEE","departmentId":1}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginJwtIsBoundToHostCompany_andForeignTokenIsRejected() throws Exception {
        String token = login(ACME_HOST, ACME_HR_EMAIL, SEED_PASSWORD);
        assertEquals(1L, jwtUtil.extractCompanyId(token));

        mockMvc.perform(get("/users")
                        .with(tenantHost(GLOBEX_HOST))
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Unauthorized"));
    }

    @Test
    void forgedCompanyIdDoesNotGrantAccess() throws Exception {
        String token = login(ACME_HOST, ACME_HR_EMAIL, SEED_PASSWORD);

        mockMvc.perform(get("/departments/9")
                        .with(tenantHost(ACME_HOST))
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Department does not exist"));

        mockMvc.perform(post("/users")
                        .with(tenantHost(ACME_HOST))
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"forged","email":"forged@company.com","firstName":"F","lastName":"G",
                                 "userRole":"EMPLOYEE","departmentId":9,"companyId":2}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Department does not exist"));
    }

    @Test
    void noEmployeeSelfServeCompanyCreate_andSecondBootstrapRejected() throws Exception {
        mockMvc.perform(post("/companies")
                        .with(tenantHost(ACME_HOST))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Initech","slug":"initech"}
                                """))
                .andExpect(status().isUnauthorized());

        String token = login(ACME_HOST, ACME_HR_EMAIL, SEED_PASSWORD);
        mockMvc.perform(post("/companies")
                        .with(tenantHost(ACME_HOST))
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Initech","slug":"initech"}
                                """))
                .andExpect(status().isNotFound());

        BootstrapCompanyRequestDto body = new BootstrapCompanyRequestDto();
        body.setCompanyName("Initech");
        body.setSlug("initech");
        body.setAdminEmail("bill@initech.com");
        body.setAdminFirstName("Bill");
        body.setAdminLastName("Lumbergh");
        body.setAdminPassword("password");

        mockMvc.perform(post("/internal/bootstrap/company")
                        .header(BootstrapProperties.HEADER, BOOTSTRAP_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Bootstrap already completed"));
    }

    @Test
    void acmeHrSeesAcmeUsersOnly() throws Exception {
        String token = login(ACME_HOST, ACME_HR_EMAIL, SEED_PASSWORD);
        MvcResult users = mockMvc.perform(get("/users").param("size", "100")
                        .with(tenantHost(ACME_HOST))
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andReturn();
        String body = users.getResponse().getContentAsString();
        assertTrue(body.contains(ACME_HR_EMAIL));
        assertFalse(body.contains("@globex.com"));
        assertFalse(body.contains(GLOBEX_HR_EMAIL));
    }
}
