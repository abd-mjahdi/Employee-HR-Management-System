package com.example.employeetimetracking.integration;

import com.example.employeetimetracking.dto.request.CreateUserRequestDto;
import com.example.employeetimetracking.dto.request.LoginRequestDto;
import com.example.employeetimetracking.dto.response.LoginResponseDto;
import com.example.employeetimetracking.integration.persistence.AbstractPostgresIT;
import com.example.employeetimetracking.model.enums.UserRole;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TenantAuthIT extends AbstractPostgresIT {

    @Test
    void loginOnAcmeHost_succeedsAndJwtIsBoundToAcme() throws Exception {
        String token = login(ACME_HOST, ACME_HR_EMAIL, SEED_PASSWORD);
        assertEquals(1L, jwtUtil.extractCompanyId(token));
        assertEquals(ACME_HR_EMAIL, jwtUtil.extractEmail(token));

        MvcResult users = mockMvc.perform(get("/users").param("size", "100")
                        .with(tenantHost(ACME_HOST))
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode content = objectMapper.readTree(users.getResponse().getContentAsString()).get("content");
        boolean sawAcmeHr = false;
        for (JsonNode node : content) {
            String email = node.get("email").asText();
            assertFalse(email.endsWith("@globex.com"), email);
            if (ACME_HR_EMAIL.equals(email)) {
                sawAcmeHr = true;
            }
        }
        assertTrue(sawAcmeHr);
    }

    @Test
    void loginOnGlobexHost_succeedsForGlobexHr() throws Exception {
        String token = login(GLOBEX_HOST, GLOBEX_HR_EMAIL, SEED_PASSWORD);
        assertEquals(2L, jwtUtil.extractCompanyId(token));
    }

    @Test
    void acmeUserOnGlobexHost_failsIdenticallyToBadPassword() throws Exception {
        MvcResult wrongTenant = mockMvc.perform(post("/auth/login")
                        .with(tenantHost(GLOBEX_HOST))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequestDto(ACME_HR_EMAIL, SEED_PASSWORD))))
                .andReturn();
        MvcResult badPassword = mockMvc.perform(post("/auth/login")
                        .with(tenantHost(GLOBEX_HOST))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequestDto(ACME_HR_EMAIL, "not-the-password"))))
                .andReturn();

        assertInvalidCredentials(wrongTenant);
        assertInvalidCredentials(badPassword);
        assertEquals(wrongTenant.getResponse().getContentAsString(),
                badPassword.getResponse().getContentAsString());
    }

    @Test
    void acmeJwtOnGlobexHost_isUnauthorized() throws Exception {
        String acmeToken = login(ACME_HOST, ACME_HR_EMAIL, SEED_PASSWORD);

        mockMvc.perform(get("/users")
                        .with(tenantHost(GLOBEX_HOST))
                        .header(HttpHeaders.AUTHORIZATION, bearer(acmeToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Unauthorized"));
    }

    @Test
    void globexResourceIdsOnAcmeHost_matchEnumerationSafeNotFound() throws Exception {
        String acmeToken = login(ACME_HOST, ACME_HR_EMAIL, SEED_PASSWORD);

        MvcResult globexDept = mockMvc.perform(get("/departments/9")
                        .with(tenantHost(ACME_HOST))
                        .header(HttpHeaders.AUTHORIZATION, bearer(acmeToken)))
                .andReturn();
        MvcResult missingDept = mockMvc.perform(get("/departments/99999")
                        .with(tenantHost(ACME_HOST))
                        .header(HttpHeaders.AUTHORIZATION, bearer(acmeToken)))
                .andReturn();

        assertEquals(404, globexDept.getResponse().getStatus());
        assertEquals(404, missingDept.getResponse().getStatus());
        assertEquals("Department does not exist", messageOf(globexDept));
        assertEquals("Department does not exist", messageOf(missingDept));

        MvcResult globexUser = mockMvc.perform(get("/users/32")
                        .with(tenantHost(ACME_HOST))
                        .header(HttpHeaders.AUTHORIZATION, bearer(acmeToken)))
                .andReturn();
        MvcResult missingUser = mockMvc.perform(get("/users/99999")
                        .with(tenantHost(ACME_HOST))
                        .header(HttpHeaders.AUTHORIZATION, bearer(acmeToken)))
                .andReturn();

        assertEquals(403, globexUser.getResponse().getStatus());
        assertEquals(403, missingUser.getResponse().getStatus());
        assertEquals("You cannot access this resource", messageOf(globexUser));
        assertEquals("You cannot access this resource", messageOf(missingUser));
    }

    @Test
    void hrCreateUser_rejectsForeignDepartmentId() throws Exception {
        String acmeToken = login(ACME_HOST, ACME_HR_EMAIL, SEED_PASSWORD);
        CreateUserRequestDto body = new CreateUserRequestDto(
                "foreign.dept",
                "foreign.dept@company.com",
                "Foreign",
                "Dept",
                UserRole.EMPLOYEE,
                9L,
                3L);

        mockMvc.perform(post("/users")
                        .with(tenantHost(ACME_HOST))
                        .header(HttpHeaders.AUTHORIZATION, bearer(acmeToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Department does not exist"));
    }

    private void assertInvalidCredentials(MvcResult result) throws Exception {
        assertEquals(401, result.getResponse().getStatus());
        LoginResponseDto body = objectMapper.readValue(
                result.getResponse().getContentAsString(), LoginResponseDto.class);
        assertFalse(body.isSuccess());
        assertEquals("invalid credentials", body.getMessage());
        assertNull(body.getToken());
    }

    private String messageOf(MvcResult result) throws Exception {
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("message").asText();
    }
}
