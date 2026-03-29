package com.example.employeetimetracking.integration;

import com.example.employeetimetracking.model.entities.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    @Test
    void login_withWrongPassword_returns401() throws Exception {
        createHrAdmin();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    { "email": "admin1@test.com", "password": "wrongpassword" }
                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_withDeactivatedAccount_returns403() throws Exception {
        User user = createHrAdmin();
        user.setIsActive(false);
        userRepository.save(user);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    { "email": "admin1@test.com", "password": "password" }
                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void protectedEndpoint_withNoToken_returns401() throws Exception {
        mockMvc.perform(get("/users/me"))
                .andExpect(status().isUnauthorized());
    }
}
