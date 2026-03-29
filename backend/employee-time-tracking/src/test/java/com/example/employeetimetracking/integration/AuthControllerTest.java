package com.example.employeetimetracking.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class AuthControllerTest extends BaseIntegrationTest{
    @Test
    void login_withValidCredentials_returns200AndToken() throws Exception{
        createHrAdmin();
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                {
                    "email":"admin1@test.com",
                    "password":"password"
                }
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.role").value("HR_ADMIN"))
                .andExpect((jsonPath("$.email").value("admin1@test.com")));
    }
}
