package com.example.employeetimetracking.integration;

import com.example.employeetimetracking.integration.persistence.AbstractPostgresIT;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReportTenantIT extends AbstractPostgresIT {

    @Test
    void payrollOnAcmeHost_returnsOnlyAcmeEmployees() throws Exception {
        String token = login(ACME_HOST, ACME_HR_EMAIL, SEED_PASSWORD);

        MvcResult result = mockMvc.perform(get("/reports/payroll")
                        .param("startDate", "2026-04-13")
                        .param("endDate", "2026-04-19")
                        .with(tenantHost(ACME_HOST))
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode employees = objectMapper.readTree(result.getResponse().getContentAsString()).get("employees");
        assertTrue(employees.isArray() && employees.size() > 0);

        boolean sawAcmeHr = false;
        for (JsonNode employee : employees) {
            long id = employee.get("employeeId").asLong();
            assertFalse(id >= 32 && id <= 35, "globex employee leaked: " + id);
            String name = employee.get("name").asText("");
            assertFalse(name.contains("Frost"), name);
            assertFalse(name.contains("Grant"), name);
            assertFalse(name.contains("Sharma"), name);
            assertFalse(name.contains("Brooks"), name);
            if (id == 1L) {
                sawAcmeHr = true;
            }
        }
        assertTrue(sawAcmeHr);
    }

    @Test
    void acmeJwtOnGlobexHost_payrollIsUnauthorized() throws Exception {
        String acmeToken = login(ACME_HOST, ACME_HR_EMAIL, SEED_PASSWORD);

        mockMvc.perform(get("/reports/payroll")
                        .param("startDate", "2026-04-13")
                        .param("endDate", "2026-04-19")
                        .with(tenantHost(GLOBEX_HOST))
                        .header(HttpHeaders.AUTHORIZATION, bearer(acmeToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Unauthorized"));
    }

    @Test
    void manager_cannotAccessHrOnlyReports() throws Exception {
        String managerToken = login(ACME_HOST, "carol.knight@company.com", SEED_PASSWORD);

        mockMvc.perform(get("/reports/department-utilization")
                        .param("startDate", "2026-04-13")
                        .param("endDate", "2026-04-19")
                        .with(tenantHost(ACME_HOST))
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/reports/payroll")
                        .param("startDate", "2026-04-13")
                        .param("endDate", "2026-04-19")
                        .with(tenantHost(ACME_HOST))
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken)))
                .andExpect(status().isForbidden());
    }
}
