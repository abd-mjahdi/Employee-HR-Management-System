package com.example.employeetimetracking.integration;

import com.example.employeetimetracking.dto.request.CreateInvitationRequestDto;
import com.example.employeetimetracking.dto.response.InvitationCreatedResponseDto;
import com.example.employeetimetracking.integration.persistence.AbstractPostgresIT;
import com.example.employeetimetracking.model.enums.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InvitationTenantIT extends AbstractPostgresIT {

    private static final String INVITEE_EMAIL = "phase11.invitee@example.com";

    @Test
    void inviteOnAcme_cannotAcceptOnGlobex_andCannotEscalateRoleOrCompany() throws Exception {
        String hrToken = login(ACME_HOST, ACME_HR_EMAIL, SEED_PASSWORD);

        MvcResult created = mockMvc.perform(post("/invitations")
                        .with(tenantHost(ACME_HOST))
                        .header(HttpHeaders.AUTHORIZATION, bearer(hrToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateInvitationRequestDto(INVITEE_EMAIL, UserRole.EMPLOYEE, 1L, 3L))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("EMPLOYEE"))
                .andReturn();
        InvitationCreatedResponseDto invitation = objectMapper.readValue(
                created.getResponse().getContentAsString(), InvitationCreatedResponseDto.class);
        assertNotNull(invitation.getToken());

        String acceptPayload = """
                {
                  "token": "%s",
                  "password": "secret1",
                  "firstName": "Pat",
                  "lastName": "Lee",
                  "role": "HR_ADMIN",
                  "companyId": 2
                }
                """.formatted(invitation.getToken());

        mockMvc.perform(post("/auth/invitations/accept")
                        .with(tenantHost(GLOBEX_HOST))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(acceptPayload))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Invitation not found"));

        mockMvc.perform(post("/auth/invitations/accept")
                        .with(tenantHost(ACME_HOST))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(acceptPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(INVITEE_EMAIL))
                .andExpect(jsonPath("$.companySlug").value("acme"));

        String inviteeToken = login(ACME_HOST, INVITEE_EMAIL, "secret1");
        assertEquals(1L, jwtUtil.extractCompanyId(inviteeToken));
        assertEquals("EMPLOYEE", jwtUtil.extractRole(inviteeToken));

        mockMvc.perform(get("/users/me")
                        .with(tenantHost(ACME_HOST))
                        .header(HttpHeaders.AUTHORIZATION, bearer(inviteeToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(INVITEE_EMAIL))
                .andExpect(jsonPath("$.userRole").value("EMPLOYEE"))
                .andExpect(jsonPath("$.departmentId").value(1));
    }
}
